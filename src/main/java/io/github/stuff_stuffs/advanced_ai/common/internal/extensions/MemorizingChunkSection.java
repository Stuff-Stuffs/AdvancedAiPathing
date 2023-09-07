package io.github.stuff_stuffs.advanced_ai.common.internal.extensions;

import net.minecraft.block.BlockState;

public interface MemorizingChunkSection {
    long advanced_ai$modCount();

    boolean advanced_ai$copy_updates(long lastModCount, BlockState[] oldStateArray, int oldStateArrayIndex, BlockState[] updateStateArray, int updateStateArrayIndex, short[] updatePosArray, int updatePosArrayIndex);
}
