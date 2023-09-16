package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;

public class UniformLocationCacheSectionImpl<T> implements LocationCacheSection<T> {
    private long[] modCounts;
    private final T val;

    public UniformLocationCacheSectionImpl(final long[] counts, final T val) {
        modCounts = counts;
        this.val = val;
    }

    @Override
    public long[] modCounts() {
        return modCounts;
    }

    @Override
    public void modCounts(final long[] modCounts) {
        this.modCounts = modCounts;
    }

    @Override
    public T get(final int index) {
        return val;
    }
}
