package io.github.projectunified.craftconfig.annotation.converter.manager;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import io.github.projectunified.craftconfig.annotation.converter.impl.DefaultConverter;
import io.github.projectunified.craftconfig.annotation.converter.impl.SimpleConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class DefaultConverterManagerTest {

    @BeforeEach
    void setUp() {
        DefaultConverterManager.unregisterConverter(TestEnum.class);
        DefaultConverterManager.unregisterConverter(TestCustomType.class);
    }

    @Test
    void getConverterForStringReturnsConverter() {
        Converter converter = DefaultConverterManager.getConverter(String.class);
        assertNotNull(converter);
        assertEquals("hello", converter.convert("hello"));
    }

    @Test
    void getConverterForIntReturnsConverter() {
        Converter converter = DefaultConverterManager.getConverter(int.class);
        assertNotNull(converter);
        assertEquals(42, converter.convert("42"));
    }

    @Test
    void getConverterForEnumReturnsConverter() {
        Converter converter = DefaultConverterManager.getConverter(TestEnum.class);
        assertNotNull(converter);
        assertEquals(TestEnum.A, converter.convert("a"));
    }

    @Test
    void getConverterForUriReturnsConverter() {
        Converter converter = DefaultConverterManager.getConverter(URI.class);
        assertNotNull(converter);
        URI uri = (URI) converter.convert("https://example.com");
        assertNotNull(uri);
        assertEquals("https://example.com", uri.toString());
    }

    @Test
    void getConverterIfDefaultWithDefaultConverterResolvesViaProviders() {
        DefaultConverter defaultConverter = new DefaultConverter();
        Converter resolved = DefaultConverterManager.getConverterIfDefault(int.class, defaultConverter);
        assertNotNull(resolved);
        assertNotEquals(defaultConverter, resolved);
        assertEquals(100, resolved.convert("100"));
    }

    @Test
    void getConverterIfDefaultWithCustomConverterReturnsCustom() {
        SimpleConverter customConverter = new SimpleConverter(raw -> "custom");
        Converter result = DefaultConverterManager.getConverterIfDefault(TestCustomType.class, customConverter);
        assertSame(customConverter, result);
        assertEquals("custom", result.convert("anything"));
    }

    @Test
    void registerConverterOverridesExisting() {
        SimpleConverter first = new SimpleConverter(raw -> "first");
        SimpleConverter second = new SimpleConverter(raw -> "second");

        DefaultConverterManager.registerConverter(TestCustomType.class, first);
        assertEquals("first", DefaultConverterManager.getConverter(TestCustomType.class).convert("x"));

        DefaultConverterManager.registerConverter(TestCustomType.class, second);
        assertEquals("second", DefaultConverterManager.getConverter(TestCustomType.class).convert("x"));
    }

    @Test
    void unregisterConverterRemovesConverter() {
        DefaultConverterManager.registerConverter(TestCustomType.class, new SimpleConverter(raw -> "exists"));
        assertNotNull(DefaultConverterManager.getConverter(TestCustomType.class));

        DefaultConverterManager.unregisterConverter(TestCustomType.class);
        Converter converter = DefaultConverterManager.getConverter(TestCustomType.class);
        assertInstanceOf(DefaultConverter.class, converter);
    }

    @Test
    void getConverterForStringReturnsSameConverterInstance() {
        Converter first = DefaultConverterManager.getConverter(String.class);
        Converter second = DefaultConverterManager.getConverter(String.class);
        assertSame(first, second);
    }

    @Test
    void getConverterForBooleanReturnsPrimitiveConverter() {
        Converter converter = DefaultConverterManager.getConverter(boolean.class);
        assertNotNull(converter);
        assertEquals(true, converter.convert("true"));
        assertEquals(false, converter.convert("false"));
    }

    @Test
    void getConverterForDoubleReturnsPrimitiveConverter() {
        Converter converter = DefaultConverterManager.getConverter(double.class);
        assertNotNull(converter);
        assertEquals(3.14, converter.convert("3.14"));
    }

    enum TestEnum {
        A, B, C
    }

    static class TestCustomType {
    }
}
