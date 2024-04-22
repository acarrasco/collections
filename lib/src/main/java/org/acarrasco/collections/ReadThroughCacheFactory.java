package org.acarrasco.collections;

import java.util.function.Function;

public interface ReadThroughCacheFactory {
    public <K, V> ReadThroughCache<K, V> build(int capacity, Function<K, V> missingValueFactory);
}
