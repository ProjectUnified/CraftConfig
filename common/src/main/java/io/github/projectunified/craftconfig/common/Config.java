package io.github.projectunified.craftconfig.common;

/**
 * The root configuration interface, extending {@link ConfigNode}.
 * Provides file-level operations beyond the node-based access.
 */
public interface Config extends ConfigNode {

    @Override
    default String[] getPath() {
        return new String[0];
    }

    @Override
    default ConfigNode getParent() {
        return null;
    }

    @Override
    default Config getConfig() {
        return this;
    }

    @Override
    default boolean hasChild() {
        return true;
    }

    /**
     * Get the name of the configuration (typically the file name)
     *
     * @return the name
     */
    String getName();

    /**
     * Set up the configuration (create file, load initial data)
     */
    void setup();

    /**
     * Save the configuration to disk
     */
    void save();

    /**
     * Reload the configuration from disk
     */
    void reload();

    /**
     * Get the original underlying object (e.g., JsonObject, ConfigurationNode)
     *
     * @return the original instance
     */
    Object getOriginal();

    /**
     * Normalize a library-specific object to plain Java types
     *
     * @param object the object to normalize
     * @return the normalized object
     */
    Object normalize(Object object);

    /**
     * Check if the object can be normalized
     *
     * @param object the object
     * @return true if it is normalizable
     */
    boolean isNormalizable(Object object);
}
