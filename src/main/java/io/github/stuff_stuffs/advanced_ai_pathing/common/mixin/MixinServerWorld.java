package io.github.stuff_stuffs.advanced_ai_pathing.common.mixin;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.job.AiJobExecutor;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.job.AiServerWorld;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ServerExtensions;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ServerWorldExtensions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements ServerWorldExtensions, AiServerWorld {
    @Override
    @Shadow
    public abstract MinecraftServer getServer();

    private final Long2ObjectMap<boolean[]> advanced_ai_pathing$invalidated = new Long2ObjectOpenHashMap<>();

    protected MixinServerWorld(final MutableWorldProperties properties, final RegistryKey<World> registryRef, final DynamicRegistryManager registryManager, final RegistryEntry<DimensionType> dimensionEntry, final Supplier<Profiler> profiler, final boolean isClient, final boolean debugWorld, final long biomeAccess, final int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Override
    public void advanced_ai_pathing$invalidate(final long chunkSectionPos) {
        ServerWorldExtensions.invalidate(advanced_ai_pathing$invalidated, chunkSectionPos, this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void invalidateChunks(final BooleanSupplier shouldKeepTicking, final CallbackInfo ci) {
        ServerWorldExtensions.purge(advanced_ai_pathing$invalidated, this);
        advanced_ai_pathing$invalidated.clear();
    }

    @Override
    public AiJobExecutor advanced_ai_pathing$executor() {
        return ((ServerExtensions) getServer()).advanced_ai_pathing$executor();
    }
}
