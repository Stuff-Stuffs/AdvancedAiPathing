package io.github.stuff_stuffs.advanced_ai.client.impl;

import io.github.stuff_stuffs.advanced_ai.client.api.debug.DebugRenderer;
import io.github.stuff_stuffs.advanced_ai.client.internal.AdvancedAiClient;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionInfo;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.RegionDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.RegionLinksDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegion;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;
import io.github.stuff_stuffs.advanced_ai.common.impl.region.ChunkSectionRegionsImpl;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;

import java.util.Map;

public class RegionLinksDebugRenderer implements DebugRenderer<RegionLinksDebugSection> {
    @Override
    public void render(final RegionLinksDebugSection data, final ChunkSectionPos pos, final WorldRenderContext context) {
        final BlockPos down = context.camera().getBlockPos().down();
        final ChunkSectionPos p = ChunkSectionPos.from(down);
        if (!p.equals(pos)) {
            return;
        }
        for (final ChunkRegionifier<?> key : data.keys()) {
            if (!AdvancedAiClient.shouldRenderLinks(key)) {
                continue;
            }
            final Map<ChunkSectionPos, DebugSectionInfo<?>> map = AdvancedAiClient.DEBUG_INFOS.get(DebugSectionType.REGION_DEBUG_TYPE);
            if (map == null) {
                return;
            }
            final DebugSectionInfo<RegionDebugSection> info = (DebugSectionInfo<RegionDebugSection>) map.get(pos);
            if (info == null) {
                return;
            }
            final ChunkSectionRegions regions = info.data().get(key);
            if (regions == null) {
                return;
            }
            final ChunkSectionRegion query = regions.query(LocationCacheSection.pack(down.getX(), down.getY(), down.getZ()));
            if (query == null) {
                return;
            }
            final long[][] longs = data.get(key);
            final int id = ChunkSectionRegionsImpl.unpackCustomPosCompact(query.id());
            if (id < longs.length) {
                final Vec3d cameraPos = context.camera().getPos();
                final MatrixStack stack = context.matrixStack();
                stack.push();
                stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                for (final long packed : longs[id]) {
                    tryRender(packed, key, context);
                }
                stack.pop();
            }
        }
    }

    private void tryRender(final long packed, final ChunkRegionifier<?> regionifier, final WorldRenderContext context) {
        final Map<ChunkSectionPos, DebugSectionInfo<?>> map = AdvancedAiClient.DEBUG_INFOS.get(DebugSectionType.REGION_DEBUG_TYPE);
        if (map == null) {
            return;
        }
        final ChunkSectionPos unpacked = ChunkSectionRegionsImpl.unpackChunkSectionPosCompact(packed, context.world());
        final DebugSectionInfo<RegionDebugSection> info = (DebugSectionInfo<RegionDebugSection>) map.get(unpacked);
        if (info == null) {
            return;
        }
        final ChunkSectionRegions regions = info.data().get(regionifier);
        if (regions == null) {
            return;
        }
        render(regions, unpacked, context, packed);
    }

    private void render(final ChunkSectionRegions regions, final ChunkSectionPos pos, final WorldRenderContext context, final long region) {
        final MatrixStack stack = context.matrixStack();
        stack.push();
        stack.translate(pos.getMinX(), pos.getMinY(), pos.getMinZ());
        final VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getLines());
        if ((region & ~ChunkSectionRegions.PREFIX_MASK) == 4095) {
            final Vec3d color = Vec3d.unpackRgb((int) HashCommon.murmurHash3(region));
            WorldRenderer.drawBox(stack, vertexConsumer, 0, 0, 0, 16, 16, 16, (float) color.x, (float) color.y, (float) color.z, 1.0F);
        } else {
            final BitSetVoxelSet shape = new BitSetVoxelSet(16, 16, 16);
            final ChunkSectionRegion r = regions.byId(region);
            if (r == null) {
                return;
            }
            final PackedList list = r.all();
            final int size = list.size();
            for (int j = 0; j < size; j++) {
                final int packed = list.get(j);
                final int x = LocationCacheSection.unpackX(packed);
                final int y = LocationCacheSection.unpackY(packed);
                final int z = LocationCacheSection.unpackZ(packed);
                shape.set(x, y, z);
            }

            final Vec3d color = Vec3d.unpackRgb((int) HashCommon.murmurHash3(region));
            shape.forEachBox((x1, y1, z1, x2, y2, z2) -> WorldRenderer.drawBox(stack, vertexConsumer, x1 + 0.125, y1 + 0.125, z1 + 0.125, x2 - 0.125, y2 - 0.125, z2 - 0.125, (float) color.x, (float) color.y, (float) color.z, 1.0F), true);
        }
        stack.pop();
    }
}
