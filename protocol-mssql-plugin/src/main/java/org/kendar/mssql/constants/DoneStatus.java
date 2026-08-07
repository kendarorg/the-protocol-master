package org.kendar.mssql.constants;

public class DoneStatus {
    public static final int DONE_FINAL = 0x0000;
    public static final int DONE_MORE = 0x0001;
    public static final int DONE_ERROR = 0x0002;
    public static final int DONE_INXACT = 0x0004;
    public static final int DONE_COUNT = 0x0010;
    public static final int DONE_ATTN = 0x0020;
    public static final int DONE_SRVERROR = 0x0100;
}
