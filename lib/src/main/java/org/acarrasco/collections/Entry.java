package org.acarrasco.collections;

/**
 * A mutable container for Key-Value pairs.
 */
public class Entry<K, V> {
    K key;
    V value;

    public Entry() {
    }

    public Entry(K key, V value) {
        this.key = key;
        this.value = value;
    }
}