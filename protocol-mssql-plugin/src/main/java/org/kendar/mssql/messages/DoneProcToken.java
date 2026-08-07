package org.kendar.mssql.messages;

import org.kendar.mssql.constants.TdsTokenType;

public class DoneProcToken extends DoneToken {
    public DoneProcToken(int status, long rowCount) {
        super(TdsTokenType.DONEPROC, status, rowCount);
    }
}
