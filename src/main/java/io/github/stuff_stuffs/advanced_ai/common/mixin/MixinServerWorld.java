package io.github.stuff_stuffs.advanced_ai.common.mixin;

import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionInfo;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.internal.ServerWorldExtensions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements ServerWorldExtensions {
    private final Long2ObjectMap<boolean[]> advanced_ai$invalidated = new Long2ObjectOpenHashMap<>();
    private final Map<DebugSectionType<?>, Map<ChunkSectionPos, DebugSectionInfo<?>>> advanced_ai$debug = new Reference2ReferenceOpenHashMap<>();

    protected MixinServerWorld(final MutableWorldProperties properties, final RegistryKey<World> registryRef, final DynamicRegistryManager registryManager, final RegistryEntry<DimensionType> dimensionEntry, final Supplier<Profiler> profiler, final boolean isClient, final boolean debugWorld, final long biomeAccess, final int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Override
    public void advanced_ai$invalidate(final long chunkSectionPos) {
        ServerWorldExtensions.invalidate(advanced_ai$invalidated, chunkSectionPos, this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void invalidateChunks(final BooleanSupplier shouldKeepTicking, final CallbackInfo ci) {
        ServerWorldExtensions.purge(advanced_ai$invalidated, this);
        advanced_ai$invalidated.clear();
        ServerWorldExtensions.debug(advanced_ai$debug, (ServerWorld) (Object) this);
        advanced_ai$debug.clear();
    }

    @Override
    public void advanced_ai$debug(final DebugSectionInfo<?> info) {
        Map<ChunkSectionPos, DebugSectionInfo<?>> map = advanced_ai$debug.get(info.type());
        if (map == null) {
            map = new Object2ReferenceOpenHashMap<>();
            advanced_ai$debug.put(info.type(), map);
        }
        map.put(info.pos(), info);
    }
}
