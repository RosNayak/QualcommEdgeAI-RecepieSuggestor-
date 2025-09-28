package com.example.recepiesuggestor.data;

import android.content.Context;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class IngredientAccumulator {

    private static IngredientAccumulator instance;

    // Use a synchronized Set to ensure thread-safe operations
    private final Map<String, String> detectedIngredients =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private IngredientAccumulator() {}

    public static synchronized IngredientAccumulator getInstance() {
        if (instance == null) {
            instance = new IngredientAccumulator();
        }
        return instance;
    }

    public void addIngredientName(Context context, String ingredientName) {
        if (ingredientName == null) return;
        String trimmed = ingredientName.trim();
        if (trimmed.isEmpty()) return;
        String key = trimmed.toLowerCase(java.util.Locale.ROOT);
        boolean added = false;
        synchronized (detectedIngredients) {
            if (!detectedIngredients.containsKey(key)) {
                detectedIngredients.put(key, trimmed); // store first-seen casing
                added = true;
            }
        }
        // Do not show Toasts here — UI will poll or listen to accumulator for updates.
    }

    /** Add multiple ingredient names (dedupes case-insensitively). */
    public void addIngredientNames(Context context, List<String> names) {
        if (names == null || names.isEmpty()) return;
        for (String n : names) addIngredientName(context, n);
    }

    /**
     * Gets a thread-safe copy of the currently detected ingredients.
     * @return A Set of ingredient names (Strings).
     */
    public Set<String> getCurrentIngredients() {
        // Return a new HashSet based on the existing synchronized set
        synchronized (detectedIngredients) {
            return new java.util.LinkedHashSet<>(detectedIngredients.values());
        }
    }

    public java.util.Map<String, String> getDetectedIngredientsMap() {
        synchronized (detectedIngredients) {
            return new java.util.LinkedHashMap<>(detectedIngredients);
        }
    }

    /**
     * Clear the accumulated ingredients.
     */
    public void clear() {
        detectedIngredients.clear();
    }
}
