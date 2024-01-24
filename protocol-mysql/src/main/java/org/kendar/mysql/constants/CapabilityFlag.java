package org.kendar.mysql.constants;

/**
 * Created by zcg on 2017/3/31.
 */
public enum CapabilityFlag {

    CLIENT_LONG_PASSWORD(0x0001, "Use the improved version of Old Password Authentication. Assumed to be set since 4.1.1."),
    CLIENT_FOUND_ROWS(0x0002, "Send found rows instead of affected rows in EOF_Packet."),
    CLIENT_LONG_FLAG(0x0004, "Longer flags in Protocol::ColumnDefinition320.Server Supports longer flags. Client Expects longer flags."),
    CLIENT_CONNECT_WITH_DB(0x0008,
            "Database (schema) name can be specified on connect in Handshake Response Packet. "
                    + " Server Supports schema-name in Handshake Response Packet. "
                    + " Client Handshake Response Packet contains a schema-name."),
    CLIENT_NO_SCHEMA(0x0010, "Server Do not permit database.table.column."),
    CLIENT_COMPRESS(0x0020, "Compression protocol supported. Server Supports compression. "
            + "Client Switches to Compression compressed protocol after successful authentication."),
    CLIENT_ODBC(0x0040, "Special handling of ODBC behavior. No special behavior since 3.22."),
    CLIENT_LOCAL_FILES(0x0080, "Can use LOAD DATA LOCAL. Server Enables the LOCAL INFILE request of LOAD DATA|XML."
            + " Client Will handle LOCAL INFILE request."),
    CLIENT_IGNORE_SPACE(0x0100, "Server Parser can ignore spaces before '('. "
            + " Client Let the parser ignore spaces before '('."),
    CLIENT_PROTOCOL_41(0x0200, "Server Supports the 4.1 protocol. Client Uses the 4.1 protocol."
            + " this value was CLIENT_CHANGE_USER in 3.22, unused in 4.0"),
    CLIENT_INTERACTIVE(0x0400, "wait_timeout versus wait_interactive_timeout. "
            + "Server Supports interactive and noninteractive clients. Client is interactive. See mysql_real_connect()"),
    CLIENT_SSL(0x0800, "Server Supports SSL. Client Switch to SSL after sending the capabilities-flags."),
    CLIENT_IGNORE_SIGPIPE(0x1000, "Client Do not issue SIGPIPE if network failures occur (libmysqlclient only)."
            + " See mysql_real_connect()"),
    CLIENT_TRANSACTIONS(0x2000, "Server Can send status flags in EOF_Packet. Client Expects status flags in EOF_Packet."
            + " This flag is optional in 3.23, but always set by the server since 4.0."),
    CLIENT_RESERVED(0x4000, "Unused. Was named CLIENT_PROTOCOL_41 in 4.1.0."),
    CLIENT_SECURE_CONNECTION(0x8000, "Server Supports Authentication::Native41. Client Supports Authentication::Native41."),
    CLIENT_MULTI_STATEMENTS(0x00010000, "Server Can handle multiple statements per COM_QUERY and COM_STMT_PREPARE."
            + " Client May send multiple statements per COM_QUERY and COM_STMT_PREPARE. "
            + "Was named CLIENT_MULTI_QUERIES in 4.1.0, renamed later. Requires CLIENT_PROTOCOL_41"),
    CLIENT_MULTI_RESULTS(0x00020000, "Server Can send multiple resultsets for COM_QUERY."
            + " Client Can handle multiple resultsets for COM_QUERY. Requires CLIENT_PROTOCOL_41"),
    CLIENT_PS_MULTI_RESULTS(0x00040000, "Server Can send multiple resultsets for COM_STMT_EXECUTE."
            + " Client Can handle multiple resultsets for COM_STMT_EXECUTE. Requires CLIENT_PROTOCOL_41"),
    CLIENT_PLUGIN_AUTH(0x00080000, "Server Sends extra data in Initial Handshake Packet and supports the pluggable "
            + "authentication protocol. Client Supports authentication plugins. Requires CLIENT_PROTOCOL_41"),
    CLIENT_CONNECT_ATTRS(0x00100000, "Server Permits connection attributes in Protocol::HandshakeResponse41."
            + " Client Sends connection attributes in Protocol::HandshakeResponse41."),
    CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA(0x00200000, "Server Understands length-encoded integer for auth response data "
            + "in Protocol::HandshakeResponse41. Client Length of auth response data in Protocol::HandshakeResponse41"
            + " is a length-encoded integer. The flag was introduced in 5.6.6, but had the wrong value."),
    CLIENT_CAN_HANDLE_EXPIRED_PASSWORDS(0x00400000, "Server Announces support for expired password extension. "
            + "Client Can handle expired passwords."),
    CLIENT_SESSION_TRACK(0x00800000, "Server Can set SERVER_SESSION_STATE_CHANGED in the Status Flags and "
            + "send session-state change data after a OK packet. "
            + "Client Expects the server to send sesson-state changes after a OK packet."),
    CLIENT_DEPRECATE_EOF(0x01000000, "Server Can send OK after a Text Resultset."
            + "Client Expects an OK (instead of EOF) after the resultset rows of a Text Resultset."),

