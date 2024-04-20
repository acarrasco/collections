package org.acarrasco.collections;

import java.util.HashSet;

import static org.junit.Assert.*;

public class ReadThroughCacheTestHelper {

    /**
     * missingValueFactory should be the identity function.
     */
    public static void testSingleThreadOneGetPerItem(ReadThroughCache<Integer, Integer> cache, int capacity,
            int loops) {
        for (int i = 0; i < loops * capacity; i++) {
            int v = cache.get(i);
            assertEquals(i * i, v);
        }
        final HashSet<Integer> expected = new HashSet<>();
        for (int i = (loops - 1) * capacity; i < loops * capacity; i++) {
            expected.add(i * i);
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache.entries()) {
            result.add(entry.value);
        }

        assertEquals(expected, result);
    }

    /**
     * missingValueFactory should be the identity function.
     */
    public static void testSingleThreadAlwaysGetZero(ReadThroughCache<Integer, Integer> cache, int capacity,
            int loops) {
        for (int i = 0; i < loops * capacity; i++) {
            assertEquals(Integer.valueOf(0), cache.get(0));
            assertEquals(Integer.valueOf(i * i), cache.get(i));
        }
        final HashSet<Integer> expected = new HashSet<>();
        expected.add(0);
        for (int i = (loops - 1) * capacity + 1; i < loops * capacity; i++) {
            expected.add(i);
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache.entries()) {
            result.add(entry.value);
        }

        assertEquals(expected, result);
    }

    /**
     * missingValueFactory should be square.
     */
    public static void testSingleThreadAlwaysLastElement(ReadThroughCache<Integer, Integer> cache, int capacity,
            int loops) {
        for (int i = 0; i < loops * capacity; i++) {
            assertEquals(Integer.valueOf(i * i), cache.get(i));
            final int last = loops * capacity - 1;
            assertEquals(Integer.valueOf(last * last), cache.get(last));
        }
        final HashSet<Integer> expected = new HashSet<>();
        for (int i = (loops - 1) * capacity; i < loops * capacity; i++) {
            expected.add(i * i);
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache.entries()) {
            result.add(entry.value);
        }

        assertEquals(expected, result);
    }

    public static void testMultiThreadedNGetsPerItem(ReadThroughCache<Integer, Integer> cache, int capacity, int loops,
            int nThreads, int gets) {
        Thread[] threads = new Thread[nThreads];

        class ElementGetter implements Runnable {
            final int offset;

            public ElementGetter(int offset) {
                this.offset = offset;
            }

            @Override
            public void run() {
                final int end = offset + capacity * loops;
                for (int i = offset; i < end; i++) {
                    for (int j = 0; j < gets; j++) {
                        assertEquals(Integer.valueOf(i * i), cache.get(i));
                    }
                }
            }
        }

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new Thread(new ElementGetter(i * capacity * loops));
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
                final int v = (j + 1) * capacity * loops - i - 1;
                possibleValues.add(v * v);
            }
        }
        final HashSet<Integer> result = new HashSet<>();
        for (Entry<Integer, Integer> entry : cache.entries()) {
            result.add(entry.value);
        }

        final HashSet<Integer> difference = new HashSet<>(result);
        difference.removeAll(possibleValues);

        assertEquals(new HashSet<>(), difference);
    }

    public static void testMultiThreadedRandomKeys(ReadThroughCache<Integer, Integer> cache, int keySpace,
            int nThreads, int getsPerThread) {
        Thread[] threads = new Thread[nThreads];

        class ElementGetter implements Runnable {
            int seed = 1;
            final static long M = 1103515245;
            final static long C = 12345;

            @Override
            public void run() {
                for (int i = 0; i < getsPerThread; i++) {
                    seed = (int)((seed * M + C) & ((1L << 31) - 1));
                    int key = seed % keySpace;
                    assertEquals(Integer.valueOf(key * key), cache.get(key));
                }
            }
        }

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new Thread(new ElementGetter());
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
