package io.github.stuff_stuffs.advanced_ai.common.api.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public interface UniverseInfo<T> {
    T fromIndex(int index);

    int toIndex(T value);

    MethodHandle fromIndexHandle();

    MethodHandle toIndexHandle();

    int size();

    Class<T> clazz();

    static <T extends Enum<T>> UniverseInfo<T> fromEnum(final Class<T> clazz) {
        final T[] constants = clazz.getEnumConstants();
        return new UniverseInfo<>() {
            @Override
            public T fromIndex(final int index) {
                return constants[index];
            }

            @Override
            public int toIndex(final T value) {
                return value.ordinal();
            }

            @Override
            public MethodHandle fromIndexHandle() {
                return MethodHandles.arrayElementGetter(constants.getClass()).bindTo(constants);
            }

            @Override
            public MethodHandle toIndexHandle() {
                try {
                    return MethodHandles.lookup().findVirtual(Enum.class, "ordinal", MethodType.methodType(Integer.TYPE));
                } catch (final NoSuchMethodException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public int size() {
                return constants.length;
            }

            @Override
            public Class<T> clazz() {
                return clazz;
            }
        };
    }
}
