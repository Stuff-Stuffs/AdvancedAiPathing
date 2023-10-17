package io.github.stuff_stuffs.advanced_ai.common.impl.ai.brain;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrain;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;

public class AiBrainContextImpl<T> implements AiBrainContext<T> {
    private final T entity;
    private final AiBrain<T> brain;
    private long seed;

    public AiBrainContextImpl(final T entity, final AiBrain<T> brain, final long seed) {
        this.entity = entity;
        this.brain = brain;
        this.seed = seed;
    }

    @Override
    public T entity() {
        return entity;
    }

    @Override
    public AiBrain<T> brain() {
        return brain;
    }

    @Override
    public long seed() {
        return seed++;
    }
}
