package org.kendar.di.simple.list;

import org.kendar.annotations.di.TpmService;

import java.util.List;

@TpmService
public class ListUser {
    private final List<ListOfInterface> items;

    public List<ListOfInterface> getItems() {
        return items;
    }

    public ListUser(List<ListOfInterface> items){

        this.items = items;
    }
}
