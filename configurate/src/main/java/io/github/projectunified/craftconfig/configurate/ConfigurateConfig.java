package io.github.projectunified.craftconfig.configurate;

import io.github.projectunified.craftconfig.common.CommentType;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigLogger;
import io.github.projectunified.craftconfig.common.ConfigNode;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The {@link Config} implementation for Configurate
 */
public class ConfigurateConfig implements Config {
    private final File file;
    private final AbstractConfigurationLoader.Builder<?, ?> builder;
    private ConfigurationLoader<? extends ConfigurationNode> loader;
    private ConfigurationNode rootNode;

    public ConfigurateConfig(File file, AbstractConfigurationLoader.Builder<?, ?> builder) {
        this.file = file;
        this.builder = builder;
        this.rootNode = builder.build().createNode();
    }

    private static Object normalizeNode(ConfigurationNode node) {
        if (node.isList()) {
            return node.childrenList();
        } else if (node.isMap()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.childrenMap().forEach((key, value) -> map.put(Objects.toString(key), value));
            return map;
        } else {
            return node.raw();
        }
    }

    @Override
    public Object get() {
        return this.rootNode;
    }

    @Override
    public void set(Object value) {
        if (value == null) {
            this.remove();
        } else {
            throw new IllegalArgumentException("You cannot set the whole configuration to a value");
        }
    }

    @Override
    public ConfigNode node(String... path) {
        if (path.length == 0) {
            return this;
        }
        return new ConfigurateConfigNode(path, this, this.rootNode);
    }

    @Override
    public void remove() {
        this.rootNode = this.loader.createNode();
    }

    @Override
    public Map<String, ConfigNode> getChildren() {
        if (!this.rootNode.isMap()) {
            return Collections.emptyMap();
        }
        Map<String, ConfigNode> nodes = new LinkedHashMap<>();
        for (Object key : this.rootNode.childrenMap().keySet()) {
            String childKey = Objects.toString(key);
            nodes.put(childKey, new ConfigurateConfigNode(new String[]{childKey}, this, this.rootNode));
        }
        return nodes;
    }

    @Override
    public List<String> getComment(CommentType type) {
        if (!(this.rootNode instanceof CommentedConfigurationNode)) return Collections.emptyList();
        CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) this.rootNode;
        if (type != CommentType.BLOCK) return Collections.emptyList();
        String comment = commentedNode.comment();
        return comment == null || comment.isEmpty() ? Collections.emptyList() : Arrays.asList(comment.split("\\r?\\n"));
    }

    @Override
    public void setComment(CommentType type, List<String> value) {
        if (!(this.rootNode instanceof CommentedConfigurationNode)) return;
        CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) this.rootNode;
        if (type != CommentType.BLOCK) return;
        commentedNode.comment(value == null || value.isEmpty() ? null : String.join("\n", value));
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public void setup() {
        if (!this.file.exists()) {
            File parentFile = this.file.getAbsoluteFile().getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            try {
                this.file.createNewFile();
            } catch (IOException e) {
                ConfigLogger.warn(ConfigurateConfig.class, "Something wrong when creating " + this.file.getName(), e);
            }
        }
        this.loader = this.builder.file(file).build();
        this.reload();
    }

    @Override
    public void save() {
        try {
            this.loader.save(this.rootNode);
        } catch (IOException e) {
            ConfigLogger.warn(ConfigurateConfig.class, "Something wrong when saving " + this.file.getName(), e);
        }
    }

    @Override
    public void reload() {
        if (this.file.length() == 0) {
            this.rootNode = this.loader.createNode();
            return;
        }
        try {
            this.rootNode = this.loader.load();
        } catch (IOException e) {
            ConfigLogger.warn(ConfigurateConfig.class, "Something wrong when loading " + this.file.getName(), e);
        }
    }

    @Override
    public Object getOriginal() {
        return this.rootNode;
    }

    @Override
    public Object normalize(Object object) {
        if (object instanceof ConfigurationNode) {
            return normalizeNode((ConfigurationNode) object);
        }
        return object;
    }

    @Override
    public boolean isNormalizable(Object object) {
        return object instanceof ConfigurationNode;
    }

    /**
     * Configurate implementation of {@link ConfigNode}.
     */
    public class ConfigurateConfigNode implements ConfigNode {
        private final String[] path;
        private final ConfigNode parent;
        private final ConfigurationNode nativeNode;

        ConfigurateConfigNode(String[] path, ConfigNode parent, ConfigurationNode nativeNode) {
            this.path = path;
            this.parent = parent;
            this.nativeNode = nativeNode;
        }

        @Override
        public String[] getPath() {
            return path;
        }

        @Override
        public ConfigNode getParent() {
            return parent;
        }

        @Override
        public Config getConfig() {
            return ConfigurateConfig.this;
        }

        @Override
        public Object get() {
            return nativeNode.node((Object[]) path).raw();
        }

        @Override
        public void set(Object value) {
            try {
                nativeNode.node((Object[]) path).set(value);
            } catch (SerializationException e) {
                ConfigLogger.warn(ConfigurateConfig.class, "Something wrong when setting " + Arrays.toString(getPath()), e);
            }
        }

        @Override
        public ConfigNode node(String... path) {
            if (path.length == 0) {
                return this;
            }
            return new ConfigurateConfigNode(path, this, this.nativeNode);
        }

        @Override
        public void remove() {
            try {
                nativeNode.node((Object[]) path).set(null);
            } catch (SerializationException e) {
                ConfigLogger.warn(ConfigurateConfig.class, "Something wrong when removing " + Arrays.toString(getPath()), e);
            }
        }

        @Override
        public boolean hasChild() {
            ConfigurationNode node = nativeNode.node((Object[]) path);
            return node.isMap() || node.isList();
        }

        @Override
        public Map<String, ConfigNode> getChildren() {
            ConfigurationNode node = nativeNode.node((Object[]) path);
            if (!node.isMap()) {
                throw new IllegalStateException("The node is not a map");
            }
            Map<String, ConfigNode> nodes = new LinkedHashMap<>();
            for (Object key : node.childrenMap().keySet()) {
                String childKey = Objects.toString(key);
                nodes.put(childKey, new ConfigurateConfigNode(new String[]{childKey}, this, node));
            }
            return nodes;
        }

        @Override
        public Object getNormalized() {
            ConfigurationNode node = nativeNode.node((Object[]) path);
            return normalizeNode(node);
        }

        @Override
        public List<String> getComment(CommentType type) {
            ConfigurationNode node = nativeNode.node((Object[]) path);
            if (!(node instanceof CommentedConfigurationNode)) return Collections.emptyList();
            CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) node;
            if (type != CommentType.BLOCK) return Collections.emptyList();
            String comment = commentedNode.comment();
            return comment == null || comment.isEmpty() ? Collections.emptyList() : Arrays.asList(comment.split("\\r?\\n"));
        }

        @Override
        public void setComment(CommentType type, List<String> value) {
            ConfigurationNode node = nativeNode.node((Object[]) path);
            if (!(node instanceof CommentedConfigurationNode)) return;
            CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) node;
            if (type != CommentType.BLOCK) return;
            commentedNode.comment(value == null || value.isEmpty() ? null : String.join("\n", value));
        }
    }
}
