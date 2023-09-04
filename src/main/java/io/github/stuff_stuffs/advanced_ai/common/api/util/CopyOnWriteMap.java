package io.github.stuff_stuffs.advanced_ai.common.api.util;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.Set;

public class CopyOnWriteMap<K, V> {
    private static final VarHandle LOOKUPS_HANDLE;
    @SuppressWarnings("FieldMayBeFinal")
    private Map<K, V> lookups = new Object2ReferenceOpenHashMap<>();

    @Nullable
    public V get(final K key) {
        //noinspection unchecked
        return ((Map<K, V>) LOOKUPS_HANDLE.getAcquire(this)).get(key);
    }

    public Set<K> keys() {
        return lookups.keySet();
    }

    public V put(final K key, final V value) {
        Map<K, V> cur;
        Map<K, V> copy;
        V res;
        do {
            //noinspection unchecked
            cur = (Map<K, V>) LOOKUPS_HANDLE.getAcquire(this);
            copy = new Object2ReferenceOpenHashMap<>(cur);
            res = copy.put(key, value);
        } while (LOOKUPS_HANDLE.compareAndExchangeRelease(this, cur, copy) != cur);
        return res;
    }

    public synchronized V remove(final K key) {
        Map<K, V> cur;
        Map<K, V> copy;
        V res;
        do {
            //noinspection unchecked
            cur = (Map<K, V>) LOOKUPS_HANDLE.getAcquire(this);
            copy = new Object2ReferenceOpenHashMap<>(cur);
            res = copy.remove(key);
        } while (LOOKUPS_HANDLE.compareAndExchangeRelease(this, cur, copy) != cur);
        return res;
    }

    static {
        try {
            LOOKUPS_HANDLE = MethodHandles.lookup().findVarHandle(CopyOnWriteMap.class, "lookups", Map.class);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
