package com.pocient.smartpantry.model;
public class RecipeIngredient {
    private int id;
    private int recipeId;
    private String ingredientName;
    private double quantity;
    private String unit;

    public RecipeIngredient(int id, int recipeId, String ingredientName,
                            double quantity, String unit) {
        this.id = id;
        this.recipeId = recipeId;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
    }

    public int getId() { return id; }
    public int getRecipeId() { return recipeId; }
    public String getIngredientName() { return ingredientName; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
}