package org.kendar.di.simple.list;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ListTwo implements ListOfInterface {
    public ListTwo() {
        System.out.println(this + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "ListTwo{}";
    }
}
