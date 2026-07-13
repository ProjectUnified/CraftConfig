package io.github.projectunified.craftconfig.annotation.converter.impl;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PrimitiveConverterProviderTest {

    private final PrimitiveConverterProvider provider = new PrimitiveConverterProvider();

    @Test
    void testBoolean() {
        Optional<Converter> opt = provider.getConverter(boolean.class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();
        assertEquals(true, converter.convert("true"));
        assertEquals(false, converter.convert("false"));
        assertEquals(false, converter.convert("anything"));
    }

    @Test
    void testByte() {
        Optional<Converter> opt = provider.getConverter(byte.class);
        assertTrue(opt.isPresent());
        assertEquals((byte) 127, provider.getConverter(byte.class).get().convert("127"));
    }

    @Test
    void testShort() {
        Optional<Converter> opt = provider.getConverter(short.class);
        assertTrue(opt.isPresent());
        assertEquals((short) 1000, provider.getConverter(short.class).get().convert("1000"));
    }

    @Test
    void testInt() {
        Optional<Converter> opt = provider.getConverter(int.class);
        assertTrue(opt.isPresent());
        assertEquals(42, provider.getConverter(int.class).get().convert("42"));
    }

    @Test
    void testLong() {
        Optional<Converter> opt = provider.getConverter(long.class);
        assertTrue(opt.isPresent());
        assertEquals(9999999999L, provider.getConverter(long.class).get().convert("9999999999"));
    }

    @Test
    void testFloat() {
        Optional<Converter> opt = provider.getConverter(float.class);
        assertTrue(opt.isPresent());
        assertEquals(3.14f, provider.getConverter(float.class).get().convert("3.14"));
    }

    @Test
    void testDouble() {
        Optional<Converter> opt = provider.getConverter(double.class);
        assertTrue(opt.isPresent());
        assertEquals(2.71828, provider.getConverter(double.class).get().convert("2.71828"));
    }

    @Test
    void testChar() {
        Optional<Converter> opt = provider.getConverter(char.class);
        assertTrue(opt.isPresent());
        assertEquals('A', provider.getConverter(char.class).get().convert("A"));
    }

    @Test
    void testBoxedBoolean() {
        Optional<Converter> opt = provider.getConverter(Boolean.class);
        assertTrue(opt.isPresent());
        assertEquals(true, opt.get().convert("true"));
    }

    @Test
    void testBoxedInteger() {
        Optional<Converter> opt = provider.getConverter(Integer.class);
        assertTrue(opt.isPresent());
        assertEquals(100, opt.get().convert("100"));
    }

    @Test
    void testBoxedDouble() {
        Optional<Converter> opt = provider.getConverter(Double.class);
        assertTrue(opt.isPresent());
        assertEquals(1.5, opt.get().convert("1.5"));
    }

    @Test
    void testInvalidIntInputReturnsNull() {
        Converter converter = provider.getConverter(int.class).get();
        assertNull(converter.convert("not_a_number"));
    }

    @Test
    void testInvalidByteInputReturnsNull() {
        Converter converter = provider.getConverter(byte.class).get();
        assertNull(converter.convert("99999"));
    }

    @Test
    void testCharTooLongReturnsNull() {
        Converter converter = provider.getConverter(char.class).get();
        assertNull(converter.convert("AB"));
    }

    @Test
    void testUnsupportedTypeReturnsEmpty() {
        Optional<Converter> opt = provider.getConverter(String.class);
        assertTrue(opt.isEmpty());
    }

    @Test
    void testUnsupportedTypeReturnsEmptyForCustomClass() {
        Optional<Converter> opt = provider.getConverter(Object.class);
        assertTrue(opt.isEmpty());
    }
}
