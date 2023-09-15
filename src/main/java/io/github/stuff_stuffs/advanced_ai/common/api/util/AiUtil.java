package io.github.stuff_stuffs.advanced_ai.common.api.util;

public final class AiUtil {
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

    private AiUtil() {
    }
}
