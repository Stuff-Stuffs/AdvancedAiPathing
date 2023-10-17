package io.github.stuff_stuffs.advanced_ai.common.api.ai.task.basic;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.Task;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskExecutionContext;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task.TaskFactory;
import io.github.stuff_stuffs.advanced_ai.common.api.ai.task_reqs.HeadedEntity;
import io.github.stuff_stuffs.advanced_ai.common.api.util.AiUtil;
import net.minecraft.util.math.Vec3d;
import org.joml.Math;

import java.util.Optional;

public class LookTask implements Task<LookTask.Result, HeadedEntity> {
    private final Vec3d start;
    private final Vec3d end;
    private final double length;
    private final double rotateSpeed;
    private int ticks;

    public static <T extends HeadedEntity> TaskFactory<T, Result, Parameters> factory() {
        return (context, parameters) -> Optional.of(new LookTask(context, parameters));
    }

    public LookTask(final HeadedEntity entity, final Parameters parameters) {
        this(entity.lookVec(), parameters.end(), parameters.rotateSpeed());
    }

    public LookTask(final Vec3d start, final Vec3d end, final double rotateSpeed) {
        this.start = start.normalize();
        this.end = end.normalize();
        length = AiUtil.arcDist(this.start, this.end);
        this.rotateSpeed = rotateSpeed;
    }

    @Override
    public Result tick(final HeadedEntity entity, final TaskExecutionContext executionContext, final EntityLogger logger) {
        final Vec3d slerped = AiUtil.slerp(start, end, Math.min(++ticks * rotateSpeed / length, 1));
        entity.setHeadPitch(Math.toDegrees(Math.sin(slerped.y)));
        entity.setHeadYaw(Math.toDegrees(Math.atan2(slerped.z, slerped.x)));
        if (ticks * rotateSpeed / length >= 1) {
            return Result.DONE;
        }
        return Result.CONTINUE;
    }

    public interface Parameters {
        Vec3d end();

        double rotateSpeed();
    }

    public enum Result {
        CONTINUE,
        DONE
    }
}
