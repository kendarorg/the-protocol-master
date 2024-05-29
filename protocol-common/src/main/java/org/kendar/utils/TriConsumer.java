package org.kendar.utils;

@FunctionalInterface
public interface TriConsumer<T,J,K> {
    void run(T t, J j, K k);
}
