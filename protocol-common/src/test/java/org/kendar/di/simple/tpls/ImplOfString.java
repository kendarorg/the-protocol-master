package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ImplOfString implements TemplateInterface<String> {
    public ImplOfString() {
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "ImplOfString{}";
    }
}
