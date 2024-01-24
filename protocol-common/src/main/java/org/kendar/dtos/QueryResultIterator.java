package org.kendar.dtos;

import java.util.Iterator;
import java.util.function.Supplier;

public class QueryResultIterator<T> implements Iterator<T> {

    private final Supplier<T> next;
    private final Supplier<Boolean> hasNext;
    private final Runnable close;

    private boolean open = true;

    public QueryResultIterator(
            Supplier<Boolean> hasNext,
            Supplier<T> next,
            Runnable close) {


        this.next = next;
        this.hasNext = hasNext;
        this.close = close;
    }

    public void close() {
        if (open) {
            close.run();
        }
    }

    @Override
    public boolean hasNext() {
        var result = hasNext.get();
        if (!result && open) {
            open = false;
            close.run();
        }
        return result;
    }

    @Override
    public T next() {
        return next.get();
    }
}
