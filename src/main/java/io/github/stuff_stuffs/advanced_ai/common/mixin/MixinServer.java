package io.github.stuff_stuffs.advanced_ai.common.mixin;

import io.github.stuff_stuffs.advanced_ai.common.impl.job.executor.SingleThreadedJobExecutor;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import io.github.stuff_stuffs.advanced_ai.common.internal.RunnableAiJobExecutor;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerExtensions;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public class MixinServer implements ServerExtensions {
    @Unique
    private final RunnableAiJobExecutor advanced_ai$executor = new SingleThreadedJobExecutor(512, AdvancedAi.JOB_LOGGER);

    @Override
    public RunnableAiJobExecutor advanced_ai$executor() {
        return advanced_ai$executor;
    }
}
