package org.kendar.sql.jdbc.proxy;

import org.kendar.sql.jdbc.BindingParameter;

import java.util.List;

public class JdbcCall {
    private final String query;
    private final List<BindingParameter> parameterValues;

    public String getQuery() {
        return query;
    }

    public List<BindingParameter> getParameterValues() {
        return parameterValues;
    }

    public JdbcCall(String query, List<BindingParameter> parameterValues) {

        this.query = query;
        this.parameterValues = parameterValues;
    }
}
