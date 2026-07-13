package io.github.projectunified.craftconfig.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GsonConfigTest {

    @TempDir
    Path tempDir;

    private File getTestFile(String name) {
        return tempDir.resolve(name).toFile();
    }

    private Object getConfigValue(GsonConfig config, String... path) {
        Object def = null;
        return config.get(def, path);
    }

    @Test
    void getOriginalReturnsJsonObject() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        Object original = config.getOriginal();
        assertInstanceOf(JsonObject.class, original);
    }

    @Test
    void setAndGetStringValue() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("hello", "key");
        Object value = getConfigValue(config, "key");
        assertNotNull(value);
        assertInstanceOf(JsonPrimitive.class, value);
        assertEquals("hello", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void setAndGetNestedPath() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("val", "a", "b", "c");
        Object value = getConfigValue(config, "a", "b", "c");
        assertNotNull(value);
        assertInstanceOf(JsonPrimitive.class, value);
        assertEquals("val", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void setNullRemovesEntry() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("hello", "key");
        assertNotNull(getConfigValue(config, "key"));
        config.set(null, "key");
        assertNull(getConfigValue(config, "key"));
    }

    @Test
    void containsTrueWhenSet() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("value", "existing");
        assertTrue(config.contains("existing"));
    }

    @Test
    void containsFalseForMissing() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        assertFalse(config.contains("missing"));
    }

    @Test
    void removeDeletesEntry() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("value", "toRemove");
        assertTrue(config.contains("toRemove"));
        config.remove("toRemove");
        assertFalse(config.contains("toRemove"));
    }

    @Test
    void clearRemovesAll() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("val1", "key1");
        config.set("val2", "key2");
        config.clear();
        assertFalse(config.contains("key1"));
        assertFalse(config.contains("key2"));
    }

    @Test
    void getNameReturnsFileName() {
        GsonConfig config = new GsonConfig(getTestFile("myfile.json"));
        assertEquals("myfile.json", config.getName());
    }

    @Test
    void getValuesShallowReturnsDirectChildren() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("v1", "top1");
        config.set("v2", "top2");
        config.set("nested", "child", "value");

        Map<String[], Object> values = config.getValues(false);
        assertEquals(3, values.size());
    }

    @Test
    void getValuesDeepRecurses() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        config.set("v1", "top");
        config.set("nested", "child", "value");

        Map<String[], Object> values = config.getValues(true);
        assertTrue(values.size() >= 2);
    }

    @Test
    void setupCreatesFileIfNotExists() {
        File file = getTestFile("newfile.json");
        assertFalse(file.exists());
        GsonConfig config = new GsonConfig(file);
        config.setup();
        assertTrue(file.exists());
    }

    @Test
    void setupLoadsExistingContent() throws IOException {
        File file = getTestFile("existing.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"loaded\":\"true\"}");
        }

        GsonConfig config = new GsonConfig(file);
        config.setup();

        Object value = getConfigValue(config, "loaded");
        assertNotNull(value);
        assertInstanceOf(JsonPrimitive.class, value);
        assertEquals("true", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void saveWritesToFile() throws IOException {
        File file = getTestFile("savetest.json");
        GsonConfig config = new GsonConfig(file);
        config.set("saved", "value");
        config.save();

        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("saved"));
        assertTrue(content.contains("value"));
    }

    @Test
    void reloadReadsLatestFromFile() throws IOException {
        File file = getTestFile("reloadtest.json");
        GsonConfig config = new GsonConfig(file);
        config.setup();

        // Write directly to file to simulate external change
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"reloaded\":\"yes\"}");
        }

        config.reload();
        Object value = getConfigValue(config, "reloaded");
        assertNotNull(value);
        assertEquals("yes", ((JsonPrimitive) value).getAsString());
    }

    @Test
    void normalizeJsonElementReturnsNormalizedObject() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        JsonPrimitive primitive = new JsonPrimitive("hello");
        Object result = config.normalize(primitive);
        assertEquals("hello", result);
    }

    @Test
    void normalizeNonJsonElementReturnsSameObject() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        String input = "just a string";
        Object result = config.normalize(input);
        assertEquals(input, result);
    }

    @Test
    void isNormalizableJsonElementReturnsTrue() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        assertTrue(config.isNormalizable(new JsonObject()));
        assertTrue(config.isNormalizable(new JsonPrimitive("test")));
        assertTrue(config.isNormalizable(new JsonArray()));
    }

    @Test
    void isNormalizableStringReturnsFalse() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        assertFalse(config.isNormalizable("not a json element"));
        assertFalse(config.isNormalizable(42));
    }

    @Test
    void getWithDefaultReturnsDefaultWhenMissing() {
        GsonConfig config = new GsonConfig(getTestFile("test.json"));
        Object def = "defaultVal";
        Object result = config.get(def, "missing");
        assertNotNull(result);
        assertTrue(result instanceof com.google.gson.JsonPrimitive);
        assertEquals("defaultVal", ((com.google.gson.JsonPrimitive) result).getAsString());
    }

    @Test
    void setupHandlesEmptyFile() {
        File file = getTestFile("empty.json");
        try {
            file.createNewFile();
        } catch (IOException e) {
            fail("Could not create test file");
        }
        GsonConfig config = new GsonConfig(file);
        assertDoesNotThrow(config::setup);
    }
}
