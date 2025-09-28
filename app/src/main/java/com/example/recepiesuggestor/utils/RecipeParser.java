package com.example.recepiesuggestor.utils;

import com.example.recepiesuggestor.Recipe;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class RecipeParser {

    public static List<Recipe> parseRecipesFromJson(String jsonString) {
        List<Recipe> recipes = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject recipeJson = jsonArray.getJSONObject(i);

                String title = recipeJson.getString("title");
                String description = recipeJson.getString("description");
                String ingredients = recipeJson.getString("ingredients");
                String instructions = recipeJson.getString("instructions");

                Recipe recipe = new Recipe(
                    title,
                    description,
                    android.R.drawable.sym_def_app_icon,
                    ingredients,
                    instructions
                );

                recipes.add(recipe);
            }

        } catch (Exception e) {
            android.util.Log.e("RecipeParser", "Failed to parse recipes JSON", e);
        }

        return recipes;
    }
}