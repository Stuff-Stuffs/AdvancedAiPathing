package io.github.stuff_stuffs.advanced_ai_pathing.common.internal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ServerExtensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

public final class AdvancedAiPathingDebug {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("aaip_location_cache").then(CommandManager.argument("radius", IntegerArgumentType.integer(0, 16)).then(CommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registryAccess, LocationClassifier.REGISTRY_KEY)).executes(context -> {
                final Vec3d position = context.getSource().getPosition();
                final ChunkSectionPos pos = ChunkSectionPos.from(position);
                final LocationClassifier<?> classifier = RegistryEntryArgumentType.getRegistryEntry(context, "type", LocationClassifier.REGISTRY_KEY).value();
                ChunkSectionPos.stream(pos, context.getArgument("radius", Integer.class)).filter(p -> context.getSource().getWorld().isInBuildLimit(p.getMinPos())).forEach(p -> processLocationCacheJob(context, p, classifier));
                return 0;
            }))));
        });
    }

    private static void processLocationCacheJob(final CommandContext<ServerCommandSource> context, final ChunkSectionPos p, final LocationClassifier<?> classifier) {
        ((ServerExtensions) context.getSource().getServer()).advanced_ai_pathing$executor().enqueue(new LocationCachingJob<>(p, context.getSource().getWorld(), classifier));
    }

    private AdvancedAiPathingDebug() {
    }
}
