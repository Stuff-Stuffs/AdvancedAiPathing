package io.github.stuff_stuffs.advanced_ai.common.impl.job.executor;

import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJob;
import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJobHandle;
import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJobHandler;
import io.github.stuff_stuffs.advanced_ai.common.internal.RunnableAiJobExecutor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class SingleThreadedJobExecutor implements RunnableAiJobExecutor {
    private final Logger logger;
    private final List<AiJobHandler> handlers;
    private final List<AiJobHandler> executionOrder;
    private final int maxWaitingTasks;
    private final ArrayDeque<HandleJobPair> jobs;

    public SingleThreadedJobExecutor(final List<AiJobHandler> handlers, final Logger logger, final int maxWaitingTasks) {
        this.handlers = handlers;
        executionOrder = new ArrayList<>(handlers);
        this.maxWaitingTasks = maxWaitingTasks;
        jobs = new ArrayDeque<>(maxWaitingTasks);
        Collections.reverse(executionOrder);
        this.logger = logger;
    }

    @Override
    public Optional<AiJobHandle> enqueue(final AiJob job) {
        return enqueue(job, null);
    }

    private Optional<AiJobHandle> enqueue(final AiJob job, final @Nullable AiJobHandler producer) {
        for (final AiJobHandler handler : handlers) {
            if (handler == producer) {
                continue;
            }
            final Optional<AiJobHandle> accepted = handler.accept(job);
            if (accepted.isPresent()) {
                return accepted;
            }
        }
        if (jobs.size() < maxWaitingTasks) {
            final HandleImpl handle = new HandleImpl();
            final HandleJobPair pair = new HandleJobPair(handle, job);
            jobs.add(pair);
            return Optional.of(handle);
        }
        return Optional.empty();
    }

    @Override
    public void run(final int millis) {
        for (final AiJobHandler handler : handlers) {
            handler.tick();
        }
        final long startTime = System.currentTimeMillis();
        final boolean infoEnabled = logger.isInfoEnabled();
        while (true) {
            while (!jobs.isEmpty()) {
                final HandleJobPair pair = jobs.peekFirst();
                final AiJobHandle handle = pair.handle;
                final AiJob job = pair.job;
                long l = 0;
                if (infoEnabled) {
                    l = System.currentTimeMillis();
                }
                if (job.run(logger)) {
                    handle.kill();
                    job.apply(logger);
                    jobs.removeFirst();
                }
                final long currentTime = System.currentTimeMillis();
                if (infoEnabled) {
                    logger.info("Spent {} milliseconds on task {}", currentTime - l, job.debugData());
                }
            }
            if (System.currentTimeMillis() - startTime > millis) {
                return;
            }
            if (jobs.isEmpty()) {
                final DelegatingHandleImpl handle = new DelegatingHandleImpl();
                for (final AiJobHandler handler : executionOrder) {
                    final AiJob job = handler.produceWork(handle);
                    if (job != null) {
                        final Optional<AiJobHandle> enqueue = enqueue(job, handler);
                        if (enqueue.isEmpty()) {
                            job.cancel(logger);
                        } else {
                            handle.delegate = enqueue.get();
                        }
                    }
                }
                if(jobs.isEmpty()) {
                    return;
                }
            }
        }
    }

    @Override
    public void stop() {
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

    private static final class DelegatingHandleImpl implements AiJobHandle {
        private boolean alive = true;
        private AiJobHandle delegate;

        @Override
        public boolean alive() {
            return delegate != null ? delegate.alive() : alive;
        }

        @Override
        public void kill() {
            alive = false;
            if (delegate != null) {
                delegate.kill();
            }
        }
    }

    private record HandleJobPair(AiJobHandle handle, AiJob job) {
    }
}
