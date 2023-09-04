package io.github.stuff_stuffs.advanced_ai.common.internal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionInfo;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.LocationCacheDebugSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.advanced_ai.common.impl.DenseLocationCacheSectionImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

public class AdvancedAi implements ModInitializer {
    public static final String MOD_ID = "advanced_ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final int UPDATES_BEFORE_REBUILD = 8;
    public static final Identifier DEBUG_CHANNEL = id("debug_channel");
    public static final UniverseInfo<CollisionHelper.FloorCollision> FLOOR_COLLISION_INFO = UniverseInfo.fromEnum(CollisionHelper.FloorCollision.class);
    public static final LocationClassifier<CollisionHelper.FloorCollision> BASIC = new LocationClassifier<>() {
        private static final CollisionHelper COLLISION_HELPER = new CollisionHelper(1, 2, 1.0);

        @Override
        public CollisionHelper.FloorCollision get(final int x, final int y, final int z, final ShapeCache cache) {
            return COLLISION_HELPER.open(x, y, z, cache);
        }

        @Override
        public boolean needsRebuild(final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int otherChunkSectionX, final int otherChunkSectionY, final int otherChunkSectionZ, final int relX, final int relY, final int relZ, final ShapeCache cache) {
            if (relY > 16) {
                return false;
            }
            final boolean xAdj = (relX == -1 || relX == 16);
            final boolean zAdj = (relZ == -1 || relZ == 16);
            if (relY > 0 & (xAdj | zAdj)) {
                final BlockState state = cache.getBlockState(chunkSectionX << 4 + relX, chunkSectionY << 4 + relY + cache.getBottomY(), chunkSectionZ << 4 + relZ);
                return state.exceedsCube();
            }
            if (relY == -1) {
                return true;
            }
            return false;
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
    public static final ProcessedLocationClassifier<CollisionHelper.FloorCollision> PROCESSED_BASIC;

    @Override
    public void onInitialize() {
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
        final WorldChunk chunk = context.getSource().getWorld().getChunk(p.getSectionX(), p.getSectionZ());
        if (chunk != null) {
            final BlockPos minPos = p.getMinPos();
            final ShapeCache cache = ShapeCache.create(context.getSource().getWorld(), minPos.add(-16, -16, -16), minPos.add(32, 32, 32), 4096);
            final long time = System.nanoTime();
            final LocationCacheSection<CollisionHelper.FloorCollision> section;
            if (tryRebuild(p, cache, BASIC)) {
                final Chunk c = cache.getChunk(p.getMinX(), p.getMinY(), p.getMinZ());
                final int yIndex = cache.sectionCoordToIndex(p.getSectionY());
                if (yIndex < 0 || yIndex >= cache.countVerticalSections()) {
                    return;
                }
                section = ((ChunkSectionExtensions) c.getSection(yIndex)).advanced_ai$sectionData().get(BASIC);
            } else {
                section = new DenseLocationCacheSectionImpl<>(collectModCounts(p, cache), cache, p, AdvancedAi.PROCESSED_BASIC);
                final Chunk cacheChunk = cache.getChunk(minPos.getX(), minPos.getY(), minPos.getZ());
                ((ChunkSectionExtensions) cacheChunk.getSection(cacheChunk.sectionCoordToIndex(p.getSectionY()))).advanced_ai$sectionData().put(BASIC, section);
            }
            final long duration = System.nanoTime() - time;
            ((ServerWorldExtensions) context.getSource().getWorld()).advanced_ai$debug(new DebugSectionInfo<>(new LocationCacheDebugSection(Map.of(BASIC, new LocationCacheDebugSection.Entry<>(BASIC, section))), p, DebugSectionType.LOCATION_CACHE_TYPE));
            System.out.println(duration / 1_000_000.0);
        }
    }

    private static long[] collectModCounts(final ChunkSectionPos p, final ShapeCache cache) {
        final long[] modCounts = new long[27];
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                final Chunk c = cache.getChunk((p.getSectionX() + i) << 4, 0, (p.getSectionZ() + j) << 4);
                for (int k = -1; k <= 1; k++) {
                    final int yIndex = cache.sectionCoordToIndex(p.getSectionY() + j);
                    final int modCountIndex = LocationCacheSection.modCountIndex(i, j, k);
                    if (c == null || yIndex < 0 || yIndex >= cache.countVerticalSections()) {
                        modCounts[modCountIndex] = -1;
                    } else {
                        final MemorizingChunkSection section = (MemorizingChunkSection) c.getSection(yIndex);
                        final long modCount = section.advanced_ai$modCount();
                        modCounts[modCountIndex] = modCount;
                    }
                }
            }
        }
        return modCounts;
    }

