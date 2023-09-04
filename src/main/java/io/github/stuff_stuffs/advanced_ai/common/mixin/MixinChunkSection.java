package io.github.stuff_stuffs.advanced_ai.common.mixin;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.util.AiUtil;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import io.github.stuff_stuffs.advanced_ai.common.internal.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai.common.internal.MemorizingChunkSection;
import io.github.stuff_stuffs.advanced_ai.common.internal.SectionData;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSection.class)
public class MixinChunkSection implements ChunkSectionExtensions, MemorizingChunkSection {
    @Unique
    private final SectionData advanced_ai$sectionData = new SectionData();
    @Unique
    private long modCount = 1;
    @Unique
    private final short[] updatedPositions = new short[AdvancedAi.UPDATES_BEFORE_REBUILD];
    @Unique
    private final BlockState[] updatedStates = new BlockState[AdvancedAi.UPDATES_BEFORE_REBUILD];
    @Unique
    final BlockState[] oldStates = new BlockState[AdvancedAi.UPDATES_BEFORE_REBUILD];

    @Inject(at = @At("RETURN"), method = "setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;")
    private void updateModCount(final int x, final int y, final int z, final BlockState state, final boolean lock, final CallbackInfoReturnable<BlockState> cir) {
        final BlockState value = cir.getReturnValue();
        boolean flag = false;
        if (value != state) {
            final boolean b0 = value.getBlock().hasDynamicBounds();
            if (b0) {
                flag = true;
            } else {
                final boolean b1 = state.getBlock().hasDynamicBounds();
                if (b1) {
                    flag = true;
                } else {
                    if (value.getCollisionShape(null, null) != state.getCollisionShape(null, null)) {
                        flag = true;
                    }
                }
            }
        }
        if (flag) {
            final int index = ((int) modCount) % AdvancedAi.UPDATES_BEFORE_REBUILD;
            updatedPositions[index] = LocationCacheSection.pack(x, y, z);
            updatedStates[index] = state;
            oldStates[index] = value;
            modCount++;
        }
    }

    @Override
    public SectionData advanced_ai$sectionData() {
        return advanced_ai$sectionData;
    }

    @Override
    public long advanced_ai$modCount() {
        return modCount;
    }

    @Override
    public boolean advanced_ai$copy_updates(final long lastModCount, final BlockState[] oldStateArray, final int oldStateArrayIndex, final BlockState[] updateStateArray, final int updateStateArrayIndex, final short[] updatePosArray, final int updatePosArrayIndex) {
        final long diff = modCount - lastModCount;
        if (diff >= AdvancedAi.UPDATES_BEFORE_REBUILD) {
            return false;
        }
        final int startIndex = ((int) lastModCount) % AdvancedAi.UPDATES_BEFORE_REBUILD;
        final int endIndex = ((int) modCount) % AdvancedAi.UPDATES_BEFORE_REBUILD;
        AiUtil.wrappingCopy(updatedStates, startIndex, endIndex, updateStateArray, updateStateArrayIndex);
        AiUtil.wrappingCopy(updatedPositions, startIndex, endIndex, updatePosArray, updatePosArrayIndex);
        AiUtil.wrappingCopy(oldStates, startIndex, endIndex, oldStateArray, oldStateArrayIndex);
        return true;
    }
}
