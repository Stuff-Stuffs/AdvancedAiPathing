package io.github.stuff_stuffs.advanced_ai.common.impl.region;

import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegion;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;
import it.unimi.dsi.fastutil.shorts.ShortArrays;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChunkSectionRegionsImpl implements ChunkSectionRegions {
    private final long prefix;
    private final ChunkSectionRegion[] regions;

    public ChunkSectionRegionsImpl(final long prefix, final ChunkSectionRegion[] regions) {
        this.prefix = prefix;
        this.regions = regions;
    }

    public ChunkSectionRegionsImpl(final PacketByteBuf buf) {
        prefix = buf.readLong();
        final int count = buf.readInt();
        regions = new ChunkSectionRegion[count];
        for (int i = 0; i < regions.length; i++) {
            final int size = buf.readInt();
            final int[] packed = buf.readIntArray();
            final short[] shorts = new short[size];
            for (int j = 0; j < size; j++) {
                shorts[j] = (short) ((packed[j / 2] >>> ((j & 1) == 1 ? 16 : 0)) & 0xFFF);
            }
            regions[i] = new ChunkSectionRegionImpl(prefix | i, shorts);
        }
    }

    @Override
    public @Nullable ChunkSectionRegion query(final short pos) {
        for (final ChunkSectionRegion region : regions) {
            if (region.contains(pos)) {
                return region;
            }
        }
        return null;
    }

    @Override
    public @Nullable ChunkSectionRegion byId(final long id) {
        if ((id & PREFIX_MASK) == prefix) {
            final int i = ChunkSectionRegions.unpackCustomPosCompact(id);
            if (i < regions.length) {
                return regions[i];
            } else {
                return null;
            }
        }
        throw new RuntimeException();
    }

    @Override
    public long prefix() {
        return prefix;
    }

    @Override
    public int regionCount() {
        return regions.length;
    }

    @Override
    public void writeToBuf(final PacketByteBuf buf) {
        buf.writeLong(prefix);
        buf.writeInt(regions.length);
        for (final ChunkSectionRegion region : regions) {
            final PackedList packedList = region.all();
            final int size = packedList.size();
            buf.writeInt(size);
            final int[] packed = new int[(size + 1) / 2];
            for (int j = 0; j < size; j++) {
                final int val = packedList.get(j);
                if ((j & 1) == 1) {
                    packed[j / 2] |= val << 16;
                } else {
                    packed[j / 2] |= val;
                }
            }
            buf.writeIntArray(packed);
        }
    }

    public static final class BuilderImpl implements Builder {
        private static final int SIZE_THRESHOLD = 2;
        private final ShortSet set = new ShortOpenHashSet();
        private final List<PartialRegion> partialRegions = new ArrayList<>(32);
        private final long pos;

        public BuilderImpl(final ChunkSectionPos pos, final HeightLimitView view) {
            this.pos = ChunkSectionRegions.packChunkSectionPosCompact(pos, view);
        }

        @Override
        public RegionKey newRegion() {
            partialRegions.add(new PartialRegion());
            return new RegionKeyImpl(this, partialRegions.size() - 1);
        }

        @Override
        public boolean contains(final short pos) {
            return set.contains(pos);
        }

        @Override
        public void expand(final RegionKey key, final short pos) {
            if (!set.add(pos)) {
                throw new RuntimeException();
            } else {
                final RegionKeyImpl regionKey = (RegionKeyImpl) key;
                if (regionKey.parent != this) {
                    throw new RuntimeException();
                }
                partialRegions.get(regionKey.id).expand(pos);
            }
        }

        @Override
        public ChunkSectionRegions build() {
            final List<ChunkSectionRegion> regions = new ArrayList<>(partialRegions.size());
            int count = 0;
            for (final PartialRegion region : partialRegions) {
                if (region.set != null && region.set.size() >= SIZE_THRESHOLD) {
                    final short[] set = region.set.toShortArray();
                    ShortArrays.quickSort(set);
                    if (count > ~PREFIX_MASK) {
                        throw new RuntimeException("Too many regions!");
                    }
                    regions.add(new ChunkSectionRegionImpl(pos | count++, set));
                }
            }
            return new ChunkSectionRegionsImpl(pos, regions.toArray(new ChunkSectionRegion[0]));
        }
    }

    private static final class PartialRegion {
        private ShortSet set = null;

        public void expand(final short pos) {
            if (set == null) {
                set = new ShortOpenHashSet(8);
            }
            set.add(pos);
        }
    }

    private record RegionKeyImpl(BuilderImpl parent, int id) implements RegionKey {
    }
}
