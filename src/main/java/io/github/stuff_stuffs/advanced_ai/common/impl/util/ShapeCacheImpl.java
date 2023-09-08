package io.github.stuff_stuffs.advanced_ai.common.impl.util;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.SectionData;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.ChunkSection;

import java.util.Arrays;

public class ShapeCacheImpl extends ChunkCache implements ShapeCache {
    private static final long DEFAULT_KEY = HashCommon.mix(BlockPos.asLong(0, 2049, 0));
    private static final BlockState AIR;
    private static final VoxelShape EMPTY;
    private final int cacheMask;
    private final long[] keys;
    private final BlockPos.Mutable mutable = new BlockPos.Mutable();
    private final BlockState[] blockStates;
    private final VoxelShape[] collisionShapes;

    public ShapeCacheImpl(final World world, final BlockPos minPos, final BlockPos maxPos, final int cacheSize) {
        super(world, minPos, maxPos);
        if ((cacheSize & cacheSize - 1) != 0) {
            throw new IllegalArgumentException("Cache size must be a power of 2!");
        }
        cacheMask = cacheSize - 1;
        keys = new long[cacheSize];
        blockStates = new BlockState[cacheSize];
        collisionShapes = new VoxelShape[cacheSize];
        Arrays.fill(keys, DEFAULT_KEY);
    }

    @Override
    public World getDelegate() {
        return world;
    }

    @Override
    public <T> T getLocationCache(final int x, final int y, final int z, final T defRet, final LocationClassifier<T> classifier) {
        if (isOutOfHeightLimit(y)) {
            return defRet;
        }
        final Chunk chunk = getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return defRet;
        }
        final ChunkSection section = chunk.getSection(world.sectionCoordToIndex(y >> 4));
        final SectionData data = ((ChunkSectionExtensions) section).advanced_ai$sectionData();
        final LocationCacheSection<T> cacheSection = data.get(classifier);
        if (cacheSection != null) {
            return cacheSection.get(x, y, z);
        }
        return defRet;
    }

    private Chunk getChunk(final int chunkX, final int chunkZ) {
        final int k = chunkX - minX;
        final int l = chunkZ - minZ;
        return k >= 0 && k < chunks.length && l >= 0 && l < chunks[k].length ? chunks[k][l] : null;
    }

    private void populateCache(final int x, final int y, final int z, final long idx, final int pos) {
        final Chunk chunk = getChunk(x >> 4, z >> 4);
        if (chunk != null) {
            final ChunkSection chunkSection = chunk.getSectionArray()[world.getSectionIndex(y)];
            keys[pos] = idx;
            if (chunkSection == null) {
                blockStates[pos] = AIR;
                collisionShapes[pos] = EMPTY;
            } else {
                final BlockState state = chunkSection.getBlockState(x & 15, y & 15, z & 15);
                blockStates[pos] = state;
                collisionShapes[pos] = state.getCollisionShape(world, mutable.set(x, y, z));
            }
        } else {
            keys[pos] = idx;
            blockStates[pos] = AIR;
            collisionShapes[pos] = EMPTY;
        }
    }

    @Override
    public BlockState getBlockState(final int x, final int y, final int z) {
        if (world.isOutOfHeightLimit(y)) {
            return AIR;
        } else {
            final long idx = HashCommon.mix(BlockPos.asLong(x, y, z));
            final int pos = (int) (idx & (long) cacheMask);
            if (keys[pos] != idx) {
                populateCache(x, y, z, idx, pos);
            }
            return blockStates[pos];
        }
    }

    @Override
    public VoxelShape getCollisionShape(final int x, final int y, final int z) {
        if (world.isOutOfHeightLimit(y)) {
            return EMPTY;
        } else {
            final long idx = HashCommon.mix(BlockPos.asLong(x, y, z));
            final int pos = (int) (idx & (long) cacheMask);
            if (keys[pos] != idx) {
                populateCache(x, y, z, idx, pos);
            }
            return collisionShapes[pos];
        }
    }

    @Override
    public Chunk getChunk(final int x, final int y, final int z) {
        return getChunk(x >> 4, z >> 4);
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    static {
        AIR = Blocks.AIR.getDefaultState();
        EMPTY = VoxelShapes.empty();
    }
}
