package org.kendar.utils.parser;

public class ItemWithValAndCount {
    private String val;
    private int counter;

    public ItemWithValAndCount() {
    }

    public ItemWithValAndCount(String val, int counter) {
        this.val = val;
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}