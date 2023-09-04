package io.github.stuff_stuffs.advanced_ai.common.api.util;

import io.github.stuff_stuffs.advanced_ai.common.impl.ShapeCacheImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public interface ShapeCache extends BlockView {
    World getDelegate();

    BlockState getBlockState(int x, int y, int z);

    VoxelShape getCollisionShape(int x, int y, int z);

    Chunk getChunk(int x, int y, int z);

    static int computeCacheSize(BlockPos minPos, BlockPos maxPos) {
        if (minPos.compareTo(maxPos) >= 0) {
            throw new IllegalArgumentException("Argument minPos must be less than maxPos!");
        } else {
            ChunkPos min = new ChunkPos(minPos);
            ChunkPos max = new ChunkPos(maxPos);
            int xSideLength = max.getEndX() - min.getStartX();
            int zSideLength = max.getEndZ() - min.getStartZ();
            int averageSideLength = (xSideLength + zSideLength) / 2;
            int cacheSizeTarget = averageSideLength * 4 - 1;
            cacheSizeTarget |= cacheSizeTarget >>> 1;
            cacheSizeTarget |= cacheSizeTarget >>> 2;
            cacheSizeTarget |= cacheSizeTarget >>> 4;
            cacheSizeTarget |= cacheSizeTarget >>> 8;
            cacheSizeTarget |= cacheSizeTarget >>> 16;
            ++cacheSizeTarget;
            int minCacheSize = 16;
            int maxCacheSize = 8192;
            return Math.max(Math.min(cacheSizeTarget, maxCacheSize), minCacheSize);
        }
    }

    static ShapeCache create(World world, BlockPos minPos, BlockPos maxPos) {
        return create(world, minPos, maxPos, computeCacheSize(minPos, maxPos));
    }

    static ShapeCache create(World world, BlockPos minPos, BlockPos maxPos, int cacheSize) {
        if (minPos.compareTo(maxPos) >= 0) {
            throw new IllegalArgumentException("Argument minPos must be less than maxPos!");
        } else if ((cacheSize & cacheSize - 1) != 0) {
            throw new IllegalArgumentException("Cache size must be a power of 2!");
        } else {
            return new ShapeCacheImpl(world, minPos, maxPos, cacheSize);
        }
    }
}
