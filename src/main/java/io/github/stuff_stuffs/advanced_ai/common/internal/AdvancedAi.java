package io.github.stuff_stuffs.advanced_ai.common.internal;

import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.job.*;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.ChunkRegionJob;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.ChunkRegionLinkJob;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.executor.SingleThreadedJobExecutor;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerExtensions;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
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
    public static final ChunkRegionifier<CollisionHelper.FloorCollision> BASIC_REGIONIFIER = new BasicChunkRegionifier();


    @Override
    public void onInitialize() {
        RegistryEntryAddedCallback.event(LocationClassifier.REGISTRY).register((rawId, id, object) -> PROCESSED_LOCATION_CLASSIFIERS.put(object, new ProcessedLocationClassifier<>(object)));
        ServerTickEvents.END_SERVER_TICK.register(server -> ((ServerExtensions) server).advanced_ai$executor().run(25));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ((ServerExtensions) server).advanced_ai$executor().stop());
        Registry.register(LocationClassifier.REGISTRY, id("basic"), BASIC);
        Registry.register(ChunkRegionifier.REGISTRY, id("basic"), BASIC_REGIONIFIER);
        Registry.register(DebugSectionType.REGISTRY, id("location_cache"), DebugSectionType.LOCATION_CACHE_TYPE);
        Registry.register(DebugSectionType.REGISTRY, id("regions"), DebugSectionType.REGION_DEBUG_TYPE);
        Registry.register(DebugSectionType.REGISTRY, id("region_link"), DebugSectionType.REGION_LINKS_DEBUG_TYPE);
        AiJobExecutor.CREATION_EVENT.register(acceptor -> {
            acceptor.accept(new AbstractPrerequisiteAiJobHandler<>(512, ChunkRegionJob.class) {
                @Override
                protected ChunkPosRegionifierKey key(final ChunkRegionJob job) {
                    return new ChunkPosRegionifierKey(job.pos, job.regionifier);
                }

                @Override
                protected @Nullable AiJob producePrerequisite(final ChunkRegionJob job, final AiJobHandle futureHandle) {
                    final ServerWorld world = job.world;
                    final ChunkSectionPos pos = job.pos;
                    if (!world.isChunkLoaded(pos.getSectionX(), pos.getSectionZ())) {
                        return null;
                    }
                    final Chunk chunk = world.getChunk(pos.getSectionX(), pos.getSectionZ(), ChunkStatus.FULL, false);
                    final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
                    if (chunk == null || yIndex < 0 || yIndex >= world.countVerticalSections()) {
                        return null;
                    }
                    if (((ChunkSectionExtensions) chunk.getSection(yIndex)).advanced_ai$sectionData().getLocationCache(job.regionifier.classifier()) == null) {
                        return new LocationCachingJob<>(pos, world, job.regionifier.classifier());
                    }
                    return null;
                }
            });
            acceptor.accept(new AbstractPrerequisiteAiJobHandler<>(512, ChunkRegionLinkJob.class) {
                @Override
                protected ChunkPosRegionifierKey key(final ChunkRegionLinkJob job) {
                    return new ChunkPosRegionifierKey(job.pos, job.regionifier);
                }

                @Override
                protected @Nullable AiJob producePrerequisite(final ChunkRegionLinkJob job, final AiJobHandle futureHandle) {
                    final ServerWorld world = job.world;
                    final ChunkSectionPos pos = job.pos;
                    final int yIndex = world.sectionCoordToIndex(pos.getSectionY());
                    if (yIndex < 0 || yIndex >= world.countVerticalSections()) {
                        return null;
                    }
                    for (int i = -1; i < 1; i++) {
                        final int adjacentYIndex = yIndex + i;
                        if (adjacentYIndex < 0 || adjacentYIndex >= world.countVerticalSections()) {
                            continue;
                        }
                        for (int j = -1; j < 1; j++) {
                            final int adjacentXIndex = pos.getSectionX() + j;
                            for (int k = -1; k < 1; k++) {
                                final int adjacentZIndex = pos.getSectionZ() + k;
                                final ChunkSectionPos adjacentPos = ChunkSectionPos.from(adjacentXIndex, world.sectionIndexToCoord(adjacentYIndex), adjacentZIndex);
                                final ChunkPosRegionifierKey key = new ChunkPosRegionifierKey(adjacentPos, job.regionifier);
                                if (hasOngoing(key)) {
                                    continue;
                                }
                                if (!world.isChunkLoaded(adjacentXIndex, adjacentZIndex)) {
                                    continue;
                                }
                                final Chunk chunk = world.getChunk(adjacentXIndex, adjacentZIndex, ChunkStatus.FULL, false);
                                if (chunk == null) {
                                    continue;
                                }
                                if (((ChunkSectionExtensions) chunk.getSection(adjacentYIndex)).advanced_ai$sectionData().getRegions(job.regionifier) == null) {
                                    addOngoing(key, futureHandle);
                                    return new ChunkRegionJob(adjacentPos, world, job.regionifier);
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        });
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            AdvancedAiDebug.init();
        }
    }

    private record ChunkPosRegionifierKey(ChunkSectionPos pos, ChunkRegionifier<?> regionifier) {
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }

    public static RunnableAiJobExecutor createExecutor() {
        final List<AiJobHandler> handlers = new ArrayList<>();
        AiJobExecutor.CREATION_EVENT.invoker().addHandlers(handlers::add);
        return new SingleThreadedJobExecutor(handlers, JOB_LOGGER, 512);
    }
}
