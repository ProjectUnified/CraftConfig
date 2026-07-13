package io.github.projectunified.craftconfig.bukkit;

import io.github.projectunified.craftconfig.common.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The bukkit configuration
 */
public class BukkitConfig implements Config {
    private static final boolean isCommentSupported;

    static {
        boolean supported;
        try {
            //noinspection ConstantValue
            supported = ConfigurationSection.class.getDeclaredMethod("getComments", String.class) != null;
        } catch (NoSuchMethodException e) {
            supported = false;
        }
        isCommentSupported = supported;
    }

    private final File file;
    private final YamlConfiguration configuration = new YamlConfiguration();

    /**
     * Create a new config
     *
     * @param file the file
     */
    public BukkitConfig(File file) {
        this.file = file;
    }

    /**
     * Create a new config
     *
     * @param plugin   the plugin
     * @param filename the file name
     */
    public BukkitConfig(Plugin plugin, String filename) {
        this(new File(plugin.getDataFolder(), filename));
        try {
            plugin.saveResource(filename, false);
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Create a new config, the file name will be "config.yml"
     *
     * @param plugin the plugin
     */
    public BukkitConfig(Plugin plugin) {
        this(plugin, "config.yml");
    }

    @Override
    public YamlConfiguration getOriginal() {
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
                ConfigLogger.warn(BukkitConfig.class, "Something wrong when creating " + this.file.getName(), e);
            }
        }
        try {
            this.configuration.load(this.file);
        } catch (IOException | InvalidConfigurationException e) {
            ConfigLogger.warn(BukkitConfig.class, "Something wrong when loading " + this.file.getName(), e);
        }
    }

    @Override
    public void save() {
        try {
            this.configuration.save(this.file);
        } catch (IOException e) {
            ConfigLogger.warn(BukkitConfig.class, "Something wrong when saving " + this.file.getName(), e);
        }
    }

    @Override
    public void reload() {
        setup();
    }

    @Override
    public Object normalize(Object object) {
        if (object instanceof ConfigurationSection) {
            return ((ConfigurationSection) object).getValues(false);
        }
        return object;
    }

    @Override
    public boolean isNormalizable(Object object) {
        return object instanceof ConfigurationSection;
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
        return new BukkitConfigNode(path, this, path);
    }

    @Override
    public void remove() {
        for (String key : this.configuration.getKeys(false)) {
            this.configuration.set(key, null);
        }
    }

    @Override
    public Map<String, ConfigNode> getChildren() {
        Map<String, ConfigNode> nodes = new LinkedHashMap<>();
        for (String key : this.configuration.getKeys(false)) {
            nodes.put(key, new BukkitConfigNode(new String[]{key}, this, new String[]{key}));
        }
        return nodes;
    }

    @Override
    public List<String> getComment(CommentType type) {
        String header = this.configuration.options().header();
        return header.isEmpty() ? Collections.emptyList() : Arrays.asList(header.split("\\r?\\n"));
    }

    @Override
    public void setComment(CommentType type, List<String> value) {
        this.configuration.options()
                .copyHeader(true)
                .header(value == null || value.isEmpty() ? null : String.join(System.lineSeparator(), value));
    }

    private String getPath(String[] path) {
        return PathString.join(String.valueOf(configuration.options().pathSeparator()), path);
    }

    /**
     * Bukkit implementation of {@link ConfigNode}.
     */
    public class BukkitConfigNode implements ConfigNode {
        private final String[] path;
        private final ConfigNode parent;
        private final String[] absolutePath;

        BukkitConfigNode(String[] path, ConfigNode parent, String[] absolutePath) {
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
            return BukkitConfig.this;
        }

        @Override
        public Object get() {
            return BukkitConfig.this.configuration.get(BukkitConfig.this.getPath(absolutePath));
        }

        @Override
        public void set(Object value) {
            BukkitConfig.this.configuration.set(BukkitConfig.this.getPath(absolutePath), value);
        }

        @Override
        public ConfigNode node(String... path) {
            if (path.length == 0) {
                return this;
            }
            String[] childAbsolutePath = PathString.concat(this.absolutePath, path);
            return new BukkitConfigNode(path, this, childAbsolutePath);
        }

        @Override
        public void remove() {
            BukkitConfig.this.configuration.set(BukkitConfig.this.getPath(absolutePath), null);
        }

        @Override
        public boolean hasChild() {
            return this.get() instanceof ConfigurationSection;
        }

        @Override
        public Map<String, ConfigNode> getChildren() {
            Map<String, ConfigNode> nodes = new LinkedHashMap<>();
            ConfigurationSection section = BukkitConfig.this.configuration.getConfigurationSection(BukkitConfig.this.getPath(absolutePath));
            if (section == null) {
                return nodes;
            }
            for (String key : section.getKeys(false)) {
                String[] childAbsolutePath = PathString.concat(this.absolutePath, new String[]{key});
                nodes.put(key, new BukkitConfigNode(new String[]{key}, this, childAbsolutePath));
            }
            return nodes;
        }

        @Override
        public List<String> getComment(CommentType type) {
            if (!isCommentSupported) return Collections.emptyList();
            String joined = BukkitConfig.this.getPath(absolutePath);
            switch (type) {
                case BLOCK:
                    return BukkitConfig.this.configuration.getComments(joined);
                case SIDE:
                    return BukkitConfig.this.configuration.getInlineComments(joined);
                default:
                    return Collections.emptyList();
            }
        }

        @Override
        public void setComment(CommentType type, List<String> value) {
            if (!isCommentSupported) return;
            String joined = BukkitConfig.this.getPath(absolutePath);
            switch (type) {
                case BLOCK:
                    BukkitConfig.this.configuration.setComments(joined, value);
                    break;
                case SIDE:
                    BukkitConfig.this.configuration.setInlineComments(joined, value);
                    break;
                default:
                    break;
            }
        }
    }
}
