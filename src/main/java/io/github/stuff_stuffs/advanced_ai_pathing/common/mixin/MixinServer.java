package io.github.stuff_stuffs.advanced_ai_pathing.common.mixin;

import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.AdvancedAiPathing;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.RunnableAiJobExecutor;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ServerExtensions;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public class MixinServer implements ServerExtensions {
    @Unique
    private RunnableAiJobExecutor advanced_ai_pathing$executor = null;

    @Override
    public RunnableAiJobExecutor advanced_ai_pathing$executor() {
        if (advanced_ai_pathing$executor == null) {
            advanced_ai_pathing$executor = AdvancedAiPathing.createExecutor();
        }
        return advanced_ai_pathing$executor;
    }
}
