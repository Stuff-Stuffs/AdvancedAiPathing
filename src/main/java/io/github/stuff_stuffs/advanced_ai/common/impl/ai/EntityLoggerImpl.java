package io.github.stuff_stuffs.advanced_ai.common.impl.ai;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import org.slf4j.Logger;

public class EntityLoggerImpl implements EntityLogger {
    private final Logger logger;

    public EntityLoggerImpl(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(final Object message, final Severity severity) {
        switch (severity) {
            case DEBUG -> logger.debug("{}", message);
            case WARN -> logger.warn("{}", message);
            case ERROR -> logger.error("{}", message);
        }
    }

    @Override
    public EntityLogger push(final Object message) {
        log(message, Severity.DEBUG);
        return this;
    }
}
