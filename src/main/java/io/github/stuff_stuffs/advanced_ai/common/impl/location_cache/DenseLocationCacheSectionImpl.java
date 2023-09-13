package io.github.stuff_stuffs.advanced_ai.common.impl.location_cache;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.util.AiUtil;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.advanced_ai.common.internal.ProcessedLocationClassifier;
import net.minecraft.util.math.ChunkSectionPos;

public class DenseLocationCacheSectionImpl<T> implements LocationCacheSection<T> {
    private long[] modCounts;
    private final long[] packed;
    private final int elementsPerLong;
    private final int bitsPerElement;
    private final UniverseInfo<T> universeInfo;

    public DenseLocationCacheSectionImpl(final long[] modCounts, final ShapeCache cache, final ChunkSectionPos pos, final ProcessedLocationClassifier<T> classifier) {
        this.modCounts = modCounts;
        universeInfo = classifier.delegate.universeInfo();
        final int round = AiUtil.roundToUpPower2(universeInfo.size());
        final int size = 32 - Integer.numberOfLeadingZeros(round) - 1;
        bitsPerElement = size;
        elementsPerLong = 64 / size;
        packed = new long[(4096 + elementsPerLong - 1) / elementsPerLong];
        final int xOff = pos.getMinX();
        final int yOff = pos.getMinY() + cache.getBottomY();
        final int zOff = pos.getMinZ();

        final int mask = (1 << bitsPerElement) - 1;
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    final short packedCoord = LocationCacheSection.pack(i, j, k);
                    final long valIndex = classifier.invoke(xOff + i, yOff + j, zOff + k, cache);
                    final int index = packedCoord / elementsPerLong;
                    final int subIndex = packedCoord % elementsPerLong * bitsPerElement;
                    packed[index] |= (valIndex & mask) << subIndex;
                }
            }
        }
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
    public T get(final int packed) {
        final int index = packed / elementsPerLong;
        final int subIndex = packed % elementsPerLong * bitsPerElement;
        final int mask = (1 << bitsPerElement) - 1;
        return universeInfo.fromIndex((int) (this.packed[index] >> subIndex) & mask);
    }
}

