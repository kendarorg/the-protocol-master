package org.kendar.di.simple.reg;

public class RegisteredItem {
    @Override
    public String toString() {
        return "RegisteredItem{}";
    }
    public RegisteredItem() {
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }
}
