package io.github.projectunified.craftconfig.annotation.converter.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultConverterTest {

    private final DefaultConverter converter = new DefaultConverter();

    @Test
    void convertReturnsSameObject() {
        String input = "hello";
        assertSame(input, converter.convert(input));
    }

    @Test
    void convertWithIntReturnsSameValue() {
        Integer input = 42;
        assertSame(input, converter.convert(input));
    }

    @Test
    void convertNullReturnsNull() {
        assertNull(converter.convert(null));
    }

    @Test
    void convertToRawReturnsSameObject() {
        String input = "hello";
        assertSame(input, converter.convertToRaw(input));
    }

    @Test
    void convertToRawNullReturnsNull() {
        assertNull(converter.convertToRaw(null));
    }

    @Test
    void convertToRawWithIntReturnsSameValue() {
        Integer input = 100;
        assertSame(input, converter.convertToRaw(input));
    }
}
