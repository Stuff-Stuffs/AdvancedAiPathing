package io.github.stuff_stuffs.advanced_ai.common.impl.region;

import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionLinkedRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegions;

import java.util.Arrays;

public class ChunkSectionLinkedRegionsImpl implements ChunkSectionLinkedRegions {
    private static final long[] EMPTY = new long[0];
    private final long prefix;
    private final ChunkSectionRegions regions;
    private final long[][] links;

    public ChunkSectionLinkedRegionsImpl(final long prefix, final ChunkSectionRegions regions, final long[][] links) {
        this.prefix = prefix;
        this.regions = regions;
        this.links = links;
    }

    @Override
    public long[] links(final long regionId) {
        if ((regionId & ChunkSectionRegions.PREFIX_MASK) == prefix) {
            final int index = (int) (regionId & ~ChunkSectionRegions.PREFIX_MASK);
            if (index < links.length) {
                return links[index];
            }
        }
        return EMPTY;
    }

    @Override
    public ChunkSectionRegions regions() {
        return regions;
    }

    public static final class BuilderImpl implements Builder {
        private final ChunkSectionRegions regions;
        private final long[][] links;

        public BuilderImpl(final ChunkSectionRegions regions) {
            this.regions = regions;
            links = new long[regions.regionCount()][];
            Arrays.fill(links, EMPTY);
        }

        @Override
        public void link(final long current, final long adjacent) {
            if (current == adjacent) {
                return;
            }
            if ((current & ChunkSectionRegions.PREFIX_MASK) == regions.prefix()) {
                if ((adjacent & ChunkSectionRegions.PREFIX_MASK) == regions.prefix()) {
                    linkBoth(current, adjacent);
                    return;
                } else {
                    final int index = (int) (current & ~ChunkSectionRegions.PREFIX_MASK);
                    if (index < links.length) {
                        final long[] copy = Arrays.copyOf(links[index], links[index].length + 1);
                        copy[copy.length - 1] = adjacent;
                        links[index] = copy;
                        return;
                    }
                }
            }
            throw new RuntimeException();
        }

        private void linkBoth(final long first, final long second) {
            final int i0 = (int) (first & ~ChunkSectionRegions.PREFIX_MASK);
            final int i1 = (int) (second & ~ChunkSectionRegions.PREFIX_MASK);
            if (i0 < links.length && i1 < links.length) {
                final long[] arr0 = Arrays.copyOf(links[i0], links[i0].length + 1);
                final long[] arr1 = Arrays.copyOf(links[i1], links[i1].length + 1);
                arr0[arr0.length - 1] = second;
                arr1[arr1.length - 1] = first;
                links[i0] = arr0;
                links[i1] = arr1;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public ChunkSectionLinkedRegions build() {
            return new ChunkSectionLinkedRegionsImpl(regions.prefix(), regions, links);
        }
    }
}
