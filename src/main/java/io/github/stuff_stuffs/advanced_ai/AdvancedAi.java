package io.github.stuff_stuffs.advanced_ai;

import io.github.stuff_stuffs.advanced_ai.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.api.util.CollisionUtil;
import io.github.stuff_stuffs.advanced_ai.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.api.util.UniverseInfo;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedAi implements ModInitializer {
    public static final String MOD_ID = "advanced_id";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final UniverseInfo<CollisionUtil.FloorCollision> FLOOR_COLLISION_INFO = UniverseInfo.fromEnum(CollisionUtil.FloorCollision.class);
    public static final LocationClassifier<CollisionUtil.FloorCollision> BASIC = new LocationClassifier<>() {
        private static final Box BOX = new Box(0, -1, 0, 1, 2, 1);

        @Override
        public CollisionUtil.FloorCollision get(final int x, final int y, final int z, final ShapeCache cache) {
            return CollisionUtil.open(x, y, z, BOX, 1 / 3.0 - 0.00000000001, cache);
        }

        @Override
        public UniverseInfo<CollisionUtil.FloorCollision> universeInfo() {
            return FLOOR_COLLISION_INFO;
        }
    };

    @Override
    public void onInitialize() {
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}