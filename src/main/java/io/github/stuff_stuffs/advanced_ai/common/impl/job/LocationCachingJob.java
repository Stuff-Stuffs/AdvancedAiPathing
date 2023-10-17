package io.github.stuff_stuffs.advanced_ai.common.impl.job;

import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJob;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.impl.pathing.location_cache.LocationCacheSectionImpl;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAiPathing;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.MemorizingChunkSection;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;

public class LocationCachingJob<T> implements AiJob {
    private final ChunkSectionPos pos;
    private final ServerWorld world;
    private final LocationClassifier<T> classifier;
    private LocationCacheSection<T> section;

    public LocationCachingJob(final ChunkSectionPos pos, final ServerWorld world, final LocationClassifier<T> classifier) {
        this.pos = pos;
        this.world = world;
        this.classifier = classifier;
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
        final int minX = pos.getMinX();
        final int minY = pos.getMinY();
        final int minZ = pos.getMinZ();
        final ShapeCache cache = ShapeCache.create(world, new BlockPos(minX - 16, minY - 16, minZ - 16), new BlockPos(minX + 31, minY + 31, minZ + 31), 2048);
        if (tryRebuild(pos, cache, classifier)) {
            return true;
        }
        section = new LocationCacheSectionImpl<>(collectModCounts(pos, cache), classifier, pos, cache);
        return true;
    }

    @Override
    public void apply(final Logger logger) {
        if (section == null || !world.isChunkLoaded(pos.getSectionX(), pos.getSectionZ())) {
            return;
        }
        final Chunk chunk = world.getChunk(pos.getSectionX(), pos.getSectionZ(), ChunkStatus.FULL, false);
        final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
        if (chunk == null || yIndex < 0 || yIndex >= world.countVerticalSections()) {
            return;
        }
        ((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai_pathing$sectionData().put(classifier, section);
    }

    @Override
    public Object debugData() {
        return "LocationCachingJob{" + pos.getSectionX() + "," + pos.getSectionY() + "," + pos.getSectionZ() + "}" + "@" + LocationClassifier.REGISTRY.getId(classifier);
    }

    public static <T> boolean tryRebuild(final ChunkSectionPos p, final ShapeCache cache, final LocationClassifier<T> classifier) {
        final Chunk centerChunk = cache.getChunk(p.getMinX(), p.getMinY(), p.getMinZ());
        if (centerChunk == null) {
            return true;
        }
        final BlockState[] oldArr = new BlockState[AdvancedAiPathing.UPDATES_BEFORE_REBUILD];
        final BlockState[] newArr = new BlockState[AdvancedAiPathing.UPDATES_BEFORE_REBUILD];
        final short[] coords = new short[AdvancedAiPathing.UPDATES_BEFORE_REBUILD];
        final LocationCacheSection<T> stale = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai_pathing$sectionData().getPossibleStale(classifier);
        if (stale == null) {
            return false;
        }
        final long[] modCounts = stale.modCounts();
        final int sectionCount = cache.countVerticalSections();
        final int bottomCoord = cache.getBottomY();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                final Chunk c = cache.getChunk((p.getSectionX() + i) << 4, 0, (p.getSectionZ() + j) << 4);
                for (int k = -1; k <= 1; k++) {
                    final int yIndex = cache.sectionCoordToIndex(p.getSectionY() + k);
                    if (yIndex < 0 || yIndex >= sectionCount) {
                        continue;
                    }
                    final int modCountIndex = LocationCacheSection.modCountIndex(i, k, j);
                    if (c == null) {
                        if (modCounts[modCountIndex] != -1) {
                            return false;
                        }
                    } else {
                        final MemorizingChunkSection section = (MemorizingChunkSection) c.getSection(yIndex);
                        final long modCount = section.advanced_ai_pathing$modCount();
                        final long currentModCount = modCounts[modCountIndex];
                        if (modCount == currentModCount) {
                            continue;
                        }
                        if (modCount < modCounts[modCountIndex] || modCount >= currentModCount + AdvancedAiPathing.UPDATES_BEFORE_REBUILD) {
                            return false;
                        }
                        if (!section.advanced_ai_pathing$copy_updates(currentModCount, oldArr, 0, newArr, 0, coords, 0)) {
                            return false;
                        }
                        final int count = (int) (modCount - currentModCount);
                        modCounts[modCountIndex] = modCount;
                        if (count == 0) {
                            continue;
                        }
                        final int baseX = i * 16;
                        final int baseY = k * 16 + bottomCoord;
                        final int baseZ = j * 16;
                        for (int l = 0; l < count; l++) {
                            if (classifier.needsRebuild(p.getSectionX(), p.getSectionY(), p.getSectionZ(), p.getSectionX() + i, p.getSectionY() + k, p.getSectionZ() + j, baseX + LocationCacheSection.unpackX(coords[l]), baseY + LocationCacheSection.unpackY(coords[l]), baseZ + LocationCacheSection.unpackZ(coords[l]), cache)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        final LocationCacheSection<T> promote = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai_pathing$sectionData().promote(classifier);
        return promote != null;
    }

    public static long[] collectModCounts(final ChunkSectionPos p, final ShapeCache cache) {
        final long[] modCounts = new long[27];
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                final Chunk c = cache.getChunk((p.getSectionX() + i) << 4, 0, (p.getSectionZ() + j) << 4);
                for (int k = -1; k <= 1; k++) {
                    final int yIndex = cache.sectionCoordToIndex(p.getSectionY() + j);
                    final int modCountIndex = LocationCacheSection.modCountIndex(i, j, k);
                    if (c == null || yIndex < 0 || yIndex >= cache.countVerticalSections()) {
                        modCounts[modCountIndex] = -1;
                    } else {
                        final MemorizingChunkSection section = (MemorizingChunkSection) c.getSection(yIndex);
                        final long modCount = section.advanced_ai_pathing$modCount();
                        modCounts[modCountIndex] = modCount;
                    }
                }
            }
        }
        return modCounts;
    }
}
