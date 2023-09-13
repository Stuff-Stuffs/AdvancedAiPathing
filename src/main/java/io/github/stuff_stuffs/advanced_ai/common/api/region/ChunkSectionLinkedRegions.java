package io.github.stuff_stuffs.advanced_ai.common.api.region;

import io.github.stuff_stuffs.advanced_ai.common.impl.region.ChunkSectionLinkedRegionsImpl;

public interface ChunkSectionLinkedRegions {
    long[] links(long regionId);

    ChunkSectionRegions regions();

    static Builder builder(final ChunkSectionRegions regions) {
        return new ChunkSectionLinkedRegionsImpl.BuilderImpl(regions);
    }

    interface Builder {
        void link(long current, long adjacent);

        ChunkSectionLinkedRegions build();
    }
}
