package org.kendar.di.simple.named;

import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;


@TpmService
public class SimpleUnnamed implements NamedBase {
    public String test;

    public SimpleUnnamed(@TpmNamed("test") String test) {
        this.test = test;
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String getTest() {
        return test;
    }

    @Override
    public String toString() {
        return "SimpleUnnamed{" +
                "test='" + test + '\'' +
                '}';
    }
}
