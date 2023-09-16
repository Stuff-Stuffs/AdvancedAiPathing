package io.github.stuff_stuffs.advanced_ai.common.api.pathing.debug;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionRegions;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class RegionDebugSection {
    private final Map<ChunkRegionifier<?>, ChunkSectionRegions> regions;

    public RegionDebugSection(final Map<ChunkRegionifier<?>, ChunkSectionRegions> regions) {
        this.regions = regions;
    }

    public RegionDebugSection(final PacketByteBuf buf) {
        final int count = buf.readInt();
        regions = new Reference2ReferenceOpenHashMap<>(count);
        for (int i = 0; i < count; i++) {
            final Identifier identifier = buf.readIdentifier();
            final ChunkRegionifier<?> regionifier = ChunkRegionifier.REGISTRY.get(identifier);
            regions.put(regionifier, ChunkSectionRegions.readFromBuf(buf));
        }
    }

    public void write(final PacketByteBuf buf) {
        buf.writeInt(regions.size());
        for (final Map.Entry<ChunkRegionifier<?>, ChunkSectionRegions> entry : regions.entrySet()) {
            buf.writeIdentifier(ChunkRegionifier.REGISTRY.getId(entry.getKey()));
            entry.getValue().writeToBuf(buf);
        }
    }

    public Set<ChunkRegionifier<?>> keys() {
        return Collections.unmodifiableSet(regions.keySet());
    }

    public ChunkSectionRegions get(final ChunkRegionifier<?> regionifier) {
        return regions.get(regionifier);
    }
}
