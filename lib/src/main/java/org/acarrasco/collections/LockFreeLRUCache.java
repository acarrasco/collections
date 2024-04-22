package org.acarrasco.collections;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

import java.util.Iterator;

/**
 * An implementation of a LRU cache that is efficient for small capacity,
 * high number of concurrent threads and high latency of calculating values.
 * 
 * Evicting and adding new elements uses locks to avoid computing the same
 * value more than necessary if two concurrent calls attempt to fetch the
 * same missing key.
 * 
 * It uses sequential scans rather than a tree or a hashmap. The rationale
 * behind it is that for small collections simpler is usually faster, and
 * each container cell being independent of each other allows for better
 * parallelism.
 */
public class LockFreeLRUCache<K, V> implements ReadThroughCache<K, V> {

    class TickEntry extends Entry<K, V> {
        /**
         * Represent when was the element last accessed.
         */
        final long tick;

        public TickEntry(long tick, K key, V value) {
            super(key, value);
            this.tick = tick;
        }
    }

    /**
     * The function that will compute or fetch a value that is not in the cache.
     */
    private final Function<K, V> missingValueFactory;

    /**
     * Pairs of Key/Value that are present in the cache.
     */
    private final AtomicReferenceArray<TickEntry> entries;

    /**
     * Each access will increase the internal tick, that will be used as a
     * timestamp;
     */
    private final AtomicLong tick = new AtomicLong(0);

    private final int capacity;

    /**
     * A special timestamp value to flag entries that are being updated.
     */
    private static final int UPDATING = -1;

    /**
     * 
     * @param capacity            The maximum number of elements that this cache can
     *                            keep.
     * @param missingValueFactory The function that will compute missing values.
     * @param timestampFactory    The function that will compute timestamps.
     */
    public LockFreeLRUCache(
            int capacity,
            Function<K, V> missingValueFactory) {

        this.capacity = capacity;
        this.missingValueFactory = missingValueFactory;

        this.entries = new AtomicReferenceArray<>(capacity);
    }

    /**
     * Returns the value associated with a key in the cache.
     * 
     * If the key is not present in the cache, it will invoke the
     * missing value factory and store it in the cache, potentially
     * evicting other key.
     */
    public V apply(K key) {
        for (int i = 0; i < this.capacity; i++) {
            TickEntry entry = findAndUpdateTimestamp(i, key);
            if (entry != null) {
                return entry.value;
            }
        }

        try {
            return addElement(key);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private TickEntry findAndUpdateTimestamp(int idx, K key) {
        boolean success = false;
        TickEntry oldEntry;
        long newTimestamp;
        do {
            oldEntry = this.entries.get(idx);
            if (oldEntry == null) {
                return null;
            }
            if (oldEntry.tick == UPDATING) {
                return null;
            }
            if (!key.equals(oldEntry.key)) {
                return null;
            }
            newTimestamp = this.tick.getAndIncrement();
            TickEntry newEntry = new TickEntry(newTimestamp, oldEntry.key, oldEntry.value);
            success = this.entries.compareAndSet(idx, oldEntry, newEntry);
        } while (!success);
        return oldEntry;
    }

    private V addElement(K key) throws InterruptedException {
        int placementIdx;

        synchronized (this) {
            // we always have to check if the element exists because it could
            // have been added before we entered the synchronized region
            // do it while we look for the least recent element
            int leastRecentIdx = 0;
            long leastRecentTick = Long.MAX_VALUE;
            int i;
            for (i = 0; i < this.capacity; i++) {
                final TickEntry entry = this.entries.get(i);
                if (entry == null) {
                    break;
                }
                boolean alreadyInCache = key.equals(entry.key);

                if (alreadyInCache && entry.tick == UPDATING) {
                    this.wait();
                    // we need to start over from the beginning...
                    // the position of our key might have changed while we were waiting!
                    i = 0;
                    leastRecentIdx = 0;
                    leastRecentTick = Long.MAX_VALUE;
                } else if (alreadyInCache && entry.tick != UPDATING) {
                    final TickEntry newEntry = new TickEntry(this.tick.getAndIncrement(), entry.key, entry.value);
                    this.entries.set(i, newEntry);
                    return newEntry.value;
                } else if (entry.tick < leastRecentTick) {
                    leastRecentIdx = i;
                    leastRecentTick = entry.tick;
                }
            }

            if (i < this.capacity) {
                placementIdx = i;
            } else {
                placementIdx = leastRecentIdx;
            }
            // the `get` method won't check a key that is updating
            // so a concurrent access will call addElement and
            this.entries.set(placementIdx, new TickEntry(UPDATING, key, null));
        }

        // this is a potentially slow operation, we can do it outside of the
        // synchronized block because we have already "claimed" the spot
        final V value = this.missingValueFactory.apply(key);

        synchronized (this) {
            this.entries.set(placementIdx, new TickEntry(this.tick.getAndIncrement(), key, value));
            // notify other threads waiting for a value to be written
            this.notifyAll();
        }
        return value;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new Iterator<Entry<K,V>>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < capacity && entries.get(i) != null;
            }

            @Override
            public Entry<K, V> next() {
                return entries.get(i++);
            }
        };
    }
}
