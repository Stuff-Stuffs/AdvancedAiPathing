package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import net.minecraft.util.math.ChunkSectionPos;

public class PreLocationCacheSectionImpl<T> implements PreLocationCacheSection<T> {
    private final Object[] vals;
    private final int[] counts;

    public PreLocationCacheSectionImpl(final LocationClassifier<T> classifier, final ChunkSectionPos pos, final ShapeCache cache) {
        vals = new Object[16 * 16 * 16];
        final UniverseInfo<T> info = classifier.universeInfo();
        counts = new int[info.size()];
        final int xOff = pos.getMinX();
        final int yOff = pos.getMinY();
        final int zOff = pos.getMinZ();
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    final T val = classifier.get(i + xOff, j + yOff, k + zOff, cache);
                    counts[info.toIndex(val)]++;
                    vals[LocationCacheSection.pack(i, j, k)] = val;
                }
            }
        }
    }

    @Override
    public int count(final int typeIndex) {
        return counts[typeIndex];
    }

    @Override
    public T get(final int index) {
        return (T) vals[index];
    }
}
