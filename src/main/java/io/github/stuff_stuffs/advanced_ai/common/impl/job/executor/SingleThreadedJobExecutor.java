package io.github.stuff_stuffs.advanced_ai.common.impl.job.executor;

import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJob;
import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJobHandle;
import io.github.stuff_stuffs.advanced_ai.common.internal.RunnableAiJobExecutor;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Optional;

public class SingleThreadedJobExecutor implements RunnableAiJobExecutor {
    private final int maxWaitingTasks;
    private final ArrayDeque<HandleJobPair> queue;
    private final Logger logger;
    private long ticks = 0;

    public SingleThreadedJobExecutor(final int maxWaitingTasks, final Logger logger) {
        this.maxWaitingTasks = maxWaitingTasks;
        queue = new ArrayDeque<>(maxWaitingTasks);
        this.logger = logger;
    }

    @Override
    public Optional<AiJobHandle> enqueue(final AiJob job, final int timeout) {
        //if (queue.size() >= maxWaitingTasks) {
        //    return Optional.empty();
        //}
        final HandleImpl handle = new HandleImpl();
        job.init(logger);
        queue.add(new HandleJobPair(handle, job, timeout >= 0 ? ticks + timeout : -1));
        return Optional.of(handle);
    }

    @Override
    public void run(final int millis) {
        final long startTime = System.currentTimeMillis();
        final boolean infoEnabled = logger.isInfoEnabled();
        while (!queue.isEmpty()) {
            final HandleJobPair peek = queue.peek();
            if (peek.timeout != -1 & peek.timeout < ticks) {
                peek.handle.kill();
                peek.job.timeout(logger);
                queue.poll();
            } else if (!peek.handle.alive()) {
                peek.job.cancel(logger);
                queue.poll();
            } else {
                long l = 0;
                if (infoEnabled) {
                    l = System.currentTimeMillis();
                }
                if (peek.job.run(logger)) {
                    peek.handle.kill();
                    peek.job.apply(logger);
                    queue.poll();
                }
                final long currentTime = System.currentTimeMillis();
                if (infoEnabled) {
                    logger.info("Spent {} milliseconds on task {}", currentTime - l, peek.job.debugData());
                }
                if (currentTime - startTime > millis) {
                    return;
                }
            }
        }
        ticks++;
    }

    @Override
    public void stop() {
        while (!queue.isEmpty()) {
            final HandleJobPair jobPair = queue.poll();
            if (jobPair.handle.alive()) {
                jobPair.handle.kill();
                jobPair.job.onServerStop(logger);
            }
        }
    }

    private record HandleJobPair(AiJobHandle handle, AiJob job, long timeout) {
    }

    private static final class HandleImpl implements AiJobHandle {
        private boolean alive = true;

        @Override
        public boolean alive() {
            return alive;
        }

        @Override
        public void kill() {
            alive = false;
        }
    }
}
