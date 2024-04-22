package org.acarrasco.collections;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;

public class MappedCacheTest {

    @Test public void testApply() {

        MappedCache<Integer, Integer> cache = new MappedCache<>(2, 2, (x) -> x * x, SynchronizedLRUCache::new);
        for (int i = 0; i < 6; i++) {
            assertEquals(Integer.valueOf(i * i), cache.apply(i));
        }
        HashSet<Integer> expected = new HashSet<>();
        for (int i = 2; i < 6; i++) {
            expected.add(i * i);
        }
        HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache) {
            result.add(entry.value);
        }
        assertEquals(expected, result);
    }

}
