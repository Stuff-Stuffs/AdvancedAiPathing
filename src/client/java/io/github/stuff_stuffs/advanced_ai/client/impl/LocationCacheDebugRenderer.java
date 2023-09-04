package io.github.stuff_stuffs.advanced_ai.client.impl;

import io.github.stuff_stuffs.advanced_ai.client.api.debug.DebugRenderer;
import io.github.stuff_stuffs.advanced_ai.client.internal.AdvancedAiClient;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.LocationCacheDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;
import org.joml.Matrix4f;

public class LocationCacheDebugRenderer implements DebugRenderer<LocationCacheDebugSection> {
    @Override
    public void render(final LocationCacheDebugSection data, final ChunkSectionPos pos, final WorldRenderContext context) {
        final Vec3d cameraPos = context.camera().getPos();
        if(cameraPos.squaredDistanceTo(pos.getMinPos().toCenterPos()) > 256*256) {
            return;
        }
        final MatrixStack stack = context.matrixStack();
        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (final LocationClassifier<?> key : data.keys()) {
            if (!AdvancedAiClient.shouldRender(key)) {
                continue;
            }
            render(key, data, pos, context);
        }
        stack.pop();
    }

    private <T> void render(final LocationClassifier<T> classifier, final LocationCacheDebugSection data, final ChunkSectionPos pos, final WorldRenderContext context) {
        final LocationCacheDebugSection.Entry<T> entry = data.get(classifier);
        final UniverseInfo<T> universeInfo = classifier.universeInfo();
        final int universeSize = universeInfo.size();
        final BitSetVoxelSet[] shapes = new BitSetVoxelSet[universeSize];
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    final T val = entry.get(LocationCacheSection.pack(x, y, z));
                    final int index = universeInfo.toIndex(val);
                    if (shapes[index] == null) {
                        shapes[index] = new BitSetVoxelSet(16, 16, 16);
                    }
                    shapes[index].set(x, y, z);
                }
            }
        }
        final MatrixStack stack = context.matrixStack();
        stack.push();
        stack.translate(pos.getMinX(), pos.getMinY(), pos.getMinZ());
        final VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());
        int i = 0;
        final int k = LocationClassifier.REGISTRY.getRawId(classifier) + 1;
        final Matrix4f mat = stack.peek().getPositionMatrix();
        for (final BitSetVoxelSet shape : shapes) {
            i++;
            if (shape == null) {
                continue;
            }
            final int color = 0xFF000000 | HashCommon.murmurHash3(HashCommon.murmurHash3(HashCommon.murmurHash3(i) + k) + k);
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                final int dx = (x2 - x1) * (x2 - x1);
                final int dy = y2 - y1;
                final int dz = z2 - z1;
                final int dist = dx + dy * dy + dz * dz;
                final float inv = MathHelper.inverseSqrt(dist);
                vertexConsumer.vertex(mat, x1, y1, z1).color(color).normal(dx * inv, dy * inv, dz * inv).next();
                vertexConsumer.vertex(mat, x2, y2, z2).color(color).normal(dx * inv, dy * inv, dz * inv).next();
            }, true);
        }
        stack.pop();
    }
}
