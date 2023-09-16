package io.github.stuff_stuffs.advanced_ai.common.internal.pathing;

import it.unimi.dsi.fastutil.Hash;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;

public class NodeVisitedMap<N> {
    protected transient long[] key;
    protected transient N[] value;
    protected transient int mask;
    protected transient boolean containsNullKey;
    protected transient int n;
    protected transient int maxFill;
    protected int size;
    protected final float f;

    public NodeVisitedMap() {
        f = 0.5F;
        n = arraySize(Hash.DEFAULT_INITIAL_SIZE, f);
        mask = n - 1;
        maxFill = maxFill(n, f);
        key = new long[n + 1];
        value = (N[]) new Object[n + 1];
    }

    public int size() {
        return size;
    }

    public int findInsertionPoint(final long k) {
        if (k == 0) {
            return containsNullKey ? n : -(n + 1);
        }
        long curr;
        final long[] key = this.key;
        int pos;
        // The starting point.
        if ((curr = key[pos = (int) it.unimi.dsi.fastutil.HashCommon.mix(k) & mask]) == 0) {
            return -(pos + 1);
        }
        if (k == curr) {
            return pos;
        }
        // There's always an unused entry.
        while (true) {
            if ((curr = key[pos = pos + 1 & mask]) == 0) {
                return -(pos + 1);
            }
            if (k == curr) {
                return pos;
            }
        }
    }

    public void insert(int pos, final long k, final N v) {
        if(pos<0) {
            pos = -pos - 1;
            if (pos == n) {
                containsNullKey = true;
            }
            key[pos] = k;
            value[pos] = v;
            if (size++ >= maxFill) {
                rehash(arraySize(size + 1, f));
            }
        } else {
            value[pos] = v;
        }
    }

    protected void rehash(final int newN) {
        final long[] key = this.key;
        final N[] value = this.value;
        final int mask = newN - 1; // Note that this is used by the hashing macro
        final long[] newKey = new long[newN + 1];
        final N[] newValue = (N[]) new Object[newN + 1];
        int i = n, pos;
        for (int j = realSize(); j-- != 0; ) {
            while (((key[--i]) == (0))) {
            }
            if (!((newKey[pos = (int) it.unimi.dsi.fastutil.HashCommon.mix((key[i])) & mask]) == (0))) {
                while (!((newKey[pos = (pos + 1) & mask]) == (0))) {
                    ;
                }
            }
            newKey[pos] = key[i];
            newValue[pos] = value[i];
        }
        newValue[newN] = value[n];
        n = newN;
        this.mask = mask;
        maxFill = maxFill(n, f);
        this.key = newKey;
        this.value = newValue;
    }

    private int realSize() {
        return containsNullKey ? size - 1 : size;
    }

    public N get(final int pos) {
        return value[pos];
    }
}
