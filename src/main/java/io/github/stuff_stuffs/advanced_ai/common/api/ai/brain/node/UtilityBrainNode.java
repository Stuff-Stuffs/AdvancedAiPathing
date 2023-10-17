package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class UtilityBrainNode<R, T> implements BrainNode<R, T> {
    private final List<Entry<R, T>> entries;
    private BrainNode<R, T> selected = null;

    private UtilityBrainNode(final List<Entry<R, T>> entries) {
        this.entries = List.copyOf(entries);
    }

    private @Nullable BrainNode<R, T> select(final double[] scores, final boolean[] visited) {
        double bestScore = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < scores.length; i++) {
            if (!visited[i] && bestScore < scores[i]) {
                bestScore = scores[i];
                index = i;
            }
        }
        if (index != -1) {
            visited[index] = true;
        }
        return index == -1 ? null : entries.get(index).node;
    }

    @Override
    public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
        final int size = entries.size();
        final boolean[] visited = new boolean[size];
        final double[] scores = new double[size];
        for (int i = 0; i < size; i++) {
            final Entry<R, T> entry = entries.get(i);
            final double score = entry.scorer.applyAsDouble(context);
            scores[i] = score;
        }
        BrainNode<R, T> sel;
        do {
            sel = select(scores, visited);
            if (sel != null && sel.init(context, logger)) {
                selected = sel;
                return true;
            }
        } while (sel != null);
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

    public static <R, T> Builder<R, T> builder() {
        return new Builder<>();
    }

    public static final class Builder<R, T> {
        private final List<Entry<R, T>> entries = new ArrayList<>();

        private Builder() {
        }

        public Builder<R, T> add(final BrainNode<R, T> node, final ToDoubleFunction<AiBrainContext<T>> scorer) {
            entries.add(new Entry<>(scorer, node));
            return this;
        }

        public BrainNode<R, T> build() {
            if (entries.isEmpty()) {
                throw new IllegalStateException();
            }
            return new UtilityBrainNode<>(entries);
        }
    }

    private record Entry<R, T>(ToDoubleFunction<AiBrainContext<T>> scorer, BrainNode<R, T> node) {
    }
}
