package io.github.projectunified.craftconfig.annotated;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigPath;
import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.annotation.converter.manager.DefaultConverterManager;
import io.github.projectunified.craftconfig.common.Config;
import io.github.projectunified.craftconfig.common.ConfigLogger;
import io.github.projectunified.craftconfig.common.DecorativeConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * The annotated {@link Config}, where any fields can be assigned to the config with the annotation {@link ConfigPath}.
 * If the field is a final non-static, don't set default value directly, use constructor instead.
 * <pre>
 * public class ExampleConfig extends AnnotatedConfig {
 *   &#64;ConfigPath("test1.path")
 *   private final String test1 = "test1"; // If the field is final, Don't do this
 *   &#64;ConfigPath("test2.path")
 *   private final String test2; // Do this instead
 *
 *   public ExampleConfig(Config config) {
 *     super(config);
 *     test2 = "test2"; // Set default value via the constructor
 *   }
 * }
 * </pre>
 */
public class AnnotatedConfig extends DecorativeConfig {
    private final Map<PathKey, Field> pathFieldMap = new HashMap<>();

    /**
     * Create an annotated config
     *
     * @param config the original config
     */
    public AnnotatedConfig(Config config) {
        super(config);
    }

    private Field getField(String... path) {
        return pathFieldMap.get(new PathKey(path));
    }

    private void setField(Field field, String... path) {
        pathFieldMap.put(new PathKey(path), field);
    }

    private boolean containsField(String... path) {
        return pathFieldMap.containsKey(new PathKey(path));
    }

    @Override
    public void setup() {
        super.setup();
        List<Field> validFields = new ArrayList<>();
        Arrays.stream(this.getClass().getDeclaredFields())
                .filter(this::checkPathField)
                .sorted(this::compareField)
                .forEach(field -> {
                    ConfigPath configPath = field.getAnnotation(ConfigPath.class);
                    setField(field, configPath.value());
                    validFields.add(field);
                });
        validFields.forEach(this::setupField);
        setupClassComment();
        this.save();
    }

    @Override
    public void set(Object value, String... path) {
        Field field = getField(path);
        if (field == null) {
            super.set(value, path);
            return;
        }
        checkAndSetField(field, value);
    }

    @Override
    public void reload() {
        super.reload();
        for (Field field : pathFieldMap.values()) {
            setupField(field);
        }
        setupClassComment();
        this.save();
    }

    private void setupClassComment() {
        if (this.getClass().isAnnotationPresent(Comment.class) && this.getComment().isEmpty()) {
            this.setComment(Arrays.asList(this.getClass().getAnnotation(Comment.class).value()));
        }
    }

    private boolean checkPathField(Field field) {
        ConfigPath configPath = field.getAnnotation(ConfigPath.class);
        if (configPath == null) {
            return false;
        }
        if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
            ConfigLogger.warn(AnnotatedConfig.class, field.getName() + " is a static final field. Ignored");
            return false;
        }
        return true;
    }

    /**
     * Compare the fields
     *
     * @param field1 the first field
     * @param field2 the second field
     * @return the comparison result
     */
    private int compareField(Field field1, Field field2) {
        ConfigPath configPath1 = field1.getAnnotation(ConfigPath.class);
        ConfigPath configPath2 = field2.getAnnotation(ConfigPath.class);
        return Integer.compare(configPath1.priority(), configPath2.priority());
    }

    private void setupField(Field field) {
        ConfigPath configPath = field.getAnnotation(ConfigPath.class);
        String[] path = configPath.value();
        Converter converter = DefaultConverterManager.getConverterIfDefault(field.getGenericType(), configPath.converter());
        Object defaultValue = this.getValue(field);

        if (!contains(path)) {
            super.set(converter.convertToRaw(defaultValue), path);
            if (field.isAnnotationPresent(Comment.class)) {
                super.setComment(Arrays.asList(field.getAnnotation(Comment.class).value()), path);
            }
        } else {
            this.setValue(field, converter.convert(this.getNormalized(path)));
        }
    }

    private void checkAndSetField(Field field, Object value) {
        ConfigPath configPath = field.getAnnotation(ConfigPath.class);
        String[] path = configPath.value();
        Converter converter = DefaultConverterManager.getConverterIfDefault(field.getGenericType(), configPath.converter());
        super.set(converter.convertToRaw(value), path);
        this.setValue(field, value);
    }

    private void setValue(Field field, Object value) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        try {
            field.set(this, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set the value for " + field.getName(), e);
        } finally {
            field.setAccessible(accessible);
        }
    }

    private Object getValue(Field field) {
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        Object value;
        try {
            value = field.get(this);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot get the value for " + field.getName(), e);
        } finally {
            field.setAccessible(accessible);
        }
        return value;
    }

    private static class PathKey {
        private final String[] path;

        private PathKey(String[] path) {
            this.path = path;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            PathKey that = (PathKey) object;
            return Objects.deepEquals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(path);
        }
    }
}
