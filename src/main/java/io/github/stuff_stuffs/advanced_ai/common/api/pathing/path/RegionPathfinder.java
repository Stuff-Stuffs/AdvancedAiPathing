package io.github.stuff_stuffs.advanced_ai.common.api.pathing.path;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionLinkedRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionRegion;
import io.github.stuff_stuffs.advanced_ai.common.api.util.AiUtil;
import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.pathing.AStar;
import io.github.stuff_stuffs.advanced_ai.common.internal.pathing.CostGetter;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

public class RegionPathfinder<T> extends AStar<RegionPathfinder.RegionNode, RegionPathfinder.Context, RegionPathfinder.Target> {
    protected final ChunkRegionifier<T> regionifier;

    public RegionPathfinder(final ChunkRegionifier<T> regionifier) {
        super(RegionNode.class);
        this.regionifier = regionifier;
    }

    @Override
    protected double heuristic(final RegionNode node, final Target target, final Context context) {
        if (target instanceof SingleTarget singleTarget) {
            return singleTargetHeuristic(node, singleTarget, context);
        } else if (target instanceof MetricTarget metricTarget) {
            return metricTargetHeuristic(node, metricTarget, context);
        }
        throw new AssertionError();
    }

    protected double singleTargetHeuristic(final RegionNode node, final SingleTarget target, final Context context) {
        final int chunkX = ChunkSectionRegions.unpackChunkSectionPosX(node.key);
        final int chunkZ = ChunkSectionRegions.unpackChunkSectionPosZ(node.key);
        final Chunk chunk = context.cache.getChunk(chunkX, 0, chunkZ);
        if (chunk == null) {
            return Double.POSITIVE_INFINITY;
        }
        final int chunkY = ChunkSectionRegions.unpackChunkSectionPosY(node.key, chunk);
        final ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(chunkY));
        final int targetX = target.target.getX();
        final int targetY = target.target.getY();
        final int targetZ = target.target.getZ();
        final int baseX = chunkX << 4;
        final int baseY = chunkY << 4;
        final int baseZ = chunkZ << 4;
        if ((node.key & ~ChunkSectionRegions.PREFIX_MASK) != 4095) {
            final ChunkSectionRegions regions = ((ChunkSectionExtensions) section).advanced_ai$sectionData().getRegions(regionifier);
            if (regions != null) {
                final ChunkSectionRegion region = regions.byId(node.key);
                if (region != null) {
                    if (target.scanAll) {
                        final PackedList all = region.all();
                        final int size = all.size();
                        double min = Double.POSITIVE_INFINITY;
                        for (int i = 0; i < size; i++) {
                            final int s = all.get(i);
                            final int dx = targetX - (baseX + LocationCacheSection.unpackX(s));
                            final int dy = targetY - (baseY + LocationCacheSection.unpackY(s));
                            final int dz = targetZ - (baseZ + LocationCacheSection.unpackZ(s));
                            min = Math.min(min, dx * dx + dy * dy + dz * dz);
                        }
                        return min;
                    } else {
                        final int s = region.any();
                        final int dx = targetX - (baseX + LocationCacheSection.unpackX(s));
                        final int dy = targetY - (baseY + LocationCacheSection.unpackY(s));
                        final int dz = targetZ - (baseZ + LocationCacheSection.unpackZ(s));
                        return dx * dx + dy * dy + dz * dz;
                    }
                }
            }
        }
        if (target.scanAll) {
            double min = Double.POSITIVE_INFINITY;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = 0; z < 4; z++) {
                        final int rx = x * 4 + baseX;
                        final int ry = y * 4 + baseY;
                        final int rz = z * 4 + baseZ;
                        final int dx = targetX - rx;
                        final int dy = targetY - ry;
                        final int dz = targetZ - rz;
                        min = Math.min(min, dx * dx + dy * dy + dz * dz);
                    }
                }
            }
            return min;
        } else {
            final int rx = 8 + baseX;
            final int ry = 8 + baseY;
            final int rz = 8 + baseZ;
            final int dx = targetX - rx;
            final int dy = targetY - ry;
            final int dz = targetZ - rz;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    protected double metricTargetHeuristic(final RegionNode node, final MetricTarget target, final Context context) {
        final int chunkX = ChunkSectionRegions.unpackChunkSectionPosX(node.key);
        final int chunkZ = ChunkSectionRegions.unpackChunkSectionPosZ(node.key);
        final Chunk chunk = context.cache.getChunk(chunkX, 0, chunkZ);
        if (chunk == null) {
            return Double.POSITIVE_INFINITY;
        }
        final int chunkY = ChunkSectionRegions.unpackChunkSectionPosY(node.key, chunk);
        final ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(chunkY));
        final int baseX = chunkX << 4;
        final int baseY = chunkY << 4;
        final int baseZ = chunkZ << 4;
        if ((node.key & ~ChunkSectionRegions.PREFIX_MASK) != 4095) {
            final ChunkSectionRegions regions = ((ChunkSectionExtensions) section).advanced_ai$sectionData().getRegions(regionifier);
            if (regions != null) {
                final ChunkSectionRegion region = regions.byId(node.key);
                if (region != null) {
                    if (target.scanAll()) {
                        final PackedList all = region.all();
                        final int size = all.size();
                        double min = Double.POSITIVE_INFINITY;
                        for (int i = 0; i < size; i++) {
                            final int s = all.get(i);
                            final int x = baseX + LocationCacheSection.unpackX(s);
                            final int y = baseY + LocationCacheSection.unpackY(s);
                            final int z = baseZ + LocationCacheSection.unpackZ(s);
                            min = Math.min(min, target.metric(context.cache, x, y, z));
                        }
                        return min;
                    } else {
                        final int s = region.any();
                        final int x = baseX + LocationCacheSection.unpackX(s);
                        final int y = baseY + LocationCacheSection.unpackY(s);
                        final int z = baseZ + LocationCacheSection.unpackZ(s);
                        return target.metric(context.cache, x, y, z);
                    }
                }
            }
        }
        if (target.scanAll()) {
            double min = Double.POSITIVE_INFINITY;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    for (int z = 0; z < 4; z++) {
                        final int rx = x * 4 + baseX;
                        final int ry = y * 4 + baseY;
                        final int rz = z * 4 + baseZ;
                        min = Math.min(min, target.metric(context.cache, rx, ry, rz));
                    }
                }
            }
            return min;
        } else {
            final int rx = 8 + baseX;
            final int ry = 8 + baseY;
            final int rz = 8 + baseZ;
            return target.metric(context.cache, rx, ry, rz);
        }
    }

    @Override
    protected double nodeCost(final RegionNode node) {
        return node.cost;
    }

    @Override
    protected long key(final RegionNode node) {
        return node.key;
    }

    @Override
    protected @Nullable RegionPathfinder.RegionNode previousNode(final RegionNode node) {
        return node.previous;
    }

    @Override
    protected int neighbours(final RegionNode previous, final Context context, final CostGetter costGetter, final RegionNode[] successors) {
        if ((previous.key & ~ChunkSectionRegions.PREFIX_MASK) == 4095) {
            //TODO handle ambiguous regions
            return 0;
        }
        final int chunkX = ChunkSectionRegions.unpackChunkSectionPosX(previous.key);
        final int chunkZ = ChunkSectionRegions.unpackChunkSectionPosZ(previous.key);
        final Chunk chunk = context.cache.getChunk(chunkX<<4, 0, chunkZ<<4);
        if (chunk == null) {
            return 0;
        }
        final int chunkY = ChunkSectionRegions.unpackChunkSectionPosY(previous.key, chunk);
        final ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(chunkY));
        final ChunkSectionLinkedRegions links = ((ChunkSectionExtensions) section).advanced_ai$sectionData().getLinks(regionifier);
        if (links == null) {
            return 0;
        }
        final long[] linksIds = links.links(previous.key);
        final double cost = previous.cost + AiUtil.fastApproximateLog(links.regions().byId(previous.key).all().size() * 4);
        int i = 0;
        for (final long id : linksIds) {
            successors[i++] = new RegionNode(id, cost, previous);
        }
        return i;
    }

    public record Context(ShapeCache cache, boolean thorough) {
    }

    public record RegionNode(long key, double cost, @Nullable RegionNode previous) {
    }

    public sealed interface Target {
    }

    public record SingleTarget(BlockPos target, boolean scanAll) implements Target {
    }

    public non-sealed interface MetricTarget extends Target {
        double metric(ShapeCache cache, int x, int y, int z);

        boolean scanAll();
    }
}
