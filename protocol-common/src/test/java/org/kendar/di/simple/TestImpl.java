package org.kendar.di.simple;

import org.kendar.di.annotations.TpmService;

@TpmService
public class TestImpl implements TestInterface {
    @Override
    public String toString() {
        return "TestImpl{}";
    }
    public TestImpl() {
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }
}
