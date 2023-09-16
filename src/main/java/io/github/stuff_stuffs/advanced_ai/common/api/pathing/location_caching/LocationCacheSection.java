package io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching;

public interface LocationCacheSection<T> {
    long[] modCounts();

    void modCounts(long[] modCounts);

    T get(int index);

    default T get(final int x, final int y, final int z) {
        return get(pack(x, y, z));
    }

    static short pack(final int x, final int y, final int z) {
        return (short) ((x & 15) << 8 | (y & 15) << 4 | z & 15);
    }

    static int unpackX(final int packed) {
        return packed >> 8 & 15;
    }

    static int unpackY(final int packed) {
        return packed >> 4 & 15;
    }

    static int unpackZ(final int packed) {
        return packed & 15;
    }

    static int modCountIndex(final int x, final int y, final int z) {
        return ((x + 1) * 3 + z + 1) * 3 + y + 1;
    }
}
