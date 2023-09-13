package io.github.stuff_stuffs.advanced_ai.common.impl.job;

import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionInfo;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.RegionDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJob;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerWorldExtensions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;

import java.util.Map;

public class ChunkRegionJob implements AiJob {
    public final ChunkSectionPos pos;
    public final ServerWorld world;
    public final ChunkRegionifier<?> regionifier;
    private ChunkSectionRegions regions;

    public ChunkRegionJob(final ChunkSectionPos pos, final ServerWorld world, final ChunkRegionifier<?> regionifier) {
        this.pos = pos;
        this.world = world;
        this.regionifier = regionifier;
    }

    @Override
    public boolean run(final Logger logger) {
        if (!world.isChunkLoaded(pos.getSectionX(), pos.getSectionZ())) {
            return true;
        }
        final Chunk chunk = world.getChunk(pos.getSectionX(), pos.getSectionZ(), ChunkStatus.FULL, false);
        final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
        if (chunk == null || yIndex < 0 || yIndex >= world.countVerticalSections()) {
            return true;
        }
        if (((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai$sectionData().getLocationCache(regionifier.classifier()) == null) {
            return true;
        }
        final int minX = pos.getMinX();
        final int minY = pos.getMinY() + world.getBottomY();
        final int minZ = pos.getMinZ();
        final ShapeCache cache = ShapeCache.create(world, new BlockPos(minX - 16, minY - 16, minZ - 16), new BlockPos(minX + 31, minY + 31, minZ + 31), 2048);
        regions = regionifier.regionify(pos, cache);
        return true;
    }

    @Override
    public void apply(final Logger logger) {
        if (regionifier == null || !world.isChunkLoaded(pos.getSectionX(), pos.getSectionZ())) {
            return;
        }
        final Chunk chunk = world.getChunk(pos.getSectionX(), pos.getSectionZ(), ChunkStatus.FULL, false);
        final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
        if (chunk == null || yIndex < 0 || yIndex >= world.countVerticalSections()) {
            return;
        }
        ((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai$sectionData().put(regionifier, regions);
        ((ServerWorldExtensions) world).advanced_ai$debug(new DebugSectionInfo<>(new RegionDebugSection(Map.of(regionifier, regions)), pos, DebugSectionType.REGION_DEBUG_TYPE));
    }

    @Override
    public Object debugData() {
        return "ChunkRegionJob{" + pos.getSectionX() + "," + pos.getSectionY() + "," + pos.getSectionZ() + "}" + "@" + ChunkRegionifier.REGISTRY.getId(regionifier);
    }
}
