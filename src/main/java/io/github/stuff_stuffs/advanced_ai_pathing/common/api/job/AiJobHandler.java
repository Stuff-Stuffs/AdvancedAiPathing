package io.github.stuff_stuffs.advanced_ai_pathing.common.api.job;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface AiJobHandler {
    void tick();

    Optional<AiJobHandle> accept(AiJob job);

    @Nullable AiJob produceWork(AiJobHandle futureHandle);
}
