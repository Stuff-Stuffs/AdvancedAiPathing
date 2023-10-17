package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrainContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class WeightedUtilityBrainNode<R, T> implements BrainNode<R, T> {
    private final ReWeightor<T> reWeightor;
    private final List<Entry<R, T>> entries;
    private BrainNode<R, T> selected = null;

    private WeightedUtilityBrainNode(final ReWeightor<T> reWeightor, final List<Entry<R, T>> entries) {
        this.reWeightor = reWeightor;
        this.entries = List.copyOf(entries);
    }

    private @Nullable BrainNode<R, T> select(final boolean[] visited, final double[] scores, final Random random) {
        double scoreSum = 0;
        for (int i = 0; i < scores.length; i++) {
            if (!visited[i]) {
                scoreSum = scoreSum + scores[i];
            }
        }
        if (!(scoreSum > 0)) {
            return null;
        }
        double s = 0;
        final double splice = random.nextDouble() * scoreSum;
        for (int i = 0; i < scores.length; i++) {
            s = s + scores[i];
            if (splice <= s) {
                visited[i] = true;
                return entries.get(i).node;
            }
        }
        return entries.get(entries.size() - 1).node;
    }

    @Override
    public boolean init(final AiBrainContext<T> context, final EntityLogger logger) {
        final int size = entries.size();
        final double[] weights = new double[size];
        double weightSum = 0;
        for (int i = 0; i < size; i++) {
            final Entry<R, T> entry = entries.get(i);
            double score = entry.scorer.applyAsDouble(context);
            if (!(score >= 0)) {
                score = 0;
            }
            weightSum = weightSum + score;
            weights[i] = score;
        }
        for (int i = 0; i < size; i++) {
            double score = reWeightor.weight(weights[i], weightSum, context);
            if (!(score >= 0)) {
                score = 0;
            }
            weights[i] = score;
        }
        final boolean[] visited = new boolean[size];
        final Random random = new Xoroshiro128PlusPlusRandom(context.seed());
        BrainNode<R, T> sel;
        do {
            sel = select(visited, weights, random);
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

    public interface ReWeightor<T> {
        double weight(double weight, double weightSum, AiBrainContext<T> context);

        static <T> ReWeightor<T> none() {
            return (weight, weightSum, context) -> weight;
        }

        static <T> ReWeightor<T> exp() {
            return (weight, weightSum, context) -> Math.exp(weight);
        }
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

        public BrainNode<R, T> build(final ReWeightor<T> reWeightor) {
            return new WeightedUtilityBrainNode<>(reWeightor, entries);
        }
    }

    private record Entry<R, T>(ToDoubleFunction<AiBrainContext<T>> scorer, BrainNode<R, T> node) {
    }
}
