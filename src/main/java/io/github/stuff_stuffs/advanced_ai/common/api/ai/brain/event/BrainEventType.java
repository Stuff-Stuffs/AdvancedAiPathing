package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.event;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public interface BrainEventType<T extends BrainEvent> {
    RegistryKey<Registry<BrainEventType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AdvancedAi.id("brain_event_types"));
    Registry<BrainEventType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    Codec<T> codec();
}
