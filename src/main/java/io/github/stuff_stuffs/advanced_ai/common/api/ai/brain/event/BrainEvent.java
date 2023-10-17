package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.event;

import com.mojang.serialization.Codec;

public interface BrainEvent {
    Codec<BrainEvent> CODEC = BrainEventType.REGISTRY.getCodec().dispatchStable(BrainEvent::type, BrainEventType::codec);

    long duration();

    BrainEventType<?> type();
}
