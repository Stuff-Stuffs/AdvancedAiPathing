package io.github.stuff_stuffs.advanced_ai.common.api.job;

import org.slf4j.Logger;

public interface AiJob {
    boolean run(Logger logger);

    void apply(Logger logger);

    default void init(final Logger logger) {
    }

    default void onServerStop(final Logger logger) {
    }

    default void cancel(final Logger logger) {
    }

    default void timeout(final Logger logger) {
    }

    default Object debugData() {
        return "NoDebugDataPresent";
    }
}
