package org.acarrasco.collections;

import java.util.function.Function;


public class LockFreeLRUCacheTest extends AbstractReadThroughCacheTest {

    @Override
    public ReadThroughCache<Integer, Integer> buildCache(Function<Integer, Integer> missingValueFactory, int capacity) {
        return new LockFreeLRUCache<>(capacity, missingValueFactory);
    }
}
