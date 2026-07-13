package io.github.projectunified.craftconfig.common;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathStringTest {

    @Test
    void asArray() {
        String[] result = PathString.asArray("hello");
        assertArrayEquals(new String[]{"hello"}, result);
    }

    @Test
    void asArrayEmpty() {
        String[] result = PathString.asArray("");
        assertArrayEquals(new String[]{""}, result);
    }

    @Test
    void concat() {
        String[] a1 = {"a", "b"};
        String[] a2 = {"c", "d"};
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, PathString.concat(a1, a2));
    }

    @Test
    void concatFirstEmpty() {
        String[] a1 = {};
        String[] a2 = {"c", "d"};
        assertArrayEquals(new String[]{"c", "d"}, PathString.concat(a1, a2));
    }

    @Test
    void concatSecondEmpty() {
        String[] a1 = {"a", "b"};
        String[] a2 = {};
        assertArrayEquals(new String[]{"a", "b"}, PathString.concat(a1, a2));
    }

    @Test
    void concatBothEmpty() {
        String[] a1 = {};
        String[] a2 = {};
        assertArrayEquals(new String[]{}, PathString.concat(a1, a2));
    }

    @Test
    void join() {
        assertEquals("a.b.c", PathString.join(".", "a", "b", "c"));
    }

    @Test
    void joinSingle() {
        assertEquals("hello", PathString.join(".", "hello"));
    }

    @Test
    void joinEmpty() {
        assertEquals("", PathString.join("."));
    }

    @Test
    void joinCustomSeparator() {
        assertEquals("a/b/c", PathString.join("/", "a", "b", "c"));
    }

    @Test
    void joinRegexSpecialSeparator() {
        assertEquals("a+b+c", PathString.join("+", "a", "b", "c"));
    }

    @Test
    void joinDotSeparator() {
        assertEquals("a.b.c", PathString.join(".", "a", "b", "c"));
    }

    @Test
    void split() {
        assertArrayEquals(new String[]{"a", "b", "c"}, PathString.split(".", "a.b.c"));
    }

    @Test
    void splitSingle() {
        assertArrayEquals(new String[]{"hello"}, PathString.split(".", "hello"));
    }

    @Test
    void splitRegexSpecialSeparator() {
        assertArrayEquals(new String[]{"a", "b", "c"}, PathString.split("+", "a+b+c"));
    }

    @Test
    void splitDotSeparator() {
        assertArrayEquals(new String[]{"a", "b", "c"}, PathString.split(".", "a.b.c"));
    }

    @Test
    void joinDefault() {
        assertEquals("a.b.c", PathString.joinDefault("a", "b", "c"));
    }

    @Test
    void splitDefault() {
        assertArrayEquals(new String[]{"a", "b", "c"}, PathString.splitDefault("a.b.c"));
    }

    @Test
    void joinMap() {
        Map<String[], Object> input = new LinkedHashMap<>();
        input.put(new String[]{"a", "b"}, 1);
        input.put(new String[]{"c", "d"}, 2);

        Map<String, Object> result = PathString.join(".", input);

        assertEquals(2, result.size());
        assertEquals(1, result.get("a.b"));
        assertEquals(2, result.get("c.d"));
    }

    @Test
    void joinMapPreservesOrder() {
        Map<String[], Object> input = new LinkedHashMap<>();
        input.put(new String[]{"x"}, 10);
        input.put(new String[]{"y"}, 20);
        input.put(new String[]{"z"}, 30);

        Map<String, Object> result = PathString.join("/", input);

        String[] keys = result.keySet().toArray(new String[0]);
        assertArrayEquals(new String[]{"x", "y", "z"}, keys);
    }

    @Test
    void splitMap() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a.b", 1);
        input.put("c.d", 2);

        Map<String[], Object> result = PathString.split(".", input);

        assertEquals(2, result.size());
        Iterator<Map.Entry<String[], Object>> it = result.entrySet().iterator();
        Map.Entry<String[], Object> first = it.next();
        assertArrayEquals(new String[]{"a", "b"}, first.getKey());
        assertEquals(1, first.getValue());
        Map.Entry<String[], Object> second = it.next();
        assertArrayEquals(new String[]{"c", "d"}, second.getKey());
        assertEquals(2, second.getValue());
    }

    @Test
    void splitMapPreservesOrder() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("x", 10);
        input.put("y", 20);
        input.put("z", 30);

        Map<String[], Object> result = PathString.split(".", input);

        String[][] keys = result.keySet().toArray(new String[0][]);
        assertArrayEquals(new String[]{"x"}, keys[0]);
        assertArrayEquals(new String[]{"y"}, keys[1]);
        assertArrayEquals(new String[]{"z"}, keys[2]);
    }

    @Test
    void joinDefaultMap() {
        Map<String[], Object> input = new LinkedHashMap<>();
        input.put(new String[]{"a", "b"}, "val1");

        Map<String, Object> result = PathString.joinDefault(input);

        assertEquals("val1", result.get("a.b"));
    }

    @Test
    void splitDefaultMap() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a.b", "val1");

        Map<String[], Object> result = PathString.splitDefault(input);

        assertArrayEquals(new String[]{"a", "b"}, result.keySet().iterator().next());
    }

    @Test
    void joinRoundTrip() {
        String[] original = {"a", "b", "c"};
        String joined = PathString.join(".", original);
        String[] split = PathString.split(".", joined);
        assertArrayEquals(original, split);
    }

    @Test
    void joinMapRoundTrip() {
        Map<String[], Object> original = new LinkedHashMap<>();
        original.put(new String[]{"a", "b"}, 1);
        original.put(new String[]{"c"}, "hello");

        Map<String, Object> joined = PathString.join(".", original);
        Map<String[], Object> split = PathString.split(".", joined);

        assertEquals(original.size(), split.size());
        Iterator<Map.Entry<String[], Object>> it = split.entrySet().iterator();
        Map.Entry<String[], Object> first = it.next();
        assertArrayEquals(new String[]{"a", "b"}, first.getKey());
        assertEquals(1, first.getValue());
        Map.Entry<String[], Object> second = it.next();
        assertArrayEquals(new String[]{"c"}, second.getKey());
        assertEquals("hello", second.getValue());
    }

    @Test
    void splitEmptyMap() {
        Map<String, Object> input = new LinkedHashMap<>();
        Map<String[], Object> result = PathString.split(".", input);
        assertTrue(result.isEmpty());
    }

    @Test
    void joinEmptyMap() {
        Map<String[], Object> input = new LinkedHashMap<>();
        Map<String, Object> result = PathString.join(".", input);
        assertTrue(result.isEmpty());
    }
}
