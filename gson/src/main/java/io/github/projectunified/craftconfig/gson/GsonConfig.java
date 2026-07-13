package io.github.projectunified.craftconfig.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigLogger;
import io.github.projectunified.craftconfig.common.ConfigNode;
import io.github.projectunified.craftconfig.common.PathString;
import io.github.projectunified.craftconfig.gson.util.GsonUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The {@link Config} implementation for Gson
 */
public class GsonConfig implements Config {
    private final Gson gson;
    private final File file;
    private JsonObject root = new JsonObject();

    public GsonConfig(File file, Gson gson) {
        this.file = file;
        this.gson = gson;
    }

    public GsonConfig(File file) {
        this(file, new Gson());
    }

    private Object getValueAt(String... absolutePath) {
        if (absolutePath.length == 0) {
            return this.root;
        }
        JsonObject parent = navigateToParent(absolutePath);
        if (parent == null) {
            return null;
        }
        String lastKey = absolutePath[absolutePath.length - 1];
        if (!parent.has(lastKey)) {
            return null;
        }
        return parent.get(lastKey);
    }

    private void setValueAt(Object value, String... absolutePath) {
        if (absolutePath.length == 0) {
            if (value instanceof JsonObject) {
                this.root = (JsonObject) value;
            }
            return;
        }
        JsonObject parent = navigateToParentOrCreate(absolutePath);
        if (parent == null) {
            return;
        }
        String lastKey = absolutePath[absolutePath.length - 1];
        if (value == null) {
            parent.remove(lastKey);
        } else {
            JsonElement element = value instanceof JsonElement ? (JsonElement) value : gson.toJsonTree(value);
            parent.add(lastKey, element);
        }
    }

    private JsonObject navigateToParent(String[] absolutePath) {
        JsonObject currentObject = this.root;
        for (int i = 0; i < absolutePath.length - 1; i++) {
            String key = absolutePath[i];
            JsonElement element = currentObject.has(key) ? currentObject.get(key) : JsonNull.INSTANCE;
            if (element.isJsonObject()) {
                currentObject = element.getAsJsonObject();
            } else {
                return null;
            }
        }
        return currentObject;
    }

    private JsonObject navigateToParentOrCreate(String[] absolutePath) {
        JsonObject currentObject = this.root;
        for (int i = 0; i < absolutePath.length - 1; i++) {
            String key = absolutePath[i];
            JsonElement element = currentObject.has(key) ? currentObject.get(key) : JsonNull.INSTANCE;
            if (element.isJsonObject()) {
                currentObject = element.getAsJsonObject();
            } else {
                JsonObject newObject = new JsonObject();
                currentObject.add(key, newObject);
                currentObject = newObject;
            }
        }
        return currentObject;
    }

    @Override
    public Object get() {
        return this.root;
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
        return new GsonConfigNode(path, this, path);
    }

    @Override
    public void remove() {
        this.root = new JsonObject();
    }

    @Override
    public Map<String, ConfigNode> getChildren() {
        Map<String, ConfigNode> nodes = new LinkedHashMap<>();
        for (String key : this.root.keySet()) {
            nodes.put(key, new GsonConfigNode(new String[]{key}, this, new String[]{key}));
        }
        return nodes;
    }

    @Override
    public String getName() {
        return file.getName();
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
                ConfigLogger.warn(GsonConfig.class, "Something wrong when creating " + file.getName(), e);
            }
        }

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonElement jsonElement = GsonUtil.parse(reader);
            if (jsonElement.isJsonObject()) {
                this.root = jsonElement.getAsJsonObject();
            }
        } catch (IOException e) {
            ConfigLogger.warn(GsonConfig.class, "Something wrong when loading " + file.getName(), e);
        }
    }

    @Override
    public void save() {
        try (
                OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
                JsonWriter jsonWriter = gson.newJsonWriter(writer)
        ) {
            gson.toJson(this.root, jsonWriter);
        } catch (IOException e) {
            ConfigLogger.warn(GsonConfig.class, "Something wrong when saving " + file.getName(), e);
        }
    }

    @Override
    public void reload() {
        this.root = new JsonObject();
        this.setup();
    }

    @Override
    public Object getOriginal() {
        return this.root;
    }

    @Override
    public Object normalize(Object object) {
        if (!isNormalizable(object)) {
            return object;
        }
        return GsonUtil.normalize((JsonElement) object, false);
    }

    @Override
    public boolean isNormalizable(Object object) {
        return object instanceof JsonElement;
    }

    /**
     * Gson implementation of {@link ConfigNode}.
     */
    public class GsonConfigNode implements ConfigNode {
        private final String[] path;
        private final ConfigNode parent;
        private final String[] absolutePath;

        GsonConfigNode(String[] path, ConfigNode parent, String[] absolutePath) {
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
            return GsonConfig.this;
        }

        @Override
        public Object get() {
            return GsonConfig.this.getValueAt(absolutePath);
        }

        @Override
        public void set(Object value) {
            GsonConfig.this.setValueAt(value, absolutePath);
        }

        @Override
        public ConfigNode node(String... path) {
            if (path.length == 0) {
                return this;
            }
            String[] childAbsolutePath = PathString.concat(this.absolutePath, path);
            return new GsonConfigNode(path, this, childAbsolutePath);
        }

        @Override
        public void remove() {
            GsonConfig.this.setValueAt(null, absolutePath);
        }

        @Override
        public boolean hasChild() {
            Object value = get();
            return value instanceof JsonObject;
        }

        @Override
        public Map<String, ConfigNode> getChildren() {
            Object value = get();
            if (!(value instanceof JsonObject)) {
                throw new IllegalStateException("The node is not a JSON object");
            }
            JsonObject object = (JsonObject) value;
            Map<String, ConfigNode> nodes = new LinkedHashMap<>();
            for (String key : object.keySet()) {
                String[] childAbsolutePath = PathString.concat(absolutePath, new String[]{key});
                nodes.put(key, new GsonConfigNode(new String[]{key}, this, childAbsolutePath));
            }
            return nodes;
        }

        @Override
        public Object getNormalized() {
            Object value = get();
            return GsonConfig.this.normalize(value);
        }
    }
}
