package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ImplOfInt implements TemplateInterface<Integer> {
    @Override
    public String toString() {
        return "ImplOfInt{}";
    }
    public ImplOfInt() {
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }
}
