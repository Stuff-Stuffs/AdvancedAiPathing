package io.github.stuff_stuffs.advanced_ai.client.api.pathing.debug;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.debug.DebugSectionType;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;

public final class DebugRendererRegistry {
    private static final Map<DebugSectionType<?>, DebugRenderer<?>> RENDERERS = new Reference2ReferenceOpenHashMap<>();

    public static <T> void register(final DebugSectionType<T> type, final DebugRenderer<T> renderer) {
        RENDERERS.put(type, renderer);
    }

    public static <T> DebugRenderer<T> get(final DebugSectionType<T> type) {
        return (DebugRenderer<T>) RENDERERS.get(type);
    }

    private DebugRendererRegistry() {
    }
}
