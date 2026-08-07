package org.kendar.mssql.constants;

public class TmRequestType {
    public static final int TM_GET_DTC_ADDRESS = 0;
    public static final int TM_PROPAGATE_XACT = 1;
    public static final int TM_BEGIN_XACT = 5;
    public static final int TM_PROMOTE_XACT = 6;
    public static final int TM_COMMIT_XACT = 7;
    public static final int TM_ROLLBACK_XACT = 8;
    public static final int TM_SAVE_XACT = 9;
}
