package org.kendar.utils.parser;

import java.util.List;

public class SubObjectArray {
    public String name;
    public List<SimpleClass> child;

    public SubObjectArray(String name, List<SimpleClass> child) {
        this.name = name;
        this.child = child;
    }

    public List<SimpleClass> getChild() {
        return child;
    }

    public void setChild(List<SimpleClass> child) {
        this.child = child;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
