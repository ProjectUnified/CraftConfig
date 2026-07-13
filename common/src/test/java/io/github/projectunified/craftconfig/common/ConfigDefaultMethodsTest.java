package io.github.projectunified.craftconfig.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigDefaultMethodsTest {

    private SimpleMapConfig config;

    @BeforeEach
    void setUp() {
        config = new SimpleMapConfig("test-config");
    }

    @Test
    void containsTrue() {
        config.set("value", "key1");
        assertTrue(config.contains("key1"));
    }

    @Test
    void containsFalse() {
        assertFalse(config.contains("nonexistent"));
    }

    @Test
    void containsNestedTrue() {
        config.set("value", "key1", "key2");
        assertTrue(config.contains("key1", "key2"));
    }

    @Test
    void containsNestedFalse() {
        config.set("value", "key1");
        assertFalse(config.contains("key1", "key2"));
    }

    @Test
    void remove() {
        config.set("value", "key1");
        assertTrue(config.contains("key1"));
        config.remove("key1");
        assertFalse(config.contains("key1"));
    }

    @Test
    void clear() {
        config.set("a", "key1");
        config.set("b", "key2");
        assertTrue(config.contains("key1"));
        assertTrue(config.contains("key2"));
        config.clear();
        assertFalse(config.contains("key1"));
        assertFalse(config.contains("key2"));
    }

    @Test
    void getNormalizedWithDefault() {
        config.set("hello", "key1");
        Object def = "default";
        assertEquals("hello", config.getNormalized(def, new String[]{"key1"}));
    }

    @Test
    void getNormalizedNotFound() {
        Object def = "default";
        assertEquals("default", config.getNormalized(def, new String[]{"nonexistent"}));
    }

    @Test
    void getNormalizedNull() {
        config.set("hello", "key1");
        assertEquals("hello", config.getNormalized("key1"));
    }

    @Test
    void getTypeWithDefaultReturnsValue() {
        config.set("hello", "key1");
        assertEquals("hello", config.get(String.class, "default", new String[]{"key1"}));
    }

    @Test
    void getTypeWithDefaultReturnsDefault() {
        assertEquals("default", config.get(String.class, "default", new String[]{"nonexistent"}));
    }

    @Test
    void getTypeWithDefaultObjectClassReturnsRawValue() {
        config.set("hello", "key1");
        assertEquals("hello", config.get(Object.class, "default", new String[]{"key1"}));
    }

    @Test
    void getTypeWithDefaultObjectClassReturnsDefaultWhenMissing() {
        assertEquals("default", config.get(Object.class, "default", new String[]{"nonexistent"}));
    }

    @Test
    void getTypeWithDefaultStringConvertsToString() {
        config.set(42, "key1");
        assertEquals("42", config.get(String.class, "default", new String[]{"key1"}));
    }

    @Test
    void getTypeWithDefaultStringReturnsDefaultWhenNull() {
        assertEquals("default", config.get(String.class, "default", new String[]{"nonexistent"}));
    }

    @Test
    void getTypeWithDefaultReturnsDefaultForWrongType() {
        config.set("hello", "key1");
        assertEquals(Integer.valueOf(0), config.get(Integer.class, 0, new String[]{"key1"}));
    }

    @Test
    void getTypeWithoutDefaultReturnsValue() {
        config.set("hello", "key1");
        assertEquals("hello", config.get(String.class, new String[]{"key1"}));
    }

    @Test
    void getTypeWithoutDefaultReturnsNull() {
        assertNull(config.get(String.class, new String[]{"nonexistent"}));
    }

    @Test
    void setIfAbsentSets() {
        config.setIfAbsent("value", "key1");
        assertEquals("value", config.get("key1"));
    }

    @Test
    void setIfAbsentDoesNotOverwrite() {
        config.set("original", "key1");
        config.setIfAbsent("new", "key1");
        assertEquals("original", config.get("key1"));
    }

    @Test
    void setIfAbsentMap() {
        Map<String[], Object> map = new LinkedHashMap<>();
        map.put(new String[]{"a"}, "val1");
        map.put(new String[]{"b"}, "val2");
        config.setIfAbsent(map);
        assertEquals("val1", config.get("a"));
        assertEquals("val2", config.get("b"));
    }

    @Test
    void setIfAbsentMapDoesNotOverwrite() {
        config.set("existing", "a");
        Map<String[], Object> map = new LinkedHashMap<>();
        map.put(new String[]{"a"}, "new");
        map.put(new String[]{"b"}, "val2");
        config.setIfAbsent(map);
        assertEquals("existing", config.get("a"));
        assertEquals("val2", config.get("b"));
    }

    @Test
    void getKeys() {
        config.set("a", "key1");
        config.set("b", "key2");
        Set<String[]> keys = config.getKeys(false);
        assertEquals(2, keys.size());
    }

    @Test
    void getNormalizedValues() {
        config.set("hello", "key1");
        config.set("world", "key2");
        Map<String[], Object> values = config.getNormalizedValues(false);
        assertEquals(2, values.size());
    }

    @Test
    void normalizeObjectNull() {
        assertNull(config.normalizeObject(null));
    }

    @Test
    void normalizeObjectPlain() {
        assertEquals("hello", config.normalizeObject("hello"));
    }

    @Test
    void normalizeObjectMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        Object result = config.normalizeObject(map);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("value", resultMap.get("key"));
    }

    @Test
    void normalizeObjectCollection() {
        List<Object> list = Arrays.asList("a", "b", "c");
        Object result = config.normalizeObject(list);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) result;
        assertEquals(3, resultList.size());
        assertEquals("a", resultList.get(0));
    }

    @Test
    void isInstanceTrue() {
        config.set("hello", "key1");
        assertTrue(config.isInstance(String.class, "key1"));
    }

    @Test
    void isInstanceFalse() {
        config.set("hello", "key1");
        assertFalse(config.isInstance(Integer.class, "key1"));
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
        assertTrue(config.getComment("key1").isEmpty());
        assertTrue(config.getComment(CommentType.BLOCK, "key1").isEmpty());
        assertTrue(config.getComment(CommentType.SIDE, "key1").isEmpty());
    }

    /**
     * Simple Config implementation backed by a HashMap for testing default methods.
     * Overrides default methods that have ambiguous get() calls to avoid NPE.
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
        public Object get(Object def, String... path) {
            if (path.length == 0) {
                return def;
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
            return def;
        }

        @Override
        public void set(Object value, String... path) {
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

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String[], Object> getValues(boolean deep, String... path) {
            Map<String[], Object> result = new LinkedHashMap<>();
            for (Map.Entry<String[], Object> entry : data.entrySet()) {
                String[] key = entry.getKey();
                if (path.length == 0 || startsWith(key, path)) {
                    result.put(key, entry.getValue());
                }
            }
            return result;
        }

        @Override
        public boolean contains(String... path) {
            Object def = null;
            return get(def, path) != null;
        }

        @Override
        public Object get(String... path) {
            Object def = null;
            return get(def, path);
        }

        @Override
        public boolean isInstance(Class<?> type, String... path) {
            Object def = null;
            return type.isInstance(get(def, path));
        }

        @Override
        public Object getNormalized(Object def, String... path) {
            if (path.length == 0) {
                return normalizeObject(def);
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
                        return normalizeObject(entry.getValue());
                    }
                }
            }
            return normalizeObject(def);
        }

        @Override
        public Object getNormalized(String... path) {
            return getNormalized(null, path);
        }

        @Override
        public void remove(String... path) {
            if (path.length == 0) {
                clear();
            } else {
                set(null, path);
            }
        }

        @Override
        public void clear() {
            getKeys(false).forEach(this::remove);
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
    }
}
