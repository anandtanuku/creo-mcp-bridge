package com.creomcp.bridge.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for JSON serialization and deserialization using Jackson.
 */
public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": true, \"message\": \"" + escape(e.getMessage()) + "\"}";
        }
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", true);
            err.put("message", "Invalid JSON: " + e.getMessage());
            return err;
        }
    }

    public static Map<String, Object> parseMap(InputStream is) {
        try {
            return mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", true);
            err.put("message", "Invalid JSON input: " + e.getMessage());
            return err;
        }
    }

    public static Map<String, Object> createSuccess(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        if (message != null) {
            map.put("message", message);
        }
        return map;
    }

    public static Map<String, Object> createError(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("error", true);
        map.put("message", message != null ? message : "Unknown error");
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}

