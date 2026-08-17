package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.util.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdvancementResourceConverter {

    public static final String ADVANCEMENT_PREFIX = "data/witherstormmod/advancements/";
    public static final String SOURCE_MARKER = "_legacy_arr_source";
    public static final String OBSERVE_WITHER_STORM_TRIGGER = "witherstormmod:observe_wither_storm";
    public static final Set<String> MAIN_CHAIN = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "main/root.json",
                    "main/summon_wither_storm.json",
                    "main/amulet.json",
                    "main/fully_link_amulet.json",
                    "main/cured_sickened_mob.json",
                    "main/escape_wither_storm.json",
                    "main/harbinger_of_cataclysmic_fates.json",
                    "main/infinite_potential.json",
                    "main/fbomb.json",
                    "main/strong_grow_weak.json",
                    "main/weakened_arise_stronger.json",
                    "main/silver_lining.json",
                    "main/belly_of_the_beast.json",
                    "main/wither_storm_defeated.json",
                    "main/activate_super_beacon.json",
                    "main/resummon_wither_storm.json",
                    "main/resummon_withered_symbiont.json",
                    "main/summon_mob_withered_beacon.json",
                    "main/ring_bell_near_storm.json",
                    "main/spyglass_at_wither_storm.json",
                    "main/nearly_kill_wither_storm.json",
                    "main/insane_dedication.json",
                    "main/overly_dedicated.json")));

    private static final String BOWELS_DIMENSION = "witherstormmod:bowels";
    private static final String LEGACY_BOWELS_DIMENSION = "wither_storm_bowels";
    private static final Set<String> LEGACY_RECIPE_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "minecraft:crafting_shaped", "minecraft:crafting_shapeless")));
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AdvancementResourceConverter() {
    }

    public static JsonObject convert(String entryName, InputStream source) throws IOException {
        String relative = relativeName(entryName);
        if (!MAIN_CHAIN.contains(relative)) {
            throw new IOException("Unsupported upstream advancement: " + entryName);
        }

        return convertParsed(entryName, relative, readAdvancement(entryName, source));
    }

    public static JsonObject convertRecipeAdvancement(String entryName, InputStream source)
            throws IOException {
        String relative = relativeName(entryName);
        if (!relative.startsWith("recipes/")) {
            throw new IOException("Not an upstream recipe advancement: " + entryName);
        }
        JsonObject advancement = readAdvancement(entryName, source);
        if (!hasLegacyRecipeReward(advancement)) return null;
        return convertParsed(entryName, relative, advancement);
    }

    private static JsonObject readAdvancement(String entryName, InputStream source) throws IOException {
        try (Reader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid upstream advancement: " + entryName, exception);
        }
    }

    private static JsonObject convertParsed(String entryName, String relative,
                                            JsonObject advancement) throws IOException {
        convertDisplay(advancement);
        convertCriteria(advancement, relative);
        convertRewards(advancement);
        advancement.remove("sends_telemetry_event");
        advancement.addProperty(SOURCE_MARKER, entryName);
        return advancement;
    }

    public static String serialize(JsonObject advancement) {
        return GSON.toJson(advancement) + "\n";
    }

    public static String sourceEntry(String relativeName) {
        if (!MAIN_CHAIN.contains(relativeName)) {
            throw new IllegalArgumentException("Unsupported upstream advancement: " + relativeName);
        }
        return ADVANCEMENT_PREFIX + relativeName;
    }

    public static boolean isGeneratedFile(JsonObject json, String entryName) {
        return json.has(SOURCE_MARKER)
                && entryName.equals(JsonUtils.getString(json, SOURCE_MARKER, ""));
    }

    private static String relativeName(String entryName) throws IOException {
        if (entryName == null || !entryName.startsWith(ADVANCEMENT_PREFIX)
                || !entryName.endsWith(".json")) {
            throw new IOException("Invalid upstream advancement path: " + entryName);
        }
        return entryName.substring(ADVANCEMENT_PREFIX.length());
    }

    private static void convertDisplay(JsonObject advancement) {
        if (!advancement.has("display")) return;
        JsonObject display = JsonUtils.getJsonObject(advancement, "display");
        JsonObject icon = JsonUtils.getJsonObject(display, "icon", new JsonObject());
        if (icon.has("item") && icon.get("item").isJsonPrimitive()) {
            String itemId = icon.get("item").getAsString();
            if ("witherstormmod:withered_symbiont_spawn_egg".equals(itemId)) {
                icon.addProperty("item", "minecraft:spawn_egg");
                icon.addProperty("nbt",
                        "{EntityTag:{id:\"witherstormmod:withered_symbiont\"}}");
            } else {
                icon.addProperty("item", convertItemId(itemId));
                applyLegacyItemMetadata(icon, itemId);
            }
        }
        if (display.has("background")) {
            String background = JsonUtils.getString(display, "background");
            display.addProperty("background", background.replace(":textures/block/", ":textures/blocks/"));
        }
    }

    private static void convertCriteria(JsonObject advancement, String relativeName) throws IOException {
        JsonObject originalCriteria = JsonUtils.getJsonObject(advancement, "criteria");
        JsonObject convertedCriteria = new JsonObject();
        Map<String, List<String>> replacements = new LinkedHashMap<String, List<String>>();

        for (Map.Entry<String, JsonElement> entry : originalCriteria.entrySet()) {
            String criterionName = entry.getKey();
            JsonObject criterion = JsonUtils.getJsonObject(entry.getValue(), "criterion").deepCopy();
            String trigger = JsonUtils.getString(criterion, "trigger");
            List<JsonObject> alternatives;

            if ("minecraft:inventory_changed".equals(trigger)) {
                alternatives = convertInventoryCriterion(criterion, relativeName);
            } else {
                convertCriterion(criterion, trigger, relativeName);
                alternatives = Collections.singletonList(criterion);
            }

            List<String> generatedNames = new ArrayList<String>();
            for (int index = 0; index < alternatives.size(); index++) {
                String generatedName = alternatives.size() == 1
                        ? criterionName : criterionName + "_alternative_" + (index + 1);
                if (originalCriteria.has(generatedName) && !generatedName.equals(criterionName)
                        || convertedCriteria.has(generatedName)) {
                    throw new JsonSyntaxException("Duplicate generated criterion '" + generatedName
                            + "' in " + relativeName);
                }
                convertedCriteria.add(generatedName, alternatives.get(index));
                generatedNames.add(generatedName);
            }
            replacements.put(criterionName, generatedNames);
        }

        advancement.add("criteria", convertedCriteria);
        rewriteRequirements(advancement, replacements);
    }

    private static void convertCriterion(JsonObject criterion, String trigger,
                                         String relativeName) throws IOException {
        JsonObject conditions = JsonUtils.getJsonObject(criterion, "conditions", new JsonObject());
        if ("minecraft:summoned_entity".equals(trigger)) {
            convertEntityCondition(conditions, "entity", relativeName);
        } else if ("minecraft:changed_dimension".equals(trigger)) {
            convertDimensionCondition(conditions, "from");
            convertDimensionCondition(conditions, "to");
        } else if ("minecraft:using_item".equals(trigger)) {
            criterion.addProperty("trigger", OBSERVE_WITHER_STORM_TRIGGER);
            convertObservedStormConditions(conditions, relativeName);
        } else if ("witherstormmod:summon_mob_withered_beacon".equals(trigger)) {
            convertEntityCondition(conditions, "resummoned", relativeName);
        } else if ("witherstormmod:cured_sickened_mob".equals(trigger)) {
            convertEntityCondition(conditions, "sickened", relativeName);
            convertEntityCondition(conditions, "converison", relativeName);
        } else if (trigger.startsWith("witherstormmod:")) {
            convertEntityCondition(conditions, "entity", relativeName);
        }
        criterion.add("conditions", conditions);
    }

    private static List<JsonObject> convertInventoryCriterion(JsonObject criterion,
                                                               String relativeName)
            throws IOException {
        JsonObject conditions = JsonUtils.getJsonObject(criterion, "conditions", new JsonObject());
        JsonArray itemPredicates = JsonUtils.getJsonArray(conditions, "items", new JsonArray());
        List<JsonArray> combinations = new ArrayList<JsonArray>();
        combinations.add(new JsonArray());

        for (JsonElement element : itemPredicates) {
            JsonObject predicate = JsonUtils.getJsonObject(element, "item predicate");
            List<JsonObject> choices = expandItemPredicate(predicate, relativeName);
            List<JsonArray> expanded = new ArrayList<JsonArray>();
            for (JsonArray combination : combinations) {
                for (JsonObject choice : choices) {
                    JsonArray next = combination.deepCopy();
                    next.add(choice);
                    expanded.add(next);
                }
            }
            combinations = expanded;
        }

        List<JsonObject> alternatives = new ArrayList<JsonObject>();
        for (JsonArray combination : combinations) {
            JsonObject convertedCriterion = criterion.deepCopy();
            JsonObject convertedConditions = conditions.deepCopy();
            convertedConditions.add("items", combination);
            convertedCriterion.add("conditions", convertedConditions);
            alternatives.add(convertedCriterion);
        }
        return alternatives;
    }

    private static List<JsonObject> expandItemPredicate(JsonObject source,
                                                        String relativeName) throws IOException {
        if (source.has("items") && source.has("tag")) {
            throw new JsonSyntaxException("Item predicate cannot contain both items and tag in "
                    + relativeName);
        }

        List<String> itemIds = new ArrayList<String>();
        if (source.has("items")) {
            JsonArray items = JsonUtils.getJsonArray(source, "items");
            for (JsonElement item : items) {
                itemIds.add(JsonUtils.getString(item, "item id"));
            }
        } else if (source.has("tag")) {
            itemIds.addAll(resolveItemTag(JsonUtils.getString(source, "tag"),
                    new LinkedHashSet<String>()));
        } else if (source.has("item")) {
            itemIds.add(JsonUtils.getString(source, "item"));
        } else {
            return Collections.singletonList(source.deepCopy());
        }

        if (itemIds.isEmpty()) {
            throw new JsonSyntaxException("Item predicate has no alternatives in " + relativeName);
        }

        List<JsonObject> alternatives = new ArrayList<JsonObject>();
        for (String itemId : new LinkedHashSet<String>(itemIds)) {
            JsonObject predicate = source.deepCopy();
            predicate.remove("items");
            predicate.remove("tag");
            predicate.addProperty("item", convertItemId(itemId, relativeName));
            applyLegacyItemMetadata(predicate, itemId);
            alternatives.add(predicate);
        }
        return alternatives;
    }

    private static List<String> resolveItemTag(String tagId, Set<String> resolving)
            throws IOException {
        if (!resolving.add(tagId)) {
            throw new IOException("Circular upstream item tag: " + tagId);
        }

        String namespace = "minecraft";
        String path = tagId;
        int separator = tagId.indexOf(':');
        if (separator >= 0) {
            namespace = tagId.substring(0, separator);
            path = tagId.substring(separator + 1);
        }
        if (namespace.isEmpty() || path.isEmpty() || namespace.indexOf('\\') >= 0
                || path.indexOf('\\') >= 0 || path.startsWith("/") || path.contains("..")) {
            throw new IOException("Invalid upstream item tag: " + tagId);
        }

        String entryName = "data/" + namespace + "/tags/items/" + path + ".json";
        JsonObject tag;
        try (InputStream source = UpstreamResourceArchive.open(entryName);
             Reader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
            tag = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid upstream item tag: " + entryName, exception);
        }

        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (JsonElement element : JsonUtils.getJsonArray(tag, "values")) {
            String value;
            boolean required = true;
            if (element.isJsonPrimitive()) {
                value = element.getAsString();
            } else {
                JsonObject object = JsonUtils.getJsonObject(element, "tag value");
                value = JsonUtils.getString(object, "id");
                required = JsonUtils.getBoolean(object, "required", true);
            }

            if (value.startsWith("#")) {
                try {
                    values.addAll(resolveItemTag(value.substring(1), resolving));
                } catch (IOException exception) {
                    if (required) throw exception;
                }
            } else {
                values.add(value);
            }
        }
        resolving.remove(tagId);
        return new ArrayList<String>(values);
    }

    private static void rewriteRequirements(JsonObject advancement,
                                            Map<String, List<String>> replacements) {
        if (!advancement.has("requirements")) return;
        JsonArray originalRequirements = JsonUtils.getJsonArray(advancement, "requirements");
        JsonArray convertedRequirements = new JsonArray();
        for (JsonElement groupElement : originalRequirements) {
            JsonArray originalGroup = JsonUtils.getJsonArray(groupElement, "requirement group");
            JsonArray convertedGroup = new JsonArray();
            for (JsonElement criterionElement : originalGroup) {
                String criterionName = JsonUtils.getString(criterionElement, "criterion name");
                List<String> generatedNames = replacements.get(criterionName);
                if (generatedNames == null) {
                    convertedGroup.add(criterionName);
                } else {
                    for (String generatedName : generatedNames) convertedGroup.add(generatedName);
                }
            }
            convertedRequirements.add(convertedGroup);
        }
        advancement.add("requirements", convertedRequirements);
    }

    private static void convertObservedStormConditions(JsonObject conditions, String relativeName)
            throws IOException {
        JsonObject item = JsonUtils.getJsonObject(conditions, "item", new JsonObject());
        List<JsonObject> alternatives = expandItemPredicate(item, relativeName);
        if (alternatives.size() != 1) {
            throw new JsonSyntaxException("Observed item predicate requires one item in " + relativeName);
        }
        conditions.add("item", alternatives.get(0));
        conditions.remove("player");
        JsonObject storm = new JsonObject();
        storm.addProperty("type", "witherstormmod:wither_storm");
        conditions.add("entity", storm);
    }

    private static void convertEntityCondition(JsonObject conditions, String key,
                                               String relativeName) {
        JsonElement entityElement = conditions.get(key);
        if (entityElement == null || entityElement.isJsonNull() || entityElement.isJsonObject()) return;
        if (!entityElement.isJsonArray()) {
            throw new JsonSyntaxException("Invalid entity predicate '" + key + "' in " + relativeName);
        }
        JsonArray predicates = entityElement.getAsJsonArray();
        if (predicates.size() != 1) {
            throw new JsonSyntaxException("Expected one entity predicate '" + key + "' in "
                    + relativeName);
        }
        JsonObject wrapper = JsonUtils.getJsonObject(predicates.get(0), "entity predicate");
        JsonObject predicate = JsonUtils.getJsonObject(wrapper, "predicate");
        JsonObject legacy = new JsonObject();
        copyIfPresent(predicate, legacy, "type");
        copyIfPresent(predicate, legacy, "distance");
        copyIfPresent(predicate, legacy, "location");
        copyIfPresent(predicate, legacy, "effects");
        if (predicate.has("nbt")) {
            String nbt = JsonUtils.getString(predicate, "nbt")
                    .replace("Resummoned", "WitherStormResummoned");
            legacy.addProperty("nbt", nbt);
        }
        conditions.add(key, legacy);
    }

    private static void copyIfPresent(JsonObject source, JsonObject target, String key) {
        if (source.has(key)) target.add(key, source.get(key).deepCopy());
    }

    private static void convertDimensionCondition(JsonObject conditions, String key) {
        if (BOWELS_DIMENSION.equals(JsonUtils.getString(conditions, key, ""))) {
            conditions.addProperty(key, LEGACY_BOWELS_DIMENSION);
        }
    }

    private static void convertRewards(JsonObject advancement) throws IOException {
        if (!advancement.has("rewards")) return;
        JsonObject rewards = JsonUtils.getJsonObject(advancement, "rewards");
        if (!rewards.has("recipes")) return;

        JsonArray converted = new JsonArray();
        for (JsonElement element : JsonUtils.getJsonArray(rewards, "recipes")) {
            String recipeId = JsonUtils.getString(element, "recipe id");
            if (isLegacyUnlockableRecipe(recipeId)) converted.add(recipeId);
        }
        if (converted.size() == 0) rewards.remove("recipes");
        else rewards.add("recipes", converted);
    }

    private static boolean hasLegacyRecipeReward(JsonObject advancement) throws IOException {
        if (!advancement.has("rewards")) return false;
        JsonObject rewards = JsonUtils.getJsonObject(advancement, "rewards");
        if (!rewards.has("recipes")) return false;
        JsonArray recipes = JsonUtils.getJsonArray(rewards, "recipes");
        if (recipes.size() == 0) return false;
        for (JsonElement element : recipes) {
            if (!isLegacyUnlockableRecipe(JsonUtils.getString(element, "recipe id"))) return false;
        }
        return true;
    }

    private static boolean isLegacyUnlockableRecipe(String recipeId) throws IOException {
        String namespace = "minecraft";
        String path = recipeId;
        int separator = recipeId.indexOf(':');
        if (separator >= 0) {
            namespace = recipeId.substring(0, separator);
            path = recipeId.substring(separator + 1);
        }
        String entryName = "data/" + namespace + "/recipes/" + path + ".json";
        for (String candidate : UpstreamResourceArchive.listEntries(
                "data/" + namespace + "/recipes/", ".json")) {
            if (!candidate.equals(entryName)) continue;
            JsonObject recipe;
            try (InputStream source = UpstreamResourceArchive.open(candidate);
                 Reader reader = new InputStreamReader(source, StandardCharsets.UTF_8)) {
                recipe = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (RuntimeException exception) {
                throw new IOException("Invalid upstream recipe reward: " + candidate, exception);
            }
            return LEGACY_RECIPE_TYPES.contains(JsonUtils.getString(recipe, "type"));
        }
        return false;
    }

    private static void applyLegacyItemMetadata(JsonObject item, String originalItemId) {
        if ("minecraft:wither_skeleton_skull".equals(originalItemId)) {
            item.addProperty("data", 1);
        } else if ("minecraft:lapis_lazuli".equals(originalItemId)) {
            item.addProperty("data", 4);
        }
    }

    private static String convertItemId(String itemId) {
        if ("minecraft:wither_skeleton_skull".equals(itemId)) {
            return "minecraft:skull";
        }
        if ("minecraft:bell".equals(itemId)) {
            return "futuremc:bell";
        }
        if ("minecraft:spyglass".equals(itemId)) {
            return "witherstormmod:phasometer";
        }
        if ("minecraft:barrel".equals(itemId)) {
            return "futuremc:barrel";
        }
        if ("minecraft:firework_rocket".equals(itemId)) {
            return "minecraft:fireworks";
        }
        if ("minecraft:lapis_lazuli".equals(itemId)) {
            return "minecraft:dye";
        }
        if ("minecraft:suspicious_stew".equals(itemId)) {
            return "futuremc:suspicious_stew";
        }
        if ("minecraft:crossbow".equals(itemId)) {
            return "crossbow:crossbow";
        }
        if ("minecraft:honey_bottle".equals(itemId)
                || "minecraft:honeycomb".equals(itemId)
                || "minecraft:wither_rose".equals(itemId)) {
            return "futuremc:" + itemId.substring("minecraft:".length());
        }
        return itemId;
    }

    private static String convertItemId(String itemId, String relativeName) {
        if ("minecraft:spyglass".equals(itemId) && relativeName.startsWith("recipes/")) {
            return "minecraft:glass_bottle";
        }
        return convertItemId(itemId);
    }
}
