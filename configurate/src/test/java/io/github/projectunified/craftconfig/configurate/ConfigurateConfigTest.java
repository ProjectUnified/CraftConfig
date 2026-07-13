package io.github.projectunified.craftconfig.configurate;

import io.github.projectunified.craftconfig.common.CommentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurateConfigTest {

    @TempDir
    Path tempDir;

    private ConfigurateConfig createConfig(String fileName) {
        File file = tempDir.resolve(fileName).toFile();
        ConfigurateConfig config = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        config.setup();
        return config;
    }

    private Object getConfigValue(ConfigurateConfig config, String... path) {
        Object def = null;
        return config.get(def, path);
    }

    private boolean pathExists(ConfigurateConfig config, String... path) {
        return getConfigValue(config, path) != null;
    }

    private boolean mapContainsKey(Map<String[], Object> map, String... key) {
        for (String[] k : map.keySet()) {
            if (Arrays.equals(k, key)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void getOriginalReturnsConfigurationNode() {
        ConfigurateConfig config = createConfig("test.yml");
        Object original = config.getOriginal();
        assertInstanceOf(ConfigurationNode.class, original);
    }

    @Test
    void setAndGetStringValue() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("hello", "key");
        assertEquals("hello", getConfigValue(config, "key"));
    }

    @Test
    void setAndGetNestedPath() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("world", "a", "b", "c");
        assertEquals("world", getConfigValue(config, "a", "b", "c"));
    }

    @Test
    void getWithDefaultReturnsDefaultWhenMissing() {
        ConfigurateConfig config = createConfig("test.yml");
        assertEquals("fallback", config.get((Object) "fallback", "missing"));
    }

    @Test
    void containsTrueWhenSet() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("value", "existing");
        assertTrue(pathExists(config, "existing"));
    }

    @Test
    void containsFalseForMissing() {
        ConfigurateConfig config = createConfig("test.yml");
        assertFalse(pathExists(config, "nonexistent"));
    }

    @Test
    void removeDeletesEntry() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("value", "toRemove");
        assertTrue(pathExists(config, "toRemove"));
        config.remove("toRemove");
        assertFalse(pathExists(config, "toRemove"));
    }

    @Test
    void clearRemovesAll() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("a", "key1");
        config.set("b", "key2");
        config.clear();
        assertFalse(pathExists(config, "key1"));
        assertFalse(pathExists(config, "key2"));
    }

    @Test
    void getNameReturnsFileName() {
        ConfigurateConfig config = createConfig("myfile.yml");
        assertEquals("myfile.yml", config.getName());
    }

    @Test
    void getValuesShallowReturnsDirectChildren() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("v1", "a", "b");
        config.set("v2", "a", "c");

        Map<String[], Object> values = config.getValues(false);
        assertEquals(1, values.size());
        assertTrue(mapContainsKey(values, "a"));
    }

    @Test
    void getValuesDeepRecurses() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("v1", "a", "b");
        config.set("v2", "a", "c");

        Map<String[], Object> values = config.getValues(true);
        assertTrue(mapContainsKey(values, "a"));
        assertTrue(mapContainsKey(values, "a", "b"));
        assertTrue(mapContainsKey(values, "a", "c"));
    }

    @Test
    void setupCreatesFileIfNotExists() {
        File file = tempDir.resolve("newfile.yml").toFile();
        assertFalse(file.exists());
        ConfigurateConfig config = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        config.setup();
        assertTrue(file.exists());
    }

    @Test
    void setupLoadsExistingContent() throws IOException {
        File file = tempDir.resolve("existing.yml").toFile();
        Files.writeString(file.toPath(), "key: value\n");
        ConfigurateConfig config = new ConfigurateConfig(file, YamlConfigurationLoader.builder());
        config.setup();
        assertEquals("value", getConfigValue(config, "key"));
    }

    @Test
    void reloadReadsLatestFromFile() throws IOException {
        ConfigurateConfig config = createConfig("reload.yml");
        config.set("original", "data");
        config.save();

        Files.writeString(tempDir.resolve("reload.yml"), "updated: fresh\n");
        config.reload();
        assertEquals("fresh", getConfigValue(config, "updated"));
        assertNull(getConfigValue(config, "original"));
    }

    @Test
    void saveWritesToFile() throws IOException {
        ConfigurateConfig config = createConfig("save.yml");
        config.set("saved", "data");
        config.save();

        File file = tempDir.resolve("save.yml").toFile();
        String content = Files.readString(file.toPath());
        assertTrue(content.contains("saved"));
        assertTrue(content.contains("data"));
    }

    @Test
    void normalizeConfigurationNodeListReturnsList() throws Exception {
        ConfigurateConfig config = createConfig("test.yml");
        ConfigurationNode root = (ConfigurationNode) config.getOriginal();
        root.node("list").appendListNode().set("a");
        root.node("list").appendListNode().set("b");

        ConfigurationNode listNode = root.node("list");
        Object normalized = config.normalize(listNode);
        assertInstanceOf(List.class, normalized);
        assertEquals(2, ((List<?>) normalized).size());
    }

    @Test
    void normalizeConfigurationNodeMapReturnsMap() throws Exception {
        ConfigurateConfig config = createConfig("test.yml");
        ConfigurationNode root = (ConfigurationNode) config.getOriginal();
        root.node("mapKey", "child1").set("val1");
        root.node("mapKey", "child2").set("val2");
        ConfigurationNode mapNode = root.node("mapKey");

        Object normalized = config.normalize(mapNode);
        assertInstanceOf(Map.class, normalized);
    }

    @Test
    void normalizeConfigurationNodeScalarReturnsRawValue() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("scalar", "testPath");
        ConfigurationNode root = (ConfigurationNode) config.getOriginal();
        ConfigurationNode scalarNode = root.node("testPath");

        Object normalized = config.normalize(scalarNode);
        assertEquals("scalar", normalized);
    }

    @Test
    void isNormalizableConfigurationNodeReturnsTrue() {
        ConfigurateConfig config = createConfig("test.yml");
        assertTrue(config.isNormalizable(config.getOriginal()));
    }

    @Test
    void isNormalizableOtherTypeReturnsFalse() {
        ConfigurateConfig config = createConfig("test.yml");
        assertFalse(config.isNormalizable("not a node"));
        assertFalse(config.isNormalizable(42));
        assertFalse(config.isNormalizable(null));
    }

    @Test
    void getCommentBlockTypeReturnsComment() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("value", "path");
        config.setComment(Arrays.asList("line1", "line2"), "path");

        List<String> comment = config.getComment(CommentType.BLOCK, "path");
        assertEquals(Arrays.asList("line1", "line2"), comment);
    }

    @Test
    void setCommentBlockTypeSetsComment() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("value", "key");
        config.setComment(CommentType.BLOCK, Arrays.asList("comment line"), "key");

        List<String> comment = config.getComment(CommentType.BLOCK, "key");
        assertEquals(1, comment.size());
        assertEquals("comment line", comment.get(0));
    }

    @Test
    void setCommentNullRemovesComment() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("value", "key");
        config.setComment(Arrays.asList("to remove"), "key");
        assertFalse(config.getComment(CommentType.BLOCK, "key").isEmpty());

        config.setComment(null, "key");
        assertTrue(config.getComment(CommentType.BLOCK, "key").isEmpty());
    }

    @Test
    void getCommentNonBlockTypeReturnsEmpty() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("value", "key");
        config.setComment(Arrays.asList("side comment"), "key");

        List<String> comment = config.getComment(CommentType.SIDE, "key");
        assertTrue(comment.isEmpty());
    }

    @Test
    void getValuesNonMapReturnsEmpty() {
        ConfigurateConfig config = createConfig("test.yml");
        config.set("scalar", "leaf");
        Map<String[], Object> values = config.getValues(false, "leaf");
        assertTrue(values.isEmpty());
    }
}
