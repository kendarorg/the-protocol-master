package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ImplOfString implements TemplateInterface<String> {
    @Override
    public String toString() {
        return "ImplOfString{}";
    }
    public ImplOfString() {
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }
}
