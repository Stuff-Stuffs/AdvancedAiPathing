package io.github.stuff_stuffs.advanced_ai.common.api.util;

public final class AiUtil {
    public static <T> void wrappingCopy(final T[] source, final int startIndex, final int endIndex, final T[] target, final int offset) {
        if (startIndex < endIndex) {
            System.arraycopy(source, startIndex, target, offset, endIndex - startIndex);
        } else {
            System.arraycopy(source, startIndex, target, offset, source.length - startIndex);
            System.arraycopy(source, 0, target, offset + source.length - startIndex, endIndex);
        }
    }

    public static void wrappingCopy(final short[] source, final int startIndex, final int endIndex, final short[] target, final int offset) {
        if (startIndex < endIndex) {
            System.arraycopy(source, startIndex, target, offset, endIndex - startIndex);
        } else {
            System.arraycopy(source, startIndex, target, offset, source.length - startIndex);
            System.arraycopy(source, 0, target, offset + source.length - startIndex, endIndex);
        }
    }

    private AiUtil() {
    }
}
