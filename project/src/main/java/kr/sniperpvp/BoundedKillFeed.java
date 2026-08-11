package kr.sniperpvp;

import java.util.ArrayDeque;
import java.util.List;

final class BoundedKillFeed<T> {
    private final int capacity;
    private final ArrayDeque<T> entries = new ArrayDeque<>();

    BoundedKillFeed(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    T addLast(T value) {
        T removed = entries.size() >= capacity ? entries.removeFirst() : null;
        entries.addLast(value);
        return removed;
    }

    boolean remove(T value) {
        return entries.remove(value);
    }

    List<T> entries() {
        return List.copyOf(entries);
    }

    int capacity() {
        return capacity;
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }
}
