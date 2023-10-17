package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain;

public interface AiBrainContext<T> {
    T entity();

    AiBrain<T> brain();

    long seed();
}
