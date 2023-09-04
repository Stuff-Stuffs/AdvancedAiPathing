package io.github.stuff_stuffs.advanced_ai.common.api.util;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class CopyOnWriteMap<K, V> {
    private volatile Map<K, V> lookups = new Object2ReferenceOpenHashMap<>();
    private final Object lock = new Object();

    @Nullable
    public V get(final K key) {
        return lookups.get(key);
    }

    public Set<K> keys() {
        return lookups.keySet();
    }

    public V put(final K key, final V value) {
        synchronized (lock) {
            final Map<K, V> lookupsCopy = new Object2ReferenceOpenHashMap<>(lookups);
            final V result = lookupsCopy.put(key, value);
            lookups = lookupsCopy;
            return result;
        }
    }

    public synchronized V remove(final K key) {
        synchronized (lock) {
            final Map<K, V> lookupsCopy = new Object2ReferenceOpenHashMap<>(lookups);
            final V result = lookupsCopy.remove(key);
            lookups = lookupsCopy;
            return result;
        }
    }
}
