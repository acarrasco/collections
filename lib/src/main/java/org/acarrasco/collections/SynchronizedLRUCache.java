package org.acarrasco.collections;

import java.util.function.Function;
import java.util.HashMap;
import java.util.Iterator;

/**
 * An implementation of a LRU cache that is efficient for large capacity.
 */
public class SynchronizedLRUCache<K, V> implements ReadThroughCache<K, V> {

    /**
     * The function that will compute or fetch a value that is not in the cache.
     */
    private final Function<K, V> missingValueFactory;

    /**
     * The maximum number of elements this cache can hold.
     */
    private final int capacity;

    /**
     * The data structure that keeps the order of last access.
     */
    private FixedLinkedList<Entry<K, V>> recencyList;

    /**
     * Associates the keys to their entries in the recencyList, so the
     * cost of moving an entry to the head is O(1).
     */
    private HashMap<K, FixedLinkedList<Entry<K, V>>.Node> keyIndex;

    public SynchronizedLRUCache(
            int capacity,
            Function<K, V> missingValueFactory) {

        this.capacity = capacity;
        this.missingValueFactory = missingValueFactory;

        this.recencyList = new FixedLinkedList<>(capacity);
        this.keyIndex = new HashMap<>();
    }

    @Override
    public synchronized V apply(K key) {
        Entry<K, V> entry;
        FixedLinkedList<Entry<K, V>>.Node node = keyIndex.get(key);

        if (node != null) {
            entry = node.value;
            node.remove();
        } else {
            if (this.keyIndex.size() < this.capacity) {
                entry = new Entry<>();
            } else {
                node = this.recencyList.head();
                entry = node.value;
                this.keyIndex.remove(entry.key);
                node.remove();
            }
            entry.key = key;
            entry.value = this.missingValueFactory.apply(key);    
        } 

        node = this.recencyList.add(entry);
        this.keyIndex.put(key, node);
        return entry.value;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return this.recencyList.iterator();
    }

}
