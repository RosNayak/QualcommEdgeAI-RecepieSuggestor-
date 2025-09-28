package com.example.recepiesuggestor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.view.PreviewView;

import com.example.recepiesuggestor.data.IngredientAccumulator;
import com.example.recepiesuggestor.models.ImageDescriberSingleton;
import com.example.recepiesuggestor.models.NLPTagger;
import com.example.recepiesuggestor.ui.CameraXController;
import com.example.recepiesuggestor.utils.PermissionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    public static final String EXTRA_TITLE = "com.example.recepiesuggestor.EXTRA_TITLE";
    public static final String EXTRA_INGREDIENTS = "com.example.recepiesuggestor.EXTRA_INGREDIENTS";
    public static final String EXTRA_INSTRUCTIONS = "com.example.recepiesuggestor.EXTRA_INSTRUCTIONS";
    public static final String EXTRA_IMAGE_ID = "com.example.recepiesuggestor.EXTRA_IMAGE_ID";

    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;

    private CameraXController cameraController;

    private TextView tvExtractedNouns;
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
                tvExtractedNouns.setText(sb.toString());
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

        // Create dummy data
        recipeList = new ArrayList<>();
//        recipeList.add(new Recipe("Pancakes", "Fluffy pancakes with syrup", android.R.drawable.sym_def_app_icon, "Ingredients: Flour, Eggs, Milk, Sugar, Baking Powder, Syrup", "Instructions: Mix dry ingredients. Mix wet ingredients. Combine. Cook on griddle."));
//        recipeList.add(new Recipe("Omelette", "Cheesy vegetable omelette", android.R.drawable.sym_def_app_icon, "Ingredients: Eggs, Cheese, Bell Peppers, Onions, Milk, Butter", "Instructions: Whisk eggs and milk. Sauté vegetables. Pour eggs into pan. Add cheese and vegetables. Fold and cook."));
//        recipeList.add(new Recipe("Pasta Carbonara", "Creamy pasta with bacon and eggs", android.R.drawable.sym_def_app_icon, "Ingredients: Spaghetti, Bacon, Eggs, Parmesan Cheese, Black Pepper", "Instructions: Cook spaghetti. Fry bacon. Whisk eggs and cheese. Combine all with cooked pasta."));
//        recipeList.add(new Recipe("Chicken Salad", "Healthy chicken salad with greens", android.R.drawable.sym_def_app_icon, "Ingredients: Cooked Chicken, Lettuce, Tomatoes, Cucumber, Mayonnaise, Lemon Juice", "Instructions: Chop chicken and vegetables. Mix with mayonnaise and lemon juice. Serve on lettuce."));

        // Initialize and set adapter
        recipeAdapter = new RecipeAdapter(recipeList, this);
        recyclerView.setAdapter(recipeAdapter);

        PreviewView previewView = findViewById(R.id.camera_preview);
        ImageButton switchBtn = findViewById(R.id.btn_switch_camera);

        cameraController = new CameraXController(this, previewView);
        PermissionHelper permissionHelper = new PermissionHelper(this);

        // Ask for camera permission, then start CameraX
        permissionHelper.ensureCameraPermission(
                () -> cameraController.start(),
                () -> Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        );

        if (switchBtn != null) {
            switchBtn.setOnClickListener(v -> cameraController.switchCamera());
        }

        // Temporary debug: show extracted nouns for a sample sentence and call debug logger
//        tvExtractedNouns = findViewById(R.id.tv_extracted_nouns);
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
        singleton.setNounsListener(nouns -> {
            Log.d("ING_LISTENER", "Received nouns: " + String.valueOf(nouns));
        });
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
    protected void onStart() {
        super.onStart();
        accumulatorHandler.post(accumulatorPoller);
    }

    @Override
    protected void onStop() {
        super.onStop();
        accumulatorHandler.removeCallbacks(accumulatorPoller);
    }
}
