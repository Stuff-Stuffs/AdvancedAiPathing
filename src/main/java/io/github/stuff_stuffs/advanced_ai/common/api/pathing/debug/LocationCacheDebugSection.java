package io.github.stuff_stuffs.advanced_ai.common.api.pathing.debug;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.network.PacketByteBuf;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class LocationCacheDebugSection {
    private final Map<LocationClassifier<?>, Entry<?>> entries;

    public LocationCacheDebugSection(final PacketByteBuf buf) {
        final int size = buf.readInt();
        entries = new Reference2ReferenceOpenHashMap<>();
        for (int i = 0; i < size; i++) {
            final Entry<?> entry = Entry.read(buf);
            entries.put(entry.classifier, entry);
        }
    }

    public LocationCacheDebugSection(final Map<LocationClassifier<?>, Entry<?>> entries) {
        this.entries = entries;
    }

    public void write(final PacketByteBuf buf) {
        buf.writeInt(entries.size());
        for (final Entry<?> entry : entries.values()) {
            entry.write(buf);
        }
    }

    public Set<LocationClassifier<?>> keys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public <T> Entry<T> get(final LocationClassifier<T> classifier) {
        return (Entry<T>) entries.get(classifier);
    }

    public static final class Entry<T> {
        private final LocationClassifier<T> classifier;
        private final int bitsPerElement;
        private final int elementsPerLong;
        private final UniverseInfo<T> universeInfo;
        private final long[] packed;

        public Entry(final PacketByteBuf buf, final LocationClassifier<T> classifier) {
            this.classifier = classifier;
            universeInfo = this.classifier.universeInfo();
            int round = universeInfo.size();
            --round;
            round |= round >>> 1;
            round |= round >>> 2;
            round |= round >>> 4;
            round |= round >>> 8;
            round |= round >>> 16;
            ++round;
            final int size = 32 - Integer.numberOfLeadingZeros(round);
            bitsPerElement = size;
            elementsPerLong = 64 / size;
            packed = buf.readLongArray();
        }

        public Entry(final LocationClassifier<T> classifier, final LocationCacheSection<T> section) {
            this.classifier = classifier;
            universeInfo = this.classifier.universeInfo();
            int round = universeInfo.size();
            --round;
            round |= round >>> 1;
            round |= round >>> 2;
            round |= round >>> 4;
            round |= round >>> 8;
            round |= round >>> 16;
            ++round;
            final int size = 32 - Integer.numberOfLeadingZeros(round);
            bitsPerElement = size;
            elementsPerLong = 64 / size;
            packed = new long[(4096 + elementsPerLong - 1) / elementsPerLong];
            final int mask = (1 << size) - 1;
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    for (int k = 0; k < 16; ++k) {
                        final short packedCoord = LocationCacheSection.pack(i, j, k);
                        final long valIndex = universeInfo.toIndex(section.get(i, j, k));
                        final int index = packedCoord / elementsPerLong;
                        final int subIndex = packedCoord % elementsPerLong * size;
                        packed[index] |= (valIndex & mask) << subIndex;
                    }
                }
            }
        }

        public T get(final int packed) {
            final int index = packed / elementsPerLong;
            final int subIndex = packed % elementsPerLong * bitsPerElement;
            final int mask = (1 << bitsPerElement) - 1;
            return universeInfo.fromIndex((int) (this.packed[index] >> subIndex & (long) mask));
        }

        public void write(final PacketByteBuf buf) {
            buf.writeIdentifier(LocationClassifier.REGISTRY.getId(classifier));
            buf.writeLongArray(packed);
        }

        public static Entry<?> read(final PacketByteBuf buf) {
            return new Entry<>(buf, LocationClassifier.REGISTRY.get(buf.readIdentifier()));
        }
    }
}
