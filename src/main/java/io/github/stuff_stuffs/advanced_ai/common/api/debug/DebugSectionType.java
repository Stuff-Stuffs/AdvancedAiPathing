package io.github.stuff_stuffs.advanced_ai.common.api.debug;

import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class DebugSectionType<T> {
    public static final RegistryKey<Registry<DebugSectionType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AdvancedAi.id("debug_types"));
    public static final Registry<DebugSectionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
    public static final DebugSectionType<LocationCacheDebugSection> LOCATION_CACHE_TYPE = new DebugSectionType<>(LocationCacheDebugSection::write, LocationCacheDebugSection::new);
    public final BiConsumer<T, PacketByteBuf> encoder;
    public final Function<PacketByteBuf, T> decoder;

    public DebugSectionType(final BiConsumer<T, PacketByteBuf> encoder, final Function<PacketByteBuf, T> decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }
}
