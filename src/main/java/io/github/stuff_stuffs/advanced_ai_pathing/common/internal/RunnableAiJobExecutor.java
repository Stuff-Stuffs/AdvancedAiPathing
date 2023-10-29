package io.github.stuff_stuffs.advanced_ai_pathing.common.internal;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.job.AiJobExecutor;

public interface RunnableAiJobExecutor extends AiJobExecutor {
    void run(int millis);
}
