package io.github.stuff_stuffs.advanced_ai.common.api.ai.task;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.impl.ai.task.TaskExecutionContextImpl;

import java.util.Optional;

public interface TaskExecutionContext {
    <R, P> Optional<InvokableTask<R>> buildTask(TaskKey<R, P> key, P parameters);

    interface InvokableTask<R> {
        R invoke(EntityLogger logger);
    }

    static <T> Builder<T> builder() {
        return new TaskExecutionContextImpl.BuilderImpl<>();
    }

    interface Builder<T> {
        <R, P> Builder<T> add(TaskKey<R, P> key, TaskFactory<? super T, R, P> factory);

        TaskExecutionContext build(T entity);
    }
}
