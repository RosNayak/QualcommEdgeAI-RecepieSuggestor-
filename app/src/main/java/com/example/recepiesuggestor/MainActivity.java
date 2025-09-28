package com.example.recepiesuggestor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.camera.view.PreviewView;

import com.example.recepiesuggestor.config.ApiKeyManager;
import com.example.recepiesuggestor.data.IngredientAccumulator;
import com.example.recepiesuggestor.models.ImageDescriberSingleton;
import com.example.recepiesuggestor.services.RecipeMatchingService;
import com.example.recepiesuggestor.services.SpeechRecognitionService;
import com.example.recepiesuggestor.ui.CameraXController;
import com.example.recepiesuggestor.utils.PermissionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        RecipeAdapter.OnRecipeClickListener,
        RecipeMatchingService.RecipeUpdateListener,
        SpeechRecognitionService.VoiceCommandListener {

    public static final String EXTRA_TITLE = "com.example.recepiesuggestor.EXTRA_TITLE";
    public static final String EXTRA_INGREDIENTS = "com.example.recepiesuggestor.EXTRA_INGREDIENTS";
    public static final String EXTRA_INSTRUCTIONS = "com.example.recepiesuggestor.EXTRA_INSTRUCTIONS";
    public static final String EXTRA_IMAGE_ID = "com.example.recepiesuggestor.EXTRA_IMAGE_ID";

    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;

    private CameraXController cameraController;
    private RecipeMatchingService recipeMatchingService;
    private SpeechRecognitionService speechService;

    private final Handler accumulatorHandler = new Handler(Looper.getMainLooper());
    private final Runnable accumulatorPoller = new Runnable() {
        @Override
        public void run() {
            try {
                java.util.Map<String,String> map = IngredientAccumulator.getInstance().getDetectedIngredientsMap();
                StringBuilder sb = new StringBuilder();
                sb.append("detectedIngredients = {\n");
                for (java.util.Map.Entry<String,String> e : map.entrySet()) {
                    sb.append("  ").append(e.getKey()).append(" : ").append(e.getValue()).append("\n");
                }
                sb.append("}");
                Log.d("INGREDIENTS_DEBUG", sb.toString());

                // Update recipes based on current ingredients
                recipeMatchingService.updateRecipes();
            } catch (Exception e) {
                Log.e("ACC_POLL", "Failed to read accumulator", e);
            }
            accumulatorHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize recipe list and adapter
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(recipeList, this);
        recyclerView.setAdapter(recipeAdapter);

        // Initialize recipe matching service
        recipeMatchingService = RecipeMatchingService.getInstance();
        recipeMatchingService.initialize(this);
        recipeMatchingService.setRecipeUpdateListener(this);

        // Initialize speech recognition service
        speechService = SpeechRecognitionService.getInstance(this);
        speechService.setVoiceCommandListener(this);

        // Check if API key is configured, prompt user if not
        checkApiKeyConfiguration();

        // Add some test ingredients to verify system works
        IngredientAccumulator.getInstance().addIngredientName(this, "tomatoes");
        IngredientAccumulator.getInstance().addIngredientName(this, "basil");
        IngredientAccumulator.getInstance().addIngredientName(this, "mozzarella");

        PreviewView previewView = findViewById(R.id.camera_preview);
        ImageButton switchBtn = findViewById(R.id.btn_switch_camera);

        cameraController = new CameraXController(this, previewView);
        PermissionHelper permissionHelper = new PermissionHelper(this);

        // Ask for camera permission, then start CameraX
        permissionHelper.ensureCameraPermission(
                () -> cameraController.start(),
                () -> Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        );

        // Ask for microphone permission, then start speech recognition
        permissionHelper.ensureMicrophonePermission(
                () -> speechService.startListening(),
                () -> Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        );

        if (switchBtn != null) {
            switchBtn.setOnClickListener(v -> cameraController.switchCamera());
        }

        // Initialize debug TextView if it exists (currently commented out in layout)
        // tvExtractedNouns = findViewById(R.id.tv_extracted_nouns);
        // ensure NLPTagger is initialized then extract nouns synchronously for debug
//        new Thread(() -> {
//            try {
//                NLPTagger tagger = NLPTagger.get(this);
////                tagger.init();
////                List<String> nouns = tagger.extractNouns("fresh tomatoes basil and mozzarella cheese");
////                String joined = nouns.toString();
////                runOnUiThread(() -> tvExtractedNouns.setText("Nouns: " + joined));
//            } catch (IOException e) {
//                Log.e("NLP_DEBUG", "Failed to init NLPTagger", e);
////                runOnUiThread(() -> tvExtractedNouns.setText("NLP init failed: " + e.getMessage()));
//            } catch (IllegalStateException e) {
//                Log.e("NLP_DEBUG", "NLP not initialized", e);
////                runOnUiThread(() -> tvExtractedNouns.setText("NLP not initialized: " + e.getMessage()));
//            }
//        }).start();

        // Also call the singleton debug logger which logs to logcat
        ImageDescriberSingleton singleton = ImageDescriberSingleton.getInstance(this);
        singleton.debugLogNouns("fresh tomatoes basil and mozzarella cheese");
        // Register to receive live noun updates from image descriptions (we log them)
        singleton.setNounsListener(nouns ->
            Log.d("ING_LISTENER", "Received nouns: " + nouns)
        );
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_TITLE, recipe.getTitle());
        intent.putExtra(EXTRA_INGREDIENTS, recipe.getIngredients());
        intent.putExtra(EXTRA_INSTRUCTIONS, recipe.getInstructions());
        intent.putExtra(EXTRA_IMAGE_ID, recipe.getImageResourceId());
        startActivity(intent);
    }

    @Override
    public void onRecipesUpdated(List<Recipe> recipes) {
        runOnUiThread(() -> {
            int oldSize = recipeList.size();
            recipeList.clear();
            recipeList.addAll(recipes);

            if (oldSize == 0 && recipes.size() > 0) {
                recipeAdapter.notifyItemRangeInserted(0, recipes.size());
            } else if (oldSize > 0 && recipes.size() == 0) {
                recipeAdapter.notifyItemRangeRemoved(0, oldSize);
            } else {
                recipeAdapter.notifyDataSetChanged();
            }

            Log.d("RECIPE_UPDATE", "Updated RecyclerView with " + recipes.size() + " recipes");
        });
    }

    @Override
    public void onUpdateCommand() {
        runOnUiThread(() -> {
            Log.d("VOICE_COMMAND", "Update command received - forcing recipe refresh");
            Toast.makeText(this, "Updating recipes...", Toast.LENGTH_SHORT).show();

            // Force recipe update by calling the service directly
            recipeMatchingService.updateRecipes(true);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        accumulatorHandler.post(accumulatorPoller);
    }

    @Override
    protected void onStop() {
        super.onStop();
        accumulatorHandler.removeCallbacks(accumulatorPoller);
        if (speechService != null) {
            speechService.stopListening();
        }
    }

    private void checkApiKeyConfiguration() {
        ApiKeyManager apiKeyManager = ApiKeyManager.getInstance(this);
        if (apiKeyManager.getApiKey() == null) {
            showApiKeyDialog();
        }
    }

    private void showApiKeyDialog() {
        EditText editText = new EditText(this);
        editText.setHint("Enter Gemini API Key");

        new AlertDialog.Builder(this)
                .setTitle("Configure Gemini API")
                .setMessage("Please enter your Gemini API key to generate recipes:")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String apiKey = editText.getText().toString().trim();
                    if (!apiKey.isEmpty()) {
                        ApiKeyManager.getInstance(this).storeApiKey(apiKey);
                        Toast.makeText(this, "API key saved securely", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }
}
