package com.wdcftgg.witherstormmod.common.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class ModelResourceConverter {

    private static final String MODEL_PREFIX = "assets/witherstormmod/models/";
    private static final String BLOCKSTATE_PREFIX = "assets/witherstormmod/blockstates/";
    private static final String PHASOMETER_MODEL = MODEL_PREFIX + "item/phasometer.json";
    private static final String PHASOMETER_GUI_MODEL = MODEL_PREFIX + "item/phasometer_gui.json";
    private static final String PHASOMETER_SOURCE = MODEL_PREFIX + "item/phasometer.json";
    private static final String CROSSBOW_MOD_ENDER_PEARL_MODEL =
            MODEL_PREFIX + "item/crossbow_mod_ender_pearl.json";
    private static final String CROSSBOW_ENDER_PEARL_SOURCE =
            MODEL_PREFIX + "item/crossbow_ender_pearl.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> PARENT_REWRITES;
    private static final Map<String, Scale> PADDED_TEXTURES;

    static {
        Map<String, String> parents = new HashMap<String, String>();
        parents.put("minecraft:block/slab", "minecraft:block/half_slab");
        parents.put("minecraft:block/slab_top", "minecraft:block/upper_slab");
        parents.put("minecraft:block/template_fence_gate", "minecraft:block/fence_gate_closed");
        parents.put("minecraft:block/template_fence_gate_open", "minecraft:block/fence_gate_open");
        parents.put("minecraft:block/template_fence_gate_wall", "minecraft:block/fence_gate_closed");
        parents.put("minecraft:block/template_fence_gate_wall_open", "minecraft:block/fence_gate_open");
        parents.put("minecraft:block/template_orientable_trapdoor_bottom", "minecraft:block/trapdoor_bottom");
        parents.put("minecraft:block/template_orientable_trapdoor_top", "minecraft:block/trapdoor_top");
        parents.put("minecraft:block/template_orientable_trapdoor_open", "minecraft:block/trapdoor_open");
        parents.put("minecraft:block/template_torch", "minecraft:block/torch");
        parents.put("minecraft:block/template_torch_wall", "minecraft:block/torch_wall");
        parents.put("minecraft:block/cube_column_horizontal", "minecraft:block/cube_column");
        parents.put("minecraft:block/orientable_with_bottom", "minecraft:block/orientable");
        parents.put("minecraft:block/template_wall_post", "minecraft:block/wall_post");
        parents.put("minecraft:block/template_wall_side", "minecraft:block/wall_side");
        // 1.20 增加了更高的墙侧模板，1.12 的普通墙侧父模型可兼容其结构。
        parents.put("minecraft:block/template_wall_side_tall", "minecraft:block/wall_side");
        parents.put("minecraft:block/template_glass_pane_post", "minecraft:block/glass_pane_post");
        parents.put("minecraft:block/template_glass_pane_side", "minecraft:block/glass_pane_side");
        parents.put("minecraft:block/template_glass_pane_side_alt", "minecraft:block/glass_pane_side_alt");
        parents.put("minecraft:block/template_glass_pane_noside", "minecraft:block/glass_pane_noside");
        parents.put("minecraft:block/template_glass_pane_noside_alt", "minecraft:block/glass_pane_noside_alt");
        parents.put("minecraft:block/door_bottom_left", "minecraft:block/door_bottom");
        parents.put("minecraft:block/door_bottom_right", "minecraft:block/door_bottom_rh");
        parents.put("minecraft:block/door_bottom_left_open", "minecraft:block/door_bottom_rh");
        parents.put("minecraft:block/door_bottom_right_open", "minecraft:block/door_bottom");
        parents.put("minecraft:block/door_top_left", "minecraft:block/door_top");
        parents.put("minecraft:block/door_top_right", "minecraft:block/door_top_rh");
        parents.put("minecraft:block/door_top_left_open", "minecraft:block/door_top_rh");
        parents.put("minecraft:block/door_top_right_open", "minecraft:block/door_top");
        parents.put("minecraft:item/template_spawn_egg", "minecraft:item/spawn_egg");
        PARENT_REWRITES = Collections.unmodifiableMap(parents);

        Map<String, Scale> padded = new HashMap<String, Scale>();
        padded.put("witherstormmod:blocks/flesh_skele", new Scale("0.75", "0.5"));
        padded.put("witherstormmod:blocks/flesh_skull_e", new Scale("0.75", "0.5"));
        padded.put("witherstormmod:blocks/flesh_zomb", new Scale("1", "0.5"));
        padded.put("witherstormmod:blocks/flesh_zomb_e", new Scale("1", "0.5"));
        PADDED_TEXTURES = Collections.unmodifiableMap(padded);
    }

    private ModelResourceConverter() {
    }

    static boolean handles(String legacyName) {
        return legacyName.startsWith(MODEL_PREFIX) && legacyName.endsWith(".json")
                || legacyName.startsWith(BLOCKSTATE_PREFIX) && legacyName.endsWith(".json");
    }

    static String sourceName(String legacyName) {
        if (!handles(legacyName)) {
            return legacyName;
        }
        if (PHASOMETER_MODEL.equals(legacyName) || PHASOMETER_GUI_MODEL.equals(legacyName)) {
            return PHASOMETER_SOURCE;
        }
        if (CROSSBOW_MOD_ENDER_PEARL_MODEL.equals(legacyName)) {
            return CROSSBOW_ENDER_PEARL_SOURCE;
        }
        return legacyName;
    }

    static byte[] convert(String legacyName, byte[] source) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(
                    new String(source, StandardCharsets.UTF_8)).getAsJsonObject();
            if (legacyName.startsWith(MODEL_PREFIX)) {
                if (PHASOMETER_MODEL.equals(legacyName)) {
                    root = requiredObject(root, "base", legacyName);
                } else if (PHASOMETER_GUI_MODEL.equals(legacyName)) {
                    JsonObject perspectives = requiredObject(root, "perspectives", legacyName);
                    root = requiredObject(perspectives, "gui", legacyName);
                } else if (CROSSBOW_MOD_ENDER_PEARL_MODEL.equals(legacyName)) {
                    // Crossbow's blockstate shorthand resolves to block/template, but this
                    // generated item model is baked directly and needs the explicit parent path.
                    root.addProperty("parent", "crossbow:block/template");
                    JsonObject textures = getOrCreateObject(root, "textures");
                    textures.addProperty(
                            "layer0", "witherstormmod:items/crossbow_mod_ender_pearl");
                }
                convertModel(root);
                if (PHASOMETER_MODEL.equals(legacyName)) {
                    JsonObject textures = getOrCreateObject(root, "textures");
                    textures.addProperty("particle", "witherstormmod:items/phasometer_model");
                }
            } else {
                root = convertBlockState(legacyName, root);
            }
            return GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            throw new IOException("Unable to convert upstream resource " + legacyName, exception);
        }
    }

    private static void convertModel(JsonObject model) {
        JsonElement parentElement = model.get("parent");
        if (parentElement != null && parentElement.isJsonPrimitive()) {
            String parent = parentElement.getAsString();
            String replacement = PARENT_REWRITES.get(parent);
            if (replacement != null) {
                model.addProperty("parent", replacement);
            }
        }

        Map<String, Scale> transformedKeys = new HashMap<String, Scale>();
        JsonObject textures = object(model.get("textures"));
        if (textures != null) {
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                String value = rewriteTexture(entry.getValue().getAsString());
                entry.setValue(new JsonPrimitive(value));
                Scale scale = PADDED_TEXTURES.get(value);
                if (scale != null) {
                    transformedKeys.put(entry.getKey(), scale);
                }
            }
        }

        JsonArray elements = array(model.get("elements"));
        if (elements == null || transformedKeys.isEmpty()) {
            return;
        }
        for (JsonElement elementValue : elements) {
            JsonObject element = object(elementValue);
            JsonObject faces = element == null ? null : object(element.get("faces"));
            if (faces == null) {
                continue;
            }
            for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                JsonObject face = object(faceEntry.getValue());
                JsonArray uv = face == null ? null : array(face.get("uv"));
                if (uv == null || uv.size() != 4 || !face.has("texture")) {
                    continue;
                }
                String reference = face.get("texture").getAsString();
                Scale scale = reference.startsWith("#")
                        ? transformedKeys.get(reference.substring(1))
                        : PADDED_TEXTURES.get(reference);
                if (scale == null) {
                    continue;
                }
                JsonArray adjusted = new JsonArray();
                adjusted.add(decimal(uv.get(0)).multiply(scale.x));
                adjusted.add(decimal(uv.get(1)).multiply(scale.y));
                adjusted.add(decimal(uv.get(2)).multiply(scale.x));
                adjusted.add(decimal(uv.get(3)).multiply(scale.y));
                face.add("uv", adjusted);
            }
        }
    }

    private static JsonObject convertBlockState(String name, JsonObject blockState) {
        rewriteModelReferences(blockState);
        String blockName = name.substring(name.lastIndexOf('/') + 1, name.length() - ".json".length());
        if (blockName.endsWith("_slab")) {
            return slabState(blockName, blockState);
        }
        if (blockName.endsWith("_wall") && !isWallException(blockName)) {
            return wallState(blockName);
        }
        if (blockName.endsWith("_button")) {
            return buttonState(blockName);
        }
        if ("tainted_fence_gate".equals(blockName)) {
            return addPoweredVariants(blockState);
        }
        if ("tainted_flesh_veins".equals(blockName)) {
            return fleshVeinsState();
        }
        if ("tainted_dust_block".equals(blockName) || "withered_phlegm_block".equals(blockName)) {
            return booleanState("powered", "witherstormmod:" + blockName);
        }
        if ("formidibomb".equals(blockName) || "super_tnt".equals(blockName)) {
            return explosiveState(blockName, blockState);
        }
        if ("tainted_wall_torch".equals(blockName) || "tainted_torch".equals(blockName)) {
            return torchState();
        }
        if ("tainted_sign".equals(blockName)) {
            return standingSignState();
        }
        if ("tainted_wall_sign".equals(blockName)) {
            return wallSignState();
        }
        if ("tainted_door".equals(blockName)) {
            return doorState(blockState);
        }
        JsonObject variants = object(blockState.get("variants"));
        if (variants != null && variants.has("")) {
            JsonElement normal = variants.remove("");
            variants.add("normal", normal);
        }
        return blockState;
    }

    private static JsonObject slabState(String name, JsonObject source) {
        JsonObject sourceVariants = object(source.get("variants"));
        JsonElement doubleVariant = sourceVariants == null ? null : sourceVariants.get("type=double");
        if (doubleVariant == null) {
            doubleVariant = model("witherstormmod:" + name);
        }
        JsonObject variants = new JsonObject();
        variants.add("half=bottom", model("witherstormmod:" + name));
        variants.add("half=top", model("witherstormmod:" + name + "_top"));
        variants.add("half=double", copy(doubleVariant));
        return rootWith("variants", variants);
    }

    private static JsonObject wallState(String name) {
        JsonArray multipart = new JsonArray();
        multipart.add(part("up", "true", model("witherstormmod:" + name + "_post")));
        multipart.add(part("north", "true", rotatedModel("witherstormmod:" + name + "_side", null, true)));
        multipart.add(part("east", "true", rotatedModel("witherstormmod:" + name + "_side", 90, true)));
        multipart.add(part("south", "true", rotatedModel("witherstormmod:" + name + "_side", 180, true)));
        multipart.add(part("west", "true", rotatedModel("witherstormmod:" + name + "_side", 270, true)));
        return rootWith("multipart", multipart);
    }

    private static JsonObject buttonState(String name) {
        Map<String, int[]> rotations = new LinkedHashMap<String, int[]>();
        rotations.put("up", new int[] {0, 0});
        rotations.put("down", new int[] {180, 0});
        rotations.put("east", new int[] {90, 90});
        rotations.put("west", new int[] {90, 270});
        rotations.put("south", new int[] {90, 180});
        rotations.put("north", new int[] {90, 0});
        JsonObject variants = new JsonObject();
        for (Map.Entry<String, int[]> entry : rotations.entrySet()) {
            for (boolean powered : new boolean[] {false, true}) {
                JsonObject variant = model("witherstormmod:" + name + (powered ? "_pressed" : ""));
                variant.addProperty("uvlock", true);
                if (entry.getValue()[0] != 0) variant.addProperty("x", entry.getValue()[0]);
                if (entry.getValue()[1] != 0) variant.addProperty("y", entry.getValue()[1]);
                variants.add("facing=" + entry.getKey() + ",powered=" + powered, variant);
            }
        }
        return rootWith("variants", variants);
    }

    private static JsonObject addPoweredVariants(JsonObject source) {
        JsonObject sourceVariants = object(source.get("variants"));
        if (sourceVariants == null) return source;
        JsonObject variants = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : sourceVariants.entrySet()) {
            String key = canonicalVariantKey(entry.getKey());
            variants.add(key, copy(entry.getValue()));
            if (!key.contains("powered=")) {
                variants.add(canonicalVariantKey(key + ",powered=false"), copy(entry.getValue()));
                variants.add(canonicalVariantKey(key + ",powered=true"), copy(entry.getValue()));
            }
        }
        return rootWith("variants", variants);
    }

    private static JsonObject fleshVeinsState() {
        JsonArray multipart = new JsonArray();
        multipart.add(part("north", "true", model("witherstormmod:tainted_flesh_veins")));
        multipart.add(part("east", "true", rotatedModel("witherstormmod:tainted_flesh_veins", 90, true)));
        multipart.add(part("south", "true", rotatedModel("witherstormmod:tainted_flesh_veins", 180, true)));
        multipart.add(part("west", "true", rotatedModel("witherstormmod:tainted_flesh_veins", 270, true)));
        JsonObject up = model("witherstormmod:tainted_flesh_veins");
        up.addProperty("x", 270);
        up.addProperty("uvlock", true);
        multipart.add(part("up", "true", up));
        return rootWith("multipart", multipart);
    }

    private static JsonObject explosiveState(String name, JsonObject source) {
        JsonObject sourceVariants = object(source.get("variants"));
        if (sourceVariants != null && sourceVariants.size() > 0) {
            JsonObject converted = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : sourceVariants.entrySet()) {
                if (entry.getKey().isEmpty()) {
                    converted.add("explode=false", copy(entry.getValue()));
                    converted.add("explode=true", copy(entry.getValue()));
                    continue;
                }
                String key = canonicalVariantKey(entry.getKey().replace("unstable=", "explode="));
                converted.add(key, copy(entry.getValue()));
            }
            return rootWith("variants", converted);
        }
        JsonObject variants = new JsonObject();
        variants.add("explode=false", model("witherstormmod:" + name));
        variants.add("explode=true", model("witherstormmod:" + name));
        return rootWith("variants", variants);
    }

    private static JsonObject torchState() {
        JsonObject variants = new JsonObject();
        variants.add("facing=up", model("witherstormmod:tainted_torch"));
        variants.add("facing=east", model("witherstormmod:tainted_wall_torch"));
        variants.add("facing=south", rotatedModel("witherstormmod:tainted_wall_torch", 90, false));
        variants.add("facing=west", rotatedModel("witherstormmod:tainted_wall_torch", 180, false));
        variants.add("facing=north", rotatedModel("witherstormmod:tainted_wall_torch", 270, false));
        return rootWith("variants", variants);
    }

    private static JsonObject standingSignState() {
        JsonObject variants = new JsonObject();
        variants.add("normal", model("witherstormmod:tainted_sign"));
        for (int rotation = 0; rotation < 16; rotation++) {
            variants.add("rotation=" + rotation, model("witherstormmod:tainted_sign"));
        }
        return rootWith("variants", variants);
    }

    private static JsonObject wallSignState() {
        JsonObject variants = new JsonObject();
        variants.add("normal", model("witherstormmod:tainted_sign"));
        variants.add("facing=north", model("witherstormmod:tainted_sign"));
        variants.add("facing=east", model("witherstormmod:tainted_sign"));
        variants.add("facing=south", model("witherstormmod:tainted_sign"));
        variants.add("facing=west", model("witherstormmod:tainted_sign"));
        return rootWith("variants", variants);
    }

    private static JsonObject doorState(JsonObject source) {
        // 客户端状态映射器会忽略 BlockDoor.POWERED。上游 1.20 键已经匹配
        // 1.12 的其余属性，只需改写模型引用。
        return source;
    }

    private static void rewriteModelReferences(JsonElement node) {
        if (node == null || node.isJsonNull()) {
            return;
        }
        if (node.isJsonArray()) {
            for (JsonElement child : node.getAsJsonArray()) rewriteModelReferences(child);
            return;
        }
        if (!node.isJsonObject()) {
            return;
        }
        JsonObject object = node.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if ("model".equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
                entry.setValue(new JsonPrimitive(entry.getValue().getAsString().replace(":block/", ":")));
            } else {
                rewriteModelReferences(entry.getValue());
            }
        }
    }

    private static boolean isWallException(String name) {
        return "tainted_zombie_wall".equals(name) || "tainted_skeleton_wall".equals(name)
                || "tainted_wall_sign".equals(name) || "tainted_wall_torch".equals(name);
    }

    private static String rewriteTexture(String value) {
        value = value.replace(":block/", ":blocks/").replace(":item/", ":items/");
        if (value.startsWith("block/")) return "blocks/" + value.substring("block/".length());
        if (value.startsWith("item/")) return "items/" + value.substring("item/".length());
        return value;
    }

    private static JsonObject booleanState(String property, String modelName) {
        JsonObject variants = new JsonObject();
        variants.add(property + "=false", model(modelName));
        variants.add(property + "=true", model(modelName));
        return rootWith("variants", variants);
    }

    private static String canonicalVariantKey(String key) {
        if (key == null || key.isEmpty() || "normal".equals(key)) return key;
        String[] properties = key.split(",");
        Arrays.sort(properties);
        return String.join(",", properties);
    }

    private static JsonObject part(String key, String value, JsonObject apply) {
        JsonObject when = new JsonObject();
        when.addProperty(key, value);
        JsonObject part = new JsonObject();
        part.add("when", when);
        part.add("apply", apply);
        return part;
    }

    private static JsonObject rotatedModel(String name, Integer y, boolean uvlock) {
        JsonObject model = model(name);
        if (y != null) model.addProperty("y", y);
        if (uvlock) model.addProperty("uvlock", true);
        return model;
    }

    private static JsonObject model(String name) {
        JsonObject model = new JsonObject();
        model.addProperty("model", name);
        return model;
    }

    private static JsonObject rootWith(String key, JsonElement value) {
        JsonObject root = new JsonObject();
        root.add(key, value);
        return root;
    }

    private static JsonObject getOrCreateObject(JsonObject root, String key) {
        JsonObject value = object(root.get(key));
        if (value == null) {
            value = new JsonObject();
            root.add(key, value);
        }
        return value;
    }

    private static JsonObject object(JsonElement value) {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonObject requiredObject(JsonObject parent, String key, String legacyName) {
        JsonObject value = object(parent.get(key));
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing " + key + " in upstream phasometer model for " + legacyName);
        }
        return value.deepCopy();
    }

    private static JsonArray array(JsonElement value) {
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static JsonElement copy(JsonElement value) {
        return value == null ? null : value.deepCopy();
    }

    private static BigDecimal decimal(JsonElement value) {
        return new BigDecimal(value.getAsString());
    }

    private static final class Scale {
        private final BigDecimal x;
        private final BigDecimal y;

        private Scale(String x, String y) {
            this.x = new BigDecimal(x);
            this.y = new BigDecimal(y);
        }
    }
}
