package io.github.stuff_stuffs.advanced_ai.common.impl.ai.task;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.Task;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskFactory;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskKey;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TaskExecutionContextImpl<T> implements TaskExecutionContext {
    private final Map<TaskKey<?, ?>, List<TaskFactory<? super T, ?, ?>>> map;
    private final T entity;

    public TaskExecutionContextImpl(final Map<TaskKey<?, ?>, List<TaskFactory<? super T, ?, ?>>> map, final T context) {
        this.map = map;
        entity = context;
    }

    @Override
    public <R, P> Optional<InvokableTask<R>> buildTask(final TaskKey<R, P> key, final P parameters) {
        final List<TaskFactory<? super T, ?, ?>> factories = map.get(key);
        for (final TaskFactory<? super T, ?, ?> factory : factories) {
            final TaskFactory<? super T, R, P> casted = (TaskFactory<? super T, R, P>) factory;
            final Optional<? extends Task<? extends R, ? super T>> taskOpt = casted.build(entity, parameters);
            if (taskOpt.isPresent()) {
                final Task<? extends R, ? super T> task = taskOpt.get();
                return Optional.of((logger) -> task.tick(entity, this, logger));
            }
        }
        return Optional.empty();
    }

    public static final class BuilderImpl<T> implements Builder<T> {
        private final Map<TaskKey<?, ?>, List<TaskFactory<? super T, ?, ?>>> map = new Object2ReferenceOpenHashMap<>();

        @Override
        public <R, P> Builder<T> add(final TaskKey<R, P> key, final TaskFactory<? super T, R, P> factory) {
            map.computeIfAbsent(key, i -> new ArrayList<>()).add(factory);
            return this;
        }

        @Override
        public TaskExecutionContext build(final T entity) {
            final Map<TaskKey<?, ?>, List<TaskFactory<? super T, ?, ?>>> copy = new Object2ReferenceOpenHashMap<>();
            for (final Map.Entry<TaskKey<?, ?>, List<TaskFactory<? super T, ?, ?>>> entry : map.entrySet()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return new TaskExecutionContextImpl<>(copy, entity);
        }
    }
}
