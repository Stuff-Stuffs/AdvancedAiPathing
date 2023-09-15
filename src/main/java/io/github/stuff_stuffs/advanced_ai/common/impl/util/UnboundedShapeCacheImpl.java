package io.github.stuff_stuffs.advanced_ai.common.impl.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class UnboundedShapeCacheImpl extends AbstractShapeCache {
    private final Long2ObjectMap<WrappedChunk> chunkCache;

    public UnboundedShapeCacheImpl(final World world, final int cacheSize) {
        super(world, cacheSize);
        chunkCache = new Long2ObjectOpenHashMap<>(48);
    }

    @Override
    protected @Nullable Chunk getChunk(final int chunkX, final int chunkZ) {
        final long key = pack(chunkX, chunkZ);
        final WrappedChunk chunk = chunkCache.get(key);
        if (chunk != null) {
            return chunk.chunk;
        }
        if (delegate.isChunkLoaded(chunkX, chunkZ)) {
            final Chunk loaded = delegate.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            chunkCache.put(key, new WrappedChunk(loaded));
            return loaded;
        } else {
            chunkCache.put(key, new WrappedChunk(null));
            return null;
        }
    }

    private static long pack(final int x, final int z) {
        return ((long) x << 32L) | ((long) z & 0xFFFF_FFFFL);
    }

    private record WrappedChunk(@Nullable Chunk chunk) {
    }
}
