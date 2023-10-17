package io.github.stuff_stuffs.advanced_ai.common.impl.job;

import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJob;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionLinkedRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;

public class ChunkRegionLinkJob implements AiJob {
    public final ChunkSectionPos pos;
    public final ServerWorld world;
    public final ChunkRegionifier<?> regionifier;
    private ChunkSectionLinkedRegions regions;

    public ChunkRegionLinkJob(final ChunkSectionPos pos, final ServerWorld world, final ChunkRegionifier<?> regionifier) {
        this.pos = pos;
        this.world = world;
        this.regionifier = regionifier;
    }

    @Override
    public boolean run(final Logger logger) {
        final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
        if (yIndex < 0 || yIndex >= world.countVerticalSections()) {
            return true;
        }
        ChunkSectionRegions localRegions = null;
        for (int i = -1; i < 1; i++) {
            final int adjacentYIndex = yIndex + i;
            if (adjacentYIndex < 0 || adjacentYIndex >= world.countVerticalSections()) {
                continue;
            }
            for (int j = -1; j < 1; j++) {
                final int adjacentXIndex = pos.getSectionX() + j;
                for (int k = -1; k < 1; k++) {
                    final int adjacentZIndex = pos.getSectionZ() + k;
                    if (!world.isChunkLoaded(adjacentXIndex, adjacentZIndex)) {
                        return true;
                    }
                    final Chunk chunk = world.getChunk(adjacentXIndex, adjacentZIndex, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        return true;
                    }
                    if (i == 0 && j == 0 && k == 0) {
                        localRegions = ((ChunkSectionExtensions) chunk.getSection(adjacentYIndex)).advanced_ai_pathing$sectionData().getRegions(regionifier);
                        if (localRegions == null) {
                            return true;
                        }
                    } else if (((ChunkSectionExtensions) chunk.getSection(adjacentYIndex)).advanced_ai_pathing$sectionData().getRegions(regionifier) == null) {
                        return true;
                    }
                }
            }
        }
        final int minX = pos.getMinX();
        final int minY = pos.getMinY();
        final int minZ = pos.getMinZ();
        final ShapeCache cache = ShapeCache.create(world, new BlockPos(minX - 16, minY - 16, minZ - 16), new BlockPos(minX + 31, minY + 31, minZ + 31), 2048);
        regions = regionifier.link(pos, localRegions, cache);
        return true;
    }

    @Override
    public void apply(final Logger logger) {
        if (regions == null || !world.isChunkLoaded(pos.getSectionX(), pos.getSectionZ())) {
            return;
        }
        final Chunk chunk = world.getChunk(pos.getSectionX(), pos.getSectionZ(), ChunkStatus.FULL, false);
        final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
        if (chunk == null || yIndex < 0 || yIndex >= world.countVerticalSections()) {
            return;
        }
        ((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai_pathing$sectionData().put(regionifier, regions);
    }

    @Override
    public Object debugData() {
        return "ChunkRegionLinkJob{" + pos.getSectionX() + "," + pos.getSectionY() + "," + pos.getSectionZ() + "}" + "@" + ChunkRegionifier.REGISTRY.getId(regionifier);
    }
}
