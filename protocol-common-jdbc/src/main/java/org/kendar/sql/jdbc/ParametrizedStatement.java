package org.kendar.sql.jdbc;

import java.sql.PreparedStatement;

public class ParametrizedStatement {
    public final String query;
    public final PreparedStatement ps;

    public ParametrizedStatement(String query, PreparedStatement ps) {
        this.query = query;
        this.ps = ps;
    }
}