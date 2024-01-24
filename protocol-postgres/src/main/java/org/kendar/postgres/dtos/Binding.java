package org.kendar.postgres.dtos;

import org.kendar.sql.jdbc.BindingParameter;

import java.util.List;

public class Binding {
    private final String statement;
    private final String portal;
    private final List<Short> formatCodes;
    private final List<BindingParameter> parameterValues;
    private boolean describable;

    public Binding(String statement, String portal, List<Short> formatCodes, List<BindingParameter> parameterValues) {
        this.statement = statement;

        this.portal = portal;
        this.formatCodes = formatCodes;
        this.parameterValues = parameterValues;
    }

    public String getPortal() {
        return portal;
    }

    public List<Short> getFormatCodes() {
        return formatCodes;
    }

    public List<BindingParameter> getParameterValues() {
        return parameterValues;
    }

    public String getStatement() {
        return statement;
    }

    public boolean isDescribable() {
        return describable;
    }

    public void setDescribable(boolean describable) {
        this.describable = describable;
    }
}
