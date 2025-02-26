package org.kendar.utils.parser;

public class SubObject {
    public String name;
    public SimpleClass child;

    public SubObject(String name, SimpleClass child) {
        this.name = name;
        this.child = child;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SimpleClass getChild() {
        return child;
    }

    public void setChild(SimpleClass child) {
        this.child = child;
    }
}
