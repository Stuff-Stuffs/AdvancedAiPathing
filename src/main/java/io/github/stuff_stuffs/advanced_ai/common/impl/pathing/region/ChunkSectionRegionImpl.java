package io.github.stuff_stuffs.advanced_ai.common.impl.pathing.region;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.region.ChunkSectionRegion;
import io.github.stuff_stuffs.advanced_ai.common.api.util.PackedList;

public class ChunkSectionRegionImpl implements ChunkSectionRegion, PackedList {
    private final long id;
    private final long[] set;
    private final int size;

    public ChunkSectionRegionImpl(final long id, final short[] set) {
        this(id, packShorts(set), set.length);
    }

    public ChunkSectionRegionImpl(final long id, final long[] set, final int size) {
        if (size == 0) {
            throw new RuntimeException();
        }
        this.id = id;
        this.set = set;
        this.size = size;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public boolean contains(final short s) {
        if (s >= 0xFFF) {
            return false;
        }
        final int rem = size % 5;
        if (rem != 0) {
            if (partialCheck(set[set.length - 1], s, rem)) {
                return true;
            }
        }
        int from = 0;
        final long[] arr = set;
        int to = rem == 0 ? arr.length - 1 : arr.length - 2;
        while (from <= to) {
            final int mid = (from + to) >>> 1;
            final long midVal = arr[mid];
            if ((midVal & 0xFFF) > s) {
                to = mid - 1;
            } else if (((midVal >>> 48) & 0xFFF) < s) {
                from = mid + 1;
            } else {
                return check(midVal, s);
            }
        }
        return false;
    }

    private static boolean partialCheck(final long l, final short s, final int last) {
        for (int i = 0; i < last; i++) {
            if (((l >>> (i * 12)) & 0xFFF) == s) {
                return true;
            }
        }
        return false;
    }

    private static boolean check(final long l, final short s) {
        for (int i = 0; i < 5; i++) {
            if (((l >>> (i * 12)) & 0xFFF) == s) {
                return true;
            }
        }
        return false;
    }

    @Override
    public short any() {
        return (short) (set[0] & 0xFFF);
    }

    @Override
    public PackedList all() {
        return this;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int get(final int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        final int arrIndex = index / 5;
        final int subIndex = index % 5;
        final int shift = subIndex * 12;
        return (int) (set[arrIndex] >>> shift) & 0xFFF;
    }

    private static long[] packShorts(final short[] set) {
        final int size = set.length;
        final int setSize = (size + 4) / 5;
        final long[] packedSet = new long[setSize];
        int arrIndex = 0;
        int subIndex = 0;
        for (final short s : set) {
            packedSet[arrIndex] |= (s & 0xFFFL) << (subIndex++ * 12);
            if (subIndex == 5) {
                arrIndex++;
                subIndex = 0;
            }
        }
        return packedSet;
    }
}
