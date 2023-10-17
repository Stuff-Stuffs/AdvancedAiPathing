package io.github.stuff_stuffs.advanced_ai.common.api.ai;

import io.github.stuff_stuffs.advanced_ai.common.impl.ai.EntityLoggerImpl;
import org.slf4j.Logger;

public interface EntityLogger {
    default void debug(final Object message) {
        log(message, Severity.DEBUG);
    }

    default void warn(final Object message) {
        log(message, Severity.WARN);
    }

    default void error(final Object message) {
        log(message, Severity.ERROR);
    }

    void log(Object message, Severity severity);

    EntityLogger push(Object message);

    static EntityLogger create(final Logger logger) {
        return new EntityLoggerImpl(logger);
    }

    enum Severity {
        DEBUG,
        WARN,
        ERROR,
    }
}
