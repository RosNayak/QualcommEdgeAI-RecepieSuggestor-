package com.example.recepiesuggestor;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class RecipeDetailActivity extends AppCompatActivity {

    private ImageView recipeImage;
    private TextView recipeTitle;
    private TextView recipeIngredients;
    private TextView recipeInstructions;
    private ImageButton customBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Enable the Up button in Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recipeImage = findViewById(R.id.detail_recipe_image);
        recipeTitle = findViewById(R.id.detail_recipe_title);
        recipeIngredients = findViewById(R.id.detail_recipe_ingredients);
        recipeInstructions = findViewById(R.id.detail_recipe_instructions);

        customBackButton = findViewById(R.id.btn_custom_back);
        customBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // or onBackPressed();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra(MainActivity.EXTRA_TITLE);
            String ingredients = intent.getStringExtra(MainActivity.EXTRA_INGREDIENTS);
            String instructions = intent.getStringExtra(MainActivity.EXTRA_INSTRUCTIONS);
            int imageId = intent.getIntExtra(MainActivity.EXTRA_IMAGE_ID, android.R.drawable.ic_menu_gallery); // Default image if not found

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            recipeTitle.setText(title);
            recipeIngredients.setText(ingredients);
            recipeInstructions.setText(instructions);
            recipeImage.setImageResource(imageId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items (like the Up button)
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
