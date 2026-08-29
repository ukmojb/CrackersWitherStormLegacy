package com.wdcftgg.witherstormmod.common.beacon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wdcftgg.witherstormmod.Tags;
import com.wdcftgg.witherstormmod.WitherStormMod;
import com.wdcftgg.witherstormmod.common.resource.RecipeResourceConverter;
import com.wdcftgg.witherstormmod.common.resource.UpstreamResourceArchive;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SuperBeaconRecipes {

    private static final int EXPECTED_RECIPE_COUNT = 30;
    private static final List<Recipe> RECIPES = loadRecipes();

    private SuperBeaconRecipes() {
    }

    public static Recipe find(List<ItemStack> inventory, ConditionTester conditions) {
        for (Recipe recipe : RECIPES) {
            if (conditions.test(recipe.condition) && recipe.matches(inventory)) {
                return recipe;
            }
        }
        return null;
    }

    private static List<Recipe> loadRecipes() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        JsonContext context = new JsonContext(Tags.MOD_ID);
        try {
            for (String entryName : UpstreamResourceArchive.listEntries(
                    RecipeResourceConverter.RECIPE_PREFIX, ".json")) {
                try {
                    JsonObject json;
                    try (InputStream stream = UpstreamResourceArchive.open(entryName);
                         InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        json = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                    String type = json.get("type").getAsString();
                    if (!"witherstormmod:item_craft_super_beacon".equals(type)
                            && !"witherstormmod:resummon_super_beacon".equals(type)) {
                        continue;
                    }
                    List<Ingredient> ingredients = new ArrayList<Ingredient>();
                    for (JsonElement ingredientElement : json.getAsJsonArray("ingredients")) {
                        ingredients.add(CraftingHelper.getIngredient(
                                RecipeResourceConverter.convertIngredient(ingredientElement), context));
                    }
                    String condition = json.has("condition") ? json.get("condition").getAsString() : "none";
                    if ("witherstormmod:resummon_super_beacon".equals(type)) {
                        recipes.add(new Recipe(condition, ingredients, ItemStack.EMPTY,
                                json.get("entity").getAsString(),
                                json.has("nbt") ? json.get("nbt").getAsString() : ""));
                    } else {
                        recipes.add(new Recipe(condition, ingredients, convertResult(json, context), null, ""));
                    }
                } catch (Exception recipeException) {

                    WitherStormMod.LOGGER.warn("Skipping external super beacon recipe {}: {}",
                            entryName, recipeException.toString());
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load super beacon recipes from the external archive", exception);
        }
        if (recipes.size() != EXPECTED_RECIPE_COUNT) {
            throw new IllegalStateException("Expected " + EXPECTED_RECIPE_COUNT
                    + " external super beacon recipes but loaded " + recipes.size());
        }
        WitherStormMod.LOGGER.info("Loaded {} super beacon recipes from the external Wither Storm archive", recipes.size());
        return Collections.unmodifiableList(recipes);
    }

    private static ItemStack convertResult(JsonObject recipe, JsonContext context) throws IOException {
        JsonElement source = recipe.get("result");
        JsonObject result;
        if (source.isJsonPrimitive()) {
            result = new JsonObject();
            result.addProperty("item", source.getAsString());
        } else if (source.isJsonObject()) {
            result = source.getAsJsonObject().deepCopy();
        } else {
            throw new IOException("Super beacon result must be an item name or object");
        }
        if (!result.has("count") && recipe.has("count")) {
            result.add("count", recipe.get("count").deepCopy());
        }
        return CraftingHelper.getItemStack(RecipeResourceConverter.convertResult(result), context);
    }

    static List<Recipe> recipesForTesting() {
        return RECIPES;
    }

    public static List<Recipe> getRecipes() {
        return RECIPES;
    }

    public interface ConditionTester {
        boolean test(String condition);
    }

    public static final class Recipe {
        public final String condition;
        public final String entity;
        public final String entityNbt;
        private final List<Ingredient> ingredients;
        private final ItemStack result;

        private Recipe(String condition, List<Ingredient> ingredients, ItemStack result,
                       String entity, String entityNbt) {
            this.condition = condition;
            this.ingredients = ingredients;
            this.result = result.copy();
            this.entity = entity;
            this.entityNbt = entityNbt;
        }

        public boolean isEntityRecipe() {
            return entity != null;
        }

        public ItemStack createResult() {
            return result.copy();
        }

        public List<Ingredient> getIngredients() {
            return ingredients;
        }

        public ItemStack getResult() {
            return result.copy();
        }

        private boolean matches(List<ItemStack> inventory) {
            List<ItemStack> remaining = new ArrayList<ItemStack>();
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) remaining.add(stack);
            }
            if (remaining.size() != ingredients.size()) return false;
            for (Ingredient ingredient : ingredients) {
                int matching = -1;
                for (int i = 0; i < remaining.size(); i++) {
                    if (ingredient.apply(remaining.get(i))) {
                        matching = i;
                        break;
                    }
                }
                if (matching < 0) return false;
                remaining.remove(matching);
            }
            return remaining.isEmpty();
        }
    }
}
