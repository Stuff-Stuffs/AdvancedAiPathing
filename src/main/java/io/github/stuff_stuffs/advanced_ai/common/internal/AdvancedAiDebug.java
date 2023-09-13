package io.github.stuff_stuffs.advanced_ai.common.internal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.ChunkRegionJob;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.ChunkRegionLinkJob;
import io.github.stuff_stuffs.advanced_ai.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai.common.internal.extensions.ServerExtensions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.concurrent.TimeUnit;

public final class AdvancedAiDebug {
    private static @Nullable AStar.PathInfo<Node> LAST_PATH = null;

    public static void init() {
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (LAST_PATH != null && LAST_PATH.path() != null) {
                if (world.getTime() % 10 != 0) {
                    return;
                }
                final DustParticleEffect effect = new DustParticleEffect(new Vector3f(1, 0, 0), 1);
                for (final Node node : LAST_PATH.path()) {
                    world.spawnParticles(effect, node.x + 0.5, node.y + 0.5, node.z + 0.5, 1, 0, 0, 0, 1);
                }
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("location_cache").then(CommandManager.argument("radius", IntegerArgumentType.integer(0, 16)).executes(context -> {
                final Vec3d position = context.getSource().getPosition();
                final ChunkSectionPos pos = ChunkSectionPos.from(position);
                ChunkSectionPos.stream(pos, context.getArgument("radius", Integer.class)).forEach(p -> processLocationCacheJob(context, p));
                return 0;
            })));
            dispatcher.register(CommandManager.literal("path_find").executes(context -> {
                final BlockPos start = BlockPos.ofFloored(context.getSource().getPosition());
                final BlockPos target = new BlockPos(start.getX() + 1, -64, start.getZ() + 1);
                final Context ctx = new Context(ShapeCache.create(context.getSource().getWorld(), start.add(-256, 0, -256), target.add(256, 0, 256), 512));
                final StopWatch stopWatch = StopWatch.createStarted();
                final AStar.PathInfo<Node> info = new AStarImpl().findPath(new Node(start.getX(), start.getY(), start.getZ(), null, 0, CollisionHelper.FloorCollision.FLOOR), ctx, target, 1, Double.POSITIVE_INFINITY, true);
                stopWatch.stop();
                final double v = stopWatch.getTime(TimeUnit.NANOSECONDS) / 1_000_000.0;
                System.out.println("Time: " + (long) v + "ms");
                System.out.println("Nodes considered: " + info.nodesConsidered());
                System.out.println("Nodes/Second: " + (long) (info.nodesConsidered() / (v / 1000.0)));
                LAST_PATH = info;
                return 0;
            }));
            dispatcher.register(CommandManager.literal("aai_regionify").then(CommandManager.argument("radius", IntegerArgumentType.integer(0, 16)).executes(context -> {
                final Vec3d position = context.getSource().getPosition();
                final ChunkSectionPos pos = ChunkSectionPos.from(position);
                ChunkSectionPos.stream(pos, context.getArgument("radius", Integer.class)).forEach(p -> processRegionifyJob(context, p));
                return 0;
            })));
            dispatcher.register(CommandManager.literal("aai_region_link").then(CommandManager.argument("radius", IntegerArgumentType.integer(0, 14)).executes(context -> {
                final Vec3d position = context.getSource().getPosition();
                final ChunkSectionPos pos = ChunkSectionPos.from(position);
                ChunkSectionPos.stream(pos, context.getArgument("radius", Integer.class)).forEach(p -> processRegionLinkJob(context, p));
                return 0;
            })));
        });
    }

    private static final class AStarImpl extends AStar<Node, Context, BlockPos> {
        private AStarImpl() {
            super(Node.class);
        }

        @Override
        protected double heuristic(final Node node, final BlockPos target, final Context context) {
            return Math.abs(node.y - target.getY());
        }

        @Override
        protected double nodeCost(final Node node) {
            return node.cost;
        }

        @Override
        protected long key(final Node node) {
            return BlockPos.asLong(node.x, node.y, node.z);
        }

        @Override
        protected @Nullable Node previousNode(final Node node) {
            return node.prev;
        }

        private @Nullable Node createDoubleHeightChecked(final int x, final int y, final int z, final Node prev, final ShapeCache shapeCache, final CostGetter costGetter) {
            final CollisionHelper.FloorCollision walkable = getLocationType(x, y + 1, z, shapeCache);
            final CollisionHelper.FloorCollision groundWalkable = getLocationType(x, y, z, shapeCache);
            if (groundWalkable != CollisionHelper.FloorCollision.CLOSED && walkable == CollisionHelper.FloorCollision.OPEN && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1) {
                return new Node(x, y, z, prev, prev.cost + 1, groundWalkable);
            }
            return null;
        }

        private Node createAir(final int x, final int y, final int z, final Node prev, final ShapeCache shapeCache, final CostGetter costGetter) {
            if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1 && getLocationType(x, y, z, shapeCache) == CollisionHelper.FloorCollision.OPEN) {
                return new Node(x, y, z, prev, prev.cost + 1, CollisionHelper.FloorCollision.OPEN);
            }
            return null;
        }

        private Node createBasic(final int x, final int y, final int z, final Node prev, final ShapeCache shapeCache, final CostGetter costGetter) {
            final CollisionHelper.FloorCollision collision;
            if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1 && (collision = getLocationType(x, y, z, shapeCache)) != CollisionHelper.FloorCollision.CLOSED) {
                return new Node(x, y, z, prev, prev.cost + 1, collision);
            }
            return null;
        }

        private Node createAuto(final int x, final int y, final int z, final Node prev, final ShapeCache shapeCache, final CostGetter costGetter) {
            final CollisionHelper.FloorCollision type;
            if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + 1 && (type = getLocationType(x, y, z, shapeCache)) != CollisionHelper.FloorCollision.CLOSED) {
                final boolean ground = type == CollisionHelper.FloorCollision.FLOOR;
                return new Node(x, y, z, prev, prev.cost + (ground ? 10 : 1), ground ? CollisionHelper.FloorCollision.FLOOR : CollisionHelper.FloorCollision.OPEN);
            }
            return null;
        }

        private CollisionHelper.FloorCollision getLocationType(final int x, final int y, final int z, final ShapeCache shapeCache) {
            return shapeCache.getLocationCache(x, y, z, CollisionHelper.FloorCollision.CLOSED, AdvancedAi.BASIC);
        }

        @Override
        public int neighbours(final Node previous, final Context context, final CostGetter costGetter, final Node[] successors) {
            int i = 0;
            final ShapeCache cache = context.cache;
            Node node;
            if (previous.collision == CollisionHelper.FloorCollision.FLOOR) {
                node = createBasic(previous.x + 1, previous.y, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                node = createBasic(previous.x - 1, previous.y, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                node = createBasic(previous.x, previous.y, previous.z + 1, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                node = createBasic(previous.x, previous.y, previous.z - 1, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
            }

            //FALL DIAGONAL
            if (previous.collision == CollisionHelper.FloorCollision.FLOOR) {
                node = createDoubleHeightChecked(previous.x + 1, previous.y - 1, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                node = createDoubleHeightChecked(previous.x - 1, previous.y - 1, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                node = createDoubleHeightChecked(previous.x, previous.y - 1, previous.z + 1, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                node = createDoubleHeightChecked(previous.x, previous.y - 1, previous.z - 1, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
            }

            //Jump
            if (previous.collision == CollisionHelper.FloorCollision.FLOOR) {
                node = createAir(previous.x, previous.y + 1, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
            }
            //down
            if (previous.collision == CollisionHelper.FloorCollision.OPEN) {
                node = createAuto(previous.x, previous.y - 1, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
            }
            return i;
        }
    }

    private record Node(int x, int y, int z, @Nullable Node prev, double cost,
                        CollisionHelper.FloorCollision collision) {
        public long pos() {
            return BlockPos.asLong(x, y, z);
        }
    }

    private record Context(ShapeCache cache) {
    }

    private static void processLocationCacheJob(final CommandContext<ServerCommandSource> context, final ChunkSectionPos p) {
        ((ServerExtensions) context.getSource().getServer()).advanced_ai$executor().enqueue(new LocationCachingJob<>(p, context.getSource().getWorld(), AdvancedAi.BASIC));
    }

    private static void processRegionifyJob(final CommandContext<ServerCommandSource> context, final ChunkSectionPos p) {
        ((ServerExtensions) context.getSource().getServer()).advanced_ai$executor().enqueue(new ChunkRegionJob(p, context.getSource().getWorld(), AdvancedAi.BASIC_REGIONIFIER));
    }

    private static void processRegionLinkJob(final CommandContext<ServerCommandSource> context, final ChunkSectionPos p) {
        ((ServerExtensions) context.getSource().getServer()).advanced_ai$executor().enqueue(new ChunkRegionLinkJob(p, context.getSource().getWorld(), AdvancedAi.BASIC_REGIONIFIER));
    }

    private AdvancedAiDebug() {
    }
}
