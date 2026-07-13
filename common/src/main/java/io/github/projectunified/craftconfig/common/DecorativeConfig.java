package io.github.projectunified.craftconfig.common;

import java.util.Map;

/**
 * A decorative {@link Config} for extending more features on the existing {@link Config}
 */
public abstract class DecorativeConfig implements Config {
    protected final Config config;

    /**
     * Create a new decorative config
     *
     * @param config the original config
     */
    protected DecorativeConfig(Config config) {
        this.config = config;
    }

    /**
     * Get the original config
     *
     * @return the config
     */
    public Config getOriginalConfig() {
        return this.config;
    }

    @Override
    public Object getOriginal() {
        return config.getOriginal();
    }

    @Override
    public Object get(Object def, String... path) {
        return config.get(def, path);
    }

    @Override
    public void set(Object value, String... path) {
        config.set(value, path);
    }

    @Override
    public boolean contains(String... path) {
        return config.contains(path);
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public Map<String[], Object> getValues(boolean deep, String... path) {
        return config.getValues(deep, path);
    }

    @Override
    public void setup() {
        config.setup();
    }

    @Override
    public void save() {
        config.save();
    }

    @Override
    public void reload() {
        config.reload();
    }

    @Override
    public Object normalize(Object object) {
        return config.normalize(object);
    }

    @Override
    public boolean isNormalizable(Object object) {
        return config.isNormalizable(object);
    }

    @Override
    public void remove(String... path) {
        config.remove(path);
    }
}
