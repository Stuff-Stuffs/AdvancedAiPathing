package io.github.stuff_stuffs.advanced_ai.client.internal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.stuff_stuffs.advanced_ai.client.api.debug.DebugRenderer;
import io.github.stuff_stuffs.advanced_ai.client.api.debug.DebugRendererRegistry;
import io.github.stuff_stuffs.advanced_ai.client.impl.LocationCacheDebugRenderer;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionInfo;
import io.github.stuff_stuffs.advanced_ai.common.api.debug.DebugSectionType;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Map;
import java.util.Set;

public class AdvancedAiClient implements ClientModInitializer {
    public static final Map<DebugSectionType<?>, Map<ChunkSectionPos, DebugSectionInfo<?>>> DEBUG_INFOS = new Reference2ReferenceOpenHashMap<>();
    private static final Set<LocationClassifier<?>> DEBUG_VISIBLE = new ObjectOpenHashSet<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AdvancedAi.DEBUG_CHANNEL, (client, handler, buf, responseSender) -> {
            final DebugSectionInfo<?> read = DebugSectionInfo.read(buf);
            if (read != null) {
                client.execute(() -> DEBUG_INFOS.computeIfAbsent(read.type(), i -> new Object2ReferenceOpenHashMap<>()).put(read.pos(), read));
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            for (final Map.Entry<DebugSectionType<?>, Map<ChunkSectionPos, DebugSectionInfo<?>>> entry : DEBUG_INFOS.entrySet()) {
                final DebugRenderer<?> renderer = DebugRendererRegistry.get(entry.getKey());
                render(renderer, entry.getValue(), context);
            }
        });
        DebugRendererRegistry.register(DebugSectionType.LOCATION_CACHE_TYPE, new LocationCacheDebugRenderer());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            final RequiredArgumentBuilder<FabricClientCommandSource, RegistryEntry.Reference<LocationClassifier<?>>> classifierArg = ClientCommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registryAccess, LocationClassifier.REGISTRY_KEY));
            final RequiredArgumentBuilder<FabricClientCommandSource, AddRemoveArgumentType.Op> addRemoveArg = ClientCommandManager.argument("add/remove", AddRemoveArgumentType.ARGUMENT_TYPE);
            final Command<FabricClientCommandSource> command = context -> {
                final RegistryEntry.Reference<?> classifier = context.getArgument("type", RegistryEntry.Reference.class);
                final AddRemoveArgumentType.Op op = context.getArgument("add/remove", AddRemoveArgumentType.Op.class);
                if (classifier.registryKey().isOf(LocationClassifier.REGISTRY_KEY)) {
                    final RegistryEntry.Reference<LocationClassifier<?>> casted = (RegistryEntry.Reference<LocationClassifier<?>>) classifier;
                    if (op == AddRemoveArgumentType.Op.ADD) {
                        DEBUG_VISIBLE.add(casted.value());
                    } else {
                        DEBUG_VISIBLE.remove(casted.value());
                    }
                    return 0;
                }
                return 1;
            };
            dispatcher.register(ClientCommandManager.literal("aai_enable_debug_location_cache").then(addRemoveArg.then(classifierArg.executes(command))));
        });
    }

    public static boolean shouldRender(final LocationClassifier<?> classifier) {
        return DEBUG_VISIBLE.contains(classifier);
    }

    private static <T> void render(final DebugRenderer<T> renderer, final Map<ChunkSectionPos, DebugSectionInfo<?>> infos, final WorldRenderContext context) {
        for (final DebugSectionInfo<?> value : infos.values()) {
            renderer.render((T) value.data(), value.pos(), context);
        }
    }
}