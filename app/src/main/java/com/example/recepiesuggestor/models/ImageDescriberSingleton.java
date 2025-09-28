package com.example.recepiesuggestor.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.imagedescription.ImageDescriber;
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions;
import com.google.mlkit.genai.imagedescription.ImageDescription;
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest;
import com.google.mlkit.genai.common.GenAiException;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import com.example.recepiesuggestor.data.IngredientAccumulator;

// for POS tagging
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageDescriberSingleton {
    // 1. The single instance of the class
    private static ImageDescriberSingleton instance;

    private static Context activityContext;

    private final ImageDescriber imageDescriber;

    // Use NLPTagger singleton for POS tagging (ensures single place for model init)
    // Listener for live noun updates
    public interface NounsListener {
        void onNouns(List<String> nouns);
    }

    private NounsListener nounsListener;

    // 3. Private constructor to prevent direct instantiation
    private ImageDescriberSingleton(Context context) {
        // Initialize the ImageLabeler here
        // Using a custom confidence threshold (e.g., 70%) for demonstration
        ImageDescriberOptions options =
                ImageDescriberOptions.builder(context).build();

        // Create the labeler instance
        imageDescriber = ImageDescription.getClient(options);
        activityContext = context;

        try {
            // Delegate initialization to NLPTagger which handles assets and threading
            com.example.recepiesuggestor.models.NLPTagger.get(context).init();
        } catch (Exception e) {
            Log.e("NLP_INIT", "Error initializing NLPTagger", e);
        }
    }

    // 4. Public method to get the single instance
    public static synchronized ImageDescriberSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new ImageDescriberSingleton(context);
        }
        return instance;
    }

    public void setNounsListener(NounsListener listener) {
        this.nounsListener = listener;
    }

    public void prepareAndStartImageDescription(
            Bitmap bitmap,
            ImageProxy imageProxy
    ) throws ExecutionException, InterruptedException {
        // Check feature availability, status will be one of the following:
        // UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE
        try {
            int featureStatus = imageDescriber.checkFeatureStatus().get();
            if (featureStatus == FeatureStatus.DOWNLOADABLE) {
                imageDescriber.downloadFeature(new DownloadCallback() {
                    @Override
                    public void onDownloadCompleted() {
                        startImageDescriptionRequest(bitmap, imageProxy);
                    }

                    @Override
                    public void onDownloadFailed(GenAiException e) { imageProxy.close();
                        Log.d("ingredients", "I am failed");}

                    @Override
                    public void onDownloadProgress(long totalBytesDownloaded) { Log.d("ingredients", "I am progress " + Long.toString(totalBytesDownloaded));}

                    @Override
                    public void onDownloadStarted(long bytesDownloaded) {Log.d("ingredients", "I am started");}
                });
            } else if (featureStatus == FeatureStatus.DOWNLOADING) {
                startImageDescriptionRequest(bitmap, imageProxy);
            } else if (featureStatus == FeatureStatus.AVAILABLE) {
                startImageDescriptionRequest(bitmap, imageProxy);
            } else if (featureStatus == FeatureStatus.UNAVAILABLE) {
                imageProxy.close();
            }
        } catch (ExecutionException | InterruptedException e) {
            imageProxy.close();
        }
    }

    public void startImageDescriptionRequest(
            Bitmap bitmap,
            ImageProxy imageProxy
    ) {
        ImageDescriptionRequest imageDescriptionRequest =
                ImageDescriptionRequest.builder(bitmap).build();

        imageDescriber.runInference(imageDescriptionRequest, newText -> {
            Log.d("ingredients", newText);

            // First try POS-based extraction via NLPTagger (delegated in extractNouns)
            List<String> nouns = extractNouns(newText);
            // If empty, run a simple fallback extractor here to ensure we return something
            if (nouns == null || nouns.isEmpty()) {
                List<String> fb = fallbackExtractNouns(newText);
                if (!fb.isEmpty()) nouns = fb;
            }

            // Add to app-scoped accumulator (deduplicated, preserved order)
            try {
                if (nouns != null && !nouns.isEmpty()) {
                    IngredientAccumulator.getInstance().addIngredientNames(activityContext, nouns);
                }
            } catch (Exception e) {
                Log.e("ING_ACC", "Failed to add nouns to IngredientAccumulator", e);
            }

            // Retrieve the cumulative (aggregated) set and log that so the log shows history
            try {
                java.util.Set<String> aggregated = IngredientAccumulator.getInstance().getCurrentIngredients();
                // Preserve insertion order by converting to a list for nicer formatting
                java.util.List<String> aggList = new java.util.ArrayList<>(aggregated);
                Log.d("ingredients_nouns", "Nouns: " + aggList.toString());
            } catch (Exception e) {
                Log.d("ingredients_nouns", "Nouns: " + String.valueOf(nouns));
            }

            // Notify listener on main thread if present (send only the current step nouns)
            if (nounsListener != null) {
                android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                List<String> finalNouns = nouns;
                h.post(() -> nounsListener.onNouns(finalNouns));
            }

            imageProxy.close();
        });
    }

    // Lightweight fallback extractor: split on non-letters, remove stopwords, dedupe
    private List<String> fallbackExtractNouns(String text) {
        List<String> nouns = new ArrayList<>();
        if (text == null || text.isEmpty()) return nouns;
        // small stopword set
        java.util.Set<String> stop = new java.util.HashSet<>(java.util.Arrays.asList(
                "a", "an", "the", "and", "or", "but", "with", "without", "of", "in", "on", "for",
                "to", "from", "by", "is", "are", "was", "were", "be", "been", "this", "that", "these",
                "those", "it", "its", "as", "at", "about", "into", "over", "under", "other", "some", "next"
        ));

        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();

        // Common patterns in image descriptions; capture groups that often contain objects
        String[] patterns = new String[]{
                "holds? (?:a |an |the )?([\\w\\s-]+?)(?:\\.|,| and |$)",
                "contains? (?:a |an |the )?([\\w\\s-]+?)(?:\\.|,| and |$)",
                "on (?:a |an |the )?([\\w\\s-]+?)(?:\\.|,| and |$)",
                "next to (?:a |an |the )?([\\w\\s-]+?)(?:\\.|,| and |$)",
                "with (?:a |an |the )?([\\w\\s-]+?)(?:\\.|,| and |$)",
                "there (?:is|are) (?:a |an |the )?([\\w\\s-]+?)(?:\\.|,| and |$)",
                "(?:a |an |the )([\\w\\s-]+?)(?:\\.|,| and |$)"
        };

        android.util.Log.d("NLP_FALLBACK", "Fallback extractor input: '" + text + "'");
        for (String pat : patterns) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pat, java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m = p.matcher(text);
                while (m.find()) {
                    String g = m.group(1);
                    android.util.Log.d("NLP_FALLBACK", "Pattern: '" + pat + "' matched group: '" + g + "'");
                    if (g == null) continue;
                    String cleaned = g.trim();
                    // split on ' and ' to get multiple items inside the group
                    String[] parts = cleaned.split("\\band\\b|,|;|\\band other\\b");
                    for (String part : parts) {
                        String candidate = part.trim().replaceAll("[^A-Za-z0-9\\s-]", "");
                        if (candidate.isEmpty()) continue;
                        // take last token as likely noun head
                        String[] tokens = candidate.split("\\s+");
                        String head = tokens[tokens.length - 1];
                        String lc = head.toLowerCase();
                        if (head.length() > 1 && !stop.contains(lc) && !lc.matches("^[0-9]+$")) {
                            android.util.Log.d("NLP_FALLBACK", "Adding candidate noun: '" + head + "'");
                            set.add(head);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // As a final fallback, pick words longer than 2 characters that aren't stopwords
        if (set.isEmpty()) {
            String[] words = text.split("[^A-Za-z0-9]+");
            for (String w : words) {
                if (w == null) continue;
                String s = w.trim();
                if (s.length() <= 2) continue;
                String lc = s.toLowerCase();
                if (stop.contains(lc)) continue;
                if (lc.matches("^[0-9]+$")) continue;
                set.add(s);
            }
        }

        nouns.addAll(set);
        android.util.Log.d("NLP_FALLBACK", "Fallback extractor result: " + nouns.toString());
        return nouns;
    }


    private List<String> extractNouns(String text) {
        // Delegate to NLPTagger for extraction. Return empty list on failure.
        try {
            com.example.recepiesuggestor.models.NLPTagger tagger = com.example.recepiesuggestor.models.NLPTagger.get(activityContext);
            // Ensure initialized (NLPTagger.init() is idempotent)
            try {
                tagger.init();
            } catch (Exception ignored) {
                // init may have already been called or fail; extractNouns will throw if not ready
            }
            return tagger.extractNouns(text);
        } catch (Exception e) {
            Log.e("NLP_EXTRACT", "Failed to extract nouns", e);
            return new ArrayList<>();
        }
    }

    /**
     * Debug helper: run the POS extractor on arbitrary text and log the nouns.
     * Call from an Activity or any Context-aware place via
     * ImageDescriberSingleton.getInstance(context).debugLogNouns("text to analyze");
     */
    public void debugLogNouns(String text) {
        List<String> nouns = extractNouns(text);
    }



    // Optional: Close the labeler when the app shuts down (e.g., in onDestroy)
    public void close() {
        imageDescriber.close();
    }
}
