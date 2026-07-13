package io.github.projectunified.craftconfig.annotation.converter.impl;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SimpleConverterTest {

    @Test
    void convertAppliesMapperFunction() {
        SimpleConverter converter = new SimpleConverter(raw -> ((String) raw).toUpperCase(Locale.ROOT));
        assertEquals("HELLO", converter.convert("hello"));
    }

    @Test
    void convertWithNumericMapper() {
        SimpleConverter converter = new SimpleConverter(raw -> Integer.parseInt((String) raw));
        assertEquals(42, converter.convert("42"));
    }

    @Test
    void convertNullReturnsNull() {
        SimpleConverter converter = new SimpleConverter(raw -> raw);
        assertNull(converter.convert(null));
    }

    @Test
    void convertToRawReturnsSameValue() {
        SimpleConverter converter = new SimpleConverter(raw -> "mapped");
        String value = "original";
        assertSame(value, converter.convertToRaw(value));
    }

    @Test
    void convertToRawNullReturnsNull() {
        SimpleConverter converter = new SimpleConverter(raw -> "mapped");
        assertNull(converter.convertToRaw(null));
    }

    @Test
    void roundTripConvertThenConvertToRaw() {
        SimpleConverter converter = new SimpleConverter(raw -> ((String) raw).toUpperCase(Locale.ROOT));
        String raw = "hello";
        Object converted = converter.convert(raw);
        assertEquals("HELLO", converted);
        assertSame(converted, converter.convertToRaw(converted));
    }
}
