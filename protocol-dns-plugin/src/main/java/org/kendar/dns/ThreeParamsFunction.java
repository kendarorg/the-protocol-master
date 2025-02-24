package org.kendar.dns;

@FunctionalInterface
public interface ThreeParamsFunction<A, B, C, D> {
    D apply(A a, B b, C c);
}
