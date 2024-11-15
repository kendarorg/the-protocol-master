package org.kendar.utils;

public class ChangeableReference<T> {
    private  T value;

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public ChangeableReference(T value) {
        this.value = value;
    }
}
