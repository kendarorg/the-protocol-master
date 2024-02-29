package org.kendar.iterators;

import java.util.Iterator;

/**
 * Empty iterator (to iterate on nothing)
 *
 * @param <T>
 */
public class EmptyIterator<T> implements Iterator<T> {
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        return null;
    }
}
