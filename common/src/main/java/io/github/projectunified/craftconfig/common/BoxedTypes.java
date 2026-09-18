package io.github.projectunified.craftconfig.common;

/**
 * Helpers to deal with primitive types, which cannot be used with
 * {@link Class#isInstance(Object)} and {@link Class#cast(Object)}.
 */
final class BoxedTypes {
    private BoxedTypes() {
    }

    /**
     * Get the wrapper class of a primitive type, or the type itself if it is not primitive.
     *
     * @param type the type class
     * @return the wrapper class of the primitive type
     */
    static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        if (type == void.class) return Void.class;
        return type;
    }
}
