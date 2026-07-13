package io.github.projectunified.craftconfig.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDefaultMethodsTest {

    private SimpleMapConfig config;

    @BeforeEach
    void setUp() {
        config = new SimpleMapConfig("test-config");
    }

    @Test
    void containsTrue() {
        config.node("key1").set("value");
        assertTrue(config.node("key1").exists());
    }

    @Test
    void containsFalse() {
        assertFalse(config.node("nonexistent").exists());
    }

    @Test
    void containsNestedTrue() {
        config.node("key1", "key2").set("value");
        assertTrue(config.node("key1", "key2").exists());
    }

    @Test
    void containsNestedFalse() {
        config.node("key1").set("value");
        assertFalse(config.node("key1", "key2").exists());
    }

    @Test
    void remove() {
        config.node("key1").set("value");
        assertTrue(config.node("key1").exists());
        config.node("key1").remove();
        assertFalse(config.node("key1").exists());
    }

    @Test
    void clear() {
        config.node("key1").set("a");
        config.node("key2").set("b");
        assertTrue(config.node("key1").exists());
        assertTrue(config.node("key2").exists());
        config.remove();
        assertFalse(config.node("key1").exists());
        assertFalse(config.node("key2").exists());
    }

    @Test
    void getNormalizedWithDefault() {
        config.node("key1").set("hello");
        Object def = "default";
        assertEquals("hello", config.node("key1").get(def));
    }

    @Test
    void getNormalizedNotFound() {
        Object def = "default";
        assertEquals("default", config.node("nonexistent").get(def));
    }

    @Test
    void getNormalizedNull() {
        config.node("key1").set("hello");
        assertEquals("hello", config.node("key1").getNormalized());
    }

    @Test
    void getTypeWithDefaultReturnsValue() {
        config.node("key1").set("hello");
        assertEquals("hello", config.node("key1").get(String.class, "default"));
    }

    @Test
    void getTypeWithDefaultReturnsDefault() {
        assertEquals("default", config.node("nonexistent").get(String.class, "default"));
    }

    @Test
    void getTypeWithDefaultObjectClassReturnsRawValue() {
        config.node("key1").set("hello");
        assertEquals("hello", config.node("key1").get(Object.class, "default"));
    }

    @Test
    void getTypeWithDefaultObjectClassReturnsDefaultWhenMissing() {
        assertEquals("default", config.node("nonexistent").get(Object.class, "default"));
    }

    @Test
    void getTypeWithDefaultStringConvertsToString() {
        config.node("key1").set(42);
        assertEquals("42", config.node("key1").get(String.class, "default"));
    }

    @Test
    void getTypeWithDefaultStringReturnsDefaultWhenNull() {
        assertEquals("default", config.node("nonexistent").get(String.class, "default"));
    }

    @Test
    void getTypeWithDefaultReturnsDefaultForWrongType() {
        config.node("key1").set("hello");
        assertEquals(Integer.valueOf(0), config.node("key1").get(Integer.class, 0));
    }

    @Test
    void getTypeWithoutDefaultReturnsValue() {
        config.node("key1").set("hello");
        assertEquals("hello", config.node("key1").get(String.class));
    }

    @Test
    void getTypeWithoutDefaultReturnsNull() {
        assertNull(config.node("nonexistent").get(String.class));
    }

    @Test
    void setAndGetRoundTrip() {
        config.node("key1").set("value");
        assertEquals("value", config.node("key1").get());
    }

    @Test
    void getKeys() {
        config.node("key1").set("a");
        config.node("key2").set("b");
        Map<String, ConfigNode> values = config.getChildren();
        assertEquals(2, values.size());
    }

    @Test
    void getNormalizedValues() {
        config.node("key1").set("hello");
        config.node("key2").set("world");
        Map<String, ConfigNode> values = config.getChildren();
        assertEquals(2, values.size());
    }

    @Test
    void normalizeObjectNull() {
        assertNull(config.normalize(null));
    }

    @Test
    void normalizeObjectPlain() {
        assertEquals("hello", config.normalize("hello"));
    }

    @Test
    void normalizeObjectMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        Object result = config.normalize(map);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("value", resultMap.get("key"));
    }

    @Test
    void normalizeObjectCollection() {
        List<Object> list = Arrays.asList("a", "b", "c");
        Object result = config.normalize(list);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) result;
        assertEquals(3, resultList.size());
        assertEquals("a", resultList.get(0));
    }

    @Test
    void isInstanceTrue() {
        config.node("key1").set("hello");
        assertNotNull(config.node("key1").get(String.class));
    }

    @Test
    void isInstanceFalse() {
        config.node("key1").set("hello");
        assertNull(config.node("key1").get(Integer.class));
    }

    @Test
    void getWithNoArgsPathReturnsNull() {
        assertNull(config.get());
    }

    @Test
    void getOriginal() {
        assertNotNull(config.getOriginal());
        assertSame(config.data, config.getOriginal());
    }

    @Test
    void getName() {
        assertEquals("test-config", config.getName());
    }

    @Test
    void getCommentReturnsEmpty() {
        assertTrue(config.node("key1").getComment().isEmpty());
        assertTrue(config.node("key1").getComment(CommentType.BLOCK).isEmpty());
        assertTrue(config.node("key1").getComment(CommentType.SIDE).isEmpty());
    }

    /**
     * Simple Config implementation backed by a HashMap for testing default methods.
     * Implements the new node-based Config API.
     */
    static class SimpleMapConfig implements Config {
        private final Map<String[], Object> data = new LinkedHashMap<>();
        private final String name;

        SimpleMapConfig(String name) {
            this.name = name;
        }

        private static boolean startsWith(String[] key, String[] prefix) {
            if (key.length < prefix.length) return false;
            for (int i = 0; i < prefix.length; i++) {
                if (!key[i].equals(prefix[i])) return false;
            }
            return true;
        }

        @Override
        public Object getOriginal() {
            return data;
        }

        @Override
        public String getName() {
            return name;
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
            for (Map.Entry<String[], Object> entry : data.entrySet()) {
                String[] key = entry.getKey();
                result.put(key[key.length - 1], new SimpleConfigNode(this, key));
            }
            return result;
        }

        @Override
        public void setup() {
        }

        @Override
        public void save() {
        }

        @Override
        public void reload() {
        }

        @Override
        public Object normalize(Object object) {
            return object;
        }

        @Override
        public boolean isNormalizable(Object object) {
            return false;
        }

        private Object getValueAt(String... path) {
            if (path.length == 0) {
                return null;
            }
            for (Map.Entry<String[], Object> entry : data.entrySet()) {
                String[] key = entry.getKey();
                if (key.length == path.length) {
                    boolean match = true;
                    for (int i = 0; i < path.length; i++) {
                        if (!key[i].equals(path[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return entry.getValue();
                    }
                }
            }
            return null;
        }

        private void setValueAt(Object value, String... path) {
            if (path.length > 0) {
                data.entrySet().removeIf(entry -> {
                    String[] key = entry.getKey();
                    if (key.length != path.length) return false;
                    for (int i = 0; i < path.length; i++) {
                        if (!key[i].equals(path[i])) return false;
                    }
                    return true;
                });
                data.put(path, value);
            }
        }

        private boolean containsPath(String... path) {
            return getValueAt(path) != null;
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
                return config.getValueAt(path);
            }

            @Override
            public void set(Object value) {
                config.setValueAt(value, path);
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
                return get() != null;
            }

            @Override
            public void remove() {
                config.setValueAt(null, path);
            }

            @Override
            public boolean hasChild() {
                return !getChildren().isEmpty();
            }

            @Override
            public Map<String, ConfigNode> getChildren() {
                Map<String, ConfigNode> result = new LinkedHashMap<>();
                for (Map.Entry<String[], Object> entry : config.data.entrySet()) {
                    String[] key = entry.getKey();
                    if (startsWith(key, path) && key.length > path.length) {
                        result.put(key[key.length - 1], new SimpleConfigNode(config, key));
                    }
                }
                return result;
            }

            @Override
            public Object getNormalized() {
                return get();
            }

            private boolean startsWith(String[] key, String[] prefix) {
                if (key.length < prefix.length) return false;
                for (int i = 0; i < prefix.length; i++) {
                    if (!key[i].equals(prefix[i])) return false;
                }
                return true;
            }
        }
    }
}
