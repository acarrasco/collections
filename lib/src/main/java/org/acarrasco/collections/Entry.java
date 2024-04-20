package org.acarrasco.collections;

/**
 * A mutable container for Key-Value pairs.
 */
public class Entry<K, V> {
    volatile K key;
    volatile V value;
}