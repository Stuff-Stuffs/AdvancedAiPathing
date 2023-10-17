package io.github.stuff_stuffs.advanced_ai.common.internal.extensions;

import net.minecraft.block.BlockState;

public interface MemorizingChunkSection {
    long advanced_ai_pathing$modCount();

    boolean advanced_ai_pathing$copy_updates(long lastModCount, BlockState[] oldStateArray, int oldStateArrayIndex, BlockState[] updateStateArray, int updateStateArrayIndex, short[] updatePosArray, int updatePosArrayIndex);
}
