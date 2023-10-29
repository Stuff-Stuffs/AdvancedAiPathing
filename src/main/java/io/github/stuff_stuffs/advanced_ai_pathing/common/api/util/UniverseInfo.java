package io.github.stuff_stuffs.advanced_ai_pathing.common.api.util;

public interface UniverseInfo<T> {
    T fromIndex(int index);

    int toIndex(T value);

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
