package com.example.recepiesuggestor;

public class Recipe {
    private String title;
    private String description;
    private int imageResourceId; // Using an int for a drawable resource for now
    private String ingredients;
    private String instructions;

    public Recipe(String title, String description, int imageResourceId, String ingredients, String instructions) {
        this.title = title;
        this.description = description;
        this.imageResourceId = imageResourceId;
        this.ingredients = ingredients;
        this.instructions = instructions;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getInstructions() {
        return instructions;
    }
}
