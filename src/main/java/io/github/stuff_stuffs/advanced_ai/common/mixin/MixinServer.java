package io.github.stuff_stuffs.advanced_ai.common.mixin;

import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import io.github.stuff_stuffs.advanced_ai.common.internal.RunnableAiJobExecutor;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerExtensions;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public class MixinServer implements ServerExtensions {
    @Unique
    private RunnableAiJobExecutor advanced_ai$executor = null;

    @Override
    public RunnableAiJobExecutor advanced_ai$executor() {
        if (advanced_ai$executor == null) {
            advanced_ai$executor = AdvancedAi.createExecutor();
        }
        return advanced_ai$executor;
    }
}
