package org.acarrasco.collections;

import java.util.Map;

/**
 * A mutable container for Key-Value pairs.
 */
public class Entry<K, V> implements Map.Entry<K, V> {
    K key;
    V value;

    public Entry() {
    }

    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }
}