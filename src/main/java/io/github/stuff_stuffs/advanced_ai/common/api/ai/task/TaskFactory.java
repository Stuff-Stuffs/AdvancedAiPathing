package io.github.stuff_stuffs.advanced_ai.common.api.ai.task;

import java.util.Optional;

public interface TaskFactory<C, R, P> {
    Optional<? extends Task<? extends R, ? super C>> build(C context, P parameters);
}
