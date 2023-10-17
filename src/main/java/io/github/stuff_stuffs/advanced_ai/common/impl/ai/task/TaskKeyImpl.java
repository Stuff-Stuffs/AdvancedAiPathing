package io.github.stuff_stuffs.advanced_ai.common.impl.ai.task;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskKey;
import net.minecraft.util.Identifier;

public class TaskKeyImpl<R, P> implements TaskKey<R, P> {
    private final Identifier id;
    private final Class<R> resultClass;
    private final Class<P> parameterClass;

    public TaskKeyImpl(final Identifier id, final Class<R> resultClass, final Class<P> parameterClass) {
        this.id = id;
        this.resultClass = resultClass;
        this.parameterClass = parameterClass;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public Class<R> resultClass() {
        return resultClass;
    }

    @Override
    public Class<P> parameterClass() {
        return parameterClass;
    }
}
