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
