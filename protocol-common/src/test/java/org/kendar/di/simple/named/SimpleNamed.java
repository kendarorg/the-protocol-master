package org.kendar.di.simple.named;

import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;

@TpmService("simpleNamed")
public class SimpleNamed implements NamedBase{
    public String getTest() {
        return test;
    }

    public String test;

    @Override
    public String toString() {
        return "SimpleNamed{" +
                "test='" + test + '\'' +
                '}';
    }

    public SimpleNamed(@TpmNamed("test")String test) {
        this.test = test;
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }
}
