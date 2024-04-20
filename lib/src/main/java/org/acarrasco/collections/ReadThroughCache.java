package org.acarrasco.collections;

public interface ReadThroughCache<K, V> {

    public V get(K key);

    public Iterable<Entry<K, V>> entries();

}