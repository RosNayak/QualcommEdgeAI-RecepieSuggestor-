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
        updateRecipes(false);
    }

    public void updateRecipes(boolean forceUpdate) {
        Set<String> availableIngredients = IngredientAccumulator.getInstance().getCurrentIngredients();
        List<String> ingredientList = new ArrayList<>(availableIngredients);

        Log.d("RECIPE_MATCHING", "Available ingredients: " + ingredientList);

        if (ingredientList.isEmpty()) {
            if (listener != null) {
                listener.onRecipesUpdated(new ArrayList<>());
            }
            return;
        }

        // Only call API when explicitly forced (voice command "Update")
        // Automatic updates on ingredient changes disabled
        if (forceUpdate) {
            lastIngredients = new ArrayList<>(ingredientList);

            if (geminiService != null) {
                Log.d("RECIPE_MATCHING", "Calling Gemini API (forced by voice command)");
                geminiService.generateRecipes(ingredientList, this);
            }
        } else {
            Log.d("RECIPE_MATCHING", "Skipping API call - only updating on voice command");
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
        // Don't clear recipes on API errors - keep existing ones
        Log.w("RECIPE_MATCHING", "Keeping existing recipes due to API error: " + error);

        // Error logged but no toast shown to user
    }
}