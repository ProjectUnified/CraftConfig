package io.github.projectunified.craftconfig.annotation.converter.impl;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EnumConverterProviderTest {

    private final EnumConverterProvider provider = new EnumConverterProvider();

    @Test
    void getConverterReturnsPresentForEnum() {
        Optional<Converter> opt = provider.getConverter(TestColor.class);
        assertTrue(opt.isPresent());
    }

    @Test
    void convertUppercaseStringToEnum() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertEquals(TestColor.RED, converter.convert("RED"));
    }

    @Test
    void convertLowercaseStringToEnum() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertEquals(TestColor.GREEN, converter.convert("green"));
    }

    @Test
    void convertMixedCaseStringToEnum() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertEquals(TestColor.BLUE, converter.convert("Blue"));
    }

    @Test
    void convertNullReturnsNull() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertNull(converter.convert(null));
    }

    @Test
    void convertInvalidValueReturnsNull() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertNull(converter.convert("YELLOW"));
    }

    @Test
    void convertToRawEnumReturnsName() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertEquals("RED", converter.convertToRaw(TestColor.RED));
    }

    @Test
    void convertToRawNullReturnsNull() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertNull(converter.convertToRaw(null));
    }

    @Test
    void convertToRawNonEnumReturnsNull() {
        Converter converter = provider.getConverter(TestColor.class).get();
        assertNull(converter.convertToRaw("not an enum"));
    }

    @Test
    void nonEnumTypeReturnsEmptyOptional() {
        Optional<Converter> opt = provider.getConverter(String.class);
        assertTrue(opt.isEmpty());
    }

    @Test
    void nonEnumTypeReturnsEmptyForPrimitive() {
        Optional<Converter> opt = provider.getConverter(int.class);
        assertTrue(opt.isEmpty());
    }

    @Test
    void roundTripConversion() {
        Converter converter = provider.getConverter(TestPriority.class).get();
        Object raw = converter.convertToRaw(TestPriority.HIGH);
        assertEquals("HIGH", raw);
        assertEquals(TestPriority.HIGH, converter.convert(raw));
    }

    enum TestColor {
        RED, GREEN, BLUE
    }

    enum TestPriority {
        LOW, MEDIUM, HIGH
    }
}
