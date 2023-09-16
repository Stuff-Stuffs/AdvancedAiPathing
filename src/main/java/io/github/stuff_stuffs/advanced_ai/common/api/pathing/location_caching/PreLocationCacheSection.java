package io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching;

public interface PreLocationCacheSection<T> {
    T get(int index);

    int count(int typeIndex);

    default T get(final int x, final int y, final int z) {
        return get(LocationCacheSection.pack(x, y, z));
    }
}
