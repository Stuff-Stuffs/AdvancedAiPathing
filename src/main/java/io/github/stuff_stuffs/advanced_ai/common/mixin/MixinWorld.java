package io.github.stuff_stuffs.advanced_ai.common.mixin;

import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerWorldExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("RETURN"))
    private void updateHook(final BlockPos pos, final BlockState state, final int flags, final int maxUpdateDepth, final CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) instanceof ServerWorld && cir.getReturnValue()) {
            ((ServerWorldExtensions) this).advanced_ai_pathing$invalidate(ChunkSectionPos.fromBlockPos(pos.asLong()));
        }
    }
}
