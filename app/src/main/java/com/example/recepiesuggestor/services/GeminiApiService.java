package com.example.recepiesuggestor.services;

import android.content.Context;
import android.util.Log;
import com.example.recepiesuggestor.config.ApiKeyManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiApiService {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";
    private static final String TAG = "GeminiApiService";

    public interface RecipeGenerationCallback {
        void onSuccess(String recipesJson);
        void onError(String error);
    }

    private static GeminiApiService instance;
    private final ExecutorService executor;
    private final Context context;

    private GeminiApiService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized GeminiApiService getInstance(Context context) {
        if (instance == null) {
            instance = new GeminiApiService(context);
        }
        return instance;
    }

    public void generateRecipes(List<String> ingredients, RecipeGenerationCallback callback) {
        Log.d(TAG, "generateRecipes called with ingredients: " + ingredients);
        executor.execute(() -> {
            try {
                String apiKey = ApiKeyManager.getInstance(context).getApiKey();
                if (apiKey == null) {
                    Log.e(TAG, "API key not configured");
                    callback.onError("API key not configured");
                    return;
                }

                Log.d(TAG, "API key found, building prompt");
                String prompt = buildPrompt(ingredients);
                Log.d(TAG, "Making API call to Gemini");
                String response = makeApiCall(apiKey, prompt);
                Log.d(TAG, "Gemini API call successful");
                callback.onSuccess(response);

            } catch (Exception e) {
                Log.e(TAG, "Failed to generate recipes", e);
                callback.onError(e.getMessage());
            }
        });
    }

    private String buildPrompt(List<String> ingredients) {
        StringBuilder sb = new StringBuilder();
        sb.append("Given these detected items: ");
        sb.append(String.join(", ", ingredients));
        sb.append("\n\nIMPORTANT: Only consider items that are FOOD INGREDIENTS. ");
        sb.append("Ignore all non-food items including: people, body parts, clothing, shoes, furniture, background objects, surfaces, materials, and any other non-edible items. ");
        sb.append("Focus only on actual food items like vegetables, fruits, meat, dairy, spices, etc.");
        sb.append("\n\nFrom the actual food ingredients identified, generate 3-5 realistic recipes. ");
        sb.append("If no food ingredients are found, return an empty array [].");
        sb.append("\n\nReturn ONLY a JSON array with this exact format:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"title\": \"Recipe Name\",\n");
        sb.append("    \"description\": \"Brief description\",\n");
        sb.append("    \"ingredients\": \"ingredient1,ingredient2,ingredient3\",\n");
        sb.append("    \"instructions\": \"Step 1\\nStep 2\\nStep 3\"\n");
        sb.append("  }\n");
        sb.append("]\n");
        sb.append("Make recipes practical and realistic. Use common cooking techniques.");

        return sb.toString();
    }

    private String makeApiCall(String apiKey, String prompt) throws Exception {
        URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            if (responseCode != 200) {
                throw new Exception("API call failed: " + response.toString());
            }

            return extractTextFromResponse(response.toString());
        }
    }

    private String extractTextFromResponse(String response) throws Exception {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray candidates = jsonResponse.getJSONArray("candidates");
        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        JSONObject part = parts.getJSONObject(0);

        String text = part.getString("text");

        // Extract JSON from the response text
        int startIndex = text.indexOf('[');
        int endIndex = text.lastIndexOf(']') + 1;

        if (startIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex);
        }

        throw new Exception("Invalid response format");
    }
}