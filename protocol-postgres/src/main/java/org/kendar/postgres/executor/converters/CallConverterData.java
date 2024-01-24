package org.kendar.postgres.executor.converters;

public class CallConverterData {
    private final String query;
    private final int params;

    public CallConverterData(String query, int params) {

        this.query = query;
        this.params = params;
    }

    public String getQuery() {
        return query;
    }

    public int getParams() {
        return params;
    }
}
