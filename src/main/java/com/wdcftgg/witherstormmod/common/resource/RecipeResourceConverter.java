package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RecipeResourceConverter {

    public static final String RECIPE_PREFIX = "data/witherstormmod/recipes/";
    private static final int WILDCARD_METADATA = Short.MAX_VALUE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, ItemMapping> ITEM_MAPPINGS;

    static {
        Map<String, ItemMapping> mappings = new HashMap<String, ItemMapping>();
        mappings.put("minecraft:suspicious_stew", item("futuremc:suspicious_stew"));
        mappings.put("minecraft:wither_skeleton_skull", item("minecraft:skull", 1));
        mappings.put("minecraft:cod", item("minecraft:fish", 0));
        mappings.put("minecraft:salmon", item("minecraft:fish", 1));
        mappings.put("minecraft:black_dye", item("minecraft:dye", 0));
        mappings.put("minecraft:lapis_lazuli", item("minecraft:dye", 4));
        mappings.put("minecraft:firework_rocket", item("minecraft:fireworks"));
        mappings.put("minecraft:spyglass", item("minecraft:glass_bottle"));
        mappings.put("minecraft:barrel", item("futuremc:barrel"));
        mappings.put("minecraft:golden_apple", item("minecraft:golden_apple", 0));
        mappings.put("minecraft:crossbow", item("crossbow:crossbow"));
        mappings.put("minecraft:phantom_membrane", item("minecraft:leather"));
        mappings.put("minecraft:honey_bottle", item("futuremc:honey_bottle"));
        mappings.put("minecraft:honeycomb", item("futuremc:honeycomb"));
        mappings.put("minecraft:wither_rose", item("futuremc:wither_rose"));
        mappings.put("minecraft:bell", item("futuremc:bell"));
        ITEM_MAPPINGS = Collections.unmodifiableMap(mappings);
    }

    private RecipeResourceConverter() {
    }

    public static JsonObject convert(String sourceName, InputStream source) throws IOException {
        try {
            JsonObject recipe = JsonParser.parseReader(
                    new InputStreamReader(source, StandardCharsets.UTF_8)).getAsJsonObject();
            String type = string(recipe, "type");
            if (!"minecraft:crafting_shaped".equals(type)
                    && !"minecraft:crafting_shapeless".equals(type)) {
                return null;
            }

            JsonObject converted = new JsonObject();
            converted.addProperty("type", type);
            if (recipe.has("group") && recipe.get("group").isJsonPrimitive()) {
                converted.addProperty("group", recipe.get("group").getAsString());
            }
            if ("minecraft:crafting_shaped".equals(type)) {
                converted.add("pattern", copy(required(recipe, "pattern")));
                JsonObject sourceKey = required(recipe, "key").getAsJsonObject();
                JsonObject key = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : sourceKey.entrySet()) {
                    key.add(entry.getKey(), convertIngredient(entry.getValue()));
                }
                converted.add("key", key);
            } else {
                JsonArray ingredients = new JsonArray();
                for (JsonElement ingredient : required(recipe, "ingredients").getAsJsonArray()) {
                    ingredients.add(convertIngredient(ingredient));
                }
                converted.add("ingredients", ingredients);
            }
            converted.add("result", convertResult(required(recipe, "result")));
            return converted;
        } catch (RuntimeException exception) {
            throw new IOException("Unable to convert upstream recipe " + sourceName, exception);
        }
    }

    public static JsonObject convertStonecutting(String sourceName, InputStream source) throws IOException {
        try {
            JsonObject recipe = JsonParser.parseReader(
                    new InputStreamReader(source, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"minecraft:stonecutting".equals(string(recipe, "type"))) {
                return null;
            }

            JsonObject converted = new JsonObject();
            converted.addProperty("type", "minecraft:stonecutting");
            converted.add("ingredient", convertIngredient(required(recipe, "ingredient")));

            JsonElement sourceResult = required(recipe, "result");
            if (!sourceResult.isJsonPrimitive()) {
                throw new IOException("Stonecutting result must be an item name");
            }
            int count = recipe.has("count") ? recipe.get("count").getAsInt() : 1;
            if (count < 1 || count > 64) {
                throw new IOException("Stonecutting result count is outside the Minecraft stack range: " + count);
            }
            converted.add("result", mappedItem(sourceResult.getAsString()).toJson(count));
            return converted;
        } catch (RuntimeException exception) {
            throw new IOException("Unable to convert upstream stonecutting recipe " + sourceName, exception);
        }
    }

    public static JsonObject convertAnvil(String sourceName, InputStream source) throws IOException {
        try {
            JsonObject recipe = JsonParser.parseReader(
                    new InputStreamReader(source, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"witherstormmod:anvil".equals(string(recipe, "type"))) {
                return null;
            }

            JsonObject converted = new JsonObject();
            converted.addProperty("type", "witherstormmod:anvil");
            converted.add("left", convertIngredient(required(recipe, "left")));
            converted.add("right", convertIngredient(required(recipe, "right")));
            converted.add("result", convertResult(required(recipe, "result")));
            converted.addProperty("cost", required(recipe, "cost").getAsInt());
            return converted;
        } catch (RuntimeException exception) {
            throw new IOException("Unable to convert upstream anvil recipe " + sourceName, exception);
        }
    }

    public static boolean isLockAmulet(String sourceName, InputStream source) throws IOException {
        try {
            JsonObject recipe = JsonParser.parseReader(
                    new InputStreamReader(source, StandardCharsets.UTF_8)).getAsJsonObject();
            return "witherstormmod:lock_amulet".equals(string(recipe, "type"));
        } catch (RuntimeException exception) {
            throw new IOException("Unable to inspect upstream amulet lock recipe " + sourceName, exception);
        }
    }

    public static byte[] serialize(JsonObject recipe) {
        return GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8);
    }

    public static JsonElement convertIngredient(JsonElement source) throws IOException {
        if (source.isJsonArray()) {
            JsonArray choices = new JsonArray();
            for (JsonElement choice : source.getAsJsonArray()) {
                JsonElement converted = convertIngredientChoice(choice);
                if (converted != null) choices.add(converted);
            }
            if (choices.size() == 0) {
                throw new IOException("Ingredient has no Minecraft 1.12.2 choices");
            }
            return choices.size() == 1 ? choices.get(0) : choices;
        }
        JsonElement converted = convertIngredientChoice(source);
        if (converted == null) {
            throw new IOException("Ingredient is unavailable in Minecraft 1.12.2");
        }
        return converted;
    }

    private static JsonElement convertIngredientChoice(JsonElement source) throws IOException {
        if (!source.isJsonObject()) {
            throw new IOException("Ingredient must be an object or an array");
        }
        JsonObject ingredient = source.getAsJsonObject();
        if (ingredient.has("tag")) {
            return convertTag(ingredient.get("tag").getAsString());
        }
        if (!ingredient.has("item")) {
            throw new IOException("Ingredient has neither item nor tag");
        }
        String sourceItem = ingredient.get("item").getAsString();
        if ("minecraft:torchflower_seeds".equals(sourceItem)) {
            return null;
        }
        return mappedItem(sourceItem).toIngredientJson(
                ingredient.has("count") ? ingredient.get("count").getAsInt() : null);
    }

    private static JsonElement convertTag(String tag) throws IOException {
        JsonArray choices = new JsonArray();
        if ("minecraft:logs".equals(tag)) {
            choices.add(item("minecraft:log", WILDCARD_METADATA).toIngredientJson(null));
            choices.add(item("minecraft:log2", WILDCARD_METADATA).toIngredientJson(null));
        } else if ("minecraft:fishes".equals(tag)) {
            choices.add(item("minecraft:fish", WILDCARD_METADATA).toIngredientJson(null));
        } else if ("witherstormmod:tainted_logs".equals(tag)) {
            choices.add(item("witherstormmod:tainted_log").toIngredientJson(null));
            choices.add(item("witherstormmod:tainted_wood").toIngredientJson(null));
        } else if ("witherstormmod:cure_base".equals(tag)) {
            choices.add(item("minecraft:slime_ball").toIngredientJson(null));
            choices.add(item("minecraft:magma_cream").toIngredientJson(null));
        } else if ("witherstormmod:cure_ingredient".equals(tag)) {
            choices.add(item("futuremc:wither_rose").toIngredientJson(null));
            choices.add(item("witherstormmod:withered_flesh").toIngredientJson(null));
            choices.add(item("witherstormmod:withered_bone").toIngredientJson(null));
            choices.add(item("witherstormmod:withered_spider_eye").toIngredientJson(null));
        } else {
            throw new IOException("No Minecraft 1.12.2 mapping for ingredient tag " + tag);
        }
        return choices.size() == 1 ? choices.get(0) : choices;
    }

    public static JsonObject convertResult(JsonElement source) throws IOException {
        String itemName;
        Integer count = null;
        JsonElement nbt = null;
        if (source.isJsonPrimitive()) {
            itemName = source.getAsString();
        } else if (source.isJsonObject()) {
            JsonObject result = source.getAsJsonObject();
            itemName = string(result, "item");
            if (result.has("count")) count = result.get("count").getAsInt();
            if (result.has("nbt")) nbt = copy(result.get("nbt"));
        } else {
            throw new IOException("Recipe result must be a string or object");
        }
        JsonObject converted = mappedItem(itemName).toJson(count);
        if (nbt != null) converted.add("nbt", nbt);
        return converted;
    }

    private static ItemMapping mappedItem(String name) {
        ItemMapping mapping = ITEM_MAPPINGS.get(name);
        return mapping == null ? item(name) : mapping;
    }

    private static ItemMapping item(String name) {
        return new ItemMapping(name, null);
    }

    private static ItemMapping item(String name, int data) {
        return new ItemMapping(name, data);
    }

    private static JsonElement required(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) throw new IOException("Missing recipe member " + key);
        return value;
    }

    private static String string(JsonObject object, String key) throws IOException {
        JsonElement value = required(object, key);
        if (!value.isJsonPrimitive()) throw new IOException("Recipe member " + key + " is not a string");
        return value.getAsString();
    }

    private static JsonElement copy(JsonElement value) {
        return value.deepCopy();
    }

    private static final class ItemMapping {
        private final String name;
        private final Integer data;

        private ItemMapping(String name, Integer data) {
            this.name = name;
            this.data = data;
        }

        private JsonObject toJson(Integer count) {
            JsonObject value = new JsonObject();
            value.addProperty("item", name);
            if (count != null) value.addProperty("count", count);
            if (data != null) value.addProperty("data", data);
            return value;
        }

        private JsonObject toIngredientJson(Integer count) {
            JsonObject value = toJson(count);
            if (data == null) value.addProperty("data", WILDCARD_METADATA);
            return value;
        }
    }
}
