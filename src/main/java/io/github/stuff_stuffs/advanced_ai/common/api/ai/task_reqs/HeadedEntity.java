package io.github.stuff_stuffs.advanced_ai.common.api.ai.task_reqs;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public interface HeadedEntity extends TaskEntity {
    double eyeHeight();

    double headPitch();

    double headYaw();

    void setHeadPitch(double headPitch);

    void setHeadYaw(double headYaw);

    default Vec3d eyePos() {
        final Box bounds = bounds();
        return bounds.getCenter().add(0, eyeHeight() - bounds.getYLength() * 0.5, 0);
    }

    default Vec3d lookVec() {
        return Vec3d.fromPolar((float) headPitch(), (float) headYaw());
    }
}
