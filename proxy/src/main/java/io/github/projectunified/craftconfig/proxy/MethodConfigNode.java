package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigNode;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A config node mapped to a method in a proxy interface.
 * Handles conversion, caching, and default values.
 */
public class MethodConfigNode {
    private final String[] path;
    private final Config config;
    private final ConfigNode node;
    private final Converter converter;
    private final Object defaultValue;
    private final List<String> comment;
    private final boolean stickyValue;
    private final AtomicReference<Object> cachedValue = new AtomicReference<>();

    MethodConfigNode(String[] path, Config config, Converter converter, Object defaultValue, List<String> comment, boolean stickyValue) {
        this.path = path;
        this.config = config;
        this.node = config.node(path);
        this.converter = converter;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.stickyValue = stickyValue;
    }

    public String[] getPath() {
        return path;
    }

    public void addDefault() {
        if (!node.exists()) {
            node.set(converter.convertToRaw(defaultValue));
        }
        if (!comment.isEmpty() && node.getComment().isEmpty()) {
            node.setComment(comment);
        }
    }

    public Object getValue() {
        Object cached = cachedValue.get();
        if (cached != null && stickyValue) {
            return cached;
        }

        Object rawValue = node.getNormalized();
        if (rawValue == null) {
            return defaultValue;
        }
        Object value = converter.convert(rawValue);
        Object finalValue = value == null ? defaultValue : value;
        if (stickyValue) {
            cachedValue.set(finalValue);
        }
        return finalValue;
    }

    public void setValue(Object value) {
        node.set(converter.convertToRaw(value));
        this.cachedValue.set(null);
    }

    public void clearCache() {
        cachedValue.set(null);
    }
}
