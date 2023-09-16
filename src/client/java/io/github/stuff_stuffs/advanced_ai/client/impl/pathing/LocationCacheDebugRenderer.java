package io.github.stuff_stuffs.advanced_ai.client.impl.pathing;

import io.github.stuff_stuffs.advanced_ai.client.api.pathing.debug.DebugRenderer;
import io.github.stuff_stuffs.advanced_ai.client.internal.AdvancedAiClient;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.debug.LocationCacheDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;

public class LocationCacheDebugRenderer implements DebugRenderer<LocationCacheDebugSection> {
    @Override
    public void render(final LocationCacheDebugSection data, final ChunkSectionPos pos, final WorldRenderContext context) {
        final Vec3d cameraPos = context.camera().getPos();
        if (cameraPos.squaredDistanceTo(pos.getCenterPos().toCenterPos()) > 32 * 32) {
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
        for (final BitSetVoxelSet shape : shapes) {
            i++;
            if (shape == null) {
                continue;
            }
            final Vec3d color = Vec3d.unpackRgb(HashCommon.murmurHash3(HashCommon.murmurHash3(HashCommon.murmurHash3(i) + k) + k));
            shape.forEachBox((x1, y1, z1, x2, y2, z2) -> WorldRenderer.drawBox(stack, vertexConsumer, x1 + 0.125, y1 + 0.125, z1 + 0.125, x2 - 0.125, y2 - 0.125, z2 - 0.125, (float) color.x, (float) color.y, (float) color.z, 1.0F), true);
        }
        stack.pop();
    }
}
