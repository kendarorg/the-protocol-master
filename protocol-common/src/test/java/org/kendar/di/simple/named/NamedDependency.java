package org.kendar.di.simple.named;

import org.kendar.di.annotations.TpmNamed;
import org.kendar.di.annotations.TpmService;

@TpmService
public class NamedDependency {
    public NamedBase named;

    public NamedDependency(@TpmNamed("simpleNamed") NamedBase named) {
        this.named = named;
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "NamedDependency{" +
                "named=" + named +
                '}';
    }
}
