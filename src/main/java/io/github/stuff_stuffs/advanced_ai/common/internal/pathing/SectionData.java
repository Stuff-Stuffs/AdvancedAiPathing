package io.github.stuff_stuffs.advanced_ai.common.internal.pathing;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionLinkedRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CopyOnWriteMap;
import org.jetbrains.annotations.Nullable;

public class SectionData {
    private State current = new State();
    private State stale = new State();

    public <T> void put(final LocationClassifier<T> classifier, final LocationCacheSection<T> section) {
        current.put(classifier, section);
        stale.remove(classifier);
    }

    public void put(final ChunkRegionifier<?> regionifier, final ChunkSectionRegions regions) {
        current.put(regionifier, regions);
        stale.regions.remove(regionifier);
    }

    public void put(final ChunkRegionifier<?> regionifier, final ChunkSectionLinkedRegions regions) {
        current.put(regionifier, regions);
        stale.regions.remove(regionifier);
    }

    public <T> @Nullable LocationCacheSection<T> getLocationCache(final LocationClassifier<T> classifier) {
        return current.get(classifier);
    }

    public @Nullable ChunkSectionRegions getRegions(final ChunkRegionifier<?> regionifier) {
        return current.get(regionifier);
    }

    public @Nullable ChunkSectionLinkedRegions getLinks(final ChunkRegionifier<?> regionifier) {
        return current.getLinks(regionifier);
    }

    public <T> @Nullable LocationCacheSection<T> getPossibleStale(final LocationClassifier<T> classifier) {
        LocationCacheSection<T> section = stale.get(classifier);
        if (section == null) {
            section = current.get(classifier);
        }
        return section;
    }

    public <T> LocationCacheSection<T> promote(final LocationClassifier<T> classifier) {
        final LocationCacheSection<T> section = stale.remove(classifier);
        if (section != null) {
            current.put(classifier, section);
        }
        return section;
    }

    public void purgeAll() {
        final State s = current;
        current = stale;
        current.clear();
        stale = s;
    }

    private static final class State {
        private final CopyOnWriteMap<LocationClassifier<?>, LocationCacheSection<?>> sections = new CopyOnWriteMap<>();
        private final CopyOnWriteMap<ChunkRegionifier<?>, ChunkSectionRegions> regions = new CopyOnWriteMap<>();
        private final CopyOnWriteMap<ChunkRegionifier<?>, ChunkSectionLinkedRegions> links = new CopyOnWriteMap<>();

        private void clear() {
            sections.clear();
            regions.clear();
            links.clear();
        }

        public <T> @Nullable LocationCacheSection<T> get(final LocationClassifier<T> classifier) {
            return (LocationCacheSection<T>) sections.get(classifier);
        }

        public <T> void put(final LocationClassifier<T> classifier, final LocationCacheSection<T> section) {
            sections.put(classifier, section);
        }

        public <T> @Nullable LocationCacheSection<T> remove(final LocationClassifier<T> classifier) {
            return (LocationCacheSection<T>) sections.remove(classifier);
        }

        public @Nullable ChunkSectionRegions get(final ChunkRegionifier<?> classifier) {
            return regions.get(classifier);
        }

        public @Nullable ChunkSectionLinkedRegions getLinks(final ChunkRegionifier<?> classifier) {
            return links.get(classifier);
        }

        public void put(final ChunkRegionifier<?> regionifier, final ChunkSectionRegions regions) {
            this.regions.put(regionifier, regions);
        }

        public void put(final ChunkRegionifier<?> classifier, final ChunkSectionLinkedRegions regions) {
            links.put(classifier, regions);
        }
    }
}
