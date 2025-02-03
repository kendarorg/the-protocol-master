package org.kendar.di.simple.reg;

import org.kendar.di.annotations.TpmService;

@TpmService
public class UsingRegistered {
    private final RegisteredItem item;

    public UsingRegistered(RegisteredItem item) {

        this.item = item;
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "UsingRegistered{" +
                "item=" + item +
                '}';
    }

    public RegisteredItem getItem() {
        return item;
    }
}
