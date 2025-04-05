package org.kendar.tests.utils;

public class ObjectContainer<T> {
    private T object;

    public ObjectContainer(final T object) {
        this.object = object;
    }

    public ObjectContainer() {
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }
}
