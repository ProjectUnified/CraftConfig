package io.github.projectunified.craftconfig.bukkit.converter;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.common.ConfigLogger;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * The {@link Converter} for Bukkit's {@link ConfigurationSerializable}
 */
public class BukkitConverter implements Converter {
    private final Method deserializeMethod;

    public BukkitConverter(Class<?> type) {
        if (!ConfigurationSerializable.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("The class must implement ConfigurationSerializable");
        }

        try {
            deserializeMethod = type.getMethod("deserialize", Map.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Cannot find deserialize method", e);
        }
    }

    @Override
    public Object convert(Object raw) {
        if (raw instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) raw;
            try {
                return deserializeMethod.invoke(null, map);
            } catch (Exception e) {
                ConfigLogger.warn(BukkitConverter.class, "Error occurred while deserializing " + raw, e);
            }
        }
        return null;
    }

    @Override
    public Object convertToRaw(Object value) {
        if (value instanceof ConfigurationSerializable) {
            return ((ConfigurationSerializable) value).serialize();
        }
        return null;
    }
}
