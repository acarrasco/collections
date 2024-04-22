package org.acarrasco.collections;

import java.util.function.Function;

public class SynchronizedLRUCacheTest extends AbstractReadThroughCacheTest {

    @Override
    public ReadThroughCache<Integer, Integer> buildCache(Function<Integer, Integer> missingValueFactory, int capacity) {
        return new SynchronizedLRUCache<>(capacity, missingValueFactory);
    }
    
}
