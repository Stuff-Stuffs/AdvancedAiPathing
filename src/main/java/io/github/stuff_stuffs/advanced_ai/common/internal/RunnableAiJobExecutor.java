package io.github.stuff_stuffs.advanced_ai.common.internal;

import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJobExecutor;

public interface RunnableAiJobExecutor extends AiJobExecutor {
    void run(int millis);

    void stop();
}
