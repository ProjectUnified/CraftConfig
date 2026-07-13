package io.github.projectunified.craftconfig.annotation.converter.impl;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.annotation.converter.ConverterProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A {@link Converter} to convert primitive types
 */
public class PrimitiveConverterProvider implements ConverterProvider {
    private static Class<?> getBoxedClass(Class<?> primitiveClass) {
        if (primitiveClass == boolean.class) {
            return Boolean.class;
        } else if (primitiveClass == byte.class) {
            return Byte.class;
        } else if (primitiveClass == short.class) {
            return Short.class;
        } else if (primitiveClass == int.class) {
            return Integer.class;
        } else if (primitiveClass == long.class) {
            return Long.class;
        } else if (primitiveClass == float.class) {
            return Float.class;
        } else if (primitiveClass == double.class) {
            return Double.class;
        } else if (primitiveClass == char.class) {
            return Character.class;
        }
        return null;
    }

    private static Function<String, Object> numericParser(Function<String, ?> parser) {
        return s -> {
            try {
                return parser.apply(s);
            } catch (Exception e) {
                return null;
            }
        };
    }

    @Override
    public Optional<Converter> getConverter(Class<?> type) {
        boolean isPrimitive = type.isPrimitive();
        Class<?> boxedClass = isPrimitive ? getBoxedClass(type) : type;

        if (boxedClass == null) {
            return Optional.empty();
        }

        Function<String, Object> converter;
        if (boxedClass == Boolean.class) {
            converter = Boolean::parseBoolean;
        } else if (boxedClass == Byte.class) {
            converter = numericParser(Byte::parseByte);
        } else if (boxedClass == Short.class) {
            converter = numericParser(Short::parseShort);
        } else if (boxedClass == Integer.class) {
            converter = numericParser(Integer::parseInt);
        } else if (boxedClass == Long.class) {
            converter = numericParser(Long::parseLong);
        } else if (boxedClass == Float.class) {
            converter = numericParser(Float::parseFloat);
        } else if (boxedClass == Double.class) {
            converter = numericParser(Double::parseDouble);
        } else if (boxedClass == Character.class) {
            converter = s -> {
                if (s.length() == 1) {
                    return s.charAt(0);
                } else {
                    return null;
                }
            };
        } else {
            return Optional.empty();
        }

        return Optional.of(new SimpleConverter(o -> {
            String string = Objects.toString(o, "");
            return converter.apply(string);
        }));
    }
}
