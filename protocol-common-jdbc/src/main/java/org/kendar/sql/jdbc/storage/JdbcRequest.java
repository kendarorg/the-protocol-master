package org.kendar.sql.jdbc.storage;

import org.kendar.sql.jdbc.BindingParameter;

import java.util.List;

public class JdbcRequest {
    private String query;
    private List<BindingParameter> parameterValues;

    /**
     * Needed for serialization
     */
    public JdbcRequest() {
    }

    public JdbcRequest(String query, List<BindingParameter> parameterValues) {
        this.query = query;
        this.parameterValues = textify(parameterValues);
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

    private List<BindingParameter> textify(List<BindingParameter> parameterValues) {
        for (var pv : parameterValues) {
            if (pv.isOutput()) {
                pv.setValue(null);
            } else if (!pv.isBinary() && pv.getValue() != null) {
                pv.setValue(pv.getValue());
            }
        }
        return parameterValues;
    }
}
