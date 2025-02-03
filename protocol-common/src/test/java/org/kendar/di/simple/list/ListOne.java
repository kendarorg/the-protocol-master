package org.kendar.di.simple.list;

import org.kendar.di.annotations.TpmPostConstruct;
import org.kendar.di.annotations.TpmService;

@TpmService
public class ListOne implements ListOfInterface {
    private boolean postConstruct = false;

    public ListOne() {
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    public boolean isPostConstruct() {
        return postConstruct;
    }

    @Override
    public String toString() {
        return "ListOne{}";
    }

    @TpmPostConstruct
    public void postConstruct() {
        postConstruct = true;
        System.out.println("Post construct " + this.toString() + " " + Thread.currentThread().getId());
    }
}
