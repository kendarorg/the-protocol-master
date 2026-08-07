package org.kendar.amqp.v10.dtos;

/**
 * Descriptor (ulong) codes for AMQP 1.0 performatives, SASL frames and message
 * sections, as defined by the OASIS AMQP 1.0 / ISO 19464 spec (§5.6.4).
 * <p>
 * A frame body is a described type: {@code 0x00} then a small-ulong descriptor
 * (format code {@code 0x53}) carrying one of these values, then the value list.
 */
public final class Performatives {

    // AMQP layer performatives (frame type 0)
    public static final long OPEN = 0x10L;
    public static final long BEGIN = 0x11L;
    public static final long ATTACH = 0x12L;
    public static final long FLOW = 0x13L;
    public static final long TRANSFER = 0x14L;
    public static final long DISPOSITION = 0x15L;
    public static final long DETACH = 0x16L;
    public static final long END = 0x17L;
    public static final long CLOSE = 0x18L;

    // SASL layer frames (frame type 1)
    public static final long SASL_MECHANISMS = 0x40L;
    public static final long SASL_INIT = 0x41L;
    public static final long SASL_CHALLENGE = 0x42L;
    public static final long SASL_RESPONSE = 0x43L;
    public static final long SASL_OUTCOME = 0x44L;

    // Message sections (inside a transfer body)
    public static final long HEADER = 0x70L;
    public static final long DELIVERY_ANNOTATIONS = 0x71L;
    public static final long MESSAGE_ANNOTATIONS = 0x72L;
    public static final long PROPERTIES = 0x73L;
    public static final long APPLICATION_PROPERTIES = 0x74L;
    public static final long DATA = 0x75L;
    public static final long AMQP_SEQUENCE = 0x76L;
    public static final long AMQP_VALUE = 0x77L;
    public static final long FOOTER = 0x78L;

    private Performatives() {
    }
}
