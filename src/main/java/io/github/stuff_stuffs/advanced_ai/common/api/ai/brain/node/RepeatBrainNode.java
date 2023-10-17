package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;

import java.util.function.Function;
import java.util.function.Predicate;

public class RepeatBrainNode<R, T> implements BrainNode<R, T> {
    private final int count;
    private final BrainNode<R, T> child;
    private final Function<AiBrainContext<T>, R> onFailure;
    private final Predicate<R> finished;
    private boolean init = false;
    private int counter;

    public RepeatBrainNode(final int count, final BrainNode<R, T> child, final Function<AiBrainContext<T>, R> onFailure, final Predicate<R> finished) {
        this.count = count;
        this.child = child;
        this.onFailure = onFailure;
        this.finished = finished;
    }

    @Override
    public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
        counter = 0;
        return init = child.init(context, logger);
    }

    @Override
    public R tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
        if (!init) {
            return onFailure.apply(entity);
        }
        while (true) {
            final R result = child.tick(entity, context, logger);
            if (!finished.test(result) || ++counter >= count) {
                return result;
            }
            child.deinit(entity, logger);
            if (!child.init(entity, logger)) {
                counter = count;
                init = false;
                return onFailure.apply(entity);
            }
        }
    }

    @Override
    public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
        if (init) {
            child.deinit(context, logger);
        }
    }
}
