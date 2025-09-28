package com.example.recepiesuggestor.services;

import android.content.Context;
import android.util.Log;
import com.example.recepiesuggestor.Recipe;
import com.example.recepiesuggestor.data.IngredientAccumulator;
import com.example.recepiesuggestor.utils.RecipeParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecipeMatchingService implements GeminiApiService.RecipeGenerationCallback {

    public interface RecipeUpdateListener {
        void onRecipesUpdated(List<Recipe> recipes);
    }

    private static RecipeMatchingService instance;
    private RecipeUpdateListener listener;
    private Context context;
    private GeminiApiService geminiService;
    private List<String> lastIngredients = new ArrayList<>();

    private RecipeMatchingService() {}

    public static synchronized RecipeMatchingService getInstance() {
        if (instance == null) {
            instance = new RecipeMatchingService();
        }
        return instance;
    }

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.geminiService = GeminiApiService.getInstance(context);
    }

    public void setRecipeUpdateListener(RecipeUpdateListener listener) {
        this.listener = listener;
    }

    public void updateRecipes() {
        Set<String> availableIngredients = IngredientAccumulator.getInstance().getCurrentIngredients();
        List<String> ingredientList = new ArrayList<>(availableIngredients);

        Log.d("RECIPE_MATCHING", "Available ingredients: " + ingredientList);

        if (ingredientList.isEmpty()) {
            if (listener != null) {
                listener.onRecipesUpdated(new ArrayList<>());
            }
            return;
        }

        // Only call API if ingredients changed
        if (!ingredientList.equals(lastIngredients)) {
            lastIngredients = new ArrayList<>(ingredientList);

            if (geminiService != null) {
                geminiService.generateRecipes(ingredientList, this);
            }
        }
    }

    @Override
    public void onSuccess(String recipesJson) {
        List<Recipe> recipes = RecipeParser.parseRecipesFromJson(recipesJson);
        Log.d("RECIPE_MATCHING", "Generated " + recipes.size() + " recipes from Gemini");

        if (listener != null) {
            listener.onRecipesUpdated(recipes);
        }
    }

    @Override
    public void onError(String error) {
        Log.e("RECIPE_MATCHING", "Failed to generate recipes: " + error);
        if (listener != null) {
            listener.onRecipesUpdated(new ArrayList<>());
        }
    }
}