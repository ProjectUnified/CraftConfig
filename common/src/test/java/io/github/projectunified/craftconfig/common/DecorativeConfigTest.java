package io.github.projectunified.craftconfig.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecorativeConfigTest {

    private SimpleConfig delegate;
    private TestDecorativeConfig decorative;

    @BeforeEach
    void setUp() {
        delegate = new SimpleConfig("test-config");
        decorative = new TestDecorativeConfig(delegate);
    }

    @Test
    void getOriginalConfig() {
        assertSame(delegate, decorative.getOriginalConfig());
    }

    @Test
    void getOriginal() {
        delegate.set("hello", "key1");
        assertSame(delegate.getOriginal(), decorative.getOriginal());
    }

    @Test
    void get() {
        delegate.set("hello", "key1");
        Object def = null;
        assertEquals("hello", decorative.get(def, new String[]{"key1"}));
    }

    @Test
    void getReturnsDefault() {
        assertEquals("default", decorative.get("default", new String[]{"nonexistent"}));
    }

    @Test
    void set() {
        decorative.set("value", "key1");
        assertEquals("value", delegate.get("key1"));
    }

    @Test
    void contains() {
        delegate.set("value", "key1");
        assertTrue(decorative.contains("key1"));
    }

    @Test
    void containsReturnsFalse() {
        assertFalse(decorative.contains("nonexistent"));
    }

    @Test
    void getName() {
        assertEquals("test-config", decorative.getName());
    }

    @Test
    void getValues() {
        delegate.set("a", "key1");
        delegate.set("b", "key2");
        Map<String[], Object> values = decorative.getValues(false);
        assertEquals(2, values.size());
    }

    @Test
    void setup() {
        decorative.setup();
        assertTrue(delegate.setupCalled);
    }

    @Test
    void save() {
        decorative.save();
        assertTrue(delegate.saveCalled);
    }

    @Test
    void reload() {
        decorative.reload();
        assertTrue(delegate.reloadCalled);
    }

    @Test
    void normalize() {
        assertEquals("hello", decorative.normalize("hello"));
    }

    @Test
    void isNormalizable() {
        assertFalse(decorative.isNormalizable("hello"));
    }

    @Test
    void remove() {
        delegate.set("value", "key1");
        assertTrue(delegate.contains("key1"));
        decorative.remove("key1");
        assertFalse(delegate.contains("key1"));
    }

    @Test
    void removeEmptyPathClears() {
        delegate.set("a", "key1");
        delegate.set("b", "key2");
        decorative.remove();
        assertFalse(delegate.contains("key1"));
        assertFalse(delegate.contains("key2"));
    }

    @Test
    void getNormalized() {
        delegate.set("hello", "key1");
        assertEquals("hello", decorative.getNormalized("key1"));
    }

    @Test
    void getNormalizedWithDefault() {
        assertEquals("default", decorative.getNormalized("default", new String[]{"nonexistent"}));
    }

    @Test
    void getTypeClass() {
        delegate.set("hello", "key1");
        assertEquals("hello", decorative.get(String.class, new String[]{"key1"}));
    }

    @Test
    void getTypeClassWithDefault() {
        delegate.set(42, "key1");
        assertEquals("42", decorative.get(String.class, "default", new String[]{"key1"}));
    }

    @Test
    void setIfAbsent() {
        decorative.setIfAbsent("value", "key1");
        assertEquals("value", delegate.get("key1"));
    }

    @Test
    void setIfAbsentDoesNotOverwrite() {
        delegate.set("original", "key1");
        decorative.setIfAbsent("new", "key1");
        assertEquals("original", delegate.get("key1"));
    }

    @Test
    void getKeys() {
        delegate.set("a", "key1");
        delegate.set("b", "key2");
        assertEquals(2, decorative.getKeys(false).size());
    }

    @Test
    void getNormalizedValues() {
        delegate.set("hello", "key1");
        delegate.set("world", "key2");
        Map<String[], Object> values = decorative.getNormalizedValues(false);
        assertEquals(2, values.size());
    }

    @Test
    void normalizeObjectMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        Object result = decorative.normalizeObject(map);
        assertTrue(result instanceof Map);
    }

    @Test
    void isInstance() {
        delegate.set("hello", "key1");
        assertTrue(decorative.isInstance(String.class, "key1"));
        assertFalse(decorative.isInstance(Integer.class, "key1"));
    }

    @Test
    void clear() {
        delegate.set("a", "key1");
        delegate.set("b", "key2");
        decorative.clear();
        assertFalse(delegate.contains("key1"));
        assertFalse(delegate.contains("key2"));
    }

    @Test
    void commentMethodsReturnEmpty() {
        assertTrue(decorative.getComment("key1").isEmpty());
        assertTrue(decorative.getComment(CommentType.BLOCK, "key1").isEmpty());
        assertTrue(decorative.getComment(CommentType.SIDE, "key1").isEmpty());
    }

    // --- Test helpers ---

    static class TestDecorativeConfig extends DecorativeConfig {
        TestDecorativeConfig(Config config) {
            super(config);
        }

        @Override
        public Object get(Object def, String... path) {
            return config.get(def, path);
        }

        @Override
        public Object get(String... path) {
            Object def = null;
            return get(def, path);
        }

        @Override
        public <T> T get(Class<T> type, T def, String... path) {
            Object value = get(def, path);
            if (type == Object.class) {
                return type.cast(value);
            }
            Object normalized = normalizeObject(value);
            if (type == String.class) {
                return normalized != null ? type.cast(String.valueOf(normalized)) : def;
            }
            return type.isInstance(normalized) ? type.cast(normalized) : def;
        }

        @Override
        public <T> T get(Class<T> type, String... path) {
            return get(type, null, path);
        }

        @Override
        public boolean contains(String... path) {
            Object def = null;
            return get(def, path) != null;
        }

        @Override
        public boolean isInstance(Class<?> type, String... path) {
            Object def = null;
            return type.isInstance(get(def, path));
        }

        @Override
        public Object getNormalized(Object def, String... path) {
            Object value = get(def, path);
            return normalizeObject(value);
        }

        @Override
        public Object getNormalized(String... path) {
            Object def = null;
            return getNormalized(def, path);
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
    }

    static class SimpleConfig implements Config {
        private final Map<String, Object> data = new LinkedHashMap<>();
        private final String name;
        boolean setupCalled = false;
        boolean saveCalled = false;
        boolean reloadCalled = false;

        SimpleConfig(String name) {
            this.name = name;
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
            Object value = data.get(path[0]);
            return value != null ? value : def;
        }

        @Override
        public void set(Object value, String... path) {
            if (path.length > 0) {
                data.put(path[0], value);
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String[], Object> getValues(boolean deep, String... path) {
            Map<String[], Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                result.put(new String[]{entry.getKey()}, entry.getValue());
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
            Object value = get(def, path);
            return normalizeObject(value);
        }

        @Override
        public Object getNormalized(String... path) {
            Object def = null;
            return getNormalized(def, path);
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
            setupCalled = true;
        }

        @Override
        public void save() {
            saveCalled = true;
        }

        @Override
        public void reload() {
            reloadCalled = true;
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
