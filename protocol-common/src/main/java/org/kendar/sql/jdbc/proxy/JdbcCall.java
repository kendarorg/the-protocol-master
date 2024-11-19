package org.kendar.sql.jdbc.proxy;

import org.kendar.sql.jdbc.BindingParameter;

import java.util.ArrayList;
import java.util.List;

public class JdbcCall {
    private List<BindingParameter> parameterValues = new ArrayList<>();
    private String query;

    public JdbcCall() {

    }

    public JdbcCall(String query, List<BindingParameter> parameterValues) {

        this.query = query;
        this.parameterValues = parameterValues;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<BindingParameter> getParameterValues() {
        return parameterValues;
    }

    public void setParameterValues(List<BindingParameter> parameterValues) {
        this.parameterValues = parameterValues;
    }
}
