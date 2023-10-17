package io.github.stuff_stuffs.advanced_ai.common.api.pathing.region;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import it.unimi.dsi.fastutil.shorts.ShortPriorityQueue;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public abstract class AbstractChunkRegionifier<T> implements ChunkRegionifier<T> {
    @Override
    public ChunkSectionRegions regionify(final ChunkSectionPos pos, final ShapeCache cache) {
        final ShortPriorityQueue queue = new ShortArrayFIFOQueue(16 * 16);
        final ChunkSectionRegions.Builder builder = ChunkSectionRegions.builder(pos, cache);
        final T defaultReturn = defaultReturn();
        final LocationClassifier<T> classifier = classifier();
        final int baseX = pos.getMinX();
        final int baseY = pos.getMinY();
        final int baseZ = pos.getMinZ();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    final short packed = LocationCacheSection.pack(x, y, z);
                    if (!builder.contains(packed)) {
                        final int rx = baseX + x;
                        final int ry = baseY + y;
                        final int rz = baseZ + z;
                        final T val = cache.getLocationCache(rx, ry, rz, defaultReturn, classifier);
                        if (valid(val)) {
                            final ChunkSectionRegions.RegionKey key = builder.newRegion();
                            final LocalPosConsumer consumer = s -> {
                                builder.expand(key, s);
                                queue.enqueue(s);
                            };
                            builder.expand(key, packed);
                            enqueueStrongAdjacent(val, rx, ry, rz, pos, cache, consumer, builder);
                            while (!queue.isEmpty()) {
                                final short s = queue.dequeueShort();
                                final int ox = LocationCacheSection.unpackX(s) + baseX;
                                final int oy = LocationCacheSection.unpackY(s) + baseY;
                                final int oz = LocationCacheSection.unpackZ(s) + baseZ;
                                final T v = cache.getLocationCache(ox, oy, oz, defaultReturn, classifier);
                                enqueueStrongAdjacent(v, ox, oy, oz, pos, cache, consumer, builder);
                            }
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    @Override
    public ChunkSectionLinkedRegions link(final ChunkSectionPos pos, final ChunkSectionRegions regions, final ShapeCache cache) {
        final int baseX = pos.getMinX();
        final int baseY = pos.getMinY();
        final int baseZ = pos.getMinZ();
        final int count = regions.regionCount();
        final LongSet visited = new LongOpenHashSet();
        final ChunkSectionLinkedRegions.Builder builder = ChunkSectionLinkedRegions.builder(regions);
        for (int i = 0; i < count; i++) {
            final long currentRegion = regions.prefix() | i;
            final ChunkSectionRegion region = regions.byId(currentRegion);
            final PackedList list = region.all();
            final int size = list.size();
            final PosConsumer posConsumer = (x, y, z) -> {
                if (cache.isOutOfHeightLimit(y)) {
                    return;
                }
                final long packed = BlockPos.asLong(x, y, z);
                if (visited.add(packed)) {
                    final Chunk chunk = cache.getChunk(x, y, z);
                    final ChunkSection section = chunk.getSection(cache.sectionCoordToIndex(y >> 4));
                    final ChunkSectionRegions r = ((ChunkSectionExtensions) section).advanced_ai_pathing$sectionData().getRegions(AbstractChunkRegionifier.this);
                    if (r == null) {
                        throw new NullPointerException();
                    }
                    final ChunkSectionRegion query = r.query(LocationCacheSection.pack(x, y, z));
                    final long linkId;
                    if (query == null) {
                        linkId = ChunkSectionRegions.packChunkSectionPosCompact(x >> 4, y >> 4, z >> 4, cache) | ~ChunkSectionRegions.PREFIX_MASK;
                    } else {
                        linkId = query.id();
                    }
                    builder.link(currentRegion, linkId);
                }
            };
            for (int j = 0; j < size; j++) {
                final int s = list.get(j);
                final int ox = LocationCacheSection.unpackX(s) + baseX;
                final int oy = LocationCacheSection.unpackY(s) + baseY;
                final int oz = LocationCacheSection.unpackZ(s) + baseZ;
                final T val = cache.getLocationCache(ox, oy, oz, defaultReturn(), classifier());
                weakAdjacent(val, ox, oy, oz, pos, cache, posConsumer);
            }
        }
        return builder.build();
    }

    protected abstract T defaultReturn();

    protected abstract boolean valid(T val);

    protected abstract void enqueueStrongAdjacent(T val, int x, int y, int z, ChunkSectionPos sectionPos, ShapeCache cache, LocalPosConsumer consumer, ChunkSectionRegions.Builder builder);

    protected abstract void weakAdjacent(T val, int x, int y, int z, ChunkSectionPos sectionPos, ShapeCache cache, PosConsumer consumer);

    public interface LocalPosConsumer {
        void accept(short s);
    }

    public interface PosConsumer {
        void accept(int x, int y, int z);
    }
}
