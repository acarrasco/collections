# Benchmark results

There are two different benchmarks: one for sequential access patterns, and another for random access patterns.

They have some common parameters described below:

- **threads**: number of concurrent threads accessing the cache.
- **capacity**: maximum number of entries that the cache can hold before start to evicting.
- **delay**: the amount of milliseconds that the *missing value factory* takes before returning a value for a missing key.
- **keySpace**: number of elements in the key space.

## Sequential access pattern benchmark

Each thread has a contiguous partition of the key space, and performs n consecutive reads of the same key before
continuing with the next key.

This benchmark should reflect scenarios with deterministic high access locality.

It has the following specific parameters:
- **repeatedGets**: number of times that a thread will repeatedly fetch the same value.

The total number of get operations performed by all threads in a scenario is determined by:
`keySpace * repeatedGets`

## Random access pattern benchmark

Each thread reads a pseudorandom key, with no predefined correlation. The factory delay is deterministcally
scaled by the key value in proportion to the key space, to simulate that some values take more time to
be computed than others.

It has the following specific paramters:

- **getsPerThread**: the total number of gets that each thread will perform.

The total number of get operations performed by all threads in a scenario is determined by:
`threads * getsPerThread`

## Results interpretation

This is how the parameters affeact each cache in relation to each other:

- **threads**: More threads is better for LockFree (threads only block on write)
- **capacity**: More capacity is better for Synchronized (constant access time)
- **delay**: Higher delay is better for LockFree (lock is released during missing value retrieval)
- **keySpace**: Bigger keySpace is better for Synchronized (a cache miss is less expensive)

## Summary

The LRU cache implemented with atomic references is promising for scenarios with lots
of threads, small capacity, and very expensive to compute values. The shortcoming
of small capacity can be overcome with a higher number of buckets in a set-associative
mapping.

## Further improvements

With some tweaks the synchronized implementation could also free the lock while
retrieving missing values.

The lock-free implementation could also be improved by keeping a pool of entries, so
they can be reused and thus alleviate the pressure on the garbage collector (in a similar
way as the FixedLinkedList implementation).
