package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class LocationCacheSectionRegistry {
    public static final int SIZE = 8;

    static {
        if ((SIZE & (SIZE - 1)) != 0 || SIZE > 16) {
            throw new AssertionError();
        }
    }

    private static final Map<Identifier, Pair<Factory, SizeEstimator>> REGISTRY = new Object2ObjectOpenHashMap<>();
    private static final Factory FALLBACK = DenseLocationCacheSubSectionImpl::new;
    private static Factory[] FLATTENED_FACTORIES = new Factory[0];
    private static SizeEstimator[] FLATTENED_ESTIMATORS = new SizeEstimator[0];

    public static void register(final Identifier id, final Factory factory, final SizeEstimator estimator) {
        REGISTRY.put(id, Pair.of(factory, estimator));
        flatten();
    }

    private static void flatten() {
        final int size = REGISTRY.size();
        FLATTENED_FACTORIES = new Factory[size];
        FLATTENED_ESTIMATORS = new SizeEstimator[size];
        int i = 0;
        for (final Pair<Factory, SizeEstimator> value : REGISTRY.values()) {
            FLATTENED_FACTORIES[i] = value.getFirst();
            FLATTENED_ESTIMATORS[i] = value.getSecond();
            i++;
        }
    }

    public static boolean has(final Identifier id) {
        return REGISTRY.containsKey(id);
    }

    public interface Factory {
        <T> LocationCacheSubSection<T> create(PreLocationCacheSection<T> section, UniverseInfo<T> info);
    }

    public interface SizeEstimator {
        <T> long estimateBytes(PreLocationCacheSection<T> section, UniverseInfo<T> info);
    }

    public static <T> Factory findBest(final PreLocationCacheSection<T> section, final UniverseInfo<T> info) {
        final Factory[] factories = FLATTENED_FACTORIES;
        final SizeEstimator[] estimators = FLATTENED_ESTIMATORS;
        int best = -1;
        long bestSize = Long.MAX_VALUE;
        for (int i = 0; i < estimators.length; i++) {
            final SizeEstimator estimator = estimators[i];
            final long estimate = estimator.estimateBytes(section, info);
            if (estimate < bestSize) {
                bestSize = estimate;
                best = i;
            }
        }
        if (best == -1) {
            return FALLBACK;
        }
        return factories[best];
    }

    private LocationCacheSectionRegistry() {
    }
}
