package org.kendar.postgres.dtos;

import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parse {
    private final String statementName;
    private final String query;
    private final List<Integer> oids;
    private final ArrayList<JDBCType> concreteTypes;
    private final List<Boolean> isOutput;
    private final Map<String, Binding> binds = new HashMap<>();
    private boolean describable;

    public Parse(String statementName, String query, List<Integer> oids,
                 ArrayList<JDBCType> concreteTypes) {

        this.statementName = statementName;
        this.query = query;
        this.oids = oids;
        this.concreteTypes = concreteTypes;
        this.isOutput = new ArrayList<>();
    }

    public List<Boolean> getIsOutput() {
        return isOutput;
    }

    public Map<String, Binding> getBinds() {
        return binds;
    }

    public String getStatementName() {
        return statementName;
    }

    public String getQuery() {
        return query;
    }

    public List<Integer> getOids() {
        return oids;
    }

    public void put(String key, Binding binding) {
        binds.put(key, binding);
    }

    public boolean isDescribable() {
        return describable;
    }

    public void setDescribable(boolean describable) {
        this.describable = describable;
    }

    public ArrayList<JDBCType> getConcreteTypes() {
        return concreteTypes;
    }
}
