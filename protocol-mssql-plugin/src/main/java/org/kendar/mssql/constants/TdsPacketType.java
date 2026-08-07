package org.kendar.mssql.constants;

public class TdsPacketType {
    public static final byte SQL_BATCH = 0x01;
    public static final byte RPC = 0x03;
    public static final byte TABULAR_RESULT = 0x04;
    public static final byte ATTENTION = 0x06;
    public static final byte BULK_LOAD = 0x07;
    public static final byte TRANSACTION_MANAGER = 0x0E;
    public static final byte LOGIN7 = 0x10;
    public static final byte PRELOGIN = 0x12;
}
