package org.acarrasco.collections;

import java.util.HashSet;
import java.util.function.Function;

import org.junit.Test;

import static org.junit.Assert.*;

public abstract class AbstractReadThroughCacheTest {

    final static Function<Integer, Integer> missingValueFactory = (x) -> (x*x);

    public abstract ReadThroughCache<Integer, Integer> buildCache(Function<Integer, Integer> missingValueFactory, int capacity);

    @Test public void testSingleThreadOneGetPerItem_capacity_1_loops_10() {
        AbstractReadThroughCacheTest.testSingleThreadOneGetPerItem(buildCache(missingValueFactory, 1), 1, 10);
    }

    @Test public void testSingleThreadOneGetPerItem_capacity_10_loops_100() {
        AbstractReadThroughCacheTest.testSingleThreadOneGetPerItem(buildCache(missingValueFactory, 10), 10, 100);
    }

    @Test public void testSingleThreadAlwaysGetZero_capacity_2_loops_10() {
        AbstractReadThroughCacheTest.testSingleThreadAlwaysGetZero(buildCache(missingValueFactory, 2), 2, 10);
    }

    @Test public void testSingleThreadAlwaysGetZero_capacity_10_loops_100() {
        AbstractReadThroughCacheTest.testSingleThreadAlwaysGetZero(buildCache(missingValueFactory, 10), 10, 100);
    }

    @Test public void testSingleThreadAlwaysLastElement_capacity_2_loops_10() {
        AbstractReadThroughCacheTest.testSingleThreadAlwaysLastElement(buildCache(missingValueFactory, 2), 2, 10);
    }

    @Test public void testSingleThreadAlwaysLastElement_capacity_10_loops_100() {
        AbstractReadThroughCacheTest.testSingleThreadAlwaysLastElement(buildCache(missingValueFactory, 10), 10, 100);
    }

    @Test public void testMultiThreadedNGetsPerItem_capacity_1_gets_5_loops_100_threads_2() {
        AbstractReadThroughCacheTest.testMultiThreadedNGetsPerItem(buildCache(missingValueFactory, 1), 1, 100, 2, 5);
    }

    @Test public void testMultiThreadedNGetsPerItem_capacity_10_gets_2_loops_100_threads_16() {
        AbstractReadThroughCacheTest.testMultiThreadedNGetsPerItem(buildCache(missingValueFactory, 10), 10, 100, 16, 2);
    }

    @Test public void testMultiThreadedRandomKeys_capacity_100_keyspace_500_getsPerThread_500_threads_8() {
        AbstractReadThroughCacheTest.testMultiThreadedRandomKeys(buildCache(missingValueFactory, 100), 500, 8, 500);
    }

    public static void testSingleThreadOneGetPerItem(ReadThroughCache<Integer, Integer> cache, int capacity,
            int loops) {
        for (int i = 0; i < loops * capacity; i++) {
            int v = cache.apply(i);
            assertEquals(i * i, v);
        }
        final HashSet<Integer> expected = new HashSet<>();
        for (int i = (loops - 1) * capacity; i < loops * capacity; i++) {
            expected.add(i * i);
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache) {
            result.add(entry.value);
        }

        assertEquals(expected, result);
    }

    public static void testSingleThreadAlwaysGetZero(ReadThroughCache<Integer, Integer> cache, int capacity,
            int loops) {
        for (int i = 0; i < loops * capacity; i++) {
            assertEquals(Integer.valueOf(0), cache.apply(0));
            assertEquals(Integer.valueOf(i * i), cache.apply(i));
        }
        final HashSet<Integer> expected = new HashSet<>();
        expected.add(0);
        for (int i = (loops - 1) * capacity + 1; i < loops * capacity; i++) {
            expected.add(i);
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache) {
            result.add(entry.key);
        }

        assertEquals(expected, result);
    }

    public static void testSingleThreadAlwaysLastElement(ReadThroughCache<Integer, Integer> cache, int capacity,
            int loops) {
        for (int i = 0; i < loops * capacity; i++) {
            assertEquals(Integer.valueOf(i * i), cache.apply(i));
            final int last = loops * capacity - 1;
            assertEquals(Integer.valueOf(last * last), cache.apply(last));
        }
        final HashSet<Integer> expected = new HashSet<>();
        for (int i = (loops - 1) * capacity; i < loops * capacity; i++) {
            expected.add(i);
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache) {
            result.add(entry.key);
        }

        assertEquals(expected, result);
    }

    public static void testMultiThreadedNGetsPerItem(ReadThroughCache<Integer, Integer> cache, int capacity, int keySpaceMult,
            int nThreads, int gets) {
        Thread[] threads = new Thread[nThreads];

        class ElementGetter implements Runnable {
            final int offset;

            public ElementGetter(int offset) {
                this.offset = offset;
            }

            @Override
            public void run() {
                final int end = offset + capacity * keySpaceMult;
                for (int i = offset; i < end; i++) {
                    for (int j = 0; j < gets; j++) {
                        assertEquals(Integer.valueOf(i * i), cache.apply(i));
                    }
                }
            }
        }

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new Thread(new ElementGetter(i * capacity * keySpaceMult));
        }
        for (int i = 0; i < nThreads; i++) {
            threads[i].start();
        }

        for (int i = 0; i < nThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        final HashSet<Integer> possibleValues = new HashSet<>();
        for (int i = 0; i < capacity; i++) {
            for (int j = 0; j < nThreads; j++) {
                final int v = (j + 1) * capacity * keySpaceMult - i - 1;
                possibleValues.add(v * v);
            }
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache) {
            result.add(entry.value);
        }

        final HashSet<Integer> difference = new HashSet<>(result);
        difference.removeAll(possibleValues);

        assertEquals(new HashSet<>(), difference);
    }

    public static void testMultiThreadedRandomKeys(ReadThroughCache<Integer, Integer> cache, int keySpace,
            int nThreads, int getsPerThread) {
        Thread[] threads = new Thread[nThreads];

        /**
         * Simplest thread local CLG, it should be fast to not cause
         * too much interference with time measurements.
         */
        class ElementGetter implements Runnable {
            int seed;
            final static long M = 1103515245;
            final static long C = 12345;

            public ElementGetter(int seed) {
                this.seed = seed;
            }

            @Override
            public void run() {
                for (int i = 0; i < getsPerThread; i++) {
                    seed = (int) ((seed * M + C) & ((1L << 31) - 1));
                    int key = seed % keySpace;
                    assertEquals(Integer.valueOf(key * key), cache.apply(key));
                }
            }
        }

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new Thread(new ElementGetter(i));
        }
        for (int i = 0; i < nThreads; i++) {
            threads[i].start();
        }

        for (int i = 0; i < nThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
