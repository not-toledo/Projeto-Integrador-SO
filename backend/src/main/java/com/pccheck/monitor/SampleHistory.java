package com.pccheck.monitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Histórico em memória com capacidade fixa: ao encher, descarta a amostra mais antiga. Thread-safe. */
public class SampleHistory<T> {

    private final int capacity;
    private final Deque<T> items = new ArrayDeque<>();

    public SampleHistory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A capacidade do histórico deve ser pelo menos 1.");
        }
        this.capacity = capacity;
    }

    public synchronized void add(T item) {
        items.addLast(item);
        while (items.size() > capacity) {
            items.removeFirst();
        }
    }

    /** Amostras da mais antiga para a mais recente. */
    public synchronized List<T> snapshot() {
        return new ArrayList<>(items);
    }

    /** As {@code limit} amostras mais recentes, da mais antiga para a mais recente. */
    public synchronized List<T> last(int limit) {
        if (limit < 1) {
            return List.of();
        }
        List<T> all = new ArrayList<>(items);
        return all.size() <= limit ? all : new ArrayList<>(all.subList(all.size() - limit, all.size()));
    }

    public synchronized T latest() {
        return items.peekLast();
    }

    public synchronized int size() {
        return items.size();
    }

    public int capacity() {
        return capacity;
    }
}
