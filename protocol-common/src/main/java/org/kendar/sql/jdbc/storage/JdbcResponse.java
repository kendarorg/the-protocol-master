package org.kendar.sql.jdbc.storage;

import org.kendar.sql.jdbc.SelectResult;

public class JdbcResponse {
    private SelectResult selectResult;
    private int intResult;

    /**
     * Needed for serialization
     */
    public JdbcResponse() {
    }

    public JdbcResponse(SelectResult selectResult) {
        this.selectResult = selectResult;
    }

    public JdbcResponse(int intResult) {
        this.intResult = intResult;
    }

    public int getIntResult() {
        return intResult;
    }

    public void setIntResult(int intResult) {
        this.intResult = intResult;
    }

    public SelectResult getSelectResult() {
        return selectResult;
    }

    public void setSelectResult(SelectResult selectResult) {
        this.selectResult = selectResult;
    }
}
