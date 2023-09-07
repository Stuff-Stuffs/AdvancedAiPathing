package io.github.stuff_stuffs.advanced_ai.common.api.job;

import java.util.Optional;

public interface AiJobExecutor {
    Optional<AiJobHandle> enqueue(AiJob job, int timeout);

    default Optional<AiJobHandle> enqueue(final AiJob job) {
        return enqueue(job, -1);
    }
}
