package io.github.stuff_stuffs.advanced_ai_pathing.common.impl.pathing.location_cache;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.UniverseInfo;
import net.minecraft.util.math.ChunkSectionPos;

public class PreLocationCacheSectionImpl<T> implements PreLocationCacheSection<T> {
    private final Object[] vals;
    private final int[] counts;

    public PreLocationCacheSectionImpl(final LocationClassifier<T> classifier, final ChunkSectionPos pos, final int offX, final int offY, final int offZ, final ShapeCache cache) {
        vals = new Object[LocationCacheSectionRegistry.SIZE * LocationCacheSectionRegistry.SIZE * LocationCacheSectionRegistry.SIZE];
        final UniverseInfo<T> info = classifier.universeInfo();
        counts = new int[info.size()];
        final int xOff = pos.getMinX() + offX;
        final int yOff = pos.getMinY() + offY;
        final int zOff = pos.getMinZ() + offZ;
        for (int y = 0; y < LocationCacheSectionRegistry.SIZE; ++y) {
            for (int x = 0; x < LocationCacheSectionRegistry.SIZE; ++x) {
                for (int z = 0; z < LocationCacheSectionRegistry.SIZE; ++z) {
                    final T val = classifier.get(x + xOff, y + yOff, z + zOff, cache);
                    counts[info.toIndex(val)]++;
                    vals[LocationCacheSectionImpl.pack(x, y, z)] = val;
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
