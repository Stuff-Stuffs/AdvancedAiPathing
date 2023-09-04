package io.github.stuff_stuffs.advanced_ai.client.api.debug;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.ChunkSectionPos;

public interface DebugRenderer<T> {
    void render(T data, ChunkSectionPos pos, WorldRenderContext context);
}