    private static <T> boolean tryRebuild(final ChunkSectionPos p, final ShapeCache cache, final LocationClassifier<T> classifier) {
        final Chunk centerChunk = cache.getChunk(p.getMinX(), p.getMinY() + cache.getBottomY(), p.getMinZ());
        if (centerChunk == null) {
            return true;
        }
        final BlockState[] oldArr = new BlockState[UPDATES_BEFORE_REBUILD];
        final BlockState[] newArr = new BlockState[UPDATES_BEFORE_REBUILD];
        final short[] coords = new short[UPDATES_BEFORE_REBUILD];
        final LocationCacheSection<T> stale = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai$sectionData().getStale(classifier);
        if (stale == null) {
            return false;
        }
        final long[] modCounts = stale.modCounts();
        final int sectionCount = cache.countVerticalSections();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                final Chunk c = cache.getChunk((p.getSectionX() + i) << 4, 0, (p.getSectionZ() + j) << 4);
                for (int k = -1; k <= 1; k++) {
                    final int yIndex = cache.sectionCoordToIndex(p.getSectionY() + k);
                    if (yIndex < 0 || yIndex >= sectionCount) {
                        continue;
                    }
                    final int modCountIndex = LocationCacheSection.modCountIndex(i, j, k);
                    if (c == null) {
                        if (modCounts[modCountIndex] != -1) {
                            return false;
                        }
                    } else {
                        final MemorizingChunkSection section = (MemorizingChunkSection) c.getSection(yIndex);
                        final long modCount = section.advanced_ai$modCount();
                        final long currentModCount = modCounts[modCountIndex];
                        if (modCount == currentModCount) {
                            continue;
                        }
                        if (modCount < modCounts[modCountIndex] || modCount >= currentModCount + UPDATES_BEFORE_REBUILD) {
                            return false;
                        }
                        if (!section.advanced_ai$copy_updates(currentModCount, oldArr, 0, newArr, 0, coords, 0)) {
                            return false;
                        }
                        final int count = (int) (modCount - currentModCount);
                        modCounts[modCountIndex] = modCount;
                        if (count == 0) {
                            continue;
                        }
                        final int baseX = i * 16;
                        final int baseY = k * 16;
                        final int baseZ = j * 16;
                        for (int l = 0; l < count; l++) {
                            if (classifier.needsRebuild(p.getSectionX(), p.getSectionY(), p.getSectionZ(), p.getSectionX() + i, p.getSectionY() + k, p.getSectionZ() + j, baseX + LocationCacheSection.unpackX(coords[l]), baseY + LocationCacheSection.unpackY(coords[l]), baseZ + LocationCacheSection.unpackZ(coords[l]), cache)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        final LocationCacheSection<T> promote = ((ChunkSectionExtensions) centerChunk.getSection(cache.sectionCoordToIndex(p.getSectionY()))).advanced_ai$sectionData().promote(classifier);
        if (promote != null) {
            promote.modCounts(modCounts);
        }
        return promote != null;
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }

    static {
        try {
            PROCESSED_BASIC = new ProcessedLocationClassifier<>(BASIC);
        } catch (final IllegalAccessException | NoSuchMethodException var1) {
            throw new RuntimeException(var1);
        }
    }
}
