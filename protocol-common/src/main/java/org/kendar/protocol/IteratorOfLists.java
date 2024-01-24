package org.kendar.protocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IteratorOfLists<T> implements Iterator<T> {
    private final List<Iterator<T>> iterators = new ArrayList<>();
    private Iterator<T> current;

    public IteratorOfLists<T> addIterator(Iterator<T> iterator) {
        iterators.add(iterator);
        return this;
    }


    @Override
    public boolean hasNext() {
        if (iterators.isEmpty()) return false;
        if (current != null && current.hasNext()) return true;
        current = iterators.get(0);
        iterators.remove(0);
        return current.hasNext();
    }

    @Override
    public T next() {
        return current.next();
    }

    public List<Iterator<T>> getIterators() {
        return iterators;
    }
}
