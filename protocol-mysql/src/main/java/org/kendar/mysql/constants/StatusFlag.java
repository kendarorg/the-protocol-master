package org.kendar.mysql.constants;

/**
 * https://dev.mysql.com/doc/internals/en/status-flags.html
 * <p>
 * <pre>
 * The status flags are a bit-field.
 *
 * Flag	                                Value	Comment
 * SERVER_STATUS_IN_TRANS	            0x0001	a transaction is active
 * SERVER_STATUS_AUTOCOMMIT	            0x0002	auto-commit is enabled
 * SERVER_MORE_RESULTS_EXISTS	        0x0008
 * SERVER_STATUS_NO_GOOD_INDEX_USED	    0x0010
 * SERVER_STATUS_NO_INDEX_USED	        0x0020
 * SERVER_STATUS_CURSOR_EXISTS	        0x0040	Used by Binary Protocol Resultset to signal that COM_STMT_FETCH must be used to fetch the row-data.
 * SERVER_STATUS_LAST_ROW_SENT	        0x0080
 * SERVER_STATUS_DB_DROPPED	            0x0100
 * SERVER_STATUS_NO_BACKSLASH_ESCAPES	0x0200
 * SERVER_STATUS_METADATA_CHANGED	    0x0400
 * SERVER_QUERY_WAS_SLOW	            0x0800
 * SERVER_PS_OUT_PARAMS	                0x1000
 * SERVER_STATUS_IN_TRANS_READONLY	    0x2000	in a read-only transaction
 * SERVER_SESSION_STATE_CHANGED	        0x4000	connection state information has changed
 * </pre>
 */
public enum StatusFlag {

    /**
     * a transaction is active
     */
    SERVER_STATUS_IN_TRANS(0x0001, "a transaction is active"),
    /**
     * auto-commit is enabled
     */
    SERVER_STATUS_AUTOCOMMIT(0x0002, "auto-commit is enabled"),

    SERVER_MORE_RESULTS_EXISTS(0x0008, ""),

    SERVER_STATUS_NO_GOOD_INDEX_USED(0x0010, ""),

    SERVER_STATUS_NO_INDEX_USED(0x0020, ""),

    /**
     * Used by Binary Protocol ResultSet to signal that COM_STMT_FETCH must be used to fetch the row-data.
     */
    SERVER_STATUS_CURSOR_EXISTS(0x0040, ""),

    SERVER_STATUS_LAST_ROW_SENT(0x0080, ""),

    SERVER_STATUS_DB_DROPPED(0x0100, ""),

    SERVER_STATUS_NO_BACKSLASH_ESCAPES(0x0200, ""),
    SERVER_STATUS_METADATA_CHANGED(0x0400, ""),
    SERVER_QUERY_WAS_SLOW(0x0800, ""),
    SERVER_PS_OUT_PARAMS(0x1000, ""),

    /**
     * in a read-only transaction
     */
    SERVER_STATUS_IN_TRANS_READONLY(0x2000, ""),

    /**
     * connection state information has changed
     */
    SERVER_SESSION_STATE_CHANGED(0x4000, "");

    private final int code;
    private final String desc;

    StatusFlag(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static void main(String[] args) {
        for (StatusFlag flags : StatusFlag.values()) {
            System.out.println(flags.getCode() + ":" + flags.getDesc());
        }
    }

    public static boolean isFlagSet(int source, int flag) {
        return (source & flag) == flag;
    }

    public static boolean isFlagSet(int source, StatusFlag flag) {
        return isFlagSet(source, flag.getCode());
    }

    public static int setFlag(int source, int flag) {
        return source |= flag;
    }

    public static int unsetFlag(int source, int flag) {
        return source & ~flag;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }


}
