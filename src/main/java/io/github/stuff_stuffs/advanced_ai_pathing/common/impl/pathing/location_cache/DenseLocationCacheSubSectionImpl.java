package io.github.stuff_stuffs.advanced_ai_pathing.common.impl.pathing.location_cache;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.AdvancedAiPathingUtil;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.UniverseInfo;

public class DenseLocationCacheSubSectionImpl<T> implements LocationCacheSubSection<T> {
    private static final int SIZE = LocationCacheSectionRegistry.SIZE;
    private final long[] packed;
    private final int elementsPerLong;
    private final int bitsPerElement;
    private final UniverseInfo<T> universeInfo;

    public DenseLocationCacheSubSectionImpl(final PreLocationCacheSection<T> section, final UniverseInfo<T> info) {
        universeInfo = info;
        final int round = AdvancedAiPathingUtil.roundToUpPower2(universeInfo.size());
        final int size = 32 - Integer.numberOfLeadingZeros(round) - 1;
        bitsPerElement = size;
        elementsPerLong = 64 / size;
        packed = new long[((SIZE * SIZE * SIZE) + elementsPerLong - 1) / elementsPerLong];

        final long mask = (1L << bitsPerElement) - 1;
        for (int i = 0; i < SIZE * SIZE * SIZE; i++) {
            final int index = i / elementsPerLong;
            final int subIndex = i % elementsPerLong * bitsPerElement;
            final int valIndex = universeInfo.toIndex(section.get(i));
            packed[index] |= (valIndex & mask) << subIndex;
        }
    }

    @Override
    public T get(final int packed) {
        final int index = packed / elementsPerLong;
        final int subIndex = packed % elementsPerLong * bitsPerElement;
        final int mask = (1 << bitsPerElement) - 1;
        return universeInfo.fromIndex((int) (this.packed[index] >> subIndex) & mask);
    }
}

