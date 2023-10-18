package io.github.stuff_stuffs.advanced_ai.common.internal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerExtensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public final class AdvancedAiPathingDebug {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("location_cache").then(CommandManager.argument("radius", IntegerArgumentType.integer(0, 16)).then(CommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registryAccess, LocationClassifier.REGISTRY_KEY)).executes(context -> {
                final Vec3d position = context.getSource().getPosition();
                final ChunkSectionPos pos = ChunkSectionPos.from(position);
                final LocationClassifier<?> classifier = RegistryEntryArgumentType.getRegistryEntry(context, "type", LocationClassifier.REGISTRY_KEY).value();
                ChunkSectionPos.stream(pos, context.getArgument("radius", Integer.class)).forEach(p -> processLocationCacheJob(context, p, classifier));
                return 0;
            }))));
        });
    }

    private static void processLocationCacheJob(final CommandContext<ServerCommandSource> context, final ChunkSectionPos p, final LocationClassifier<?> classifier) {
        ((ServerExtensions) context.getSource().getServer()).advanced_ai_pathing$executor().enqueue(new LocationCachingJob<>(p, context.getSource().getWorld(), classifier));
    }


    private record Node(int x, int y, int z, @Nullable Node prev, double cost,
                        CollisionHelper.FloorCollision collision) {
        public long pos() {
            return BlockPos.asLong(x, y, z);
        }
    }

    private record Context(ShapeCache cache) {
    }

    private AdvancedAiPathingDebug() {
    }
}
