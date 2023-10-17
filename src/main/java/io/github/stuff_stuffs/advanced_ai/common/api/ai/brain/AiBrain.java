package io.github.stuff_stuffs.advanced_ai.common.api.ai.brain;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.event.BrainEvent;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.event.BrainEventType;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.brain.node.BrainNode;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import io.github.stuff_stuffs.advanced_ai.common.impl.ai.brain.AiBrainImpl;

import java.util.List;

public interface AiBrain<T> {
    void submitEvent(BrainEvent event);

    List<BrainEvent> query(long since);

    <Event extends BrainEvent> List<Event> query(BrainEventType<Event> type, long since);

    long age();

    void tick(T entity);

    static <T> AiBrain<T> create(final BrainNode<Unit, T> node, final TaskExecutionContext taskExecutionContext) {
        return new AiBrainImpl<>(node, taskExecutionContext);
    }
}
