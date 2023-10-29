package io.github.stuff_stuffs.advanced_ai_pathing.common.api.job;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.Optional;
import java.util.function.Consumer;

public interface AiJobExecutor {
    Event<CreationEvent> CREATION_EVENT = EventFactory.createArrayBacked(CreationEvent.class, events -> acceptor -> {
        for (final CreationEvent event : events) {
            event.addHandlers(acceptor);
        }
    });


    Optional<AiJobHandle> enqueue(final AiJob job);

    interface CreationEvent {
        void addHandlers(Consumer<AiJobHandler> acceptor);
    }
}
