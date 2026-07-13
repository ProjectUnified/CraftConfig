package io.github.projectunified.craftconfig.annotated;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.common.CommentType;
import io.github.projectunified.craftconfig.common.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnnotatedConfigTest {

    private SimpleConfig backingConfig;
    private TestAnnotatedConfig annotatedConfig;

    @BeforeEach
    void setUp() {
        backingConfig = new SimpleConfig("test-config");
        annotatedConfig = new TestAnnotatedConfig(backingConfig);
        annotatedConfig.setup();
    }

    @Test
    void setupWritesDefaultsToConfig() {
        assertEquals("defaultString", backingConfig.getValueAt("test", "string"));
        assertEquals(42, backingConfig.getValueAt("test", "int"));
        assertEquals(true, backingConfig.getValueAt("test", "bool"));
    }

    @Test
    void setupWritesEnumDefaultToConfig() {
        assertEquals("VALUE_A", backingConfig.getValueAt("test", "enum"));
    }

    @Test
    void setupWritesListDefaultToConfig() {
        Object list = backingConfig.getValueAt("test", "list");
        assertNotNull(list);
        assertTrue(list instanceof List);
        List<?> listValue = (List<?>) list;
        assertEquals(2, listValue.size());
        assertEquals("a", listValue.get(0));
        assertEquals("b", listValue.get(1));
    }

    @Test
    void setupReadsExistingValuesIntoFields() {
        backingConfig.set("fromConfig", "test", "string");
        backingConfig.set(99, "test", "int");
        backingConfig.set(false, "test", "bool");

        TestAnnotatedConfig config = new TestAnnotatedConfig(backingConfig);
        config.setup();

        assertEquals("fromConfig", config.stringField);
        assertEquals(99, config.intField);
        assertEquals(false, config.boolField);
    }

    @Test
    void setupDoesNotOverwriteExistingConfigValues() {
        backingConfig.set("existingValue", "test", "string");

        annotatedConfig = new TestAnnotatedConfig(backingConfig);
        annotatedConfig.setup();

        assertEquals("existingValue", backingConfig.getValueAt("test", "string"));
    }

    @Test
    void setupIgnoresStaticFinalFields() {
        StaticFinalAnnotatedConfig config = new StaticFinalAnnotatedConfig(backingConfig);
        config.setup();

        assertEquals("staticDefault", StaticFinalAnnotatedConfig.STATIC_FINAL);
        assertFalse(backingConfig.containsPath("static", "path"));
    }

    @Test
    void setupHandlesFinalFieldSetViaConstructor() {
        ConstructorFinalAnnotatedConfig config = new ConstructorFinalAnnotatedConfig(backingConfig);
        config.setup();

        assertEquals("constructorValue", config.finalField);
        assertEquals("constructorValue", backingConfig.getValueAt("constructor", "path"));
    }

    @Test
    void setupHandlesConstructorFinalFieldReadsExistingValue() {
        backingConfig.set("existingFinal", "constructor", "path");

        ConstructorFinalAnnotatedConfig config = new ConstructorFinalAnnotatedConfig(backingConfig);
        config.setup();

        assertEquals("existingFinal", config.finalField);
    }

    @Test
    void commentOnFieldSetsCommentOnFirstSetup() {
        CommentedFieldConfig config = new CommentedFieldConfig(backingConfig);
        config.setup();

        List<String> comment = backingConfig.getStoredComment("commented", "field");
        assertEquals(1, comment.size());
        assertEquals("This is a field comment", comment.get(0));
    }

    @Test
    void commentOnFieldDoesNotOverwriteExistingComment() {
        backingConfig.set("existingValue", "commented", "field");
        backingConfig.storeComment(Arrays.asList("Existing comment"), "commented", "field");

        CommentedFieldConfig config = new CommentedFieldConfig(backingConfig);
        config.setup();

        List<String> comment = backingConfig.getStoredComment("commented", "field");
        assertEquals(1, comment.size());
        assertEquals("Existing comment", comment.get(0));
    }

    @Test
    void commentOnClassSetsBlockComment() {
        ClassCommentConfig config = new ClassCommentConfig(backingConfig);
        config.setup();

        List<String> comment = backingConfig.getStoredRootComment();
        assertEquals(1, comment.size());
        assertEquals("Class level comment", comment.get(0));
    }

    @Test
    void commentOnClassDoesNotOverwriteExistingComment() {
        backingConfig.storeRootComment(Arrays.asList("Existing class comment"));

        ClassCommentConfig config = new ClassCommentConfig(backingConfig);
        config.setup();

        List<String> comment = backingConfig.getStoredRootComment();
        assertEquals(1, comment.size());
        assertEquals("Existing class comment", comment.get(0));
    }

    @Test
    void reloadReadsUpdatedValuesIntoFields() {
        assertEquals("defaultString", annotatedConfig.stringField);

        backingConfig.set("updatedValue", "test", "string");
        annotatedConfig.reload();

        assertEquals("updatedValue", annotatedConfig.stringField);
    }

    @Test
    void reloadReadsUpdatedIntValue() {
        assertEquals(42, annotatedConfig.intField);

        backingConfig.set(100, "test", "int");
        annotatedConfig.reload();

        assertEquals(100, annotatedConfig.intField);
    }

    @Test
    void reloadReadsUpdatedBoolValue() {
        assertEquals(true, annotatedConfig.boolField);

        backingConfig.set(false, "test", "bool");
        annotatedConfig.reload();

        assertEquals(false, annotatedConfig.boolField);
    }

    @Test
    void reloadReadsUpdatedEnumValue() {
        assertEquals(TestEnum.VALUE_A, annotatedConfig.enumField);

        backingConfig.set(TestEnum.VALUE_B, "test", "enum");
        annotatedConfig.reload();

        assertEquals(TestEnum.VALUE_B, annotatedConfig.enumField);
    }

    @Test
    void setWithAnnotatedPathUpdatesConfigAndField() {
        annotatedConfig.set("newValue", "test", "string");

        assertEquals("newValue", annotatedConfig.stringField);
        assertEquals("newValue", backingConfig.getValueAt("test", "string"));
    }

    @Test
    void setWithAnnotatedIntPathUpdatesConfigAndField() {
        annotatedConfig.set(999, "test", "int");

        assertEquals(999, annotatedConfig.intField);
        assertEquals(999, backingConfig.getValueAt("test", "int"));
    }

    @Test
    void setWithAnnotatedBoolPathUpdatesConfigAndField() {
        annotatedConfig.set(false, "test", "bool");

        assertEquals(false, annotatedConfig.boolField);
        assertEquals(false, backingConfig.getValueAt("test", "bool"));
    }

    @Test
    void setWithNonAnnotatedPathDelegatesNormally() {
        annotatedConfig.set("nonAnnotatedValue", "some", "other", "path");

        assertEquals("nonAnnotatedValue", backingConfig.getValueAt("some", "other", "path"));
    }

    @Test
    void setWithAnnotatedEnumPathUpdatesConfigAndField() {
        annotatedConfig.set(TestEnum.VALUE_B, "test", "enum");

        assertEquals(TestEnum.VALUE_B, annotatedConfig.enumField);
        assertEquals("VALUE_B", backingConfig.getValueAt("test", "enum"));
    }

    @Test
    void priorityDeterminesSetupOrder() {
        SimpleConfig config = new SimpleConfig("priority-test");
        PriorityTestAnnotatedConfig priorityConfig = new PriorityTestAnnotatedConfig(config);
        priorityConfig.setup();

        assertEquals("lowPriority", priorityConfig.lowField);
        assertEquals("highPriority", priorityConfig.highField);
    }

    // --- Helper config classes ---

    enum TestEnum {
        VALUE_A, VALUE_B, VALUE_C
    }

    static class TestAnnotatedConfig extends AnnotatedConfig {
        @ConfigPath({"test", "string"})
        String stringField = "defaultString";

        @ConfigPath({"test", "int"})
        int intField = 42;

        @ConfigPath({"test", "bool"})
        boolean boolField = true;

        @ConfigPath({"test", "enum"})
        TestEnum enumField = TestEnum.VALUE_A;

        @ConfigPath({"test", "list"})
        List<String> listField = Arrays.asList("a", "b");

        TestAnnotatedConfig(Config config) {
            super(config);
        }
    }

    static class StaticFinalAnnotatedConfig extends AnnotatedConfig {
        static final String STATIC_FINAL = "staticDefault";

        @ConfigPath({"static", "path"})
        static final String staticField = "staticValue";

        StaticFinalAnnotatedConfig(Config config) {
            super(config);
        }
    }

    static class ConstructorFinalAnnotatedConfig extends AnnotatedConfig {
        @ConfigPath({"constructor", "path"})
        final String finalField;

        ConstructorFinalAnnotatedConfig(Config config) {
            super(config);
            finalField = "constructorValue";
        }
    }

    static class CommentedFieldConfig extends AnnotatedConfig {
        @ConfigPath({"commented", "field"})
        @Comment("This is a field comment")
        String field = "defaultValue";

        CommentedFieldConfig(Config config) {
            super(config);
        }

        @Override
        public void setComment(CommentType type, List<String> value, String... path) {
            getOriginalConfig().setComment(type, value, path);
        }

        @Override
        public List<String> getComment(CommentType type, String... path) {
            return getOriginalConfig().getComment(type, path);
        }
    }

    @Comment("Class level comment")
    static class ClassCommentConfig extends AnnotatedConfig {
        @ConfigPath({"class", "field"})
        String field = "defaultValue";

        ClassCommentConfig(Config config) {
            super(config);
        }

        @Override
        public void setComment(CommentType type, List<String> value, String... path) {
            getOriginalConfig().setComment(type, value, path);
        }

        @Override
        public List<String> getComment(CommentType type, String... path) {
            return getOriginalConfig().getComment(type, path);
        }
    }

    static class PriorityTestAnnotatedConfig extends AnnotatedConfig {
        @ConfigPath(value = {"priority", "low"}, priority = 10)
        String lowField = "lowPriority";

        @ConfigPath(value = {"priority", "high"}, priority = 1)
        String highField = "highPriority";

        PriorityTestAnnotatedConfig(Config config) {
            super(config);
        }
    }

    /**
     * Simple Config implementation backed by a HashMap for testing.
     * Uses PathKey for content-based array key comparison.
     * Overrides problematic default methods from Config interface
     * that suffer from overload ambiguity with null arguments.
     */
    static class SimpleConfig implements Config {
        private final Map<PathKey, Object> data = new LinkedHashMap<>();
        private final Map<PathKey, List<String>> comments = new LinkedHashMap<>();
        private final String name;
        private List<String> rootComment = new ArrayList<>();

        SimpleConfig(String name) {
            this.name = name;
        }

        private static boolean startsWith(String[] key, String[] prefix) {
            if (key.length < prefix.length) return false;
            for (int i = 0; i < prefix.length; i++) {
                if (!key[i].equals(prefix[i])) return false;
            }
            return true;
        }

        Object getValueAt(String... path) {
            return data.get(new PathKey(path));
        }

        boolean containsPath(String... path) {
            return data.containsKey(new PathKey(path));
        }

        List<String> getStoredComment(String... path) {
            List<String> c = comments.get(new PathKey(path));
            return c != null ? Collections.unmodifiableList(c) : Collections.emptyList();
        }

        List<String> getStoredRootComment() {
            return Collections.unmodifiableList(rootComment);
        }

        void storeComment(List<String> value, String... path) {
            comments.put(new PathKey(path), new ArrayList<>(value));
        }

        void storeRootComment(List<String> value) {
            rootComment = new ArrayList<>(value);
        }

        @Override
        public Object getOriginal() {
            return data;
        }

        @Override
        public boolean contains(String... path) {
            if (path.length == 0) return false;
            return data.containsKey(new PathKey(path));
        }

        @Override
        public Object get(Object def, String... path) {
            if (path.length == 0) return def;
            Object value = data.get(new PathKey(path));
            return value != null ? value : def;
        }

        @Override
        public void set(Object value, String... path) {
            if (path.length > 0) {
                data.put(new PathKey(path), value);
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String[], Object> getValues(boolean deep, String... path) {
            Map<String[], Object> result = new LinkedHashMap<>();
            for (Map.Entry<PathKey, Object> entry : data.entrySet()) {
                String[] key = entry.getKey().path;
                if (path.length == 0 || startsWith(key, path)) {
                    result.put(key, entry.getValue());
                }
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

        @Override
        public Object getNormalized(Object def, String... path) {
            if (path.length == 0) return def;
            Object value = data.get(new PathKey(path));
            return value != null ? value : def;
        }

        @Override
        public List<String> getComment(CommentType type, String... path) {
            if (path.length == 0) {
                return type == CommentType.BLOCK ? Collections.unmodifiableList(rootComment) : Collections.emptyList();
            }
            List<String> comment = comments.get(new PathKey(path));
            if (comment == null) return Collections.emptyList();
            return Collections.unmodifiableList(comment);
        }

        @Override
        public void setComment(CommentType type, List<String> value, String... path) {
            if (path.length == 0) {
                if (type == CommentType.BLOCK && value != null) {
                    rootComment = new ArrayList<>(value);
                }
            } else {
                if (value != null) {
                    comments.put(new PathKey(path), new ArrayList<>(value));
                }
            }
        }

        static class PathKey {
            private final String[] path;

            PathKey(String[] path) {
                this.path = path;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                PathKey pathKey = (PathKey) o;
                return Arrays.equals(path, pathKey.path);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(path);
            }
        }
    }
}
