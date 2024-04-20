package org.acarrasco.collections;

import org.junit.Test;
import static org.junit.Assert.*;

public class SynchronizedLRUCacheTest {
    

    @Test public void testCapacity10_loops10() {
        final int capacity = 10;
        final int loops = 10;
        ReadThroughCacheTestHelper.testSingleThreadOneGetPerItem(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
        ReadThroughCacheTestHelper.testSingleThreadAlwaysGetZero(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
        ReadThroughCacheTestHelper.testSingleThreadAlwaysLastElement(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
    }

    @Test public void testCapacity100_loops100() {
        final int capacity = 100;
        final int loops = 100;
        ReadThroughCacheTestHelper.testSingleThreadOneGetPerItem(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
        ReadThroughCacheTestHelper.testSingleThreadAlwaysGetZero(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
        ReadThroughCacheTestHelper.testSingleThreadAlwaysLastElement(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
    }


    @Test public void testCapacity100_loops10000() {
        final int capacity = 100;
        final int loops = 10000;
        ReadThroughCacheTestHelper.testSingleThreadOneGetPerItem(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
        ReadThroughCacheTestHelper.testSingleThreadAlwaysGetZero(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
        ReadThroughCacheTestHelper.testSingleThreadAlwaysLastElement(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops);
    }


    @Test public void testConcurrent_Threads2_Capacity2_loops10000() throws InterruptedException {
        final int threads = 2;
        final int capacity = 2;
        final int loops = 10000;
        ReadThroughCacheTestHelper.testMultiThreadedNGetsPerItem(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops, threads, 1);
    }


    @Test public void testConcurrent_Threads10_Capacity20_loops10000() throws InterruptedException {
        final int threads = 10;
        final int capacity = 20;
        final int loops = 10000;
        ReadThroughCacheTestHelper.testMultiThreadedNGetsPerItem(new SynchronizedLRUCache<>(capacity, (x) -> (x)), capacity, loops, threads, 1);
    }
}
