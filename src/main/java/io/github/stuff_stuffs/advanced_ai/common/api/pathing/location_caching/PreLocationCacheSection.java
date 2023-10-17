package io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching;

import io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache.LocationCacheSectionImpl;

public interface PreLocationCacheSection<T> {
    T get(int index);

    int count(int typeIndex);

    default T get(final int x, final int y, final int z) {
        return get(LocationCacheSectionImpl.pack(x, y, z));
    }
}
