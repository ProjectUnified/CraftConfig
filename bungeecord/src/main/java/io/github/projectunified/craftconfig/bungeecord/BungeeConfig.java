package io.github.projectunified.craftconfig.bungeecord;

import io.github.projectunified.craftconfig.common.CommentType;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigLogger;
import io.github.projectunified.craftconfig.common.ConfigNode;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.projectunified.craftconfig.common.PathString;
import static io.github.projectunified.craftconfig.common.PathString.joinDefault;

/**
 * The bungeecord configuration
 */
public class BungeeConfig implements Config {
    private final File file;
    private Configuration configuration = new Configuration();

    public BungeeConfig(File file) {
        this.file = file;
    }

    public BungeeConfig(Plugin plugin, String filename) {
        this(new File(plugin.getDataFolder(), filename));
    }

    @Override
    public Configuration getOriginal() {
        return this.configuration;
    }

    @Override
    public String getName() {
        return this.file.getName();
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
                ConfigLogger.warn(BungeeConfig.class, "Something wrong when creating " + this.file.getName(), e);
            }
        }
        try {
            this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        } catch (IOException e) {
            ConfigLogger.warn(BungeeConfig.class, "Something wrong when loading " + this.file.getName(), e);
        }
    }

    @Override
    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(this.configuration, this.file);
        } catch (IOException e) {
            ConfigLogger.warn(BungeeConfig.class, "Something wrong when saving " + this.file.getName(), e);
        }
    }

    @Override
    public void reload() {
        this.setup();
    }

    @Override
    public Object normalize(Object object) {
        if (object instanceof Configuration) {
            return ((Configuration) object).getKeys().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            key -> key,
                            key -> ((Configuration) object).get(key),
                            (a, b) -> b,
                            LinkedHashMap::new
                    ));
        }
        return object;
    }

    @Override
    public boolean isNormalizable(Object object) {
        return object instanceof Configuration;
    }

    @Override
    public Object get() {
        return this.configuration;
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
        return new BungeeConfigNode(path, this, path);
    }

    @Override
    public void remove() {
        this.configuration = new Configuration();
    }

    @Override
    public Map<String, ConfigNode> getChildren() {
        Map<String, ConfigNode> nodes = new LinkedHashMap<>();
        for (String key : this.configuration.getKeys()) {
            nodes.put(key, new BungeeConfigNode(new String[]{key}, this, new String[]{key}));
        }
        return nodes;
    }

    @Override
    public List<String> getComment(CommentType type) {
        return Collections.emptyList();
    }

    @Override
    public void setComment(CommentType type, List<String> value) {
    }

    /**
     * BungeeCord implementation of {@link ConfigNode}.
     */
    public class BungeeConfigNode implements ConfigNode {
        private final String[] path;
        private final ConfigNode parent;
        private final String[] absolutePath;

        BungeeConfigNode(String[] path, ConfigNode parent, String[] absolutePath) {
            this.path = path;
            this.parent = parent;
            this.absolutePath = absolutePath;
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
            return BungeeConfig.this;
        }

        @Override
        public Object get() {
            return BungeeConfig.this.configuration.get(joinDefault(absolutePath));
        }

        @Override
        public void set(Object value) {
            BungeeConfig.this.configuration.set(joinDefault(absolutePath), value);
        }

        @Override
        public ConfigNode node(String... path) {
            if (path.length == 0) {
                return this;
            }
            String[] childAbsolutePath = PathString.concat(this.absolutePath, path);
            return new BungeeConfigNode(path, this, childAbsolutePath);
        }

        @Override
        public void remove() {
            BungeeConfig.this.configuration.set(joinDefault(absolutePath), null);
        }

        @Override
        public boolean hasChild() {
            return BungeeConfig.this.configuration.getSection(joinDefault(absolutePath)) != null;
        }

        @Override
        public Map<String, ConfigNode> getChildren() {
            Map<String, ConfigNode> nodes = new LinkedHashMap<>();
            Configuration currentSection = BungeeConfig.this.configuration.getSection(joinDefault(absolutePath));
            if (currentSection == null) {
                return nodes;
            }
            for (String key : currentSection.getKeys()) {
                String[] childAbsolutePath = PathString.concat(this.absolutePath, new String[]{key});
                nodes.put(key, new BungeeConfigNode(new String[]{key}, this, childAbsolutePath));
            }
            return nodes;
        }

        @Override
        public Object getNormalized() {
            Object value = BungeeConfig.this.configuration.get(joinDefault(absolutePath));
            return BungeeConfig.this.normalize(value);
        }

        @Override
        public List<String> getComment(CommentType type) {
            return Collections.emptyList();
        }

        @Override
        public void setComment(CommentType type, List<String> value) {
        }
    }
}
