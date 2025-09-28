package com.example.recepiesuggestor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.camera.view.PreviewView;

import com.example.recepiesuggestor.config.ApiKeyManager;
import com.example.recepiesuggestor.data.IngredientAccumulator;
import com.example.recepiesuggestor.services.RecipeMatchingService;
import com.example.recepiesuggestor.services.SpeechRecognitionService;
import com.example.recepiesuggestor.ui.CameraXController;
import com.example.recepiesuggestor.utils.PermissionHelper;

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

                // Note: Recipes only update when user says "Update" voice command
                // Automatic recipe updates disabled - ingredients tracked but not triggering recipe refresh
            } catch (Exception e) {
                Log.e("ACC_POLL", "Failed to read accumulator", e);
            }
            accumulatorHandler.postDelayed(this, 2000);
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

        // Configure API key programmatically
        ApiKeyManager.getInstance(this).storeApiKey("AIzaSyDCdr4sP3eHLvtyQ0DN8GKodwWhL5RPh4M");

        // Recipe updates disabled on app start - only triggered by "Update" voice command
        Log.d("RECIPE_INIT", "App initialized - say 'Update' to generate recipes");

        PreviewView previewView = findViewById(R.id.camera_preview);
//        ImageButton switchBtn = findViewById(R.id.btn_switch_camera);

        cameraController = new CameraXController(this, previewView);
        PermissionHelper permissionHelper = new PermissionHelper(this);

        // Ask for camera permission, then start CameraX
        permissionHelper.ensureCameraPermission(
                () -> cameraController.start(),
                () -> Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        );

        // Ask for microphone permission, then start speech recognition (optional feature)
        permissionHelper.ensureMicrophonePermission(
                () -> {
                    speechService.startListening();
                    Toast.makeText(this, "Voice commands enabled - say 'Update' to refresh recipes", Toast.LENGTH_SHORT).show();
                },
                () -> {
                    Log.w("VOICE_COMMANDS", "Microphone permission denied - voice commands disabled");
                    Toast.makeText(this, "Voice commands disabled - say 'Update' manually to refresh recipes", Toast.LENGTH_SHORT).show();
                }
        );

        // Test button to manually trigger recipe generation (bypasses voice command issues)
        Button testRecipesBtn = findViewById(R.id.btn_test_recipes);
        if (testRecipesBtn != null) {
            testRecipesBtn.setOnClickListener(v -> {
                Log.d("TEST_BUTTON", "Test Recipes button clicked - manually triggering recipe update");
                Toast.makeText(this, "Manually updating recipes...", Toast.LENGTH_SHORT).show();

                try {
                    recipeMatchingService.updateRecipes(true);
                } catch (Exception e) {
                    Log.e("TEST_BUTTON", "Failed to update recipes via test button", e);
                    Toast.makeText(this, "Failed to update recipes", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            Log.d("VOICE_COMMAND", "Update command received from SpeechRecognitionService - forcing recipe refresh");
            Toast.makeText(this, "Voice command detected: Updating recipes...", Toast.LENGTH_SHORT).show();

            // Store current recipes as backup in case API fails
            List<Recipe> currentRecipes = new ArrayList<>(recipeList);

            // Force recipe update by calling the service directly
            try {
                recipeMatchingService.updateRecipes(true);
            } catch (Exception e) {
                Log.e("VOICE_COMMAND", "Failed to update recipes via voice command", e);
                // Keep current recipes if update fails
            }
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


}
