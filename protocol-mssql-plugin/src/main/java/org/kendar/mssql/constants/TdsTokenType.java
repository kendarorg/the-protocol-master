package org.kendar.mssql.constants;

public class TdsTokenType {
    public static final byte RETURNSTATUS = (byte) 0x79;
    public static final byte COLMETADATA = (byte) 0x81;
    public static final byte ERROR = (byte) 0xAA;
    public static final byte INFO = (byte) 0xAB;
    public static final byte RETURNVALUE = (byte) 0xAC;
    public static final byte LOGINACK = (byte) 0xAD;
    public static final byte ROW = (byte) 0xD1;
    public static final byte ENVCHANGE = (byte) 0xE3;
    public static final byte DONE = (byte) 0xFD;
    public static final byte DONEPROC = (byte) 0xFE;
    public static final byte DONEINPROC = (byte) 0xFF;
}
