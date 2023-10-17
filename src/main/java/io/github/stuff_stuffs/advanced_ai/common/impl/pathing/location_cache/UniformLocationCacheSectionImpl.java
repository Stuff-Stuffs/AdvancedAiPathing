package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache;

public class UniformLocationCacheSectionImpl<T> implements LocationCacheSubSection<T> {
    private final T val;

    public UniformLocationCacheSectionImpl(final T val) {
        this.val = val;
    }

    @Override
    public T get(final int index) {
        return val;
    }
}
