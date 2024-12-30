package org.kendar.di.simple.reg;

import org.kendar.annotations.di.TpmService;

@TpmService
public class UsingRegistered {
    private final RegisteredItem item;

    public RegisteredItem getItem() {
        return item;
    }

    public UsingRegistered(RegisteredItem item){

        this.item = item;
    }
}
