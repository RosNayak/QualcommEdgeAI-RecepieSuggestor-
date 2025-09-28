package com.example.recepiesuggestor.models;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

public final class NLPTagger {

    private static volatile NLPTagger INSTANCE;

    private final Context appContext;
    private final ExecutorService bg;
    private SimpleTokenizer tokenizer;
    private POSTaggerME posTagger;
    private final Object posLock = new Object();

    /** Change this if you use a different filename. */
    // Try perceptron first (often better), then maxent as fallback. Both are in assets.
    private static final String[] POS_MODEL_ASSET_PATHS = {"en-pos-perceptron.bin", "en-pos-maxent.bin"};

    // Lightweight stopwords for fallback heuristic
    private static final Set<String> STOPWORDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "a", "an", "the", "and", "or", "but", "with", "without", "of", "in", "on", "for", "to",
        "from", "by", "is", "are", "was", "were", "be", "been", "this", "that", "these", "those",
        "it", "its", "as", "at", "about", "into", "over", "under", "other", "some"
    ));

    private NLPTagger(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.bg = Executors.newSingleThreadExecutor();
    }

    public static NLPTagger get(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (NLPTagger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NLPTagger(context);
                }
            }
        }
        return INSTANCE;
    }

    /** Call once (e.g., in Application.onCreate or first use). */
    public void init() throws IOException {
        if (tokenizer != null && posTagger != null) return;

        tokenizer = SimpleTokenizer.INSTANCE;
        // Try multiple model files present in assets
        POSModel posModel = null;
        for (String p : POS_MODEL_ASSET_PATHS) {
            try (InputStream in = appContext.getAssets().open(p)) {
                posModel = new POSModel(in);
                break;
            } catch (IOException ignored) {
                // try next
            }
        }
        if (posModel != null) {
            posTagger = new POSTaggerME(posModel);
            android.util.Log.d("NLP_INIT", "Loaded POS model and initialized POSTaggerME");
        } else {
            android.util.Log.d("NLP_INIT", "No POS model found in assets: tried " + java.util.Arrays.toString(POS_MODEL_ASSET_PATHS));
        }
    }

    /** Synchronous noun extraction (NN, NNS, NNP, NNPS). */
    @NonNull
    public List<String> extractNouns(@NonNull String text) {
        if (text.isEmpty()) return new ArrayList<>();
        if (tokenizer == null || posTagger == null) {
            throw new IllegalStateException("NLP not initialized. Call NLPTagger.init() first.");
        }

        // Try POS tagging first
        Set<String> nouns = new LinkedHashSet<>();
        try {
            if (posTagger != null) {
                String[] tokens = tokenizer.tokenize(text);
                String[] tags;
                synchronized (posLock) {
                    tags = posTagger.tag(tokens);
                }
                for (int i = 0; i < tokens.length; i++) {
                    String tag = tags[i];
                    if (tag != null && (tag.equals("NN") || tag.equals("NNS") || tag.equals("NNP") || tag.equals("NNPS"))) {
                        nouns.add(tokens[i]);
                    }
                }
            }
        } catch (Exception e) {
            // ignore and fall through to heuristics
        }

        // If POS tagging found no nouns, try improved strategies:
        if (nouns.isEmpty()) {
            // Strategy 1: prepend a short context sentence to help the tagger
            String cleaned = text == null ? "" : text.trim();
            if (!cleaned.endsWith(".") && !cleaned.endsWith("!") && !cleaned.endsWith("?")) {
                cleaned = cleaned + ".";
            }
            String prefixed = "The image contains " + cleaned;
            try {
                if (posTagger != null) {
                    String[] tokens = tokenizer.tokenize(prefixed);
                    String[] tags;
                    synchronized (posLock) {
                        tags = posTagger.tag(tokens);
                    }
                    for (int i = 0; i < tokens.length; i++) {
                        String tag = tags[i];
                        if (tag != null && (tag.equals("NN") || tag.equals("NNS") || tag.equals("NNP") || tag.equals("NNPS"))) {
                            nouns.add(tokens[i]);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Final heuristic fallback: split on conjunctions and separators and filter stopwords
        if (nouns.isEmpty()) {
            // Split on common conjunctions/separators (word-boundaries for 'and/on/in/with'), commas, semicolons,
            // forward slash, or backslash. Use proper escaping for Java string literals.
            String[] parts = text.split("\\band\\b|,|;|\\bon\\b|\\bin\\b|\\bwith\\b|/|\\\\");
            for (String p : parts) {
                String w = p.trim();
                if (w.isEmpty()) continue;
                // take last word of the part (likely noun)
                String[] words = w.split("\\s+");
                String candidate = words[words.length - 1].replaceAll("[^a-zA-Z0-9]", "");
                String lc = candidate.toLowerCase();
                if (candidate.length() > 1 && !STOPWORDS.contains(lc) && lc.matches(".*[a-zA-Z].*")) {
                    nouns.add(candidate);
                }
            }
        }

        return new ArrayList<>(nouns);
    }

    /** Example async helper if you prefer off the UI thread. */
    public interface NounsCallback {
        void onNounsReady(@NonNull List<String> nouns);
        void onError(@NonNull Exception e);
    }

    public void extractNounsAsync(@NonNull String text, @NonNull NounsCallback cb) {
        bg.execute(() -> {
            try {
                if (tokenizer == null || posTagger == null) init();
                List<String> nouns = extractNouns(text);
                cb.onNounsReady(nouns);
            } catch (Exception e) {
                cb.onError(e);
            }
        });
    }
}
