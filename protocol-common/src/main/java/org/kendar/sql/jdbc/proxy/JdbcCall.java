package org.kendar.sql.jdbc.proxy;

import org.kendar.sql.jdbc.BindingParameter;

import java.util.List;

public class JdbcCall {
    private String query;
    private final List<BindingParameter> parameterValues;

    public JdbcCall(String query, List<BindingParameter> parameterValues) {

        this.query = query;
        this.parameterValues = parameterValues;
    }

    public String getQuery() {
        return query;
    }

    public List<BindingParameter> getParameterValues() {
        return parameterValues;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
