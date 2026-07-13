package io.github.projectunified.craftconfig.proxy;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.annotation.StickyValue;
import io.github.projectunified.craftconfig.common.CommentType;
import io.github.projectunified.craftconfig.common.Config;
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
        config.set("custom", "name");
        config.set(8080, "port");
        config.set(false, "enabled");

        assertEquals("custom", proxy.getName());
        assertEquals(8080, proxy.getPort());
        assertFalse(proxy.isEnabled());
    }

    @Test
    void isGetterReadsFromCorrectPath() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        config.set(true, "debug");
        assertTrue(proxy.isDebug());
        config.set(false, "debug");
        assertFalse(proxy.isDebug());
    }

    @Test
    void setterSetsValueAndSaves() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config);
        proxy.setName("newValue");
        assertEquals("newValue", config.get("name"));
        assertTrue(config.saved);
    }

    @Test
    void setterClearsCache() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config, true, true);
        proxy.getName();
        config.set("cached", "name");
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
        config.set("reloaded", "name");
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
        assertEquals("default", config.get("name"));
        assertEquals(42, config.get("port"));
        assertEquals(true, config.get("enabled"));
    }

    @Test
    void commentAnnotationSetsCommentOnFirstSetup() {
        ConfigGenerator.newInstance(CommentedConfig.class, config, true, false, true);
        List<String> comment = config.getComment("name");
        assertEquals(Arrays.asList("This is the name"), comment);
    }

    @Test
    void nestedPathWorksCorrectly() {
        TestConfigProxy proxy = ConfigGenerator.newInstance(TestConfigProxy.class, config, true, false, true);
        assertEquals("nestedDefault", proxy.getNestedValue());
        config.set("customNested", "server", "name");
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
        config.set("changed", "value");
        assertEquals("sticky", proxy.getStickyValue());
    }

    @Test
    void nonStickyValueReturnsLatestValue() {
        NonStickyConfig proxy = ConfigGenerator.newInstance(NonStickyConfig.class, config, true, false, true);
        assertEquals("nonSticky", proxy.getNonStickyValue());
        config.set("changed", "value");
        assertEquals("changed", proxy.getNonStickyValue());
    }

    @Test
    void reloadClearsStickyCache() {
        StickyConfig proxy = ConfigGenerator.newInstance(StickyConfig.class, config, true, true, true);
        assertEquals("sticky", proxy.getStickyValue());
        config.set("changed", "value");
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
        public Object get(Object def, String... path) {
            return data.getOrDefault(joinPath(path), def);
        }

        @Override
        public Object get(String... path) {
            return data.getOrDefault(joinPath(path), null);
        }

        @Override
        public boolean contains(String... path) {
            return data.containsKey(joinPath(path));
        }

        @Override
        public void set(Object value, String... path) {
            String key = joinPath(path);
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        @Override
        public String getName() {
            return "test-config";
        }

        @Override
        public Map<String[], Object> getValues(boolean deep, String... path) {
            Map<String[], Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String[] parts = entry.getKey().split("\0", -1);
                if (parts.length >= path.length) {
                    boolean matches = true;
                    for (int i = 0; i < path.length; i++) {
                        if (!path[i].equals(parts[i])) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        if (deep || parts.length == path.length) {
                            result.put(parts, entry.getValue());
                        }
                    }
                }
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
        public List<String> getComment(String... path) {
            return comments.getOrDefault(joinPath(path), Collections.emptyList());
        }

        @Override
        public void setComment(List<String> value, String... path) {
            if (path.length == 0) {
                classComment = value != null ? value : Collections.emptyList();
            } else {
                comments.put(joinPath(path), value);
            }
        }

        @Override
        public List<String> getComment(CommentType type, String... path) {
            if (path.length == 0) {
                return classComment;
            }
            return comments.getOrDefault(joinPath(path), Collections.emptyList());
        }

        @Override
        public void setComment(CommentType type, List<String> value, String... path) {
            setComment(value, path);
        }
    }
}
