package io.github.stuff_stuffs.advanced_ai.common.internal;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;

import java.lang.invoke.MethodHandle;

public final class ProcessedLocationClassifier<T> {
    public final LocationClassifier<T> delegate;
    public final MethodHandle getHandle;
    public final MethodHandle toIndexHandle;
    public final MethodHandle fromIndexHandle;

    public ProcessedLocationClassifier(final LocationClassifier<T> delegate) {
        this.delegate = delegate;
        getHandle = delegate.specialGetHandle();
        toIndexHandle = delegate.universeInfo().toIndexHandle();
        fromIndexHandle = delegate.universeInfo().fromIndexHandle();
    }

    public int invoke(final int x, final int y, final int z, final ShapeCache cache) {
        try {
            final T var = (T) getHandle.invoke(x, y, z, cache);
            return (int) toIndexHandle.invoke(var);
        } catch (final Throwable var6) {
            throw new RuntimeException(var6);
        }
    }
}

