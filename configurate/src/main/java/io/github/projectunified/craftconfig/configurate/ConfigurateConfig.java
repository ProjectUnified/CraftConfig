package io.github.projectunified.craftconfig.configurate;

import io.github.projectunified.craftconfig.common.CommentType;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigLogger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static io.github.projectunified.craftconfig.common.PathString.asArray;
import static io.github.projectunified.craftconfig.common.PathString.concat;

/**
 * The {@link Config} implementation for Configurate
 */
public class ConfigurateConfig implements Config {
    private final File file;
    private final AbstractConfigurationLoader.Builder<?, ?> builder;
    private ConfigurationLoader<? extends ConfigurationNode> loader;
    private ConfigurationNode rootNode;

    /**
     * Create a new config
     *
     * @param file    the file
     * @param builder the config builder
     */
    public ConfigurateConfig(File file, AbstractConfigurationLoader.Builder<?, ?> builder) {
        this.file = file;
        this.builder = builder;
        this.rootNode = builder.build().createNode();
    }

    @Override
    public Object getOriginal() {
        return this.rootNode;
    }

    @Override
    public Object get(Object def, String... path) {
        ConfigurationNode node = this.rootNode.node((Object[]) path);
        Object value = node.raw();
        return value == null ? def : value;
    }

    @Override
    public void set(Object value, String... path) {
        ConfigurationNode node = this.rootNode.node((Object[]) path);
        try {
            node.set(value);
        } catch (SerializationException e) {
            ConfigLogger.warn(ConfigurateConfig.class, "Something wrong when setting " + Arrays.toString(path), e);
        }
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public Map<String[], Object> getValues(boolean deep, String... path) {
        ConfigurationNode node;
        if (path.length == 0) {
            node = this.rootNode;
        } else {
            node = this.rootNode.node((Object[]) path);
        }
        if (!node.isMap()) {
            return Collections.emptyMap();
        }
        Map<String[], Object> map = new LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String[] key = asArray(Objects.toString(entry.getKey()));
            ConfigurationNode value = entry.getValue();
            map.put(key, value.raw());
            if (value.isMap() && deep) {
                Map<String[], Object> subMap = getValues(true, concat(path, key));
                for (Map.Entry<String[], Object> subEntry : subMap.entrySet()) {
                    map.put(concat(key, subEntry.getKey()), subEntry.getValue());
                }
            }
        }
        return map;
    }

    @Override
    public void clear() {
        this.rootNode = this.loader.createNode();
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
    public Object normalize(Object object) {
        if (object instanceof ConfigurationNode) {
            ConfigurationNode node = (ConfigurationNode) object;
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
        return object;
    }

    @Override
    public boolean isNormalizable(Object object) {
        return object instanceof ConfigurationNode;
    }

    @Override
    public List<String> getComment(CommentType type, String... path) {
        ConfigurationNode node = this.rootNode.node((Object[]) path);
        if (!(node instanceof CommentedConfigurationNode)) return Collections.emptyList();
        CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) node;
        if (type != CommentType.BLOCK) return Collections.emptyList();
        String comment = commentedNode.comment();
        return comment == null || comment.isEmpty() ? Collections.emptyList() : Arrays.asList(comment.split("\\r?\\n"));
    }

    @Override
    public void setComment(CommentType type, List<String> value, String... path) {
        ConfigurationNode node = this.rootNode.node((Object[]) path);
        if (!(node instanceof CommentedConfigurationNode)) return;
        CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) node;
        if (type != CommentType.BLOCK) return;
        commentedNode.comment(value == null || value.isEmpty() ? null : String.join("\n", value));
    }
}
