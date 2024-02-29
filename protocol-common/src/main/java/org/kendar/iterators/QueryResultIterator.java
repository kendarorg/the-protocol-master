package org.kendar.iterators;

import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Iterator for query results. Contains a callback to stop the iterator
 * e.g. in case of asynchronous cancel of operations
 *
 * @param <T>
 */
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
