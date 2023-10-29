package io.github.stuff_stuffs.advanced_ai_pathing.common.internal;

import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job.LocationCachingJob;

public interface RunnableAiJobExecutor  {
    void run(int millis);

    void enqueue(LocationCachingJob<?> job);
}
