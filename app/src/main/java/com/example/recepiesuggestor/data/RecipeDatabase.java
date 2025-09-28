package com.example.recepiesuggestor.data;

import com.example.recepiesuggestor.Recipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipeDatabase {

    private static List<Recipe> recipes = new ArrayList<>();

    static {
        initializeRecipes();
    }

    private static void initializeRecipes() {
        recipes.add(new Recipe(
            "Tomato Basil Salad",
            "Fresh tomato salad with basil and mozzarella",
            android.R.drawable.sym_def_app_icon,
            "tomatoes,basil,mozzarella,olive oil,salt",
            "1. Slice tomatoes\n2. Tear basil leaves\n3. Add mozzarella chunks\n4. Drizzle olive oil\n5. Season with salt"
        ));

        recipes.add(new Recipe(
            "Scrambled Eggs",
            "Simple and fluffy scrambled eggs",
            android.R.drawable.sym_def_app_icon,
            "eggs,milk,butter,salt,pepper",
            "1. Crack eggs into bowl\n2. Add milk and whisk\n3. Heat butter in pan\n4. Pour eggs and stir gently\n5. Season to taste"
        ));

        recipes.add(new Recipe(
            "Banana Smoothie",
            "Creamy banana smoothie",
            android.R.drawable.sym_def_app_icon,
            "banana,milk,honey,ice",
            "1. Peel banana\n2. Add to blender with milk\n3. Add honey for sweetness\n4. Blend with ice\n5. Serve chilled"
        ));

        recipes.add(new Recipe(
            "Apple Slices with Peanut Butter",
            "Healthy apple and peanut butter snack",
            android.R.drawable.sym_def_app_icon,
            "apple,peanut butter",
            "1. Wash and slice apple\n2. Serve with peanut butter for dipping"
        ));

        recipes.add(new Recipe(
            "Cheese Toast",
            "Quick grilled cheese toast",
            android.R.drawable.sym_def_app_icon,
            "bread,cheese,butter",
            "1. Butter bread slices\n2. Add cheese between slices\n3. Grill until golden brown\n4. Serve hot"
        ));

        recipes.add(new Recipe(
            "Vegetable Stir Fry",
            "Mixed vegetable stir fry",
            android.R.drawable.sym_def_app_icon,
            "carrots,broccoli,bell pepper,onion,oil,soy sauce",
            "1. Chop all vegetables\n2. Heat oil in pan\n3. Stir fry vegetables\n4. Add soy sauce\n5. Cook until tender"
        ));

        recipes.add(new Recipe(
            "Pasta with Tomato Sauce",
            "Classic pasta with tomato sauce",
            android.R.drawable.sym_def_app_icon,
            "pasta,tomatoes,garlic,olive oil,basil",
            "1. Cook pasta according to package\n2. Sauté garlic in olive oil\n3. Add chopped tomatoes\n4. Add basil\n5. Toss with pasta"
        ));

        recipes.add(new Recipe(
            "Chicken Salad",
            "Fresh chicken and vegetable salad",
            android.R.drawable.sym_def_app_icon,
            "chicken,lettuce,cucumber,tomatoes,carrots",
            "1. Cook and dice chicken\n2. Chop vegetables\n3. Mix all ingredients\n4. Add dressing of choice\n5. Serve fresh"
        ));
    }

    public static List<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes);
    }

    public static List<Recipe> getMatchingRecipes(List<String> availableIngredients) {
        List<Recipe> matchingRecipes = new ArrayList<>();

        for (Recipe recipe : recipes) {
            if (canMakeRecipe(recipe, availableIngredients)) {
                matchingRecipes.add(recipe);
            }
        }

        return matchingRecipes;
    }

    private static boolean canMakeRecipe(Recipe recipe, List<String> availableIngredients) {
        String[] requiredIngredients = recipe.getIngredients().toLowerCase().split(",");
        android.util.Log.d("RECIPE_MATCH", "Checking recipe: " + recipe.getTitle());
        android.util.Log.d("RECIPE_MATCH", "Required: " + java.util.Arrays.toString(requiredIngredients));
        android.util.Log.d("RECIPE_MATCH", "Available: " + availableIngredients);

        for (String required : requiredIngredients) {
            required = required.trim();
            boolean found = false;

            for (String available : availableIngredients) {
                if (available.toLowerCase().contains(required) || required.contains(available.toLowerCase())) {
                    android.util.Log.d("RECIPE_MATCH", "Match found: '" + required + "' with '" + available + "'");
                    found = true;
                    break;
                }
            }

            if (!found) {
                android.util.Log.d("RECIPE_MATCH", "Missing ingredient: '" + required + "'");
                return false;
            }
        }

        android.util.Log.d("RECIPE_MATCH", "Recipe " + recipe.getTitle() + " CAN be made!");
        return true;
    }
}