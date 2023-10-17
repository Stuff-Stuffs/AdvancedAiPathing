package io.github.stuff_stuffs.advanced_ai.common.api.ai.task_reqs;

import net.minecraft.entity.Entity;

public interface ReachingEntity extends TaskEntity {
    double reach();

    boolean attack(Entity target);
}
