package io.github.stuff_stuffs.advanced_ai.common.api.region;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;

public interface ChunkRegionifier<T> {
    LocationClassifier<T> classifier();

    ChunkSectionRegions regionify(ChunkSectionPos pos, ShapeCache cache);
}
