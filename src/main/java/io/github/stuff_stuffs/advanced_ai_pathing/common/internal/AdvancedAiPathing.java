package io.github.stuff_stuffs.advanced_ai_pathing.common.internal;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.job.AiJobExecutor;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.job.AiJobHandler;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.PreLocationCacheSection;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job.executor.SingleThreadedJobExecutor;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.pathing.location_cache.LocationCacheSectionRegistry;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.pathing.location_cache.LocationCacheSubSection;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.pathing.location_cache.UniformLocationCacheSectionImpl;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ServerExtensions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AdvancedAiPathing implements ModInitializer {
    public static final String MOD_ID = "advanced_ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Logger JOB_LOGGER = LoggerFactory.getLogger(MOD_ID + ":job_executor");
    public static final int UPDATES_BEFORE_REBUILD = 24;


    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> ((ServerExtensions) server).advanced_ai_pathing$executor().run(25));
        LocationCacheSectionRegistry.register(id("uniform"), new LocationCacheSectionRegistry.Factory() {
            @Override
            public <T> LocationCacheSubSection<T> create(final PreLocationCacheSection<T> section, final UniverseInfo<T> info) {
                return new UniformLocationCacheSectionImpl<>(section.get(0));
            }
        }, new LocationCacheSectionRegistry.SizeEstimator() {
            @Override
            public <T> long estimateBytes(final PreLocationCacheSection<T> section, final UniverseInfo<T> info) {
                int nonZeroCount = 0;
                final int universeSize = info.size();
                for (int i = 0; i < universeSize; i++) {
                    if (section.count(i) != 0) {
                        nonZeroCount++;
                    }
                }
                if (nonZeroCount == 1) {
                    return 1;
                }
                return Long.MAX_VALUE;
            }
        });
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            AdvancedAiPathingDebug.init();
        }
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
