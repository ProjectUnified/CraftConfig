package io.github.projectunified.craftconfig.common;

import java.util.*;

/**
 * A node in the configuration tree.
 * Provides access to values, child nodes, and comments.
 */
public interface ConfigNode {

    /**
     * Get the path relative to the origin node.
     * When obtained from Config.node(), returns the full path from root.
     * When obtained from ConfigNode.node() or getChildren(), returns the relative path.
     *
     * @return the path segments
     */
    String[] getPath();

    /**
     * Get the parent node (the origin).
     * When obtained from Config.node(), returns the Config (root node).
     * When obtained from ConfigNode.node() or getChildren(), returns that ConfigNode.
     *
     * @return the parent node
     */
    ConfigNode getParent();

    /**
     * Get the root Config that owns this node
     *
     * @return the owning Config
     */
    Config getConfig();

    /**
     * Get the value at this node
     *
     * @return the value, or null if not present
     */
    Object get();

    /**
     * Get the value at this node, returning a default if absent
     *
     * @param def the default value
     * @return the value, or the default if not present
     */
    default Object get(Object def) {
        Object value = get();
        return value != null ? value : def;
    }

    /**
     * Get a typed value at this node
     *
     * @param type the type class
     * @param <T>  the type
     * @return the typed value, or null if not present or not assignable
     */
    default <T> T get(Class<T> type) {
        return get(type, null);
    }

    /**
     * Get a typed value at this node with a default
     *
     * @param type the type class
     * @param def  the default value
     * @param <T>  the type
     * @return the typed value, or the default if not present or not assignable
     */
    default <T> T get(Class<T> type, T def) {
        Object value = getNormalized();
        if (value == null) {
            return def;
        }
        if (type == String.class) {
            return type.cast(String.valueOf(value));
        }
        return type.isInstance(value) ? type.cast(value) : def;
    }

    /**
     * Set the value at this node
     *
     * @param value the value to set
     */
    void set(Object value);

    /**
     * Set the value at this node if it does not already exist
     *
     * @param value the value to set
     */
    default void setIfAbsent(Object value) {
        if (!exists()) {
            set(value);
        }
    }

    /**
     * Navigate to a child node.
     * Should only be called if {@link #hasChild()} returns true.
     *
     * @param path the path segments (relative to this node)
     * @return the child node
     */
    ConfigNode node(String... path);

    /**
     * Check if this node exists in the configuration
     *
     * @return true if this node has a value
     */
    default boolean exists() {
        return get() != null;
    }

    /**
     * Remove this node from the configuration
     */
    void remove();

    /**
     * Check if this node has children
     *
     * @return true if this node has child nodes
     */
    boolean hasChild();

    /**
     * Get the child nodes of this node.
     * Should only be called if {@link #hasChild()} returns true.
     *
     * @return map of child key to child node
     */
    Map<String, ConfigNode> getChildren();

    /**
     * Get the normalized value at this node (plain Java types).
     * Recursively normalizes Maps and Collections.
     *
     * @return the normalized value, or null if not present
     */
    default Object getNormalized() {
        Object value = get();
        return normalizeObject(value);
    }

    /**
     * Normalize an object and its elements if it is a Map or Collection.
     * Uses the owning Config's normalize/isNormalizable methods.
     *
     * @param object the object
     * @return the normalized object
     */
    default Object normalizeObject(Object object) {
        if (object == null) {
            return null;
        }
        Config config = getConfig();
        Object normalized = config.isNormalizable(object) ? config.normalize(object) : object;
        if (normalized instanceof Map) {
            Map<Object, Object> normalizedMap = new LinkedHashMap<>();
            ((Map<?, ?>) normalized).forEach((k, v) -> normalizedMap.put(k, normalizeObject(v)));
            return normalizedMap;
        } else if (normalized instanceof Collection) {
            List<Object> normalizedList = new ArrayList<>();
            ((Collection<?>) normalized).forEach(v -> normalizedList.add(normalizeObject(v)));
            return normalizedList;
        }
        return normalized;
    }

    /**
     * Get the block comment for this node
     *
     * @return the comment lines, or empty list if none
     */
    default List<String> getComment() {
        return getComment(CommentType.BLOCK);
    }

    /**
     * Set the block comment for this node
     *
     * @param value the comment lines, or null to remove
     */
    default void setComment(List<String> value) {
        setComment(CommentType.BLOCK, value);
    }

    /**
     * Get the comment for this node
     *
     * @param type the comment type
     * @return the comment lines, or empty list if none
     */
    default List<String> getComment(CommentType type) {
        return Collections.emptyList();
    }

    /**
     * Set the comment for this node
     *
     * @param type  the comment type
     * @param value the comment lines, or null to remove
     */
    default void setComment(CommentType type, List<String> value) {
    }
}
