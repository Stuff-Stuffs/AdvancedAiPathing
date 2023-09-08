package io.github.stuff_stuffs.advanced_ai.common.api.region;

import org.jetbrains.annotations.Nullable;

public interface ChunkSectionRegions {
    long PREFIX_MASK = -65536L;

    @Nullable ChunkSectionRegion query(short packed);

    @Nullable ChunkSectionRegion byId(long id);

    long prefix();

    interface Builder {
        RegionKey newRegion();

        boolean contains(short pos);

        void expand(RegionKey key, short pos);

        ChunkSectionRegions build();
    }

    interface RegionKey {
    }
}
