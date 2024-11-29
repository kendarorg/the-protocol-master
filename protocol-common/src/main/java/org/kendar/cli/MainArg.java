package org.kendar.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainArg {
    final String id;
    final List<String> values = new ArrayList<>();

    public MainArg(String id) {
        this.id = id;
    }

    public List<String> getValues() {

        return values;
    }

    public String getId() {
        return id;
    }

    public void addValue(String value) {
        values.add(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MainArg mainArg = (MainArg) o;
        return Objects.equals(id, mainArg.id) && Objects.equals(values, mainArg.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, values);
    }

    @Override
    public String toString() {
        return "MainArg{" +
                "id='" + id + '\'' +
                ", values=" + values +
                '}';
    }
}
