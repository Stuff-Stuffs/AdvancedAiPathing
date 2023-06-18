package io.github.stuff_stuffs.advanced_ai;

import io.github.stuff_stuffs.advanced_ai.api.util.CollisionUtil;
import io.github.stuff_stuffs.advanced_ai.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.impl.DenseLocationCacheSectionImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.glfw.GLFW;

public class AdvancedAiClient implements ClientModInitializer {
    public static final KeyBinding TEST_KEY_BIND = new KeyBinding(AdvancedAi.MOD_ID + ".test", GLFW.GLFW_KEY_F8, "misc");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(TEST_KEY_BIND);
        ServerTickEvents.END_WORLD_TICK.register(new ServerTickEvents.EndWorldTick() {
            @Override
            public void onEndTick(final ServerWorld world) {
                if (TEST_KEY_BIND.wasPressed()) {
                    final BlockPos pos = MinecraftClient.getInstance().cameraEntity.getBlockPos();
                    final ChunkSectionPos sectionPos = ChunkSectionPos.from(pos);
                    final BlockPos minPos = sectionPos.getMinPos();
                    final ShapeCache cache = ShapeCache.create(world, minPos.add(-16, -16, -16), minPos.add(32, 32, 32), 2048);
                    final long time = System.nanoTime();
                    final DenseLocationCacheSectionImpl<CollisionUtil.FloorCollision> section = new DenseLocationCacheSectionImpl<>(cache, sectionPos, AdvancedAi.BASIC);
                    final long duration = System.nanoTime() - time;
                    System.out.println(duration / 1_000_000.0);
                }
            }
        });
    }
}