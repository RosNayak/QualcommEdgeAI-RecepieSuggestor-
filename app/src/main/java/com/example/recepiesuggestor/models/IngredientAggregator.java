package com.example.recepiesuggestor.models;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * App-scoped aggregator for extracted ingredient nouns.
 * - Deduplicates while preserving insertion order.
 * - Live for the app process lifetime.
 * - Thread-safe.
 */
public final class IngredientAggregator {
    private static volatile IngredientAggregator INSTANCE;
    private final Context appContext;
    // Use a LinkedHashMap to deduplicate case-insensitively while preserving insertion order
    // Key: normalized lowercase form; Value: original first-seen casing for display
    private final java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
    private final List<AggregationListener> listeners = new ArrayList<>();
    private final Object lock = new Object();

    private IngredientAggregator(@NonNull Context ctx) {
        this.appContext = ctx.getApplicationContext();
    }

    public static IngredientAggregator getInstance(@NonNull Context ctx) {
        if (INSTANCE == null) {
            synchronized (IngredientAggregator.class) {
                if (INSTANCE == null) INSTANCE = new IngredientAggregator(ctx);
            }
        }
        return INSTANCE;
    }

    /** Add multiple items to the aggregated set. Ignores null/empty strings. */
    public void addAll(@NonNull Collection<String> items) {
        if (items.isEmpty()) return;
        boolean changed = false;
        synchronized (lock) {
            for (String s : items) {
                if (s == null) continue;
                String t = s.trim();
                if (t.isEmpty()) continue;
                String key = t.toLowerCase(java.util.Locale.ROOT);
                if (!map.containsKey(key)) {
                    map.put(key, t); // store first-seen casing
                    changed = true;
                }
            }
        }
        if (changed) notifyListeners();
    }

    /** Add a single ingredient. */
    public void add(@NonNull String item) {
        String t = item == null ? "" : item.trim();
        if (t.isEmpty()) return;
        boolean changed = false;
        synchronized (lock) {
            String key = t.toLowerCase(java.util.Locale.ROOT);
            if (!map.containsKey(key)) {
                map.put(key, t);
                changed = true;
            }
        }
        if (changed) notifyListeners();
    }

    public List<String> getAll() {
        synchronized (lock) {
            return new ArrayList<>(map.values());
        }
    }

    public void clear() {
        synchronized (lock) {
            map.clear();
        }
        notifyListeners();
    }

    // Listener support for UI components that want live updates
    public interface AggregationListener {
        void onIngredientsUpdated(@NonNull List<String> aggregated);
    }

    public void registerListener(@NonNull AggregationListener l) {
        synchronized (lock) {
            if (!listeners.contains(l)) listeners.add(l);
        }
        // Immediately notify with current snapshot on main thread
        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> l.onIngredientsUpdated(getAll()));
    }

    public void unregisterListener(@NonNull AggregationListener l) {
        synchronized (lock) {
            listeners.remove(l);
        }
    }

    private void notifyListeners() {
        List<AggregationListener> copy;
        List<String> snapshot = getAll();
        synchronized (lock) {
            copy = new ArrayList<>(listeners);
        }
        if (copy.isEmpty()) return;
        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> {
            for (AggregationListener l : copy) {
                try {
                    l.onIngredientsUpdated(snapshot);
                } catch (Exception ignored) {}
            }
        });
    }
}
