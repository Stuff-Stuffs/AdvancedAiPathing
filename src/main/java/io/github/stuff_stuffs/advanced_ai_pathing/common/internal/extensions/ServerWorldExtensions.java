package io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.Map;
import java.util.function.Function;

public interface ServerWorldExtensions {
    void advanced_ai_pathing$invalidate(long chunkSectionPos);

    static void invalidate(final Long2ObjectMap<boolean[]> map, final long chunkSectionPos, final World world) {
        final long chunkPos = ChunkPos.toLong(ChunkSectionPos.unpackX(chunkSectionPos), ChunkSectionPos.unpackZ(chunkSectionPos));
        boolean[] invalidated = map.get(chunkPos);
        final boolean p;
        if (invalidated == null) {
            p = true;
            invalidated = new boolean[world.countVerticalSections()];
        } else {
            p = false;
        }
        final int y = ChunkSectionPos.unpackY(chunkSectionPos);
        invalidated[world.sectionCoordToIndex(y)] = true;
        if (p) {
            map.put(chunkPos, invalidated);
        }
    }

    static void purge(final Long2ObjectMap<boolean[]> map, final World world) {
        final Map<ChunkPos, Chunk> cache = new Object2ReferenceOpenHashMap<>();
        final Function<ChunkPos, Chunk> cacheGetter = pos -> world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        for (final Long2ObjectMap.Entry<boolean[]> entry : map.long2ObjectEntrySet()) {
            final ChunkPos key = new ChunkPos(entry.getLongKey());
            final boolean[] invalids = entry.getValue();
            for (int i = 0; i < invalids.length; i++) {
                if (invalids[i]) {
                    for (int xOff = -1; xOff <= 1; xOff++) {
                        for (int zOff = -1; zOff <= 1; zOff++) {
                            final ChunkPos cursor = new ChunkPos(key.x + xOff, key.z + zOff);
                            final Chunk chunk = cache.computeIfAbsent(cursor, cacheGetter);
                            if (chunk == null) {
                                continue;
                            }
                            for (int yOff = -1; yOff <= 1; yOff++) {
                                final int y = i + yOff;
                                if (y < 0 || y >= world.countVerticalSections()) {
                                    continue;
                                }
                                final ChunkSection section = chunk.getSection(y);
                                if (section == null) {
                                    continue;
                                }
                                ((ChunkSectionExtensions) section).advanced_ai_pathing$sectionData().purgeAll();
                            }
                        }
                    }
                }
            }
        }
    }
}
