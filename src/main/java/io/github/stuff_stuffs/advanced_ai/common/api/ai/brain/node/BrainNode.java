package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface BrainNode<R, T> {
    boolean init(AiBrainContext<T> context, EntityLogger logger);

    R tick(AiBrainContext<T> entity, TaskExecutionContext context, EntityLogger logger);

    void deinit(AiBrainContext<T> context, EntityLogger logger);

    default <R0> BrainNode<R0, T> adapt(final Function<R, R0> adaptor) {
        return new BrainNode<>() {
            @Override
            public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
                return BrainNode.this.init(context, logger);
            }

            @Override
            public R0 tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
                return adaptor.apply(BrainNode.this.tick(entity, context, logger));
            }

            @Override
            public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
                BrainNode.this.deinit(context, logger);
            }
        };
    }

    default <R0> BrainNode<R0, T> adapt(final BiFunction<R, AiBrainContext<T>, R0> adaptor) {
        return new BrainNode<>() {
            @Override
            public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
                return BrainNode.this.init(context, logger);
            }

            @Override
            public R0 tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
                return adaptor.apply(BrainNode.this.tick(entity, context, logger), entity);
            }

            @Override
            public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
                BrainNode.this.deinit(context, logger);
            }
        };
    }

    default BrainNode<R, T> predicated(final Predicate<AiBrainContext<T>> predicate) {
        return new BrainNode<>() {
            @Override
            public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
                if (!predicate.test(context)) {
                    return false;
                }
                return BrainNode.this.init(context, logger);
            }

            @Override
            public R tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
                return BrainNode.this.tick(entity, context, logger);
            }

            @Override
            public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
                BrainNode.this.deinit(context, logger);
            }
        };
    }
}