    CLIENT_OPTIONAL_RESULTSET_METADATA(0x02000000, ""),
    CLIENT_ZSTD_COMPRESSION_ALGORITHM(0x04000000, ""),
    CLIENT_QUERY_ATTRIBUTES(0x08000000, ""),
    MULTI_FACTOR_AUTHENTICATION(0x10000000, ""),
    CLIENT_CAPABILITY_EXTENSION(0x20000000, ""),
    CLIENT_SSL_VERIFY_SERVER_CERT(0x40000000, ""),
    CLIENT_REMEMBER_OPTIONS(0x80000000, "");


    private final int code;

    private final String desc;

    CapabilityFlag(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static boolean isFlagSet(int source, int flag) {
        return (source & flag) == flag;
    }

    public static boolean isFlagSet(int source, CapabilityFlag flag) {
        return isFlagSet(source, flag.getCode());
    }

    public static int setFlag(int source, int flag) {
        return source |= flag;
    }

    public static int unsetFlag(int source, int flag) {
        return source & ~flag;
    }

    public static int getFakeServerCapabilities() {
        int flag = 0;
        //lower
        flag |= CapabilityFlag.CLIENT_LONG_PASSWORD.getCode();
        flag |= CapabilityFlag.CLIENT_FOUND_ROWS.getCode();
        flag |= CapabilityFlag.CLIENT_LONG_FLAG.getCode();
        flag |= CapabilityFlag.CLIENT_CONNECT_WITH_DB.getCode();
        flag |= CapabilityFlag.CLIENT_ODBC.getCode();
        flag |= CapabilityFlag.CLIENT_IGNORE_SPACE.getCode();
        flag |= CapabilityFlag.CLIENT_PROTOCOL_41.getCode();
        flag |= CapabilityFlag.CLIENT_INTERACTIVE.getCode();
        flag |= CapabilityFlag.CLIENT_IGNORE_SIGPIPE.getCode();
        flag |= CapabilityFlag.CLIENT_TRANSACTIONS.getCode();
        flag |= CapabilityFlag.CLIENT_SECURE_CONNECTION.getCode();
        //upper
        flag |= CapabilityFlag.CLIENT_MULTI_STATEMENTS.getCode();
        flag |= CapabilityFlag.CLIENT_MULTI_RESULTS.getCode();
        flag |= CapabilityFlag.CLIENT_PS_MULTI_RESULTS.getCode();
        flag |= CapabilityFlag.CLIENT_PLUGIN_AUTH.getCode();
        flag |= CapabilityFlag.CLIENT_CONNECT_ATTRS.getCode();
        flag |= CapabilityFlag.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA.getCode();
        flag |= CapabilityFlag.CLIENT_OPTIONAL_RESULTSET_METADATA.getCode();
        return flag;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
