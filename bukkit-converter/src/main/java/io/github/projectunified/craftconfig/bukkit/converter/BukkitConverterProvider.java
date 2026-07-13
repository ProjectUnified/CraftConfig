package io.github.projectunified.craftconfig.bukkit.converter;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.annotation.converter.ConverterProvider;
import io.github.projectunified.craftconfig.annotation.converter.manager.DefaultConverterManager;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Optional;

/**
 * The {@link ConverterProvider} for Bukkit
 */
public class BukkitConverterProvider implements ConverterProvider {
    /**
     * Register the provider
     */
    public static void register() {
        DefaultConverterManager.registerProvider(new BukkitConverterProvider());
    }

    @Override
    public Optional<Converter> getConverter(Class<?> type) {
        return ConfigurationSerializable.class.isAssignableFrom(type) ? Optional.of(new BukkitConverter(type)) : Optional.empty();
    }
}
