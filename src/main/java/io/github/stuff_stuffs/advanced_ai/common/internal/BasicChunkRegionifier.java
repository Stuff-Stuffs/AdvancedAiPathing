package io.github.stuff_stuffs.advanced_ai.common.internal;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.region.AbstractChunkRegionifier;
import io.github.stuff_stuffs.advanced_ai.common.api.region.ChunkSectionRegions;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.util.math.ChunkSectionPos;

public class BasicChunkRegionifier extends AbstractChunkRegionifier<CollisionHelper.FloorCollision> {
    @Override
    protected CollisionHelper.FloorCollision defaultReturn() {
        return CollisionHelper.FloorCollision.CLOSED;
    }

    @Override
    protected boolean valid(final CollisionHelper.FloorCollision val) {
        return val == CollisionHelper.FloorCollision.FLOOR;
    }

    @Override
    protected void enqueueStrongAdjacent(final CollisionHelper.FloorCollision val, final int x, final int y, final int z, final ChunkSectionPos sectionPos, final ShapeCache cache, final LocalPosConsumer consumer, final ChunkSectionRegions.Builder builder) {
        if (val == CollisionHelper.FloorCollision.FLOOR) {
            final short up = LocationCacheSection.pack(x, y + 1, z);
            if ((y & 15) != 15 && !builder.contains(up) && cache.getLocationCache(x, y + 1, z, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.OPEN) {
                consumer.accept(up);
            }
            final short west = LocationCacheSection.pack(x - 1, y, z);
            if ((x & 15) != 0 && !builder.contains(west) && cache.getLocationCache(x - 1, y, z, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
                consumer.accept(west);
            }
            final short east = LocationCacheSection.pack(x + 1, y, z);
            if ((x & 15) != 15 && !builder.contains(east) && cache.getLocationCache(x + 1, y, z, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
                consumer.accept(east);
            }
            final short north = LocationCacheSection.pack(x, y, z - 1);
            if ((z & 15) != 0 && !builder.contains(north) && cache.getLocationCache(x, y, z - 1, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
                consumer.accept(north);
            }
            final short south = LocationCacheSection.pack(x, y, z + 1);
            if ((z & 15) != 15 && !builder.contains(south) && cache.getLocationCache(x, y, z + 1, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
                consumer.accept(south);
            }
        } else if (val == CollisionHelper.FloorCollision.OPEN) {
            final short down = LocationCacheSection.pack(x, y - 1, z);
            if ((y & 15) != 0 && !builder.contains(down) && cache.getLocationCache(x, y - 1, z, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.FLOOR) {
                consumer.accept(down);
            }
            final short west = LocationCacheSection.pack(x - 1, y, z);
            if ((x & 15) != 0 && !builder.contains(west) && cache.getLocationCache(x - 1, y, z, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.FLOOR) {
                consumer.accept(west);
            }
            final short east = LocationCacheSection.pack(x + 1, y, z);
            if ((x & 15) != 15 && !builder.contains(east) && cache.getLocationCache(x + 1, y, z, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.FLOOR) {
                consumer.accept(east);
            }
            final short north = LocationCacheSection.pack(x, y, z - 1);
            if ((z & 15) != 0 && !builder.contains(north) && cache.getLocationCache(x, y, z - 1, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.FLOOR) {
                consumer.accept(north);
            }
            final short south = LocationCacheSection.pack(x, y, z + 1);
            if ((z & 15) != 15 && !builder.contains(south) && cache.getLocationCache(x, y, z + 1, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.FLOOR) {
                consumer.accept(south);
            }
        }
    }

    @Override
    protected void weakAdjacent(final CollisionHelper.FloorCollision val, final int x, final int y, final int z, final ChunkSectionPos sectionPos, final ShapeCache cache, final PosConsumer consumer) {
        if (val == CollisionHelper.FloorCollision.OPEN && ((y & 15) == 0 || cache.getLocationCache(x, y - 1, z, defaultReturn(), classifier()) == CollisionHelper.FloorCollision.OPEN)) {
            consumer.accept(x, y - 1, z);
        }
        if ((y & 15) == 15 && cache.getLocationCache(x, y + 1, z, CollisionHelper.FloorCollision.CLOSED, classifier()) == CollisionHelper.FloorCollision.OPEN) {
            consumer.accept(x, y + 1, z);
        }
        if ((x & 15) == 0 && cache.getLocationCache(x - 1, y, z, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
            consumer.accept(x - 1, y, z);
        }
        if ((x & 15) == 15 && cache.getLocationCache(x + 1, y, z, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
            consumer.accept(x + 1, y, z);
        }
        if ((z & 15) == 0 && cache.getLocationCache(x, y, z - 1, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
            consumer.accept(x, y, z - 1);
        }
        if ((z & 15) == 15 && cache.getLocationCache(x, y, z + 1, CollisionHelper.FloorCollision.CLOSED, classifier()) != CollisionHelper.FloorCollision.CLOSED) {
            consumer.accept(x, y, z + 1);
        }
    }

    @Override
    public LocationClassifier<CollisionHelper.FloorCollision> classifier() {
        return AdvancedAi.BASIC;
    }
}
