package n1ql.query.generator.services;

import com.google.gson.*;

import java.util.*;

/**
 * Parses JSON documents to extract field paths for autocomplete suggestions.
 */
public class JsonSchemaParser {

    /**
     * Extracts all field paths from a JSON string.
     * @param json The JSON string to parse
     * @return A sorted list of field paths (e.g., "name", "address.city", "tags[0]")
     */
    public static List<String> extractFieldPaths(String json) {
        Set<String> fields = new TreeSet<>();
        
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>(fields);
        }
        
        try {
            JsonElement element = JsonParser.parseString(json);
            extractPaths(element, "", fields);
        } catch (JsonSyntaxException e) {
            // Invalid JSON, return empty list
            return new ArrayList<>();
        }
        
        return new ArrayList<>(fields);
    }

    /**
     * Extracts all field names (without paths) from a JSON string.
     * @param json The JSON string to parse
     * @return A sorted list of field names
     */
    public static List<String> extractFieldNames(String json) {
        Set<String> fields = new TreeSet<>();
        
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>(fields);
        }
        
        try {
            JsonElement element = JsonParser.parseString(json);
            extractNames(element, fields);
        } catch (JsonSyntaxException e) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(fields);
    }

    /**
     * Extracts field paths along with their detected types.
     * @param json The JSON string to parse
     * @return A map of field paths to their JSON types
     */
    public static Map<String, String> extractFieldsWithTypes(String json) {
        Map<String, String> fields = new LinkedHashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return fields;
        }
        
        try {
            JsonElement element = JsonParser.parseString(json);
            extractPathsWithTypes(element, "", fields);
        } catch (JsonSyntaxException e) {
            return new LinkedHashMap<>();
        }
        
        return fields;
    }

    private static void extractPaths(JsonElement element, String prefix, Set<String> fields) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                fields.add(path);
                extractPaths(entry.getValue(), path, fields);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            if (!arr.isEmpty()) {
                // Add array notation
                String arrayPath = prefix + "[]";
                fields.add(arrayPath);
                
                // Extract from first element as representative
                extractPaths(arr.get(0), prefix + "[0]", fields);
            }
        }
        // Primitives don't have nested fields
    }

    private static void extractNames(JsonElement element, Set<String> fields) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                fields.add(entry.getKey());
                extractNames(entry.getValue(), fields);
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            if (!arr.isEmpty()) {
                extractNames(arr.get(0), fields);
            }
        }
    }

    private static void extractPathsWithTypes(JsonElement element, String prefix, Map<String, String> fields) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                String type = getJsonType(entry.getValue());
                fields.put(path, type);
                
                if (entry.getValue().isJsonObject() || entry.getValue().isJsonArray()) {
                    extractPathsWithTypes(entry.getValue(), path, fields);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            if (!arr.isEmpty()) {
                String arrayPath = prefix + "[]";
                fields.put(arrayPath, "array");
                extractPathsWithTypes(arr.get(0), prefix + "[0]", fields);
            }
        }
    }

    private static String getJsonType(JsonElement element) {
        if (element.isJsonNull()) {
            return "null";
        } else if (element.isJsonObject()) {
            return "object";
        } else if (element.isJsonArray()) {
            return "array";
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return "boolean";
            } else if (primitive.isNumber()) {
                return "number";
            } else {
                return "string";
            }
        }
        return "unknown";
    }

    /**
     * Validates if the given string is valid JSON.
     * @param json The string to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            JsonParser.parseString(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Formats JSON with indentation.
     * @param json The JSON string to format
     * @return Formatted JSON or original string if invalid
     */
    public static String formatJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(element);
        } catch (JsonSyntaxException e) {
            return json;
        }
    }

    /**
     * Minifies JSON by removing whitespace.
     * @param json The JSON string to minify
     * @return Minified JSON or original string if invalid
     */
    public static String minifyJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return new Gson().toJson(element);
        } catch (JsonSyntaxException e) {
            return json;
        }
    }
}
