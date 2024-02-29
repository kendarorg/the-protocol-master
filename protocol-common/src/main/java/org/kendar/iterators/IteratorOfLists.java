package org.kendar.iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Iterate on multiple lists
 *
 * @param <T>
 */
public class IteratorOfLists<T> implements Iterator<T> {
    private final List<Iterator<T>> iterators = new ArrayList<>();
    private Iterator<T> current;

    public IteratorOfLists<T> addIterator(Iterator<T> iterator) {
        iterators.add(iterator);
        return this;
    }


    @Override
    public boolean hasNext() {
        if (iterators.isEmpty() && current == null) {
            return false;
        }
        if (current != null && current.hasNext()) return true;
        if (!iterators.isEmpty()) {
            current = iterators.remove(0);
            return current.hasNext();
        }
        return false;
    }

    @Override
    public T next() {
        return current.next();
    }

    public List<Iterator<T>> getIterators() {
        return iterators;
    }
}
