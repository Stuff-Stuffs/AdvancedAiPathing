package io.github.stuff_stuffs.advanced_ai.common.api.region;

import io.github.stuff_stuffs.advanced_ai.common.impl.region.ChunkSectionRegionsImpl;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;

public interface ChunkSectionRegions {
    long X_BITS = 22;
    long Y_BITS = 8;
    long Z_BITS = X_BITS;
    long X_SHIFT = Long.SIZE - X_BITS;
    long Y_SHIFT = X_SHIFT - Y_BITS;
    long Z_SHIFT = Y_SHIFT - Z_BITS;
    long CUSTOM_SHIFT = 0;
    long X_MASK = ((1L << X_BITS) - 1L) << X_SHIFT;
    long Y_MASK = ((1L << Y_BITS) - 1L) << Y_SHIFT;
    long Z_MASK = ((1L << Z_BITS) - 1L) << Z_SHIFT;
    long PREFIX_MASK = -4096L;

    @Nullable ChunkSectionRegion query(short packed);

    ChunkSectionRegion byId(long id);

    long prefix();

    int regionCount();

    void writeToBuf(PacketByteBuf buf);

    interface Builder {
        RegionKey newRegion();

        boolean contains(short pos);

        void expand(RegionKey key, short pos);

        ChunkSectionRegions build();
    }

    interface RegionKey {
    }

    static Builder builder(final ChunkSectionPos pos, final HeightLimitView view) {
        return new ChunkSectionRegionsImpl.BuilderImpl(pos, view);
    }

    static ChunkSectionRegions readFromBuf(final PacketByteBuf buf) {
        return new ChunkSectionRegionsImpl(buf);
    }

    static ChunkSectionPos unpackChunkSectionPosCompact(final long packed, final HeightLimitView view) {
        return ChunkSectionPos.from(unpackChunkSectionPosX(packed), unpackChunkSectionPosY(packed, view), unpackChunkSectionPosZ(packed));
    }

    static int unpackChunkSectionPosX(final long packed) {
        final int i = (int) ((packed & X_MASK) >>> X_SHIFT);
        if ((i & (1 << (X_BITS - 1))) != 0) {
            return -(i & ((1 << (X_BITS - 1)) - 1));
        }
        return i;
    }

    static int unpackChunkSectionPosY(final long packed, final HeightLimitView view) {
        final int yIndex = (int) ((packed & Y_MASK) >>> Y_SHIFT);
        return view.sectionIndexToCoord(yIndex);
    }

    static int unpackChunkSectionPosZ(final long packed) {
        final int i = (int) ((packed & Z_MASK) >>> Z_SHIFT);
        if ((i & (1 << (Z_BITS - 1))) != 0) {
            return -(i & ((1 << (Z_BITS - 1)) - 1));
        }
        return i;
    }

    static int unpackCustomPosCompact(final long packed) {
        return (int) ((packed & ~PREFIX_MASK) >>> CUSTOM_SHIFT);
    }

    static long packChunkSectionPosCompact(final ChunkSectionPos pos, final HeightLimitView view) {
        return packChunkSectionPosCompact(pos.getSectionX(), pos.getSectionY(), pos.getSectionZ(), view);
    }

    static long packChunkSectionPosCompact(final int x, final int y, final int z, final HeightLimitView view) {
        final int yIndex = view.sectionCoordToIndex(y);
        if (yIndex > 255 || yIndex < 0) {
            throw new RuntimeException();
        }
        long xl = x;
        if (xl < 0) {
            xl = -xl;
            xl |= 1 << (X_BITS - 1);
        }
        long zl = z;
        if (zl < 0) {
            zl = -zl;
            zl |= 1 << (Z_BITS - 1);
        }
        return xl << X_SHIFT | ((long) yIndex & 255) << Y_SHIFT | zl << Z_SHIFT;
    }
}
