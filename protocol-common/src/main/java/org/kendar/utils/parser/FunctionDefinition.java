package org.kendar.utils.parser;

import java.util.Objects;

public class FunctionDefinition {
    public String name;
    public int paramsCount;
    public FunctionDefinition(String name, int paramsCount) {
        this.name = name;
        this.paramsCount = paramsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FunctionDefinition that = (FunctionDefinition) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
