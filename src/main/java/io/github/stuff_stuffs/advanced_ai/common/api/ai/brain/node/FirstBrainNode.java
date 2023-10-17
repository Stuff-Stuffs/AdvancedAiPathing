package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;

import java.util.List;

public class FirstBrainNode<R, T> implements BrainNode<R, T> {
    private final List<BrainNode<R, T>> children;
    private BrainNode<R, T> selected = null;

    public FirstBrainNode(final List<BrainNode<R, T>> children) {
        this.children = children;
    }

    @Override
    public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
        for (final BrainNode<R, T> child : children) {
            if (child.init(context, logger)) {
                selected = child;
                return true;
            }
        }
        return false;
    }

    @Override
    public R tick(final AiBrainContext<T> entity, final TaskExecutionContext context, final EntityLogger logger) {
        return selected.tick(entity, context, logger);
    }

    @Override
    public void deinit(final AiBrainContext<T> context, final EntityLogger logger) {
        selected.deinit(context, logger);
        selected = null;
    }
}
