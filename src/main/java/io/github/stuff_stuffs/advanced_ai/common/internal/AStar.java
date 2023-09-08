package io.github.stuff_stuffs.advanced_ai.common.internal;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public abstract class AStar<N, C, T> {
    public static final int MAX_SUCCESSORS = 384;
    private final Class<N> nodeClass;

    protected AStar(final Class<N> nodeClass) {
        this.nodeClass = nodeClass;
    }

    public PathInfo<N> findPath(final N start, final C context, final T target, final double error, final double maxCost, final boolean partial) {
        final N[] successors = (N[]) Array.newInstance(nodeClass, MAX_SUCCESSORS);
        final PathingHeap<WrappedPathNode<N>> queue = new PathingHeap<>();
        final NodeVisitedMap<WrappedPathNode<N>> visited = new NodeVisitedMap<>();
        double bestDist = Double.POSITIVE_INFINITY;
        WrappedPathNode<N> best = null;
        final WrappedPathNode<N> wrappedStart = wrap(start, 1, heuristic(start, target, context));
        wrappedStart.handle = queue.insert(wrappedStart.heuristicDistance + nodeCost(wrappedStart.delegate), wrappedStart);
        final long startKey = key(start);
        visited.insert(visited.findInsertionPoint(startKey), startKey, wrappedStart);
        final CostGetter costGetter = key -> {
            final int point = visited.findInsertionPoint(key);
            if (point < 0) {
                return Double.POSITIVE_INFINITY;
            }
            return nodeCost(visited.get(point).delegate);
        };
        //While there is more nodes to visit
        while (!queue.isEmpty()) {
            final WrappedPathNode<N> current = queue.deleteMin().getValue();
            //Check if the node is too far away
            if (nodeCost(current.delegate) > maxCost) {
                continue;
            }
            //Is the node the best node so far
            if (current.heuristicDistance < bestDist) {
                bestDist = current.heuristicDistance;
                best = current;
            }
            //Is the node at the goal
            if (heuristic(current.delegate, target, context) < error) {
                return createPath(visited.size(), current);
            }
            //Get adjacent nodes, fill the array with them, return how many neighbours were found
            final int count = neighbours(current.delegate, context, costGetter, successors);
            //For each neighbour found
            for (int i = 0; i < count; i++) {
                final N next = successors[i];
                final long pos = key(next);
                final int index = visited.findInsertionPoint(pos);
                if (index < 0) {
                    final WrappedPathNode<N> wrapped = wrap(next, current.nodeCount + 1, heuristic(next, target, context));
                    visited.insert(index, pos, wrapped);
                    wrapped.handle = queue.insert(wrapped.heuristicDistance + nodeCost(next), wrapped);
                } else {
                    final WrappedPathNode<N> node = visited.get(index);
                    final double v = nodeCost(next);
                    if (v + 0.1 < nodeCost(node.delegate)) {
                        final double heuristicDistance = node.heuristicDistance;
                        final WrappedPathNode<N> wrapped = wrap(next, current.nodeCount + 1, heuristicDistance);
                        visited.insert(index, pos, wrapped);
                        if (node.handle.isValid()) {
                            wrapped.handle = node.handle;
                            wrapped.handle.setValue(wrapped);
                            wrapped.handle.decreaseKey(v + heuristicDistance);
                        } else {
                            wrapped.handle = queue.insert(v + heuristicDistance, wrapped);
                        }
                    }
                }
            }
        }
        return best == null ? new PathInfo<>(visited.size(), null) : partial ? createPath(visited.size(), best) : new PathInfo<>(visited.size(), null);
    }

    protected abstract double heuristic(N node, T target, C context);

    protected abstract double nodeCost(N node);

    protected abstract long key(N node);

    protected abstract @Nullable N previousNode(N node);

    protected abstract int neighbours(N previous, C context, CostGetter costGetter, N[] successors);

    private PathInfo<N> createPath(final int considered, final WrappedPathNode<N> wrapped) {
        final List<N> list = new ArrayList<>(wrapped.nodeCount);
        for (int i = 0; i < wrapped.nodeCount; i++) {
            list.add(null);
        }
        N node = wrapped.delegate;
        int i = wrapped.nodeCount - 1;
        while (node != null && i >= 0) {
            list.set(i, node);
            i--;
            node = previousNode(node);
        }
        if (i + 1 != 0) {
            throw new RuntimeException("Invalid previousNodeGetter!");
        }
        return new PathInfo<>(considered, list);
    }

    public record PathInfo<T>(int nodesConsidered, @Nullable List<T> path) {
    }

    private static <T> WrappedPathNode<T> wrap(final T delegate, final int nodeCount, final double heuristic) {
        return new WrappedPathNode<>(delegate, nodeCount, heuristic);
    }

    public static final class WrappedPathNode<T> {
        public final T delegate;
        public final int nodeCount;
        public final double heuristicDistance;
        public PathingHeap.Node<WrappedPathNode<T>> handle;

        public WrappedPathNode(final T delegate, final int nodeCount, final double heuristicDistance) {
            this.delegate = delegate;
            this.nodeCount = nodeCount;
            this.heuristicDistance = heuristicDistance;
        }
    }
}
