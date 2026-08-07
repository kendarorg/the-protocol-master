package org.kendar.kafka.utils;

import org.kendar.kafka.enums.KafkaApiKeys;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Best-effort readable view of a recorded frame, stored under {@code decoded}
 * next to the raw bytes so recordings are browsable/verifiable (the analog of
 * amqp-10's decoded JSON — but here the raw bytes stay the replay source of
 * truth, so a decode gap can never corrupt replay). Every frame gets its header
 * (api, version, correlationId, clientId); Produce requests and Fetch responses
 * additionally get topics/partitions/records with clear-text keys and values.
 * Any parse problem degrades to the header info plus a note, never an error.
 */
public final class KafkaFrameDescriber {

    private KafkaFrameDescriber() {
    }

    /** Describes a client request frame (size + header v1/v2 + body). */
    public static Map<String, Object> describeRequest(byte[] frame) {
        var out = new LinkedHashMap<String, Object>();
        try {
            var in = new KafkaBBuffer(frame);
            in.getInt();                                   // size
            short apiKey = in.getShort();
            short apiVersion = in.getShort();
            boolean flexible = KafkaApiKeys.isFlexible(apiKey, apiVersion);
            out.put("api", KafkaApiKeys.nameOf(apiKey));
            out.put("apiVersion", (int) apiVersion);
            out.put("correlationId", in.getInt());
            out.put("clientId", in.readString());          // classic string even in flexible headers
            if (flexible) {
                in.readTaggedFieldsRaw();                  // header tagged fields (v2)
            }
            if (apiKey == KafkaApiKeys.PRODUCE) {
                describeProduceBody(in, apiVersion, flexible, out);
            }
        } catch (RuntimeException ex) {
            out.put("note", "partial decode: " + ex.getMessage());
        }
        return out;
    }

    /** Describes a broker response frame; api key/version come from the paired request. */
    public static Map<String, Object> describeResponse(byte[] frame, short apiKey, short apiVersion) {
        var out = new LinkedHashMap<String, Object>();
        try {
            var in = new KafkaBBuffer(frame);
            in.getInt();                                   // size
            out.put("api", KafkaApiKeys.nameOf(apiKey));
            out.put("apiVersion", (int) apiVersion);
            out.put("correlationId", in.getInt());
            boolean flexible = KafkaApiKeys.isFlexible(apiKey, apiVersion);
            // ApiVersions responses keep header v0 even when flexible (KIP-511).
            if (flexible && apiKey != KafkaApiKeys.API_VERSIONS) {
                in.readTaggedFieldsRaw();                  // header tagged fields (v1)
            }
            if (apiKey == KafkaApiKeys.FETCH) {
                describeFetchBody(in, apiVersion, flexible, out);
            }
        } catch (RuntimeException ex) {
            out.put("note", "partial decode: " + ex.getMessage());
        }
        return out;
    }

    // ---- Produce request body ----

    private static void describeProduceBody(KafkaBBuffer in, short version, boolean flexible,
                                            Map<String, Object> out) {
        if (version >= 3) {
            out.put("transactionalId", in.readString(flexible));
        }
        out.put("acks", (int) in.getShort());
        out.put("timeoutMs", in.getInt());
        var topics = new ArrayList<Map<String, Object>>();
        int topicCount = in.readArrayCount(flexible);
        for (int t = 0; t < topicCount; t++) {
            var topic = new LinkedHashMap<String, Object>();
            topic.put("topic", in.readString(flexible));
            var partitions = new ArrayList<Map<String, Object>>();
            int partCount = in.readArrayCount(flexible);
            for (int p = 0; p < partCount; p++) {
                var part = new LinkedHashMap<String, Object>();
                part.put("partition", in.getInt());
                part.put("records", describeBatches(readRecordsBlob(in, flexible)));
                if (flexible) {
                    in.readTaggedFieldsRaw();
                }
                partitions.add(part);
            }
            topic.put("partitions", partitions);
            if (flexible) {
                in.readTaggedFieldsRaw();
            }
            topics.add(topic);
        }
        out.put("topics", topics);
    }

    // ---- Fetch response body (v4+) ----

