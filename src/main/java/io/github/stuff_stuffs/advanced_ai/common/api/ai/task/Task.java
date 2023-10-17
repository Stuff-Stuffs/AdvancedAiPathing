package io.github.stuff_stuffs.advanced_ai.common.api.ai.task;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;

import java.util.function.Function;

public interface Task<R, T> {
    R tick(T entity, TaskExecutionContext executionContext, EntityLogger logger);

    default <T0> Task<R, T0> adaptEntity(final Function<T0, T> adaptor) {
        return (entity, executionContext, logger) -> tick(adaptor.apply(entity), executionContext, logger);
    }

    default <T0> Task<T0, T> adaptResult(final Function<R, T0> adaptor) {
        return (entity, executionContext, logger) -> adaptor.apply(tick(entity, executionContext, logger));
    }
}
