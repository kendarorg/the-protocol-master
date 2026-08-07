package org.kendar.mssql.messages;

import org.kendar.mssql.constants.TdsTokenType;

public class InfoToken extends ErrorToken {
    public InfoToken(int number, String message) {
        super(number, message);
    }

    @Override
    protected byte getTokenType() {
        return TdsTokenType.INFO;
    }

    @Override
    protected byte getSeverity() {
        return 0;
    }
}
