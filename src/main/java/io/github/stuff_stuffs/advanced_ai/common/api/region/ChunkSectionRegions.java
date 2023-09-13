package io.github.stuff_stuffs.advanced_ai.common.api.region;

import io.github.stuff_stuffs.advanced_ai.common.impl.region.ChunkSectionRegionsImpl;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;

public interface ChunkSectionRegions {
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
}
