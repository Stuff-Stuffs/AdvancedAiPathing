package io.github.stuff_stuffs.advanced_ai.common.api.pathing.region;

import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;

public interface ChunkSectionRegion {
    long id();

    boolean contains(short s);

    short any();

    PackedList all();
}
