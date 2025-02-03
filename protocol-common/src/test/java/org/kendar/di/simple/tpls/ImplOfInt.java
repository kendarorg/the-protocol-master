package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ImplOfInt implements TemplateInterface<Integer> {
    public ImplOfInt() {
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "ImplOfInt{}";
    }
}
