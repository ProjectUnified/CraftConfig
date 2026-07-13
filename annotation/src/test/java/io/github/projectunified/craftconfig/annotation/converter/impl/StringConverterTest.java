package io.github.projectunified.craftconfig.annotation.converter.impl;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringConverterTest {

    @Test
    void ofCreatesWorkingConverter() {
        StringConverter<Integer> converter = StringConverter.of(Integer::parseInt, Object::toString);
        assertEquals(123, converter.convert("123"));
        assertEquals("456", converter.convertToRaw(456));
    }

    @Test
    void convertWithValidInput() {
        StringConverter<String> converter = StringConverter.of(
                s -> s.toUpperCase(Locale.ROOT),
                s -> s.toLowerCase(Locale.ROOT)
        );
        assertEquals("HELLO", converter.convert("hello"));
    }

    @Test
    void convertNullReturnsNull() {
        StringConverter<Integer> converter = StringConverter.of(Integer::parseInt, Object::toString);
        assertNull(converter.convert(null));
    }

    @Test
    void convertInvalidStringReturnsNull() {
        StringConverter<Integer> converter = StringConverter.of(Integer::parseInt, Object::toString);
        assertNull(converter.convert("not_a_number"));
    }

    @Test
    void convertToRawWithNullReturnsNull() {
        StringConverter<Integer> converter = StringConverter.of(Integer::parseInt, Object::toString);
        assertNull(converter.convertToRaw(null));
    }

    @Test
    void convertToRawWithInvalidValueReturnsNull() {
        StringConverter<String> converter = StringConverter.of(
                s -> {
                    throw new RuntimeException("fail");
                },
                Object::toString
        );
        assertNull(converter.convertToRaw(123));
    }

    @Test
    void roundTripConversion() {
        StringConverter<Integer> converter = StringConverter.of(Integer::parseInt, Object::toString);
        Integer original = 42;
        Object raw = converter.convertToRaw(original);
        assertEquals("42", raw);
        Object restored = converter.convert(raw);
        assertEquals(42, restored);
    }
}
