package io.github.stuff_stuffs.advanced_ai.common.api.util;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Math;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AiUtil {
    public static void interact(final GameProfile profile, final ServerWorld world, final Consumer<ServerPlayerEntity> consumer) {
        final FakePlayer player = FakePlayer.get(world, profile);
        consumer.accept(player);
    }

    public static <T> T interact(final GameProfile profile, final ServerWorld world, final Function<ServerPlayerEntity, T> consumer) {
        final FakePlayer player = FakePlayer.get(world, profile);
        return consumer.apply(player);
    }

    public static double arcDist(final Vec3d start, final Vec3d end) {
        return Math.acos(MathHelper.clamp(start.dotProduct(end), -1, 1));
    }

    public static Vec3d slerp(final Vec3d start, final Vec3d end, final double progress) {
        final double omega = Math.acos(MathHelper.clamp(start.dotProduct(end), -1, 1));
        final double sinO = Math.sin(omega);
        if (Math.abs(sinO) < 0.001) {
            return start.multiply(1 - progress).add(end.multiply(progress)).normalize();
        }
        final double sin1PO = Math.sin((1 - progress) * omega);
        final double sinPO = Math.sin(progress * omega);
        return start.multiply(sin1PO / sinO).add(end.multiply(sinPO / sinO));
    }

    public static int roundToUpPower2(int v) {
        --v;
        v |= v >>> 1;
        v |= v >>> 2;
        v |= v >>> 4;
        v |= v >>> 8;
        v |= v >>> 16;
        ++v;
        return v;
    }

    public static <T> void wrappingCopy(final T[] source, final int startIndex, final int endIndex, final T[] target, final int offset) {
        if (startIndex <= endIndex) {
            System.arraycopy(source, startIndex, target, offset, endIndex - startIndex);
        } else {
            System.arraycopy(source, startIndex, target, offset, source.length - startIndex);
            System.arraycopy(source, 0, target, offset + source.length - startIndex, endIndex);
        }
    }

    public static void wrappingCopy(final short[] source, final int startIndex, final int endIndex, final short[] target, final int offset) {
        if (startIndex <= endIndex) {
            System.arraycopy(source, startIndex, target, offset, endIndex - startIndex);
        } else {
            System.arraycopy(source, startIndex, target, offset, source.length - startIndex);
            System.arraycopy(source, 0, target, offset + source.length - startIndex, endIndex);
        }
    }

    public static float fastApproximateLog(final float x) {
        final int i = 0x5F400000 - (Float.floatToRawIntBits(x) >> 1);
        final float y = Float.intBitsToFloat(i);
        return y * (1.47f - 0.47f * x * y * y);
    }

    public static <T, C> T raycast(final Vec3d start, final Vec3d end, final C context, final BiFunction<C, BlockPos, T> blockHitFactory, final Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            final double d = MathHelper.lerp(-1.0E-7, end.x, start.x);
            final double e = MathHelper.lerp(-1.0E-7, end.y, start.y);
            final double f = MathHelper.lerp(-1.0E-7, end.z, start.z);
            final double g = MathHelper.lerp(-1.0E-7, start.x, end.x);
            final double h = MathHelper.lerp(-1.0E-7, start.y, end.y);
            final double i = MathHelper.lerp(-1.0E-7, start.z, end.z);
            int j = MathHelper.floor(g);
            int k = MathHelper.floor(h);
            int l = MathHelper.floor(i);
            final BlockPos.Mutable mutable = new BlockPos.Mutable(j, k, l);
            final T object = blockHitFactory.apply(context, mutable);
            if (object != null) {
                return object;
            } else {
                final double m = d - g;
                final double n = e - h;
                final double o = f - i;
                final int p = MathHelper.sign(m);
                final int q = MathHelper.sign(n);
                final int r = MathHelper.sign(o);
                final double s = p == 0 ? Double.MAX_VALUE : (double) p / m;
                final double t = q == 0 ? Double.MAX_VALUE : (double) q / n;
                final double u = r == 0 ? Double.MAX_VALUE : (double) r / o;
                double v = s * (p > 0 ? 1.0 - MathHelper.fractionalPart(g) : MathHelper.fractionalPart(g));
                double w = t * (q > 0 ? 1.0 - MathHelper.fractionalPart(h) : MathHelper.fractionalPart(h));
                double x = u * (r > 0 ? 1.0 - MathHelper.fractionalPart(i) : MathHelper.fractionalPart(i));

                while (v <= 1.0 || w <= 1.0 || x <= 1.0) {
                    if (v < w) {
                        if (v < x) {
                            j += p;
                            v += s;
                        } else {
                            l += r;
                            x += u;
                        }
                    } else if (w < x) {
                        k += q;
                        w += t;
                    } else {
                        l += r;
                        x += u;
                    }

                    final T object2 = blockHitFactory.apply(context, mutable.set(j, k, l));
                    if (object2 != null) {
                        return object2;
                    }
                }

                return missFactory.apply(context);
            }
        }
    }

    private AiUtil() {
    }
}
