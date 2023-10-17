package io.github.stuff_stuffs.advanced_ai.common.api.ai.task_reqs;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public interface TaskEntity {
    UUID id();

    Vec3d pos();

    Box bounds();

    ServerWorld world();

    GameProfile delegateProfile();
}
