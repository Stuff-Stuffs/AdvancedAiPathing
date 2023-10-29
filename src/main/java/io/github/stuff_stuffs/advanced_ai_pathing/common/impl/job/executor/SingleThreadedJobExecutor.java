package io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job.executor;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.RunnableAiJobExecutor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.slf4j.Logger;

import java.util.*;

public class SingleThreadedJobExecutor implements RunnableAiJobExecutor {
    protected final Logger logger;
    protected final int maxWaitingTasks;
    protected final ArrayDeque<LocationCachingJob<?>> jobs;
    protected final Long2ObjectMap<Set<LocationClassifier<?>>> ongoing;

    public SingleThreadedJobExecutor(final Logger logger, final int maxWaitingTasks) {
        this.maxWaitingTasks = maxWaitingTasks;
        jobs = new ArrayDeque<>(maxWaitingTasks);
        this.logger = logger;
        ongoing = new Long2ObjectOpenHashMap<>();
    }

    @Override
    public void run(final int millis) {
        final long startTime = System.nanoTime();
        final boolean infoEnabled = logger.isInfoEnabled();
        while (!jobs.isEmpty()) {
            final LocationCachingJob<?> job = jobs.removeFirst();
            long l = 0;
            if (infoEnabled) {
                l = System.nanoTime();
            }
            job.run();
            final long key = job.pos.asLong();
            final Set<LocationClassifier<?>> classifiers = ongoing.get(key);
            if(classifiers!=null&&classifiers.remove(job.classifier)&&classifiers.isEmpty()) {
                ongoing.remove(key);
            }
            final long currentTime = System.nanoTime();
            if (infoEnabled) {
                logger.info("Spent {} milliseconds on task {}",(currentTime - l)*0.000001, job.debugData());
            }
            if ((currentTime - startTime)*0.000001 > millis) {
                return;
            }
        }
    }

    @Override
    public void enqueue(LocationCachingJob<?> job) {
        final long key = job.pos.asLong();
        final Set<LocationClassifier<?>> classifiers = ongoing.get(key);
        if(classifiers==null || classifiers.add(job.classifier)) {
            jobs.add(job);
            if(classifiers==null) {
                Set<LocationClassifier<?>> set = new ObjectOpenHashSet<>();
                set.add(job.classifier);
                ongoing.put(key, set);
            } else {
                classifiers.add(job.classifier);
            }
        }
    }

    protected static final class HandleImpl {
        private boolean alive = true;

        public boolean alive() {
            return alive;
        }

        public void kill() {
            alive = false;
        }
    }
}
