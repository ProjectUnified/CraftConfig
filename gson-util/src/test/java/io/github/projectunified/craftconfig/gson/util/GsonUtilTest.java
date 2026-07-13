package io.github.projectunified.craftconfig.gson.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GsonUtilTest {

    // ==================== parse(String) ====================

    @Test
    void parseString_validJson_returnsJsonObject() {
        JsonElement result = GsonUtil.parse("{\"key\": \"value\"}");
        assertTrue(result.isJsonObject());
        assertEquals("value", result.getAsJsonObject().get("key").getAsString());
    }

    @Test
    void parseString_invalidJson_throwsException() {
        assertThrows(Exception.class, () -> GsonUtil.parse("{invalid json}"));
    }

    @Test
    void parseString_emptyObject_returnsEmptyJsonObject() {
        JsonElement result = GsonUtil.parse("{}");
        assertTrue(result.isJsonObject());
        assertTrue(result.getAsJsonObject().isEmpty());
    }

    @Test
    void parseString_jsonArray_returnsJsonArray() {
        JsonElement result = GsonUtil.parse("[1, 2, 3]");
        assertTrue(result.isJsonArray());
        JsonArray array = result.getAsJsonArray();
        assertEquals(3, array.size());
    }

    // ==================== parse(Reader) ====================

    @Test
    void parseReader_validJson_returnsElement() {
        JsonElement result = GsonUtil.parse(new StringReader("{\"a\": 1}"));
        assertTrue(result.isJsonObject());
        assertEquals(1, result.getAsJsonObject().get("a").getAsInt());
    }

    // ==================== getElements(JsonArray) ====================

    @Test
    void getElements_array_returnsList() {
        JsonArray array = GsonUtil.parse("[1, 2, 3]").getAsJsonArray();
        List<JsonElement> elements = GsonUtil.getElements(array);
        assertEquals(3, elements.size());
        assertEquals(1, elements.get(0).getAsInt());
        assertEquals(2, elements.get(1).getAsInt());
        assertEquals(3, elements.get(2).getAsInt());
    }

    @Test
    void getElements_emptyArray_returnsEmptyList() {
        JsonArray array = GsonUtil.parse("[]").getAsJsonArray();
        List<JsonElement> elements = GsonUtil.getElements(array);
        assertTrue(elements.isEmpty());
    }

    // ==================== getElements(JsonObject) ====================

    @Test
    void getElements_object_returnsMap() {
        JsonObject object = GsonUtil.parse("{\"a\": 1, \"b\": 2}").getAsJsonObject();
        Map<String, JsonElement> elements = GsonUtil.getElements(object);
        assertEquals(2, elements.size());
        assertEquals(1, elements.get("a").getAsInt());
        assertEquals(2, elements.get("b").getAsInt());
    }

    @Test
    void getElements_emptyObject_returnsEmptyMap() {
        JsonObject object = GsonUtil.parse("{}").getAsJsonObject();
        Map<String, JsonElement> elements = GsonUtil.getElements(object);
        assertTrue(elements.isEmpty());
    }

    // ==================== isEmpty ====================

    @Test
    void isEmpty_nullArray_returnsTrue() {
        assertTrue(GsonUtil.isEmpty(null));
    }

    @Test
    void isEmpty_emptyArray_returnsTrue() {
        JsonArray array = GsonUtil.parse("[]").getAsJsonArray();
        assertTrue(GsonUtil.isEmpty(array));
    }

    @Test
    void isEmpty_nonEmptyArray_returnsFalse() {
        JsonArray array = GsonUtil.parse("[1]").getAsJsonArray();
        assertFalse(GsonUtil.isEmpty(array));
    }

    // ==================== normalize - primitives ====================

    @Test
    void normalize_primitiveString_returnsString() {
        JsonElement element = GsonUtil.parse("\"hello\"");
        Object result = GsonUtil.normalize(element, false);
        assertEquals("hello", result);
    }

    @Test
    void normalize_primitiveBoolean_returnsBoolean() {
        JsonElement element = GsonUtil.parse("true");
        Object result = GsonUtil.normalize(element, false);
        assertEquals(true, result);
    }

    @Test
    void normalize_primitiveNumber_returnsNumber() {
        JsonElement element = GsonUtil.parse("42");
        Object result = GsonUtil.normalize(element, false);
        assertInstanceOf(Number.class, result);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    void normalize_primitiveDecimal_returnsNumber() {
        JsonElement element = GsonUtil.parse("3.14");
        Object result = GsonUtil.normalize(element, false);
        assertInstanceOf(Number.class, result);
        assertEquals(3.14, ((Number) result).doubleValue(), 0.001);
    }

    // ==================== normalize - JsonNull ====================

    @Test
    void normalize_jsonNull_returnsNull() {
        Object result = GsonUtil.normalize(JsonNull.INSTANCE, false);
        assertNull(result);
    }

    // ==================== normalize - JsonArray shallow ====================

    @Test
    void normalize_arrayShallow_returnsListOfJsonElements() {
        JsonElement element = GsonUtil.parse("[1, 2, 3]");
        Object result = GsonUtil.normalize(element, false);
        assertInstanceOf(List.class, result);
        @SuppressWarnings("unchecked")
        List<JsonElement> list = (List<JsonElement>) result;
        assertEquals(3, list.size());
        assertTrue(list.get(0).isJsonPrimitive());
    }

    // ==================== normalize - JsonArray deep ====================

    @Test
    void normalize_arrayDeep_returnsListOfNormalizedObjects() {
        JsonElement element = GsonUtil.parse("[1, \"two\", true]");
        Object result = GsonUtil.normalize(element, true);
        assertInstanceOf(List.class, result);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) result;
        assertEquals(3, list.size());
        assertEquals(1, ((Number) list.get(0)).intValue());
        assertEquals("two", list.get(1));
        assertEquals(true, list.get(2));
    }

    @Test
    void normalize_arrayDeep_nestedArray_returnsNestedList() {
        JsonElement element = GsonUtil.parse("[[1, 2], [3, 4]]");
        Object result = GsonUtil.normalize(element, true);
        assertInstanceOf(List.class, result);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) result;
        assertEquals(2, list.size());
        assertInstanceOf(List.class, list.get(0));
        @SuppressWarnings("unchecked")
        List<Object> inner = (List<Object>) list.get(0);
        assertEquals(2, inner.size());
        assertEquals(1, ((Number) inner.get(0)).intValue());
    }

    // ==================== normalize - JsonObject shallow ====================

    @Test
    void normalize_objectShallow_returnsMapOfJsonElements() {
        JsonElement element = GsonUtil.parse("{\"a\": 1, \"b\": 2}");
        Object result = GsonUtil.normalize(element, false);
        assertInstanceOf(Map.class, result);
        @SuppressWarnings("unchecked")
        Map<String, JsonElement> map = (Map<String, JsonElement>) result;
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").getAsInt());
        assertEquals(2, map.get("b").getAsInt());
    }

    // ==================== normalize - JsonObject deep ====================

    @Test
    void normalize_objectDeep_returnsMapOfNormalizedObjects() {
        JsonElement element = GsonUtil.parse("{\"a\": 1, \"b\": \"two\", \"c\": true}");
        Object result = GsonUtil.normalize(element, true);
        assertInstanceOf(Map.class, result);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals(3, map.size());
        assertEquals(1, ((Number) map.get("a")).intValue());
        assertEquals("two", map.get("b"));
        assertEquals(true, map.get("c"));
    }

    @Test
    void normalize_objectDeep_nestedObject_returnsNestedMap() {
        JsonElement element = GsonUtil.parse("{\"outer\": {\"inner\": 42}}");
        Object result = GsonUtil.normalize(element, true);
        assertInstanceOf(Map.class, result);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertInstanceOf(Map.class, map.get("outer"));
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) map.get("outer");
        assertEquals(42, ((Number) inner.get("inner")).intValue());
    }
}
