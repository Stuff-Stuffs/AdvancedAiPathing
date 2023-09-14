package io.github.stuff_stuffs.advanced_ai.client.impl;

import io.github.stuff_stuffs.advanced_ai.client.api.debug.DebugRenderer;
import io.github.stuff_stuffs.advanced_ai.client.internal.AdvancedAiClient;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.RegionDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;

public class ChunkRegionDebugRenderer implements DebugRenderer<RegionDebugSection> {
    @Override
    public void render(final RegionDebugSection data, final ChunkSectionPos pos, final WorldRenderContext context) {
        final Vec3d cameraPos = context.camera().getPos();
        if (cameraPos.squaredDistanceTo(pos.getCenterPos().toCenterPos()) > 48 * 48) {
            return;
        }
        final MatrixStack stack = context.matrixStack();
        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (final ChunkRegionifier<?> key : data.keys()) {
            if (!AdvancedAiClient.shouldRender(key)) {
                continue;
            }
            render(key, data, pos, context);
        }
        stack.pop();
    }

    private void render(final ChunkRegionifier<?> key, final RegionDebugSection data, final ChunkSectionPos pos, final WorldRenderContext context) {
        final ChunkSectionRegions regions = data.get(key);
        final int regionCount = regions.regionCount();
        final BitSetVoxelSet[] shapes = new BitSetVoxelSet[regionCount];
        for (int i = 0; i < regionCount; i++) {
            final BitSetVoxelSet voxelSet = new BitSetVoxelSet(16, 16, 16);
            shapes[i] = voxelSet;
            final PackedList list = regions.byId(regions.prefix() | i).all();
            final int size = list.size();
            for (int j = 0; j < size; j++) {
                final int packed = list.get(j);
                final int x = LocationCacheSection.unpackX(packed);
                final int y = LocationCacheSection.unpackY(packed);
                final int z = LocationCacheSection.unpackZ(packed);
                voxelSet.set(x, y, z);
            }
        }
        final MatrixStack stack = context.matrixStack();
        stack.push();
        stack.translate(pos.getMinX(), pos.getMinY(), pos.getMinZ());
        final VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());
        int i = 0;
        final int k = ChunkRegionifier.REGISTRY.getRawId(key) + 1;
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
