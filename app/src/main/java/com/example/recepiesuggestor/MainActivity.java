package com.example.recepiesuggestor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create dummy data
        recipeList = new ArrayList<>();
        recipeList.add(new Recipe("Pancakes", "Fluffy pancakes with syrup", android.R.drawable.sym_def_app_icon, "Ingredients: Flour, Eggs, Milk, Sugar, Baking Powder, Syrup", "Instructions: Mix dry ingredients. Mix wet ingredients. Combine. Cook on griddle."));
        recipeList.add(new Recipe("Omelette", "Cheesy vegetable omelette", android.R.drawable.sym_def_app_icon, "Ingredients: Eggs, Cheese, Bell Peppers, Onions, Milk, Butter", "Instructions: Whisk eggs and milk. Sauté vegetables. Pour eggs into pan. Add cheese and vegetables. Fold and cook."));
        recipeList.add(new Recipe("Pasta Carbonara", "Creamy pasta with bacon and eggs", android.R.drawable.sym_def_app_icon, "Ingredients: Spaghetti, Bacon, Eggs, Parmesan Cheese, Black Pepper", "Instructions: Cook spaghetti. Fry bacon. Whisk eggs and cheese. Combine all with cooked pasta."));
        recipeList.add(new Recipe("Chicken Salad", "Healthy chicken salad with greens", android.R.drawable.sym_def_app_icon, "Ingredients: Cooked Chicken, Lettuce, Tomatoes, Cucumber, Mayonnaise, Lemon Juice", "Instructions: Chop chicken and vegetables. Mix with mayonnaise and lemon juice. Serve on lettuce."));

        // Initialize and set adapter
        recipeAdapter = new RecipeAdapter(recipeList, this);
        recyclerView.setAdapter(recipeAdapter);
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
}
