package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;

public class LocationCacheSectionImpl<T> implements LocationCacheSection<T> {
    private static final int SIZE = LocationCacheSectionRegistry.SIZE;
    private static final int LOWER_BITS = Integer.numberOfTrailingZeros(SIZE);
    private static final int Z_SIG_MASK = 15 & (-1 << LOWER_BITS);
    private static final int Y_SIG_MASK = Z_SIG_MASK << 4;
    private static final int X_SIG_MASK = Z_SIG_MASK << 8;
    private static final int Z_SIG_SHIFT = LOWER_BITS;
    private static final int Y_SIG_SHIFT = 4 + LOWER_BITS - (4 - LOWER_BITS);
    private static final int X_SIG_SHIFT = 8 + LOWER_BITS - 2 * (4 - LOWER_BITS);
    private static final int Z_LOWER_MASK = ((1 << LOWER_BITS) - 1);
    private static final int Y_LOWER_MASK = ((1 << LOWER_BITS) - 1) << 4;
    private static final int X_LOWER_MASK = ((1 << LOWER_BITS) - 1) << 8;
    private static final int Z_LOWER_SHIFT = 0;
    private static final int Y_LOWER_SHIFT = 4 - LOWER_BITS;
    private static final int X_LOWER_SHIFT = 8 - (LOWER_BITS * 2);
    private long[] modCounts;
    private final LocationCacheSubSection<T>[] subSections;

    public LocationCacheSectionImpl(final long[] counts, final LocationClassifier<T> classifier, final ChunkSectionPos pos, final ShapeCache cache) {
        modCounts = counts;
        final int sectionsPerAxis = 16 / SIZE;
        subSections = new LocationCacheSubSection[sectionsPerAxis * sectionsPerAxis * sectionsPerAxis];
        for (int i = 0; i < sectionsPerAxis; i++) {
            for (int j = 0; j < sectionsPerAxis; j++) {
                for (int k = 0; k < sectionsPerAxis; k++) {
                    final PreLocationCacheSection<T> pre = new PreLocationCacheSectionImpl<>(classifier, pos, i * SIZE, j * SIZE, k * SIZE, cache);
                    final LocationCacheSectionRegistry.Factory factory = LocationCacheSectionRegistry.findBest(pre, classifier.universeInfo());
                    subSections[(i * sectionsPerAxis + j) * sectionsPerAxis + k] = factory.create(pre, classifier.universeInfo());
                }
            }
        }
    }

    public static int pack(final int x, final int y, final int z) {
        return ((x & Z_LOWER_MASK) << (2 * LOWER_BITS)) | ((y & Z_LOWER_MASK) << LOWER_BITS) | (z & Z_LOWER_MASK);
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
        final int x = (index & X_SIG_MASK) >>> X_SIG_SHIFT;
        final int y = (index & Y_SIG_MASK) >>> Y_SIG_SHIFT;
        final int z = (index & Z_SIG_MASK) >>> Z_SIG_SHIFT;
        final int lowerX = ((index & X_LOWER_MASK) >>> X_LOWER_SHIFT);
        final int lowerY = ((index & Y_LOWER_MASK) >>> Y_LOWER_SHIFT);
        final int lowerZ = ((index & Z_LOWER_MASK) >>> Z_LOWER_SHIFT);
        return subSections[x | y | z].get(lowerX | lowerY | lowerZ);
    }
}
