package io.github.stuff_stuffs.advanced_ai.common.api.region;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkSectionPos;

public interface ChunkRegionifier<T> {
    RegistryKey<Registry<ChunkRegionifier<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AdvancedAi.id("regionifiers"));
    Registry<ChunkRegionifier<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    LocationClassifier<T> classifier();

    ChunkSectionRegions regionify(ChunkSectionPos pos, ShapeCache cache);

    ChunkSectionLinkedRegions link(ChunkSectionPos pos, ChunkSectionRegions regions, ShapeCache cache);
}
