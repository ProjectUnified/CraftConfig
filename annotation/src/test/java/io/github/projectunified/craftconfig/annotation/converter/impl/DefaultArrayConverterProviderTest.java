package io.github.projectunified.craftconfig.annotation.converter.impl;

import io.github.projectunified.craftconfig.annotation.converter.Converter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultArrayConverterProviderTest {

    private final DefaultArrayConverterProvider provider = new DefaultArrayConverterProvider();

    @Test
    void testIntArrayConversion() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();

        Object result = converter.convert(new int[]{1, 2, 3});
        assertTrue(result.getClass().isArray());
        assertArrayEquals(new int[]{1, 2, 3}, (int[]) result);
    }

    @Test
    void testIntArrayConversionWithStrings() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();

        Object result = converter.convert(new Object[]{"10", "20", "30"});
        assertArrayEquals(new int[]{10, 20, 30}, (int[]) result);
    }

    @Test
    void testCollectionToArrayConversion() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();

        List<Object> collection = Arrays.asList("5", "15", "25");
        Object result = converter.convert(collection);
        assertTrue(result.getClass().isArray());
        assertArrayEquals(new int[]{5, 15, 25}, (int[]) result);
    }

    @Test
    void testConvertNullReturnsNull() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();
        assertNull(converter.convert(null));
    }

    @Test
    void testNonArrayTypeReturnsEmptyOptional() {
        Optional<Converter> opt = provider.getConverter(String.class);
        assertTrue(opt.isEmpty());
    }

    @Test
    void testPrimitiveArrayReturnsEmptyForInt() {
        Optional<Converter> opt = provider.getConverter(int.class);
        assertTrue(opt.isEmpty());
    }

    @Test
    void testStringArrayConversion() {
        Optional<Converter> opt = provider.getConverter(String[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();

        Object result = converter.convert(new Object[]{"a", "b", "c"});
        assertTrue(result.getClass().isArray());
        String[] resultArray = (String[]) result;
        assertEquals(3, resultArray.length);
        assertEquals("a", resultArray[0]);
        assertEquals("b", resultArray[1]);
        assertEquals("c", resultArray[2]);
    }

    @Test
    void testConvertToRawIntArray() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();

        Object raw = converter.convertToRaw(new int[]{1, 2, 3});
        assertTrue(raw.getClass().isArray());
        Object[] rawArray = (Object[]) raw;
        assertEquals(3, rawArray.length);
        assertEquals(1, rawArray[0]);
        assertEquals(2, rawArray[1]);
        assertEquals(3, rawArray[2]);
    }

    @Test
    void testConvertToRawNullReturnsNull() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();
        assertNull(converter.convertToRaw(null));
    }

    @Test
    void testConvertToRawNonArrayReturnsNull() {
        Optional<Converter> opt = provider.getConverter(int[].class);
        assertTrue(opt.isPresent());
        Converter converter = opt.get();
        assertNull(converter.convertToRaw("not an array"));
    }
}
