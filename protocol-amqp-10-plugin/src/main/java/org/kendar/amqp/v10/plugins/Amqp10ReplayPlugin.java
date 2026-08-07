package org.kendar.amqp.v10.plugins;

import org.kendar.amqp.v10.codec.Amqp10FrameEncoder;
import org.kendar.amqp.v10.codec.Amqp10TypeReader;
import org.kendar.amqp.v10.codec.DescribedType;
import org.kendar.amqp.v10.dtos.Performatives;
import org.kendar.amqp.v10.messages.RawFrame;
import org.kendar.buffers.BBuffer;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.PluginContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Broker-less replay of recorded AMQP 1.0 sessions. Frames were recorded as raw
 * bytes ({@link RawFrame}); on each client frame the base plugin matches the next
 * recorded interaction (by caller/type + recorded order) and this class writes the
 * recorded response frames back to the client — no broker involved.
 * <p>
 * Connection open (SASL + open) and session open (begin) replay verbatim. Link
 * setup ({@code attach}) needs per-client rewriting: the recorded broker attach
 * echoes the <i>recorded</i> client's link name, so we decode the current client's
 * attach (name = field 0) with the M2 codec and re-encode the recorded attach
 * response to carry that name — otherwise qpid rejects the link.
 */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10ReplayPlugin extends BasicReplayPlugin<BasicAysncReplayPluginSettings> {
    private static final Logger log = LoggerFactory.getLogger(Amqp10ReplayPlugin.class);
    private static final String CLIENT_ATTACH_NAMES = "CLIENT_ATTACH_NAMES";

    public Amqp10ReplayPlugin(JsonMapper mapper, StorageRepository storage) {
        super(mapper, storage);
    }

    @Override
    public Class<?> getSettingClass() {
        return BasicAysncReplayPluginSettings.class;
    }

    @Override
    public String getProtocol() {
        return "amqp10";
    }

    @Override
    protected boolean hasCallbacks() {
        return true;
    }

    @Override
    protected Map<String, String> buildTag(Object in) {
        // No semantic tags yet: matching is by caller/type + recorded order.
        return new HashMap<>();
    }

    /**
     * The base plugin pushes a recorded response only if its tags overlap the
     * connection's context tags. Recorded responses are tagged {@code output=RawFrame}
     * (see {@link Amqp10RecordPlugin#buildTag}); returning the same key here makes
     * every recorded frame correlate to the (single) replay connection.
     */
    @Override
    protected Map<String, String> getContextTags(ProtoContext context) {
        var tags = new HashMap<String, String>();
        tags.put("output", "RawFrame");
        return tags;
    }

    /** Capture the client's attach link name(s) so the recorded response can adopt them. */
    @Override
    protected boolean sendAndForget(PluginContext pluginContext, Object in) {
        if (in instanceof RawFrame) {
            var name = attachName(((RawFrame) in).getRaw());
            if (name != null) {
                clientAttachNames((NetworkProtoContext) pluginContext.getContext()).add(name);
                log.debug("[REPLAY] captured client attach link name '{}'", name);
            }
        }
        return super.sendAndForget(pluginContext, in);
    }

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> items) {
        var ctx = (NetworkProtoContext) context;
        for (var item : items) {
            if (item.getOutput() == null) {
                continue;
            }
            var frame = toRawFrame(item.getOutput());
            if (frame == null) {
                continue;
            }
            var bytes = frame.getRaw();
            if (bytes != null && descriptorOf(bytes) == Performatives.ATTACH) {
                var name = clientAttachNames(ctx).poll();
                if (name != null) {
                    var rewritten = rewriteAttachName(bytes, name);
                    if (rewritten != null) {
                        frame.setRaw(rewritten);
                        log.debug("[REPLAY] rewrote recorded attach #{} with link name '{}'", item.getIndex(), name);
                    }
                }
            }
            log.debug("[REPLAY] writing recorded frame #{} ({} bytes)", item.getIndex(),
                    frame.getRaw() == null ? 0 : frame.getRaw().length);
            ctx.write(frame);
        }
    }

    @SuppressWarnings("unchecked")
    private static Deque<String> clientAttachNames(NetworkProtoContext ctx) {
        var q = (Deque<String>) ctx.getValue(CLIENT_ATTACH_NAMES);
        if (q == null) {
            q = new ArrayDeque<>();
            ctx.setValue(CLIENT_ATTACH_NAMES, q);
        }
        return q;
    }

    /**
     * Reconstructs a verbatim frame from a recorded RawFrame JSON (avoids Jackson
     * ctor issues). New recordings carry the readable {@code decoded} tree as the
     * primary representation; {@code raw} is only present as a fallback (or in
     * pre-M5 scenarios) and wins when both exist.
     */
    private RawFrame toRawFrame(Object output) {
        try {
            var node = mapper.toJsonNode(output);
            if (node == null) {
                return null;
            }
            byte[] bytes;
            if (node.get("raw") != null && !node.get("raw").isNull()) {
                bytes = Base64.getDecoder().decode(node.get("raw").asText());
            } else if (node.get("decoded") != null && !node.get("decoded").isNull()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> decoded = mapper.deserialize(node.get("decoded"), Map.class);
                bytes = Amqp10FrameEncoder.encode(decoded);
            } else {
                return null;
            }
            byte frameType = node.get("frameType") != null ? (byte) node.get("frameType").asInt() : 0;
            var rf = new RawFrame(-1, frameType);
            if (node.get("channel") != null) {
                rf.setChannel((short) node.get("channel").asInt());
            }
            rf.setRaw(bytes);
            return rf;
        } catch (Exception e) {
            log.error("[REPLAY] cannot rebuild recorded frame", e);
            return null;
        }
    }

    // --- codec helpers (frame body is a described performative over a field list) ---

    private static int bodyStart(byte[] frame) {
        return (frame[4] & 0xFF) * 4; // DOFF in 4-byte words
    }

    private static DescribedType performative(byte[] frame) {
        if (frame == null || frame.length < 8) {
            return null;
        }
        // protocol header (AMQP...) or empty/heartbeat frame — not a performative
        if (frame[0] == 'A' && frame[1] == 'M' && frame[2] == 'Q' && frame[3] == 'P') {
            return null;
        }
        var bs = bodyStart(frame);
        if (bs < 8 || bs >= frame.length) {
            return null;
        }
        try {
            var bb = BBuffer.of(frame);
            bb.setPosition(bs);
            var obj = new Amqp10TypeReader().readAny(bb);
            return (obj instanceof DescribedType) ? (DescribedType) obj : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static long descriptorOf(byte[] frame) {
        var dt = performative(frame);
        return dt == null ? -1 : dt.descriptorCode();
    }

    private static String attachName(byte[] frame) {
        var dt = performative(frame);
        if (dt != null && dt.descriptorCode() == Performatives.ATTACH && dt.getValue() instanceof List) {
            var list = (List<?>) dt.getValue();
            if (!list.isEmpty() && list.get(0) instanceof String) {
                return (String) list.get(0);
            }
        }
        return null;
    }

    /**
     * Surgically replaces the attach {@code name} (field 0, a str8/str32) in the raw
     * frame bytes, adjusting only the enclosing list-size and frame-size fields. Avoids
     * a full codec re-encode (whose list/array handling isn't byte-faithful for attach).
     */
    private static byte[] rewriteAttachName(byte[] frame, String newName) {
        try {
            int p = bodyStart(frame);
            if ((frame[p] & 0xFF) != 0x00) {
                return null; // not a described type
            }
            p++;
            int dc = frame[p] & 0xFF;   // descriptor constructor
            p++;
            if (dc == 0x53) {
                p += 1;
            } else if (dc == 0x80) {
                p += 8;
            } else if (dc != 0x44) {
                return null;
            }
            int listCode = frame[p] & 0xFF;
            int listSizePos;
            int listSizeWidth;
            int elemsStart;
            if (listCode == 0xC0) {
                listSizePos = p + 1;
                listSizeWidth = 1;
                elemsStart = p + 3; // code + size(1) + count(1)
            } else if (listCode == 0xD0) {
                listSizePos = p + 1;
                listSizeWidth = 4;
                elemsStart = p + 9; // code + size(4) + count(4)
            } else {
                return null; // list0 or unexpected — no name to replace
            }
            int nameStart = elemsStart;
            int nc = frame[nameStart] & 0xFF;
            int nameEnd;
            if (nc == 0xA1) {
                nameEnd = nameStart + 2 + (frame[nameStart + 1] & 0xFF);
            } else if (nc == 0xB1) {
                nameEnd = nameStart + 5 + beInt(frame, nameStart + 1);
            } else if (nc == 0x40) {
                nameEnd = nameStart + 1; // null name — replace anyway
            } else {
                return null;
            }

            var nb = newName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] enc = nb.length <= 0xFF
                    ? concat(new byte[]{(byte) 0xA1, (byte) nb.length}, nb)
                    : concat(concat(new byte[]{(byte) 0xB1}, beBytes(nb.length)), nb);
            int delta = enc.length - (nameEnd - nameStart);

            var out = new byte[frame.length + delta];
            System.arraycopy(frame, 0, out, 0, nameStart);
            System.arraycopy(enc, 0, out, nameStart, enc.length);
            System.arraycopy(frame, nameEnd, out, nameStart + enc.length, frame.length - nameEnd);

            // adjust the list size field (bytes following the size field)
            if (listSizeWidth == 1) {
                out[listSizePos] = (byte) ((out[listSizePos] & 0xFF) + delta);
            } else {
                putBeInt(out, listSizePos, beInt(out, listSizePos) + delta);
            }
            // adjust the frame size (first 4 bytes, includes itself)
            putBeInt(out, 0, beInt(out, 0) + delta);
            return out;
        } catch (Exception e) {
            log.error("[REPLAY] cannot rewrite attach name", e);
            return null;
        }
    }

    private static int beInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    private static void putBeInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 24);
        b[off + 1] = (byte) (v >> 16);
        b[off + 2] = (byte) (v >> 8);
        b[off + 3] = (byte) v;
    }

    private static byte[] beBytes(int v) {
        return new byte[]{(byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v};
    }

    private static byte[] concat(byte[] a, byte[] b) {
        var r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
