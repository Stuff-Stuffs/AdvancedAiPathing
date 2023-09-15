package io.github.stuff_stuffs.advanced_ai.common.impl.util;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.SectionData;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public abstract class AbstractShapeCache implements ShapeCache {
    protected static final long DEFAULT_KEY = HashCommon.mix(BlockPos.asLong(0, 2049, 0));
    protected static final BlockState AIR;
    protected static final VoxelShape EMPTY;
    protected final int cacheMask;
    private final long[] keys;
    protected final BlockPos.Mutable mutable = new BlockPos.Mutable();
    private final BlockState[] blockStates;
    private final VoxelShape[] collisionShapes;
    protected final World delegate;

    public AbstractShapeCache(final World world, final int cacheSize) {
        if ((cacheSize & cacheSize - 1) != 0) {
            throw new IllegalArgumentException("Cache size must be a power of 2!");
        }
        delegate = world;
        cacheMask = cacheSize - 1;
        keys = new long[cacheSize];
        blockStates = new BlockState[cacheSize];
        collisionShapes = new VoxelShape[cacheSize];
        Arrays.fill(keys, DEFAULT_KEY);
    }

    @Override
    public World getDelegate() {
        return delegate;
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
        final ChunkSection section = chunk.getSection(delegate.getSectionIndex(y));
        final SectionData data = ((ChunkSectionExtensions) section).advanced_ai$sectionData();
        final LocationCacheSection<T> cacheSection = data.getLocationCache(classifier);
        if (cacheSection != null) {
            return cacheSection.get(x, y, z);
        }
        return defRet;
    }

    protected Chunk getChunk(final BlockPos pos) {
        return getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
    }

    protected abstract @Nullable Chunk getChunk(final int chunkX, final int chunkZ);

    private void populateCache(final int x, final int y, final int z, final long idx, final int pos) {
        final Chunk chunk = getChunk(x >> 4, z >> 4);
        if (chunk != null) {
            final ChunkSection chunkSection = chunk.getSectionArray()[delegate.getSectionIndex(y)];
            keys[pos] = idx;
            if (chunkSection == null) {
                blockStates[pos] = AIR;
                collisionShapes[pos] = EMPTY;
            } else {
                final BlockState state = chunkSection.getBlockState(x & 15, y & 15, z & 15);
                blockStates[pos] = state;
                collisionShapes[pos] = state.getCollisionShape(delegate, mutable.set(x, y, z));
            }
        } else {
            keys[pos] = idx;
            blockStates[pos] = AIR;
            collisionShapes[pos] = EMPTY;
        }
    }

    @Override
    public BlockState getBlockState(final int x, final int y, final int z) {
        if (delegate.isOutOfHeightLimit(y)) {
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

    @Nullable
    @Override
    public BlockEntity getBlockEntity(final BlockPos pos) {
        final Chunk chunk = getChunk(pos);
        return chunk.getBlockEntity(pos);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        if (isOutOfHeightLimit(pos)) {
            return Fluids.EMPTY.getDefaultState();
        } else {
            final Chunk chunk = getChunk(pos);
            return chunk.getFluidState(pos);
        }
    }

    @Override
    public VoxelShape getCollisionShape(final int x, final int y, final int z) {
        if (delegate.isOutOfHeightLimit(y)) {
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

    @Override
    public int getBottomY() {
        return delegate.getBottomY();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    static {
        AIR = Blocks.AIR.getDefaultState();
        EMPTY = VoxelShapes.empty();
    }
}
