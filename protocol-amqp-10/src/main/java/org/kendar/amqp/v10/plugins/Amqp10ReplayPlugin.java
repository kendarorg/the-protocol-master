package org.kendar.amqp.v10.plugins;

import org.kendar.amqp.v10.messages.RawFrame;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.BasicReplayPlugin;
import org.kendar.plugins.settings.BasicAysncReplayPluginSettings;
import org.kendar.protocol.context.NetworkProtoContext;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Broker-less replay of recorded AMQP 1.0 sessions. Frames were recorded as raw
 * bytes ({@link RawFrame}); on each client frame the base plugin matches the next
 * recorded interaction (by caller/type + recorded order) and this class writes the
 * recorded response frames back to the client verbatim — no broker involved.
 */
@Extension
@TpmService(tags = "amqp10")
public class Amqp10ReplayPlugin extends BasicReplayPlugin<BasicAysncReplayPluginSettings> {
    private static final Logger log = LoggerFactory.getLogger(Amqp10ReplayPlugin.class);

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

    @Override
    protected void sendBackResponses(ProtoContext context, List<StorageItem> items) {
        var ctx = (NetworkProtoContext) context;
        for (var item : items) {
            if (item.getOutput() == null) {
                continue;
            }
            var frame = toRawFrame(item.getOutput());
            if (frame != null) {
                log.debug("[REPLAY] writing recorded frame #{} ({} bytes)", item.getIndex(),
                        frame.getRaw() == null ? 0 : frame.getRaw().length);
                ctx.write(frame);
            }
        }
    }

    /** Reconstructs a verbatim frame from a recorded RawFrame JSON (avoids Jackson ctor issues). */
    private RawFrame toRawFrame(Object output) {
        try {
            var node = mapper.toJsonNode(output);
            if (node == null || node.get("raw") == null) {
                return null;
            }
            var bytes = Base64.getDecoder().decode(node.get("raw").asText());
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
}
