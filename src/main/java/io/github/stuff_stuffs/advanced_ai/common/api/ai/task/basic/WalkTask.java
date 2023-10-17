package io.github.stuff_stuffs.advanced_ai.common.api.ai.task.basic;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.Task;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskFactory;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task_reqs.PathingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class WalkTask implements Task<WalkTask.Result, PathingEntity> {
    private final Vec3d target;
    private final double maxError;

    public static <T extends PathingEntity> TaskFactory<T, Result, Parameters> factory() {
        return (context, parameters) -> Optional.of(new WalkTask(parameters.target(), parameters.maxError()));
    }

    public WalkTask(final Vec3d target, final double maxError) {
        this.target = target;
        this.maxError = maxError;
    }

    @Override
    public Result tick(final PathingEntity entity, final TaskExecutionContext executionContext, final EntityLogger logger) {
        if (entity.walkTowards(target, logger)) {
            return Result.CONTINUE;
        }
        return entity.pos().squaredDistanceTo(target) < maxError * maxError ? Result.FINISHED : Result.CANT_REACH;
    }

    public interface Parameters {
        Vec3d target();

        double maxError();
    }

    public enum Result {
        CONTINUE,
        CANT_REACH,
        FINISHED
    }
}
