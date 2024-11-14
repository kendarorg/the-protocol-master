package org.kendar.http.utils;

@FunctionalInterface
public interface ConsumeException<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t) throws Exception;
}