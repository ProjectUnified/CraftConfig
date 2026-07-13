package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.common.Config;

import java.lang.reflect.Proxy;

/**
 * The main class of the config proxy system.
 * Use this class to create a proxied interface with a config.
 */
public final class ConfigGenerator {
    private ConfigGenerator() {
    }

    /**
     * Create a new mapped instance of the class from the config
     *
     * @param clazz       The class to create
     * @param config      The config to use
     * @param setupConfig Whether to set up the config
     * @param stickyValue True if the value should be sticky (keep the value in the cache)
     * @param addDefault  True if the default value should be added to the config
     * @param <T>         The class type
     * @return The new instance
     */
    public static <T> T newInstance(Class<T> clazz, Config config, boolean setupConfig, boolean stickyValue, boolean addDefault) {
        if (!clazz.isAnnotationPresent(ConfigNode.class)) {
            throw new IllegalArgumentException("The class " + clazz.getName() + " must be annotated with @ConfigNode");
        }
        if (setupConfig) {
            config.setup();
        }
        return clazz.cast(
                Proxy.newProxyInstance(
                        clazz.getClassLoader(),
                        new Class[]{clazz},
                        new ConfigInvocationHandler<>(clazz, config, stickyValue, addDefault)));
    }

    /**
     * Create a new mapped instance of the class from the config
     *
     * @param clazz       The class to create
     * @param config      The config to use
     * @param setupConfig Whether to set up the config
     * @param stickyValue True if the value should be sticky (keep the value in the cache)
     * @param <T>         The class type
     * @return The new instance
     */
    public static <T> T newInstance(Class<T> clazz, Config config, boolean setupConfig, boolean stickyValue) {
        return newInstance(clazz, config, setupConfig, stickyValue, true);
    }

    /**
     * Create a new mapped instance of the class from the config
     *
     * @param clazz       The class to create
     * @param config      The config to use
     * @param setupConfig Whether to set up the config
     * @param <T>         The class type
     * @return The new instance
     */
    public static <T> T newInstance(Class<T> clazz, Config config, boolean setupConfig) {
        return newInstance(clazz, config, setupConfig, false);
    }

    /**
     * Create a new mapped instance of the class from the config, also set up the config
     *
     * @param clazz  The class to create
     * @param config The config to use
     * @param <T>    The class type
     * @return The new instance
     */
    public static <T> T newInstance(Class<T> clazz, Config config) {
        return newInstance(clazz, config, true);
    }
}
