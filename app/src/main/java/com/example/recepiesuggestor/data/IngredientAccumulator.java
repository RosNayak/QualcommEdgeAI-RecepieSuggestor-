package com.example.recepiesuggestor.data;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class IngredientAccumulator {

    private static IngredientAccumulator instance;

    // Use a synchronized Set to ensure thread-safe operations
    private final Set<String> detectedIngredients =
            Collections.synchronizedSet(new HashSet<>());

    private IngredientAccumulator() {}

    public static synchronized IngredientAccumulator getInstance() {
        if (instance == null) {
            instance = new IngredientAccumulator();
        }
        return instance;
    }

    public void addLabels(Context context, List<com.google.mlkit.vision.label.ImageLabel> labels) {
        for (com.google.mlkit.vision.label.ImageLabel label : labels) {
            String ingredientName = label.getText();
            detectedIngredients.add(ingredientName);
            Toast.makeText(context, ingredientName, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Gets a thread-safe copy of the currently detected ingredients.
     * @return A Set of ingredient names (Strings).
     */
    public Set<String> getCurrentIngredients() {
        // Return a new HashSet based on the existing synchronized set
        return new HashSet<>(detectedIngredients);
    }

    /**
     * Clear the accumulated ingredients.
     */
    public void clear() {
        detectedIngredients.clear();
    }
}
