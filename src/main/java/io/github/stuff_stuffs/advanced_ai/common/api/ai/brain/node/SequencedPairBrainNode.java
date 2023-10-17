package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;

import java.util.function.Function;
import java.util.function.Predicate;

public class SequencedPairBrainNode<R0, R1, R2, T> implements BrainNode<R0, T> {
    private final BrainNode<R1, T> first;
    private final BrainNode<R2, T> second;
    private final Predicate<R1> firstFinished;
    private final Function<R1, R0> firstAdaptor;
    private final Function<R2, R0> secondAdaptor;
    private final Function<AiBrainContext<T>, R0> secondInitFailed;
    private boolean doneFirst;
    private boolean init;
    private boolean errorInInit = false;

    public SequencedPairBrainNode(final BrainNode<R1, T> first, final BrainNode<R2, T> second, final Predicate<R1> firstFinished, final Function<R1, R0> firstAdaptor, final Function<R2, R0> secondAdaptor, final Function<AiBrainContext<T>, R0> secondInitFailed) {
        this.first = first;
        this.second = second;
        this.firstFinished = firstFinished;
        this.firstAdaptor = firstAdaptor;
        this.secondAdaptor = secondAdaptor;
        this.secondInitFailed = secondInitFailed;
    }

    @Override
    public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
        doneFirst = false;
        return init = first.init(context, logger);
    }

    @Override
    public R0 tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
        if (errorInInit) {
            return secondInitFailed.apply(entity);
        }
        if (!doneFirst) {
            final R1 res = first.tick(entity, context, logger);
            if (firstFinished.test(res)) {
                doneFirst = true;
                init = false;
            }
            return firstAdaptor.apply(res);
        } else {
            if (!init) {
                if (!second.init(entity, logger)) {
                    errorInInit = true;
                    return secondInitFailed.apply(entity);
                } else {
                    init = true;
                }
            }
            return secondAdaptor.apply(second.tick(entity, context, logger));
        }
    }

    @Override
    public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
        if (doneFirst && init) {
            second.deinit(context, logger);
        } else if (init) {
            first.deinit(context, logger);
        }
        errorInInit = false;
        doneFirst = false;
        init = false;
    }
}
