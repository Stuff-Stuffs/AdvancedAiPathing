package io.github.stuff_stuffs.advanced_ai.common.api.job;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Optional;

public abstract class AbstractPrerequisiteAiJobHandler<T extends AiJob, K> implements AiJobHandler {
    private final int maxWaitingTasks;
    private final Class<T> clazz;
    private final ArrayDeque<HandleJobPair<T>> jobs;
    private final Object2ReferenceOpenHashMap<K, AiJobHandle> ongoing;

    public AbstractPrerequisiteAiJobHandler(final int maxWaitingTasks, final Class<T> clazz) {
        this.maxWaitingTasks = maxWaitingTasks;
        jobs = new ArrayDeque<>(maxWaitingTasks);
        this.clazz = clazz;
        ongoing = new Object2ReferenceOpenHashMap<>();
    }

    @Override
    public void tick() {
        final ObjectIterator<Object2ReferenceMap.Entry<K, AiJobHandle>> iterator = ongoing.object2ReferenceEntrySet().fastIterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getValue().alive()) {
                iterator.remove();
            }
        }
    }

    @Override
    public Optional<AiJobHandle> accept(final AiJob job) {
        if (jobs.size() < maxWaitingTasks && clazz.isInstance(job)) {
            final DelegatingHandleImpl handle = new DelegatingHandleImpl();
            final HandleJobPair<T> pair = new HandleJobPair<>(handle, (T) job);
            jobs.add(pair);
            return Optional.of(handle);
        }
        return Optional.empty();
    }

    @Override
    public @Nullable AiJob produceWork(final AiJobHandle futureHandle) {
        while (!jobs.isEmpty()) {
            final HandleJobPair<T> pair = jobs.peekFirst();
            if (!pair.handle.alive) {
                jobs.removeFirst();
            } else {
                final AiJob job = producePrerequisite(pair.job, futureHandle);
                if (job != null) {
                    return job;
                } else {
                    jobs.removeFirst();
                    pair.handle.delegate = futureHandle;
                    return pair.job;
                }
            }
        }
        return null;
    }

    protected void addOngoing(final K key, final AiJobHandle handle) {
        ongoing.put(key, handle);
    }

    protected boolean hasOngoing(final K key) {
        final AiJobHandle handle = ongoing.get(key);
        if (handle == null) {
            return false;
        }
        return handle.alive();
    }

    protected abstract K key(T job);

    protected abstract @Nullable AiJob producePrerequisite(T job, AiJobHandle futureHandle);

    private record HandleJobPair<T extends AiJob>(DelegatingHandleImpl handle, T job) {
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
}
