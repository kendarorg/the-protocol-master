package org.kendar.di.simple;

import org.kendar.di.annotations.TpmService;

@TpmService
public class TestImpl implements TestInterface {
    public TestImpl() {
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "TestImpl{}";
    }
}
