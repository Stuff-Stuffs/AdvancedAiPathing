package io.github.stuff_stuffs.advanced_ai.common.api.ai.task_reqs;

import io.github.stuff_stuffs.advanced_ai.common.api.ai.EntityLogger;
import net.minecraft.util.math.Vec3d;

public interface PathingEntity extends TaskEntity {
    boolean walkTowards(Vec3d pos, EntityLogger logger);
}
