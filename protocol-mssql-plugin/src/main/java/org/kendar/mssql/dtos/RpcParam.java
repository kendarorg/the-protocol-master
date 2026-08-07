package org.kendar.mssql.dtos;

import org.kendar.sql.jdbc.BindingParameter;

public class RpcParam {
    private final String name;
    private final int status;
    private final BindingParameter parameter;

    public RpcParam(String name, int status, BindingParameter parameter) {
        this.name = name;
        this.status = status;
        this.parameter = parameter;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public boolean isOutput() {
        return (status & 0x01) == 0x01;
    }

    public BindingParameter getParameter() {
        return parameter;
    }
}
