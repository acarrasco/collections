package org.acarrasco.collections;

import java.util.function.Function;

public class CacheBenchmarkRandomPattern {
    public static void main(String[] args) {
        System.out.println("type\tthreads\tcapacity\tkeySpace\tfactoryDelay\tgetsPerThread\ttime");
        for (int threads = 2; threads <= 16; threads *= 2) {
            for (int capacity = 64; capacity <= 32768; capacity *= 8) {
                for (int keySpaceMult = 2; keySpaceMult <= 1000; keySpaceMult *= 4) {
                    for (int delay = 1; delay < 100; delay *= 4) {
                        final int keySpace = capacity * keySpaceMult;
                        double hitRate = Math.min(1.0, ((double) capacity / keySpace));
                        final int getsPerThread = (int)(50000 / ((1 - hitRate) * delay * delay));
                        long time;
                        time = testLockFree(capacity, threads, keySpace, getsPerThread, delay);
                        System.out.println(
                                "lockFree\t" + threads + "\t" + capacity + "\t" + keySpace + "\t" + delay + "\t"
                                        + getsPerThread + "\t" + time);
                        time = testSynchronized(capacity, threads, keySpace, getsPerThread, delay);
                        System.out.println(
                                "synchronized\t" + threads + "\t" + capacity + "\t" + keySpace + "\t" + delay + "\t"
                                        + getsPerThread + "\t" + time);
                    }
                }
            }
        }
    }

    static class MissingValueFactory implements Function<Integer, Integer> {

        final long delay;
        final long keySpace;

        public MissingValueFactory(long delay, int keySpace) {
            this.delay = delay;
            this.keySpace = keySpace;
        }

        @Override
        public Integer apply(Integer x) {
            try {
                long scaledDelay = delay * (x + 1) / keySpace;
                Thread.sleep(scaledDelay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return x * x;
        }
    }

    static long testLockFree(int capacity, int threads, int keySpace, int getsPerThread, long factoryDelay) {
        LockFreeLRUCache<Integer, Integer> cache = new LockFreeLRUCache<>(capacity,
                new MissingValueFactory(factoryDelay, keySpace));
        return timeIt(
                () -> ReadThroughCacheTestHelper.testMultiThreadedRandomKeys(cache, keySpace,
                        threads, getsPerThread));
    }

    static long testSynchronized(int capacity, int threads, int keySpace, int getsPerThread, long factoryDelay) {
        SynchronizedLRUCache<Integer, Integer> cache = new SynchronizedLRUCache<>(capacity,
                new MissingValueFactory(factoryDelay, keySpace));
        return timeIt(
                () -> ReadThroughCacheTestHelper.testMultiThreadedRandomKeys(cache, keySpace,
                        threads, getsPerThread));
    }

    static long timeIt(Runnable function) {
        final long start = System.currentTimeMillis();
        function.run();
        final long end = System.currentTimeMillis();
        return end - start;
    }
}
