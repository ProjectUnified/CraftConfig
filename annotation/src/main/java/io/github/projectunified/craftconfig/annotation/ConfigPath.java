package io.github.projectunified.craftconfig.annotation;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.annotation.converter.impl.DefaultConverter;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The main annotation to set the config path
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPath {
    /**
     * Get the config path
     *
     * @return the path
     */
    @NotNull String[] value();

    /**
     * Get the converter
     *
     * @return the converter
     */
    @NotNull Class<? extends Converter> converter() default DefaultConverter.class;

    /**
     * Get the priority
     *
     * @return the priority
     */
    int priority() default 0;
}
