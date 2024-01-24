package org.kendar.postgres.constants;

public class TypesOids {
    public static final int Int8 = 20;
    public static final int Float8 = 701;
    public static final int Int4 = 23;
    public static final int Numeric = 1700;
    public static final int Float4 = 700;
    public static final int Int2 = 21;
    public static final int Money = 790;

    // Boolean
    public static final int Bool = 16;

    // Geometric
    public static final int Box = 603;
    public static final int Circle = 718;
    public static final int Line = 628;
    public static final int LSeg = 601;
    public static final int Path = 602;
    public static final int Point = 600;
    public static final int Polygon = 604;

    // Character
    public static final int BPChar = 1042;
    public static final int Text = 25;
    public static final int Varchar = 1043;
    public static final int Name = 19;
    public static final int Char = 18;

    // Binary data
    public static final int Bytea = 17;

    // Date/Time
    public static final int Date = 1082;
    public static final int Time = 1083;
    public static final int Timestamp = 1114;
    public static final int TimestampTz = 1184;
    public static final int Interval = 1186;
    public static final int TimeTz = 1266;
    public static final int Abstime = 702;

    // Network address
    public static final int Inet = 869;
    public static final int Cidr = 650;
    public static final int Macaddr = 829;
    public static final int Macaddr8 = 774;

    // Bit string
    public static final int Bit = 1560;
    public static final int Varbit = 1562;

    // Text search
    public static final int TsVector = 3614;
    public static final int TsQuery = 3615;
    public static final int Regconfig = 3734;

    // UUID
    public static final int Uuid = 2950;

    // XML
    public static final int Xml = 142;

    // JSON
    public static final int Json = 114;
    public static final int Jsonb = 3802;
    public static final int JsonPath = 4072;

    // public
    public static final int Refcursor = 1790;
    public static final int Oidvector = 30;
    public static final int Int2vector = 22;
    public static final int Oid = 26;
    public static final int Xid = 28;
    public static final int Xid8 = 5069;
    public static final int Cid = 29;
    public static final int Regtype = 2206;
    public static final int Tid = 27;
    public static final int PgLsn = 3220;

    // Special
    public static final int Record = 2249;
    public static final int Void = 2278;
    public static final int Unknown = 705;

    // Range types
    public static final int Int4Range = 3904;
    public static final int Int8Range = 3926;
    public static final int NumRange = 3906;
    public static final int TsRange = 3908;
    public static final int TsTzRange = 3910;
    public static final int DateRange = 3912;

    // Multirange types
    public static final int Int4Multirange = 4451;
    public static final int Int8Multirange = 4536;
    public static final int NumMultirange = 4532;
    public static final int TsMultirange = 4533;
    public static final int TsTzMultirange = 4534;
    public static final int DateMultirange = 4535;
}
