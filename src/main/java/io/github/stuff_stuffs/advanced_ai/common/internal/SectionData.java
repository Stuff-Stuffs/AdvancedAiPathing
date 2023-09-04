package io.github.stuff_stuffs.advanced_ai.common.internal;

import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationCacheSection;
import io.github.stuff_stuffs.advanced_ai.common.api.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CopyOnWriteMap;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class SectionData {
    private final CopyOnWriteMap<LocationClassifier<?>, LocationCacheSection<?>> currentMap = new CopyOnWriteMap<>();
    private final CopyOnWriteMap<LocationClassifier<?>, LocationCacheSection<?>> staleMap = new CopyOnWriteMap<>();

    public <T> @Nullable LocationCacheSection<T> get(final LocationClassifier<T> classifier) {
        return (LocationCacheSection<T>) currentMap.get(classifier);
    }

    public <T> @Nullable LocationCacheSection<T> getStale(final LocationClassifier<T> classifier) {
        LocationCacheSection<?> section = staleMap.get(classifier);
        if (section == null) {
            section = currentMap.get(classifier);
        }
        return (LocationCacheSection<T>) section;
    }

    public <T> void put(final LocationClassifier<T> classifier, final LocationCacheSection<T> section) {
        currentMap.put(classifier, section);
    }

    public void purgeAll() {
        final Set<LocationClassifier<?>> staleKeys = staleMap.keys();
        final Set<LocationClassifier<?>> currentKeys = currentMap.keys();
        for (final LocationClassifier<?> key : staleKeys) {
            purge(key);
        }
        for (final LocationClassifier<?> key : currentKeys) {
            if (!staleKeys.contains(key)) {
                purge(key);
            }
        }
    }

    public void purge(final LocationClassifier<?> classifier) {
        final LocationCacheSection<?> removed = currentMap.remove(classifier);
        if (removed != null) {
            staleMap.put(classifier, removed);
        } else {
            if (staleMap.get(classifier) != null) {
                staleMap.remove(classifier);
            }
        }
    }

    public <T> LocationCacheSection<T> promote(final LocationClassifier<T> classifier) {
        final LocationCacheSection<?> removed = staleMap.remove(classifier);
        if (removed != null) {
            currentMap.put(classifier, removed);
            return (LocationCacheSection<T>) removed;
        }
        return null;
    }
}
