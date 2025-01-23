package org.kendar.di.simple.list;

import org.kendar.di.annotations.TpmService;

@TpmService
public class ListTwo implements ListOfInterface {
    @Override
    public String toString() {
        return "ListTwo{}";
    }



    public ListTwo() {
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }
}
