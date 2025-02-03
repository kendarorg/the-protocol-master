package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ExtendedImplOfInt extends ImplOfInt {
    public ExtendedImplOfInt() {
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "ExtendedImplOfInt{}";
    }
}
