package io.github.stuff_stuffs.advanced_ai.common.api.ai.task.basic;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskKey;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAi;

public final class BasicTaskKeys {
    public static final TaskKey<WalkTask.Result, WalkTask.Parameters> WALK_TASK_KEY = TaskKey.create(AdvancedAi.id("walk"), WalkTask.Result.class, WalkTask.Parameters.class);
    public static final TaskKey<LookTask.Result, LookTask.Parameters> LOOK_TASK_KEY = TaskKey.create(AdvancedAi.id("look"), LookTask.Result.class, LookTask.Parameters.class);

    private BasicTaskKeys() {
    }
}
