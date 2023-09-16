package io.github.stuff_stuffs.advanced_ai.common.api.util;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.Map;

public class CopyOnWriteMap<K, V> {
    private static final VarHandle DELEGATE_HANDLE;
    @SuppressWarnings("FieldMayBeFinal")
    private Map<K, V> delegate = Collections.emptyMap();

    @Nullable
    public V get(final K key) {
        //noinspection unchecked
        return ((Map<K, V>) DELEGATE_HANDLE.getAcquire(this)).get(key);
    }

    public V put(final K key, final V value) {
        Map<K, V> cur;
        Map<K, V> copy;
        V res;
        do {
            //noinspection unchecked
            cur = (Map<K, V>) DELEGATE_HANDLE.getAcquire(this);
            copy = new Object2ReferenceOpenHashMap<>(cur);
            res = copy.put(key, value);
        } while (DELEGATE_HANDLE.compareAndExchangeRelease(this, cur, copy) != cur);
        return res;
    }

    public V remove(final K key) {
        Map<K, V> cur;
        Map<K, V> copy;
        V res;
        do {
            //noinspection unchecked
            cur = (Map<K, V>) DELEGATE_HANDLE.getAcquire(this);
            if (!cur.containsKey(key)) {
                return null;
            }
            if (cur.size() == 1) {
                copy = Collections.emptyMap();
                res = cur.get(key);
            } else {
                copy = new Object2ReferenceOpenHashMap<>(cur);
                res = copy.remove(key);
            }
        } while (DELEGATE_HANDLE.compareAndExchangeRelease(this, cur, copy) != cur);
        return res;
    }

    public void clear() {
        Map<K, V> cur;
        do {
            //noinspection unchecked
            cur = (Map<K, V>) DELEGATE_HANDLE.getAcquire(this);
        } while (DELEGATE_HANDLE.compareAndExchangeRelease(this, cur, Collections.emptyMap()) != cur);
    }

    static {
        try {
            DELEGATE_HANDLE = MethodHandles.lookup().findVarHandle(CopyOnWriteMap.class, "delegate", Map.class);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
