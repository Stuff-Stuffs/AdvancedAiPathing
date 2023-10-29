package io.github.stuff_stuffs.advanced_ai_pathing.common.api.util;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.util.BoundedShapeCacheImpl;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.util.UnboundedShapeCacheImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

public interface ShapeCache extends BlockView {
    World getDelegate();

    <T> @Nullable T getLocationCache(int x, int y, int z, T defRet, LocationClassifier<T> classifier);

    BlockState getBlockState(int x, int y, int z);

    VoxelShape getCollisionShape(int x, int y, int z);

    Chunk getChunk(int x, int y, int z);

    static ShapeCache create(final World world, final BlockPos minPos, final BlockPos maxPos, final int cacheSize) {
        if (minPos.getX() >= maxPos.getX() || minPos.getZ() >= maxPos.getZ()) {
            throw new IllegalArgumentException("Argument minPos must be less than maxPos!");
        } else if ((cacheSize & cacheSize - 1) != 0) {
            throw new IllegalArgumentException("Cache size must be a power of 2!");
        } else {
            return new BoundedShapeCacheImpl(minPos, maxPos, world, cacheSize);
        }
    }

    static ShapeCache createUnbounded(final World world, final int cacheSize) {
        if ((cacheSize & cacheSize - 1) != 0) {
            throw new IllegalArgumentException("Cache size must be a power of 2!");
        } else {
            return new UnboundedShapeCacheImpl(world, cacheSize);
        }
    }
}
