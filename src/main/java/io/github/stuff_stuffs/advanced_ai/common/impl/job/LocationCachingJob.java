package io.github.stuff_stuffs.advanced_ai.common.impl.job;

import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionInfo;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.LocationCacheDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJob;
import io.github.stuff_stuffs.advanced_ai.common.api.job.AiJobHandle;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.impl.DenseLocationCacheSectionImpl;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import io.github.stuff_stuffs.advanced_ai.common.internal.ProcessedLocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.MemorizingChunkSection;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerWorldExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;

import java.util.Map;

public class LocationCachingJob<T> implements AiJob {
    private final ChunkSectionPos pos;
    private final ServerWorld world;
    private final LocationClassifier<T> classifier;

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
        final int minY = pos.getMinY() + world.getBottomY();
        final int minZ = pos.getMinZ();
        final ShapeCache cache = ShapeCache.create(world, new BlockPos(minX - 16, minY - 16, minZ - 16), new BlockPos(minX + 31, minY + 31, minZ + 31), 2048);
        if (tryRebuild(pos, cache, classifier)) {
            return true;
        }
        final LocationCacheSection<T> section = new DenseLocationCacheSectionImpl<>(collectModCounts(pos, cache), cache, pos, (ProcessedLocationClassifier<T>) AdvancedAi.PROCESSED_LOCATION_CLASSIFIERS.get(classifier));
        ((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai$sectionData().put(classifier, section);
        ((ServerWorldExtensions) world).advanced_ai$debug(new DebugSectionInfo<>(new LocationCacheDebugSection(Map.of(classifier, new LocationCacheDebugSection.Entry<>(classifier, section))), pos, DebugSectionType.LOCATION_CACHE_TYPE));
        return true;
    }

    @Override
    public Object debugData() {
        return "LocationCachingJob{" + pos.getSectionX() + "," + pos.getSectionY() + "," + pos.getSectionZ() + "}" + "@" + LocationClassifier.REGISTRY.getId(classifier);
    }

    public static <T> boolean tryRebuild(final ChunkSectionPos p, final ShapeCache cache, final LocationClassifier<T> classifier) {
        final Chunk centerChunk = cache.getChunk(p.getMinX(), p.getMinY() + cache.getBottomY(), p.getMinZ());
        if (centerChunk == null) {
            return true;
        }
        final BlockState[] oldArr = new BlockState[AdvancedAi.UPDATES_BEFORE_REBUILD];
        final BlockState[] newArr = new BlockState[AdvancedAi.UPDATES_BEFORE_REBUILD];
        final short[] coords = new short[AdvancedAi.UPDATES_BEFORE_REBUILD];
        final LocationCacheSection<T> stale = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai$sectionData().getStale(classifier);
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
                        final long modCount = section.advanced_ai$modCount();
                        final long currentModCount = modCounts[modCountIndex];
                        if (modCount == currentModCount) {
                            continue;
                        }
                        if (modCount < modCounts[modCountIndex] || modCount >= currentModCount + AdvancedAi.UPDATES_BEFORE_REBUILD) {
                            return false;
                        }
                        if (!section.advanced_ai$copy_updates(currentModCount, oldArr, 0, newArr, 0, coords, 0)) {
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
        final LocationCacheSection<T> promote = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai$sectionData().promote(classifier);
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
                        final long modCount = section.advanced_ai$modCount();
                        modCounts[modCountIndex] = modCount;
                    }
                }
            }
        }
        return modCounts;
    }
}
