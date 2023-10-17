package io.github.stuff_stuffs.advanced_ai.common.api.ai.task;

import io.github.stuff_stuffs.advanced_ai.common.impl.ai.task.TaskKeyImpl;
import net.minecraft.util.Identifier;

public interface TaskKey<R, P> {
    Identifier id();

    Class<R> resultClass();

    Class<P> parameterClass();

    static <R, P> TaskKey<R, P> create(final Identifier id, final Class<R> resultClass, final Class<P> parameterClass) {
        return new TaskKeyImpl<>(id, resultClass, parameterClass);
    }
}
