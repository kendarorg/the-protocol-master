package org.kendar.mssql.messages;

import org.kendar.mssql.constants.TdsTokenType;

public class DoneInProcToken extends DoneToken {
    public DoneInProcToken(int status, long rowCount) {
        super(TdsTokenType.DONEINPROC, status, rowCount);
    }
}
