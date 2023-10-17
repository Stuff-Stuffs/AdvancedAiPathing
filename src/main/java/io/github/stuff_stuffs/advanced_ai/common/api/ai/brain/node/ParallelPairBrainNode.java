package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;

import java.util.function.BiFunction;

public class ParallelPairBrainNode<R0, R1, R2, T> implements BrainNode<R0, T> {
    private final BrainNode<R1, T> first;
    private final BrainNode<R2, T> second;
    private final BiFunction<R1, R2, R0> merger;

    public ParallelPairBrainNode(final BrainNode<R1, T> first, final BrainNode<R2, T> second, final BiFunction<R1, R2, R0> merger) {
        this.first = first;
        this.second = second;
        this.merger = merger;
    }

    @Override
    public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
        if (first.init(context, logger)) {
            if (second.init(context, logger)) {
                return true;
            }
            first.deinit(context, logger);
            return false;
        }
        return false;
    }

    @Override
    public R0 tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
        return merger.apply(first.tick(entity, context, logger), second.tick(entity, context, logger));
    }

    @Override
    public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
        first.deinit(context, logger);
        second.deinit(context, logger);
    }
}
