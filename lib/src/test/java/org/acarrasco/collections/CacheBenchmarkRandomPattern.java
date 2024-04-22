package org.acarrasco.collections;

import java.util.function.Function;
import java.util.Map;

public class CacheBenchmarkRandomPattern {
    public static void main(String[] args) {
        final Map<String, ReadThroughCacheFactory> constructors = Map.of(
                "lockfree", LockFreeLRUCache::new,
                "synchronized", SynchronizedLRUCache::new);

        System.out.println("type\tthreads\tcapacity\tfactoryDelay\tkeySpace\tgetsPerThread\ttime");
        for (int threadsPow = 1; threadsPow < 5; threadsPow++) {
            for (int capacity = 64; capacity <= 32768; capacity *= 8) {
                for (int delay = 1; delay <= 25; delay *= 5) {
                    for (int keySpaceMult = 1; keySpaceMult <= 256; keySpaceMult *= 4) {
                        final int threads = 1 << threadsPow;
                        final int keySpace = capacity * keySpaceMult - 1;
                        final int getsPerThread = (int) (10000 / (delay * delay));

                        Function<Integer, Integer> missingValueFactory = new MissingValueFactory(delay, keySpace);
                        for (Map.Entry<String, ReadThroughCacheFactory> constructor : constructors.entrySet()) {
                            final ReadThroughCache<Integer, Integer> cache = constructor.getValue().build(capacity,
                                    missingValueFactory);
                            final long time = timeIt(
                                    () -> AbstractReadThroughCacheTest.testMultiThreadedRandomKeys(cache, keySpace,
                                            threads, getsPerThread));
                            System.out.println(constructor.getKey() + "\t" + threads + "\t" + capacity + "\t" + keySpace
                                    + "\t" + delay + "\t"
                                    + getsPerThread + "\t" + time);
                        }
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

    static long timeIt(Runnable function) {
        final long start = System.currentTimeMillis();
        function.run();
        final long end = System.currentTimeMillis();
        return end - start;
    }
}
