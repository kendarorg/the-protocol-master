package org.kendar.mysql.constants;

import java.util.HashMap;
import java.util.Map;

public enum Language {
    BIG5_CHINESE_CO(1),
    LATIN2_CZECH_CS(2),
    DEC8_SWEDISH_CI(3),
    CP850_GENERAL_CI(4),
    LATIN1_GERMAN_CI(5),
    HP8_ENGLISH_CI(6),
    KOI8R_GENERAL_CI(7),
    LATIN1_SWEDISH_CI(8),
    LATIN2_GENERAL_CI(9),
    SWE7_SWEDISH_CI(10),
    UTF8_GENERAL_CI(33),
    UTF8MB4_0900_AI_CI(45),
    BINARY(63);
    private static final Map<Integer, Language> BY_INT = new HashMap<>();

    static {
        for (Language e : values()) {
            BY_INT.put(e.value, e);
        }
    }

    private final int value;

    Language(int value) {

        this.value = value;
    }

    public static Language of(int value) {
        return BY_INT.get(value);
    }

    public int getValue() {
        return value;
    }

}
