package org.acarrasco.collections;

import java.util.function.Function;
import java.util.HashMap;

public class SynchronizedLRUCache<K, V> implements ReadThroughCache<K, V> {

    /**
     * The function that will compute or fetch a value that is not in the cache.
     */
    private final Function<K, V> missingValueFactory;

    private final int capacity;

    private FixedLinkedList<Entry<K, V>> recencyList;

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
    public synchronized V get(K key) {
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
    public Iterable<Entry<K, V>> entries() {
        return this.recencyList;
    }

}
