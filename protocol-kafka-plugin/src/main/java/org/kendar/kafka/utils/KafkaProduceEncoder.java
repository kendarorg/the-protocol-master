package org.kendar.kafka.utils;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

/**
 * Encodes a single-record Produce (0) request, version 9 (flexible), carrying one
 * RecordBatch v2 with a computed CRC32C. This is the only place the module ever
 * <b>encodes</b> a record batch (protocol-kafka.md §7); everywhere else record
 * batches are opaque blobs. No compression, no transactional/idempotent fields
 * (producerId/epoch = -1).
 */
public final class KafkaProduceEncoder {
    private static final short PRODUCE_API_KEY = 0;
    private static final short PRODUCE_VERSION = 9;

    private KafkaProduceEncoder() {
    }

    /**
     * @param acks 1 = leader ack (broker will respond), 0 = no response, -1 = all
     */
    public static byte[] encode(int correlationId, String clientId, String topic, int partition,
                                byte[] key, byte[] value, short acks, int timeoutMs, long timestamp) {
        var batch = recordBatch(key, value, timestamp);

        var body = new KafkaBBuffer();
        body.writeCompactString(null);                 // transactional_id (null)
        body.writeShort(acks);
        body.writeInt(timeoutMs);
        body.writeUnsignedVarint(2);                   // topic_data: 1 element (compact = count+1)
        body.writeCompactString(topic);
        body.writeUnsignedVarint(2);                   // partitions: 1 element
        body.writeInt(partition);
        body.writeCompactBytes(batch);                 // records
        body.writeUnsignedVarint(0);                   // partition tagged fields
        body.writeUnsignedVarint(0);                   // topic tagged fields
        body.writeUnsignedVarint(0);                   // body tagged fields

        var header = new KafkaBBuffer();
        header.writeShort(PRODUCE_API_KEY);
        header.writeShort(PRODUCE_VERSION);
        header.writeInt(correlationId);
        header.writeString(clientId);                  // client_id is a classic string even in flexible headers
        header.writeUnsignedVarint(0);                 // header tagged fields (v2)

        var frame = new KafkaBBuffer();
        frame.write(header.getAll());
        frame.write(body.getAll());
        var frameBytes = frame.getAll();

        var out = new KafkaBBuffer();
        out.writeInt(frameBytes.length);
        out.write(frameBytes);
        return out.getAll();
    }

    /** Builds a RecordBatch v2 containing one record. */
    private static byte[] recordBatch(byte[] key, byte[] value, long timestamp) {
        // --- inner record ---
        var recordBody = new KafkaBBuffer();
        recordBody.write((byte) 0);                    // attributes
        recordBody.writeVarlong(0);                    // timestampDelta
        recordBody.writeVarint(0);                     // offsetDelta
        if (key == null) {
            recordBody.writeVarint(-1);
        } else {
            recordBody.writeVarint(key.length);
            recordBody.write(key);
        }
        if (value == null) {
            recordBody.writeVarint(-1);
        } else {
            recordBody.writeVarint(value.length);
            recordBody.write(value);
        }
        recordBody.writeVarint(0);                     // header count
        var recordBodyBytes = recordBody.getAll();

        var record = new KafkaBBuffer();
        record.writeVarint(recordBodyBytes.length);    // record length
        record.write(recordBodyBytes);
        var recordBytes = record.getAll();

        // --- batch content after the crc field (crc is computed over this) ---
        var afterCrc = new KafkaBBuffer();
        afterCrc.writeShort((short) 0);                // attributes
        afterCrc.writeInt(0);                          // lastOffsetDelta
        afterCrc.writeLong(timestamp);                 // baseTimestamp
        afterCrc.writeLong(timestamp);                 // maxTimestamp
        afterCrc.writeLong(-1L);                       // producerId
        afterCrc.writeShort((short) -1);               // producerEpoch
        afterCrc.writeInt(-1);                         // baseSequence
        afterCrc.writeInt(1);                          // records count
        afterCrc.write(recordBytes);
        var afterCrcBytes = afterCrc.getAll();

        var crc32c = new CRC32C();
        crc32c.update(afterCrcBytes, 0, afterCrcBytes.length);
        int crc = (int) crc32c.getValue();

        var batch = new KafkaBBuffer();
        batch.writeLong(0L);                           // baseOffset
        batch.writeInt(4 + 1 + 4 + afterCrcBytes.length); // batchLength (after this field)
        batch.writeInt(0);                             // partitionLeaderEpoch
        batch.write((byte) 2);                         // magic
        batch.writeInt(crc);                           // CRC32C (uint32 bit pattern)
        batch.write(afterCrcBytes);
        return batch.getAll();
    }

    public static byte[] utf8(String s) {
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }
}
