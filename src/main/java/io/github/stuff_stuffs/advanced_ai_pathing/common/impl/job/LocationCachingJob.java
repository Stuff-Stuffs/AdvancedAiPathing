package io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.pathing.location_cache.LocationCacheSectionImpl;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.AdvancedAiPathing;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.MemorizingChunkSection;
import it.unimi.dsi.fastutil.shorts.Short2ByteMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

public class LocationCachingJob<T> {
    public final ChunkSectionPos pos;
    private final ServerWorld world;
    public final LocationClassifier<T> classifier;

    public LocationCachingJob(final ChunkSectionPos pos, final ServerWorld world, final LocationClassifier<T> classifier) {
        this.pos = pos;
        this.world = world;
        this.classifier = classifier;
    }

    public void run() {
        if (!world.isChunkLoaded(pos.getSectionX(), pos.getSectionZ())) {
            return;
        }
        final Chunk chunk = world.getChunk(pos.getSectionX(), pos.getSectionZ(), ChunkStatus.FULL, false);
        final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
        if (chunk == null || yIndex < 0 || yIndex >= world.countVerticalSections()) {
            return;
        }
        final int minX = pos.getMinX();
        final int minY = pos.getMinY();
        final int minZ = pos.getMinZ();
        final ShapeCache cache = ShapeCache.create(world, new BlockPos(minX - 16, minY - 16, minZ - 16), new BlockPos(minX + 31, minY + 31, minZ + 31), 2048);
        if (tryRebuild(pos, cache, classifier)) {
            return;
        }
        LocationCacheSection<T> section = new LocationCacheSectionImpl<>(collectModCounts(pos, cache), classifier, pos, cache);
        ((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai_pathing$sectionData().put(classifier, section);
    }

    public String debugData() {
        return "LocationCachingJob{" + pos.getSectionX() + "," + pos.getSectionY() + "," + pos.getSectionZ() + "}" + "@" + LocationClassifier.REGISTRY.getId(classifier);
    }

    public static <T> boolean tryRebuild(final ChunkSectionPos p, final ShapeCache cache, final LocationClassifier<T> classifier) {
        final Chunk centerChunk = cache.getChunk(p.getMinX(), p.getMinY(), p.getMinZ());
        if (centerChunk == null) {
            return true;
        }
        final LocationCacheSection<T> stale = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai_pathing$sectionData().getPossibleStale(classifier);
        if (stale == null) {
            return false;
        }
        final BlockState[] oldArr = new BlockState[AdvancedAiPathing.UPDATES_BEFORE_REBUILD];
        final BlockState[] newArr = new BlockState[AdvancedAiPathing.UPDATES_BEFORE_REBUILD];
        final short[] coords = new short[AdvancedAiPathing.UPDATES_BEFORE_REBUILD];
        Short2ByteMap first = new Short2ByteOpenHashMap(AdvancedAiPathing.UPDATES_BEFORE_REBUILD * 2);
        ShortSet set = new ShortOpenHashSet(AdvancedAiPathing.UPDATES_BEFORE_REBUILD * 2);
        final long[] modCounts = stale.modCounts();
        final int sectionCount = cache.countVerticalSections();
        BlockPos.Mutable scratch = new BlockPos.Mutable();
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
                        final int count = (int) (modCount - currentModCount);
                        if (count == 0) {
                            continue;
                        }
                        modCounts[modCountIndex] = modCount;
                        if (modCount == currentModCount) {
                            continue;
                        }
                        if (modCount < modCounts[modCountIndex] || modCount >= currentModCount + AdvancedAiPathing.UPDATES_BEFORE_REBUILD) {
                            return false;
                        }
                        if (!section.advanced_ai_pathing$copy_updates(currentModCount, oldArr, 0, newArr, 0, coords, 0)) {
                            return false;
                        }
                        first.clear();
                        for (byte l = 0; l < count; l++) {
                            first.putIfAbsent(coords[l], l);
                        }
                        final int baseX = i * 16;
                        final int baseY = k * 16;
                        final int baseZ = j * 16;
                        for (int l = count-1; l >= 0; l--) {
                            final short coord = coords[l];
                            if(!set.add(coord)) {
                                continue;
                            }
                            int f = first.get(coord);
                            if(f!=l && oldArr[f] == newArr[l]) {
                                continue;
                            }
                            if (
                                    classifier.needsRebuild(
                                            p.getSectionX(),
                                            p.getSectionY(),
                                            p.getSectionZ(),
                                            p.getSectionX() + i,
                                            p.getSectionY() + k,
                                            p.getSectionZ() + j,
                                            baseX + LocationCacheSection.unpackX(coord),
                                            baseY + LocationCacheSection.unpackY(coord),
                                            baseZ + LocationCacheSection.unpackZ(coord),
                                            cache,
                                            oldArr[l],
                                            newArr[l],
                                            scratch)
                            ) {
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
                    final int yIndex = cache.sectionCoordToIndex(p.getSectionY() + k);
                    final int modCountIndex = LocationCacheSection.modCountIndex(i, k, j);
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