    private static void describeFetchBody(KafkaBBuffer in, short version, boolean flexible,
                                          Map<String, Object> out) {
        if (version >= 1) {
            out.put("throttleMs", in.getInt());
        }
        if (version >= 7) {
            out.put("errorCode", (int) in.getShort());
            out.put("sessionId", in.getInt());
        }
        var topics = new ArrayList<Map<String, Object>>();
        int topicCount = in.readArrayCount(flexible);
        for (int t = 0; t < topicCount; t++) {
            var topic = new LinkedHashMap<String, Object>();
            if (version >= 13) {
                topic.put("topicId", in.readUuid().toString());
            } else {
                topic.put("topic", in.readString(flexible));
            }
            var partitions = new ArrayList<Map<String, Object>>();
            int partCount = in.readArrayCount(flexible);
            for (int p = 0; p < partCount; p++) {
                var part = new LinkedHashMap<String, Object>();
                part.put("partition", in.getInt());
                part.put("errorCode", (int) in.getShort());
                part.put("highWatermark", in.getLong());
                if (version >= 4) {
                    part.put("lastStableOffset", in.getLong());
                }
                if (version >= 5) {
                    part.put("logStartOffset", in.getLong());
                }
                if (version >= 4) {
                    int aborted = in.readArrayCount(flexible);
                    for (int a = 0; a < aborted; a++) {
                        in.getLong();                      // producerId
                        in.getLong();                      // firstOffset
                        if (flexible) {
                            in.readTaggedFieldsRaw();
                        }
                    }
                }
                if (version >= 11) {
                    part.put("preferredReadReplica", in.getInt());
                }
                part.put("records", describeBatches(readRecordsBlob(in, flexible)));
                if (flexible) {
                    in.readTaggedFieldsRaw();
                }
                partitions.add(part);
            }
            topic.put("partitions", partitions);
            if (flexible) {
                in.readTaggedFieldsRaw();
            }
            topics.add(topic);
        }
        out.put("topics", topics);
    }

    // ---- RecordBatch v2 ----

    private static byte[] readRecordsBlob(KafkaBBuffer in, boolean flexible) {
        if (flexible) {
            return in.readCompactBytes();
        }
        int len = in.getInt();
        return len < 0 ? null : in.getBytes(len);
    }

    /** One entry per record; concatenated batches are walked in sequence. */
    private static List<Map<String, Object>> describeBatches(byte[] blob) {
        var records = new ArrayList<Map<String, Object>>();
        if (blob == null) {
            return records;
        }
        var in = new KafkaBBuffer(blob);
        while (blob.length - in.getPosition() >= 61) {
            long baseOffset = in.getLong();
            int batchLength = in.getInt();
            int batchEnd = in.getPosition() + batchLength;
            in.getInt();                                   // partitionLeaderEpoch
            byte magic = in.get();
            in.getInt();                                   // crc
            short attributes = in.getShort();
            in.getInt();                                   // lastOffsetDelta
            long baseTimestamp = in.getLong();
            in.getLong();                                  // maxTimestamp
            in.getLong();                                  // producerId
            in.getShort();                                 // producerEpoch
            in.getInt();                                   // baseSequence
            int count = in.getInt();
            int compression = attributes & 0x07;
            if (magic != 2 || compression != 0) {
                var rec = new LinkedHashMap<String, Object>();
                rec.put("baseOffset", baseOffset);
                rec.put("note", magic != 2 ? "magic v" + magic + " batch not decoded"
                        : "compressed batch (codec " + compression + ") not decoded");
                records.add(rec);
            } else {
                for (int r = 0; r < count; r++) {
                    int length = in.readVarint();
                    int recEnd = in.getPosition() + length;
                    in.get();                              // attributes
                    long tsDelta = in.readVarlong();
                    int offDelta = in.readVarint();
                    var rec = new LinkedHashMap<String, Object>();
                    rec.put("offset", baseOffset + offDelta);
                    rec.put("timestamp", baseTimestamp + tsDelta);
                    putBytes(rec, "key", readVarintBytes(in));
                    putBytes(rec, "value", readVarintBytes(in));
                    records.add(rec);
                    in.setPosition(recEnd);                // skip headers
                }
            }
            in.setPosition(batchEnd);
        }
        return records;
    }

    private static byte[] readVarintBytes(KafkaBBuffer in) {
        int len = in.readVarint();
        return len < 0 ? null : in.getBytes(len);
    }

    /** Clear text when printable UTF-8, else base-64 under {@code <name>B64}. */
    private static void putBytes(Map<String, Object> rec, String name, byte[] bytes) {
        if (bytes == null) {
            rec.put(name, null);
            return;
        }
        var text = new String(bytes, StandardCharsets.UTF_8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 0xFFFD || (c < 0x20 && c != '\t' && c != '\n' && c != '\r')) {
                rec.put(name + "B64", Base64.getEncoder().encodeToString(bytes));
                return;
            }
        }
        rec.put(name, text);
    }
}
