package io.github.stuff_stuffs.advanced_ai.common.impl.ai.brain;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.AiBrain;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.event.BrainEvent;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.event.BrainEventType;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node.BrainNode;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AiBrainImpl<T> implements AiBrain<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedAi.MOD_ID + ":entity");
    private final BrainNode<Unit, T> root;
    private final TaskExecutionContext taskExecutionContext;
    private final EntityLogger logger = EntityLogger.create(LOGGER);
    private final PriorityQueue<EventEntry> expirationQueue = new ObjectHeapPriorityQueue<>(EventEntry.EXPIRATION_COMPARATOR);
    private final SortedSet<EventEntry> allEvents = new ObjectAVLTreeSet<>(EventEntry.START_COMPARATOR);
    private final Map<BrainEventType<?>, SortedSet<EventEntry>> byType = new Object2ReferenceOpenHashMap<>();
    private long age;

    public AiBrainImpl(final BrainNode<Unit, T> root, final TaskExecutionContext taskExecutionContext) {
        this.root = root;
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void submitEvent(final BrainEvent event) {
        final EventEntry entry = new EventEntry(event, age + event.duration(), age);
        expirationQueue.enqueue(entry);
        allEvents.add(entry);
        byType.computeIfAbsent(event.type(), i -> new ObjectAVLTreeSet<>(EventEntry.START_COMPARATOR)).add(entry);
    }

    @Override
    public List<BrainEvent> query(final long since) {
        final EventEntry s = new EventEntry(null, -1, since);
        final SortedSet<EventEntry> entries = allEvents.tailSet(s);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        final List<BrainEvent> events = new ArrayList<>(entries.size());
        for (final EventEntry entry : entries) {
            events.add(entry.event);
        }
        return events;
    }

    @Override
    public <Event extends BrainEvent> List<Event> query(final BrainEventType<Event> type, final long since) {
        final SortedSet<EventEntry> allEntries = byType.get(type);
        if (allEntries == null || allEntries.isEmpty()) {
            return Collections.emptyList();
        }
        final EventEntry s = new EventEntry(null, -1, since);
        final SortedSet<EventEntry> entries = allEntries.tailSet(s);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Event> events = new ArrayList<>(entries.size());
        for (final EventEntry entry : entries) {
            events.add((Event) entry.event);
        }
        return events;
    }

    @Override
    public long age() {
        return age;
    }

    @Override
    public void tick(final T entity) {
        final AiBrainContextImpl<T> context = new AiBrainContextImpl<>(entity, this, HashCommon.murmurHash3(HashCommon.murmurHash3(age) + 1));
        if (age == 0) {
            if (!root.init(context, logger)) {
                throw new RuntimeException();
            }
        }
        while (!expirationQueue.isEmpty() && expirationQueue.first().expiration < age) {
            final EventEntry entry = expirationQueue.dequeue();
            allEvents.remove(entry);
            final SortedSet<EventEntry> set = byType.get(entry.event.type());
            if (set != null) {
                set.remove(entry);
                if (set.isEmpty()) {
                    byType.get(entry.event.type());
                }
            }
        }
        root.tick(context, taskExecutionContext, logger);
        age++;
    }

    private record EventEntry(BrainEvent event, long expiration, long start) {
        public static final Comparator<EventEntry> EXPIRATION_COMPARATOR = Comparator.comparingLong(EventEntry::expiration);
        public static final Comparator<EventEntry> START_COMPARATOR = Comparator.comparingLong(EventEntry::start);
    }
}
