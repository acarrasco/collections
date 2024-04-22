package org.acarrasco.collections;

import java.util.function.Function;

/**
 * Rather than declaring a `get` method we use `apply` to implement the general `Function`
 * interface, so they are composable and reusable with any functional framework.
 */
public interface ReadThroughCache<K, V> extends Function<K, V>, Iterable<Entry<K,V>> {
    
}