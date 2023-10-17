package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.Task;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BrainNodeUtil {
    public static <R, T> BrainNode<R, T> constant(final R result) {
        return terminal(context -> result);
    }

    public static <R, T> BrainNode<R, T> terminal(final Function<AiBrainContext<T>, R> result) {
        return new BrainNode<>() {
            @Override
            public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
                return true;
            }

            @Override
            public R tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
                return result.apply(entity);
            }

            @Override
            public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {

            }
        };
    }

    public static <R, T> BrainNode<R, T> task(final Function<AiBrainContext<T>, Optional<Task<R, AiBrainContext<T>>>> taskFactory) {
        return new BrainNode<>() {
            private Task<R, AiBrainContext<T>> task = null;

            @Override
            public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
                final Optional<Task<R, AiBrainContext<T>>> task = taskFactory.apply(context);
                if (task.isPresent()) {
                    this.task = task.get();
                    return true;
                }
                return false;
            }

            @Override
            public R tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
                return task.tick(entity, context, logger);
            }

            @Override
            public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
                task = null;
            }
        };
    }

    public static <R, T> BrainNode<R, T> chainedParallel(final Iterable<BrainNode<R, T>> iterable, final BinaryOperator<R> merger, final R defaultReturn) {
        final Iterator<BrainNode<R, T>> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return constant(defaultReturn);
        }
        BrainNode<R, T> next = iterator.next();
        while (iterator.hasNext()) {
            next = new ParallelPairBrainNode<>(next, iterator.next(), merger);
        }
        return next;
    }

    public static <R, T> BrainNode<R, T> chainedSequence(final Iterable<BrainNode<R, T>> iterable,  final Predicate<R> finished, final R defaultReturn) {
        final Iterator<BrainNode<R, T>> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return constant(defaultReturn);
        }
        BrainNode<R, T> next = iterator.next();
        while (iterator.hasNext()) {
            next = new SequencedPairBrainNode<>(next, iterator.next(), finished, Function.identity(), Function.identity(), context -> defaultReturn);
        }
        return next;
    }

    private BrainNodeUtil() {
    }
}
