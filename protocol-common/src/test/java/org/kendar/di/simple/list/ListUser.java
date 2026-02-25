package org.kendar.di.simple.list;

import org.kendar.di.annotations.TpmService;

import java.util.List;

@TpmService
public class ListUser {
    private final List<ListOfInterface> items;

    public ListUser(List<ListOfInterface> items) {

        this.items = items;
        System.out.println(this + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "ListUser{" +
                "items=" + items +
                '}';
    }

    public List<ListOfInterface> getItems() {
        return items;
    }
}
