package org.kendar.di.simple.reg;

public class RegisteredItem {
    public RegisteredItem() {
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "RegisteredItem{}";
    }
}
