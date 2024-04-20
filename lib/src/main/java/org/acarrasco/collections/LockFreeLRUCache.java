package org.acarrasco.collections;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.Arrays;

/**
 * The code isn't going to win any beauty contests, it is just a proof of
 * concept to see if it is worth to use CAS operations instead of locking
 * to increase the read throughput in a read-through LRU cache.
 * 
 * Evicting and adding new elements uses locks to avoid computing the same
 * value more than necessary if two concurrent calls attempt to fetch the
 * same missing key.
 * 
 * It uses sequential scans rather than a tree or a hashmap. The rationale
 * behind it is that for small collections simpler is usually faster.
 */
public class LockFreeLRUCache<K, V> implements ReadThroughCache<K, V> {

    /**
     * The function that will compute or fetch a value that is not in the cache.
     */
    private final Function<K, V> missingValueFactory;

    /**
     * Pairs of Key/Value that are present in the cache.
     */
    private final Entry<K, V>[] entries;

    /**
     * The last access timestamp for each entry.
     */
    private final AtomicLong[] timestamps;

    /**
     * Each access will increase the internal tick, that will be used as a timestamp;
     */
    private final AtomicLong tick = new AtomicLong(0);

    /**
     * How many elements are in the cache. No entries will be evicted until
     * the size reaches the capacity of the cache.
     */
    private int size = 0;

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
    @SuppressWarnings("unchecked")
    public LockFreeLRUCache(
            int capacity,
            Function<K, V> missingValueFactory) {

        this.missingValueFactory = missingValueFactory;

        this.entries = new Entry[capacity];
        this.timestamps = new AtomicLong[capacity];

        for (int i = 0; i < capacity; i++) {
            this.entries[i] = new Entry<>();
            this.timestamps[i] = new AtomicLong();
        }
    }

    /**
     * Returns the value associated with a key in the cache.
     * 
     * If the key is not present in the cache, it will invoke the
     * missing value factory and store it in the cache, potentially
     * evicting other key.
     */
    public V get(K key) {
        Entry<K, V> found = new Entry<>();

        for (int i = 0; i < this.size; i++) {
            if (findAndUpdateTimestamp(i, key, found)) {
                return found.value;
            }
        }

        try {
            return addElement(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if key entry matches the requested key, and if so updates the
     * access timestamp atomically.
     * 
     * If the key was evicted after comparing it, the CAS update of the timestamp
     * will fail and it will be re-checked.
     * 
     * @param idx    The index of the entry to check.
     * @param key    The key to check
     * @param result Will contain the value of the entry.
     * @return `true` if the key matched the entry `idx`, `false` if it didn't
     *         match.
     */
    private boolean findAndUpdateTimestamp(int idx, K key, Entry<K, V> result) {
        boolean success = false;
        long newTimestamp;
        long previousTimestamp;
        do {
            previousTimestamp = this.timestamps[idx].get();
            if (previousTimestamp == UPDATING) {
                return false;
            }
            
            // copy the values so they can't change after the atomic update of timestamp
            result.key = this.entries[idx].key;
            result.value = this.entries[idx].value;

            if (!key.equals(result.key)) {
                return false;
            } else {
                newTimestamp = this.tick.getAndIncrement();
                success = timestamps[idx].compareAndSet(previousTimestamp, newTimestamp);
            }
        } while (!success);
        return true;
    }

    private V addElement(K key) throws InterruptedException {
        int placementIdx;

        synchronized (this) {
            // we always have to check if the element exists because it could
            // have been added before we entered the synchronized region
            // do it while we look for the least recent element
            int leastRecentIdx = 0;
            long leastRecentTimestamp = Long.MAX_VALUE;
            int i;
            for (i = 0; i < this.size; i++) {

                boolean alreadyInCache = this.entries[i].key.equals(key);
                final long candidateTimestamp = this.timestamps[i].get();

                if (alreadyInCache) {
                    if (candidateTimestamp == UPDATING) {
                        this.wait();
                        // we need to start over from the beginning...
                        // the position of our key might have changed while we were waiting!
                        return this.addElement(key);
                    } else {
                        return this.entries[i].value;
                    }
                }

                if (candidateTimestamp < leastRecentTimestamp) {
                    leastRecentIdx = i;
                    leastRecentTimestamp = candidateTimestamp;
                }
            }

            if (this.size < this.entries.length) {
                placementIdx = this.size;
                this.size++;
            } else {
                placementIdx = leastRecentIdx;
            }
            // the `get` method won't check a key that is updating
            // so a concurrent access will call addElement and
            this.timestamps[placementIdx].set(UPDATING);
            this.entries[placementIdx].key = key;
        }

        // this is a potentially slow operation, we can do it outside of the
        // synchronized block because we have already "claimed" the spot
        final V value = this.missingValueFactory.apply(key);

        synchronized (this) {
            this.entries[placementIdx].value = value;
            final long currentTimestamp = this.tick.getAndIncrement();

            this.timestamps[placementIdx].set(currentTimestamp);
            // notify other threads waiting for a value to be written
            this.notifyAll();
        }

        return value;
    }

    @Override
    public Iterable<Entry<K, V>> entries() {
        return Arrays.asList(this.entries).subList(0, this.size);
    }
}
