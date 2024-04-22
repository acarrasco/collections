package org.acarrasco.collections;

import java.util.Iterator;
import java.util.function.Function;

public class MappedCache<K, V> implements ReadThroughCache<K, V> {

    private final ReadThroughCache<K, V>[] buckets;

    @SuppressWarnings("unchecked")
    public MappedCache(int buckets, int bucketCapacity, Function<K, V> missingValueFactory,
            ReadThroughCacheFactory cacheFactory) {

        this.buckets = new ReadThroughCache[buckets];
        for (int i = 0; i < buckets; i++) {
            this.buckets[i] = cacheFactory.build(bucketCapacity, missingValueFactory);
        }
    }

    @Override
    public V apply(K key) {
        final int bucket = key.hashCode() % this.buckets.length;
        return this.buckets[bucket].apply(key);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new Iterator<Entry<K,V>>() {
            int bucket = 0;
            Iterator<Entry<K, V>> bucketIterator = buckets[0].iterator();
            @Override
            public boolean hasNext() {
                while (!bucketIterator.hasNext() && bucket + 1 < buckets.length) {
                    bucket++;
                    bucketIterator = buckets[bucket].iterator();
                }
                return bucketIterator.hasNext();
            }
            @Override
            public Entry<K, V> next() {
                return bucketIterator.next();
            }
        };
    }
}
