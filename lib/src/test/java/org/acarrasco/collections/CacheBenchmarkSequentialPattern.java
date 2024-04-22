package org.acarrasco.collections;

import java.util.function.Function;
import java.util.Map;

public class CacheBenchmarkSequentialPattern {
    public static void main(String[] args) {
        final Map<String, ReadThroughCacheFactory> constructors = Map.of(
                "lockfree", LockFreeLRUCache::new,
                "synchronized", SynchronizedLRUCache::new);

        System.out.println("type\tthreads\tcapacity\tfactoryDelay\tkeySpace\trepeatedGets\ttime");
        for (int threadsPow = 1; threadsPow <= 4; threadsPow++) {
            for (int capacityPow = 6; capacityPow <= 14; capacityPow += 3) {
                for (int delay = 1; delay <= 25; delay *= 5) {
                    for (int getsPow = 0; getsPow <= 4; getsPow+=2) {
                        final int capacity = 1 << capacityPow;
                        final int threads = 1 << threadsPow;
                        final int repeatedGets = 1 << getsPow;
                        final int keySpaceMult = Math.max(1,
                                1000 / (capacity * threadsPow * delay * delay * (1 + getsPow)));

                        Function<Integer, Integer> missingValueFactory = new MissingValueFactory(delay);
                        for (Map.Entry<String, ReadThroughCacheFactory> constructor : constructors.entrySet()) {
                            final ReadThroughCache<Integer, Integer> cache = constructor.getValue().build(capacity,
                                    missingValueFactory);
                            final long time = timeIt(
                                    () -> AbstractReadThroughCacheTest.testMultiThreadedNGetsPerItem(cache, capacity,
                                            keySpaceMult, threads, repeatedGets));
                            System.out.println(
                                    constructor.getKey() + "\t" + threads + "\t" + capacity + "\t" + delay
                                            + "\t" + capacity * keySpaceMult + "\t" + repeatedGets + "\t" + time);
                        }
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

    static long timeIt(Runnable function) {
        final long start = System.currentTimeMillis();
        function.run();
        final long end = System.currentTimeMillis();
        return end - start;
    }
}
