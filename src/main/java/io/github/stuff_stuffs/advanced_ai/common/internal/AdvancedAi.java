package io.github.stuff_stuffs.advanced_ai.common.internal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerExtensions;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public class AdvancedAi implements ModInitializer {
    public static final String MOD_ID = "advanced_ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Logger JOB_LOGGER = LoggerFactory.getLogger(MOD_ID + ":job_executor");
    public static final int UPDATES_BEFORE_REBUILD = 8;
    public static final Identifier DEBUG_CHANNEL = id("debug_channel");
    public static final Map<LocationClassifier<?>, ProcessedLocationClassifier<?>> PROCESSED_LOCATION_CLASSIFIERS = new Reference2ReferenceOpenHashMap<>();
    public static final UniverseInfo<CollisionHelper.FloorCollision> FLOOR_COLLISION_INFO = UniverseInfo.fromEnum(CollisionHelper.FloorCollision.class);
    public static final LocationClassifier<CollisionHelper.FloorCollision> BASIC = new LocationClassifier<>() {
        private static final CollisionHelper COLLISION_HELPER = new CollisionHelper(1, 2, 1);

        @Override
        public CollisionHelper.FloorCollision get(final int x, final int y, final int z, final ShapeCache cache) {
            return COLLISION_HELPER.open(x, y, z, cache);
        }

        @Override
        public boolean needsRebuild(final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int otherChunkSectionX, final int otherChunkSectionY, final int otherChunkSectionZ, final int relX, final int relY, final int relZ, final ShapeCache cache) {
            if (relY > 16 | relY < -1) {
                return false;
            }
            if (relY == -1) {
                return true;
            }
            final boolean xAdj = (relX == -1 | relX == 16);
            final boolean zAdj = (relZ == -1 | relZ == 16);
            return xAdj | zAdj;
        }

        @Override
        public UniverseInfo<CollisionHelper.FloorCollision> universeInfo() {
            return AdvancedAi.FLOOR_COLLISION_INFO;
        }

        @Override
        public MethodHandle specialGetHandle() {
            try {
                return MethodHandles.lookup().findSpecial(getClass(), "get", MethodType.methodType(CollisionHelper.FloorCollision.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, ShapeCache.class), getClass()).bindTo(this);
            } catch (final NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @Override
    public void onInitialize() {
        RegistryEntryAddedCallback.event(LocationClassifier.REGISTRY).register((rawId, id, object) -> PROCESSED_LOCATION_CLASSIFIERS.put(object, new ProcessedLocationClassifier<>(object)));
        ServerTickEvents.END_SERVER_TICK.register(server -> ((ServerExtensions) server).advanced_ai$executor().run(25));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ((ServerExtensions) server).advanced_ai$executor().stop());
        Registry.register(LocationClassifier.REGISTRY, id("basic"), BASIC);
        Registry.register(DebugSectionType.REGISTRY, id("location_cache"), DebugSectionType.LOCATION_CACHE_TYPE);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("location_cache").then(CommandManager.argument("radius", IntegerArgumentType.integer(1, 8)).executes(new Command<ServerCommandSource>() {
            @Override
            public int run(final CommandContext<ServerCommandSource> context) {
                final Vec3d position = context.getSource().getPosition();
                final ChunkSectionPos pos = ChunkSectionPos.from(position);
                ChunkSectionPos.stream(pos, context.getArgument("radius", Integer.class)).forEach(p -> process(context, p));
                return 0;
            }
        }))));
    }

    private static void process(final CommandContext<ServerCommandSource> context, final ChunkSectionPos p) {
        ((ServerExtensions) context.getSource().getServer()).advanced_ai$executor().enqueue(new LocationCachingJob<>(p, context.getSource().getWorld(), BASIC));
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}
