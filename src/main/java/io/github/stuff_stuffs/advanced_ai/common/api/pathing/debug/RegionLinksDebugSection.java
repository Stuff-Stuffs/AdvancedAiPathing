package io.github.stuff_stuffs.advanced_ai.common.api.pathing.debug;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionLinkedRegions;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.network.PacketByteBuf;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class RegionLinksDebugSection {
    private final Map<ChunkRegionifier<?>, long[][]> regions;

    public RegionLinksDebugSection(final Map<ChunkRegionifier<?>, ChunkSectionLinkedRegions> regions) {
        this.regions = new Reference2ReferenceOpenHashMap<>(regions.size());
        for (final Map.Entry<ChunkRegionifier<?>, ChunkSectionLinkedRegions> entry : regions.entrySet()) {
            final ChunkSectionLinkedRegions r = entry.getValue();
            final long[][] arr = new long[r.regions().regionCount()][];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = r.links(r.regions().prefix() | i);
            }
            this.regions.put(entry.getKey(), arr);
        }
    }

    public RegionLinksDebugSection(final PacketByteBuf buf) {
        final int expected = buf.readInt();
        regions = new Reference2ReferenceOpenHashMap<>(expected);
        for (int i = 0; i < expected; i++) {
            final ChunkRegionifier<?> regionifier = ChunkRegionifier.REGISTRY.get(buf.readIdentifier());
            final int count = buf.readInt();
            final long[][] arr = new long[count][];
            for (int j = 0; j < count; j++) {
                arr[j] = buf.readLongArray();
            }
            regions.put(regionifier, arr);
        }
    }

    public void write(final PacketByteBuf buf) {
        buf.writeInt(regions.size());
        for (final Map.Entry<ChunkRegionifier<?>, long[][]> entry : regions.entrySet()) {
            buf.writeIdentifier(ChunkRegionifier.REGISTRY.getId(entry.getKey()));
            final int count = entry.getValue().length;
            buf.writeInt(count);
            for (int i = 0; i < count; i++) {
                buf.writeLongArray(entry.getValue()[i]);
            }
        }
    }

    public Set<ChunkRegionifier<?>> keys() {
        return Collections.unmodifiableSet(regions.keySet());
    }

    public long[][] get(final ChunkRegionifier<?> regionifier) {
        return regions.get(regionifier);
    }
}
