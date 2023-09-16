package io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching;

import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.lang.invoke.MethodHandle;

public interface LocationClassifier<T> {
    RegistryKey<Registry<LocationClassifier<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AdvancedAi.id("location_classifiers"));
    Registry<LocationClassifier<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    T get(int x, int y, int z, ShapeCache cache);

    boolean needsRebuild(int chunkSectionX, int chunkSectionY, int chunkSectionZ, int otherChunkSectionX, int otherChunkSectionY, int otherChunkSectionZ, int x, int y, int z, ShapeCache cache);

    UniverseInfo<T> universeInfo();

    MethodHandle specialGetHandle();
}
