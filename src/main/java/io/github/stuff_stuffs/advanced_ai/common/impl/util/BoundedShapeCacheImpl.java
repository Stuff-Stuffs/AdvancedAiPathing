package io.github.stuff_stuffs.advanced_ai.common.impl.util;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import org.jetbrains.annotations.Nullable;

public class BoundedShapeCacheImpl extends AbstractShapeCache {
    protected final int minX;
    protected final int minZ;
    protected final Chunk[][] chunks;

    public BoundedShapeCacheImpl(final BlockPos minPos, final BlockPos maxPos, final World world, final int cacheSize) {
        super(world, cacheSize);
        minX = ChunkSectionPos.getSectionCoord(minPos.getX());
        minZ = ChunkSectionPos.getSectionCoord(minPos.getZ());
        final int i = ChunkSectionPos.getSectionCoord(maxPos.getX());
        final int j = ChunkSectionPos.getSectionCoord(maxPos.getZ());
        chunks = new Chunk[i - minX + 1][j - minZ + 1];
        final ChunkManager chunkManager = world.getChunkManager();

        for (int k = minX; k <= i; ++k) {
            for (int l = minZ; l <= j; ++l) {
                chunks[k - minX][l - minZ] = chunkManager.getWorldChunk(k, l);
            }
        }
    }

    @Override
    protected @Nullable Chunk getChunk(final int chunkX, final int chunkZ) {
        final int i = chunkX - minX;
        final int j = chunkZ - minZ;
        if (i >= 0 && i < chunks.length && j >= 0 && j < chunks[i].length) {
            return chunks[i][j];
        } else {
            return null;
        }
    }
}
