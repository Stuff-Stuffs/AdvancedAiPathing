package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.util.AiUtil;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;

public class DenseLocationCacheSectionImpl<T> implements LocationCacheSection<T> {
    private long[] modCounts;
    private final long[] packed;
    private final int elementsPerLong;
    private final int bitsPerElement;
    private final UniverseInfo<T> universeInfo;

    public DenseLocationCacheSectionImpl(final long[] modCounts, final PreLocationCacheSection<T> section, final LocationClassifier<T> classifier) {
        this.modCounts = modCounts;
        universeInfo = classifier.universeInfo();
        final int round = AiUtil.roundToUpPower2(universeInfo.size());
        final int size = 32 - Integer.numberOfLeadingZeros(round) - 1;
        bitsPerElement = size;
        elementsPerLong = 64 / size;
        packed = new long[(4096 + elementsPerLong - 1) / elementsPerLong];

        final long mask = (1L << bitsPerElement) - 1;
        for (int i = 0; i < 16 * 16 * 16; i++) {
            final int index = i / elementsPerLong;
            final int subIndex = i % elementsPerLong * bitsPerElement;
            final int valIndex = universeInfo.toIndex(section.get(i));
            packed[index] |= (valIndex & mask) << subIndex;
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

