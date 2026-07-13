package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.annotation.StickyValue;
import io.github.projectunified.craftconfig.common.CommentType;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigGeneratorTest {

    private SimpleMapConfig config;

    @BeforeEach
    void setUp() {
        config = new SimpleMapConfig();
    }

    @Test
    void newInstanceReturnsProxyOfCorrectType() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertNotNull(proxy);
        assertTrue(proxy instanceof TestConfigProxy);
    }

    @Test
    void getterReturnsDefaultValueWhenConfigIsEmpty() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertEquals("default", proxy.getName());
        assertEquals(42, proxy.getPort());
        assertTrue(proxy.isEnabled());
    }

    @Test
    void getterReturnsConfigValueWhenSet() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        config.node("name").set("custom");
        config.node("port").set(8080);
        config.node("enabled").set(false);

        assertEquals("custom", proxy.getName());
        assertEquals(8080, proxy.getPort());
        assertFalse(proxy.isEnabled());
    }

    @Test
    void isGetterReadsFromCorrectPath() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        config.node("debug").set(true);
        assertTrue(proxy.isDebug());
        config.node("debug").set(false);
        assertFalse(proxy.isDebug());
    }

    @Test
    void setterSetsValueAndSaves() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        proxy.setName("newValue");
        assertEquals("newValue", config.node("name").get());
        assertTrue(config.saved);
    }

    @Test
    void setterClearsCache() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config, true, true);
        proxy.getName();
        config.node("name").set("cached");
        proxy.setName("newValue");
        assertEquals("newValue", proxy.getName());
    }

    @Test
    void getConfigReturnsUnderlyingConfig() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        Config returnedConfig = proxy.getConfig();
        assertSame(config, returnedConfig);
    }

    @Test
    void reloadConfigReloadsAndClearsCaches() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config, true, true);
        proxy.getName();
        config.node("name").set("reloaded");
        config.reloaded = false;
        proxy.reloadConfig();
        assertTrue(config.reloaded);
        assertEquals("reloaded", proxy.getName());
    }

    @Test
    void toStringReturnsClassName() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertEquals(TestConfigProxy.class.toString(), proxy.toString());
    }

    @Test
    void hashCodeReturnsClassHashCode() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertEquals(TestConfigProxy.class.hashCode(), proxy.hashCode());
    }

    @Test
    void equalsReturnsTrueForSameProxy() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertTrue(proxy.equals(proxy));
    }

    @Test
    void equalsReturnsFalseForDifferentProxy() {
        TestConfigProxy proxy1 = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        TestConfigProxy proxy2 = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertFalse(proxy1.equals(proxy2));
    }

    @Test
    void addDefaultSetsDefaultsInConfig() {
        ConfigGenerator.newInstance(TestConfigProxy.class, config, true, false, true);
        assertEquals("default", config.node("name").get());
        assertEquals(42, config.node("port").get());
        assertEquals(true, config.node("enabled").get());
    }

    @Test
    void commentAnnotationSetsCommentOnFirstSetup() {
        ConfigGenerator.newInstance(CommentedConfig.class, config, true, false, true);
        List<String> comment = config.node("name").getComment();
        assertEquals(Arrays.asList("This is the name"), comment);
    }

    @Test
    void nestedPathWorksCorrectly() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config, true, false, true);
        assertEquals("nestedDefault", proxy.getNestedValue());
        config.node("server", "name").set("customNested");
        assertEquals("customNested", proxy.getNestedValue());
    }

    @Test
    void defaultVoidMethodWithNonConfigPathExecutesDefaultBody() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        assertDoesNotThrow(proxy::unknownMethod);
    }

    @Test
    void stickyValueReturnsCachedValue() {
        StickyConfig proxy = ConfigGenerator.newInstance(StickyConfig.class, config, true, true, true);
        assertEquals("sticky", proxy.getStickyValue());
        config.node("value").set("changed");
        assertEquals("sticky", proxy.getStickyValue());
    }

    @Test
    void nonStickyValueReturnsLatestValue() {
        NonStickyConfig proxy = ConfigGenerator.newInstance(NonStickyConfig.class, config, true, false, true);
        assertEquals("nonSticky", proxy.getNonStickyValue());
        config.node("value").set("changed");
        assertEquals("changed", proxy.getNonStickyValue());
    }

    @Test
    void reloadClearsStickyCache() {
        StickyConfig proxy = ConfigGenerator.newInstance(StickyConfig.class, config, true, true, true);
        assertEquals("sticky", proxy.getStickyValue());
        config.node("value").set("changed");
        proxy.reloadConfig();
        assertEquals("changed", proxy.getStickyValue());
    }

    @Test
    void setupConfigIsCalledWhenTrue() {
        ConfigGenerator.newInstance(TestConfigProxy.class, config, true);
        assertTrue(config.setupCalled);
    }

    @Test
    void setupConfigIsNotCalledWhenFalse() {
        ConfigGenerator.newInstance(TestConfigProxy.class, config, false);
        assertFalse(config.setupCalled);
    }

    public interface TestConfigProxy {
        @ConfigPath({"name"})
        default String getName() {
            return "default";
        }

        void setName(String name);

        @ConfigPath({"port"})
        default int getPort() {
            return 42;
        }

        @ConfigPath({"enabled"})
        default boolean isEnabled() {
            return true;
        }

        @ConfigPath({"debug"})
        default boolean isDebug() {
            return false;
        }

        @ConfigPath({"server", "name"})
        default String getNestedValue() {
            return "nestedDefault";
        }

        Config getConfig();

        void reloadConfig();

        default void unknownMethod() {
        }
    }

    public interface CommentedConfig {
        @ConfigPath({"name"})
        @Comment("This is the name")
        default String getName() {
            return "default";
        }

        void setName(String name);
    }

    public interface StickyConfig {
        @ConfigPath({"value"})
        @StickyValue
        default String getStickyValue() {
            return "sticky";
        }

        void setStickyValue(String value);

        void reloadConfig();
    }

    public interface NonStickyConfig {
        @ConfigPath({"value"})
        default String getNonStickyValue() {
            return "nonSticky";
        }

        void setNonStickyValue(String value);
    }

    public static class SimpleMapConfig implements Config {
        private final Map<String, Object> data = new HashMap<>();
        private final Map<String, List<String>> comments = new HashMap<>();
        boolean saved = false;
        boolean reloaded = false;
        boolean setupCalled = false;
        private List<String> classComment = Collections.emptyList();

        private static String joinPath(String... path) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.length; i++) {
                if (i > 0) sb.append('\0');
                sb.append(path[i]);
            }
            return sb.toString();
        }

        @Override
        public Object getOriginal() {
            return data;
        }

        @Override
        public String getName() {
            return "test-config";
        }

        @Override
        public ConfigNode node(String... path) {
            return new SimpleConfigNode(this, path);
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public void set(Object value) {
        }

        @Override
        public void remove() {
            data.clear();
        }

        @Override
        public Map<String, ConfigNode> getChildren() {
            Map<String, ConfigNode> result = new LinkedHashMap<>();
            for (String key : data.keySet()) {
                String[] parts = key.split("\0", -1);
                result.put(parts[parts.length - 1], new SimpleConfigNode(this, parts));
            }
            return result;
        }

        @Override
        public void setup() {
            setupCalled = true;
        }

        @Override
        public void save() {
            saved = true;
        }

        @Override
        public void reload() {
            reloaded = true;
        }

        @Override
        public Object normalize(Object object) {
            return object;
        }

        @Override
        public boolean isNormalizable(Object object) {
            return false;
        }

        @Override
        public List<String> getComment(CommentType type) {
            return type == CommentType.BLOCK ? classComment : Collections.emptyList();
        }

        @Override
        public void setComment(CommentType type, List<String> value) {
            if (type == CommentType.BLOCK) {
                classComment = value != null ? value : Collections.emptyList();
            }
        }

        private static class SimpleConfigNode implements ConfigNode {
            private final SimpleMapConfig config;
            private final String[] path;
            private final ConfigNode parent;

            SimpleConfigNode(SimpleMapConfig config, String[] path) {
                this(config, path, null);
            }

            SimpleConfigNode(SimpleMapConfig config, String[] path, ConfigNode parent) {
                this.config = config;
                this.path = path;
                this.parent = parent;
            }

            private static String joinPath(String... path) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < path.length; i++) {
                    if (i > 0) sb.append('\0');
                    sb.append(path[i]);
                }
                return sb.toString();
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
                return config;
            }

            @Override
            public Object get() {
                return config.data.getOrDefault(joinPath(path), null);
            }

            @Override
            public void set(Object value) {
                String key = joinPath(path);
                if (value == null) {
                    config.data.remove(key);
                } else {
                    config.data.put(key, value);
                }
            }

            @Override
            public ConfigNode node(String... childPath) {
                String[] fullPath = new String[path.length + childPath.length];
                System.arraycopy(path, 0, fullPath, 0, path.length);
                System.arraycopy(childPath, 0, fullPath, path.length, childPath.length);
                return new SimpleConfigNode(config, fullPath, this);
            }

            @Override
            public boolean exists() {
                return config.data.containsKey(joinPath(path));
            }

            @Override
            public void remove() {
                config.data.remove(joinPath(path));
            }

            @Override
            public boolean hasChild() {
                return !getChildren().isEmpty();
            }

            @Override
            public Map<String, ConfigNode> getChildren() {
                Map<String, ConfigNode> result = new LinkedHashMap<>();
                String prefix = joinPath(path);
                for (String key : config.data.keySet()) {
                    if (key.startsWith(prefix) && !key.equals(prefix)) {
                        String[] parts = key.split("\0", -1);
                        if (parts.length == path.length + 1) {
                            result.put(parts[parts.length - 1], new SimpleConfigNode(config, parts));
                        }
                    }
                }
                return result;
            }

            @Override
            public Object getNormalized() {
                return get();
            }

            @Override
            public List<String> getComment(CommentType type) {
                return config.comments.getOrDefault(joinPath(path), Collections.emptyList());
            }

            @Override
            public void setComment(CommentType type, List<String> value) {
                if (path.length == 0) {
                    config.setComment(type, value);
                } else {
                    if (value != null) {
                        config.comments.put(joinPath(path), value);
                    }
                }
            }
        }
    }
}
