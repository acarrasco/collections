package org.acarrasco.collections;

import java.util.function.Function;

public class CacheBenchmarkSequentialPattern {
    public static void main(String[] args) {
        System.out.println("type\tthreads\tcapacity\tgets\tfactoryDelay\tloops\ttime");
        for (int threadsPow = 1; threadsPow <= 4; threadsPow++) {
            for (int capacity = 2048; capacity <= 4096; capacity *= 8) {
                for (int getsPow = 0; getsPow <= 4; getsPow++) {
                    for (int delay = 1; delay < 100; delay *= 4) {
                        final int threads = 1 << threadsPow;
                        final int gets = 1 << getsPow;
                        final int loops = Math.max(1, 1000 / (capacity * threadsPow * delay * (1 + getsPow)));
                        long time;
                        time = testLockFree(capacity, threads, loops, gets, delay);
                        System.out.println(
                                "lockFree\t" + threads + "\t" + capacity + "\t" + gets + "\t" + delay + "\t" + loops + "\t" + time);
                        time = testSynchronized(capacity, threads, loops, gets, delay);
                        System.out.println(
                                "synchronized\t" + threads + "\t" + capacity + "\t" + gets + "\t" + delay + "\t" + loops + "\t" + time);
                    }
                }
            }
        }
    }

    static class MissingValueFactory implements Function<Integer, Integer> {

        final long delay;

        public MissingValueFactory(long delay) {
            this.delay = delay;
        }

        @Override
        public Integer apply(Integer x) {
            try {
                Thread.sleep(this.delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return x * x;
        }
    }

    static long testLockFree(int capacity, int threads, int loops, int gets, long factoryDelay) {
        LockFreeLRUCache<Integer, Integer> cache = new LockFreeLRUCache<>(capacity,
                new MissingValueFactory(factoryDelay));
        return timeIt(
                () -> ReadThroughCacheTestHelper.testMultiThreadedNGetsPerItem(cache, capacity, loops, threads, gets));
    }

    static long testSynchronized(int capacity, int threads, int loops, int gets, long factoryDelay) {
        SynchronizedLRUCache<Integer, Integer> cache = new SynchronizedLRUCache<>(capacity,
                new MissingValueFactory(factoryDelay));
        return timeIt(
                () -> ReadThroughCacheTestHelper.testMultiThreadedNGetsPerItem(cache, capacity, loops, threads, gets));
    }

    static long timeIt(Runnable function) {
        final long start = System.currentTimeMillis();
        function.run();
        final long end = System.currentTimeMillis();
        return end - start;
    }
}
