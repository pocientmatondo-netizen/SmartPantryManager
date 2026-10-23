package com.pocient.smartpantry.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pocient.smartpantry.model.PantryItem;
import com.pocient.smartpantry.model.Recipe;
import com.pocient.smartpantry.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all SQLite work: creating tables, seeding recipes on first run,
 * and CRUD operations for pantry items.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smartpantry.db";
    private static final int DB_VERSION = 1;

    // pantry_items table
    public static final String TABLE_PANTRY = "pantry_items";
    public static final String COL_P_ID = "id";
    public static final String COL_P_NAME = "name";
    public static final String COL_P_QTY = "quantity";
    public static final String COL_P_UNIT = "unit";
    public static final String COL_P_EXPIRY = "expiry_date";

    // recipes table
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_R_ID = "id";
    public static final String COL_R_NAME = "name";
    public static final String COL_R_STEPS = "steps";

    // recipe_ingredients table
    public static final String TABLE_RI = "recipe_ingredients";
    public static final String COL_RI_ID = "id";
    public static final String COL_RI_RECIPE_ID = "recipe_id";
    public static final String COL_RI_NAME = "ingredient_name";
    public static final String COL_RI_QTY = "quantity";
    public static final String COL_RI_UNIT = "unit";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // enforce recipe_id link
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " ("
                + COL_P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_P_NAME + " TEXT NOT NULL, "
                + COL_P_QTY + " REAL NOT NULL, "
                + COL_P_UNIT + " TEXT NOT NULL, "
                + COL_P_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " ("
                + COL_R_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_R_NAME + " TEXT NOT NULL, "
                + COL_R_STEPS + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RI + " ("
                + COL_RI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RI_RECIPE_ID + " INTEGER NOT NULL, "
                + COL_RI_NAME + " TEXT NOT NULL, "
                + COL_RI_QTY + " REAL NOT NULL, "
                + COL_RI_UNIT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + COL_RI_RECIPE_ID + ") REFERENCES "
                + TABLE_RECIPES + "(" + COL_R_ID + ") ON DELETE CASCADE)");

        seedRecipes(db); // runs once, on first run only
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RI);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    // ===================== PANTRY CRUD =====================

    /** CREATE: adds a pantry item and returns its new row id (-1 on failure). */
    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_P_NAME, item.getName());
        values.put(COL_P_QTY, item.getQuantity());
        values.put(COL_P_UNIT, item.getUnit());
        values.put(COL_P_EXPIRY, item.getExpiryDate());
        return db.insert(TABLE_PANTRY, null, values);
    }

    /** READ: returns every pantry item, sorted by name. */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PANTRY, null, null, null, null, null,
                COL_P_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                items.add(cursorToPantryItem(c));
            }
        }
        return items;
    }

    /** READ: returns one pantry item by id, or null if not found. */
    public PantryItem getPantryItem(int id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PANTRY, null, COL_P_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToPantryItem(c);
            }
        }
        return null;
    }

    /** UPDATE: saves changes to an existing item. Returns rows affected. */
    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_P_NAME, item.getName());
        values.put(COL_P_QTY, item.getQuantity());
        values.put(COL_P_UNIT, item.getUnit());
        values.put(COL_P_EXPIRY, item.getExpiryDate());
        return db.update(TABLE_PANTRY, values, COL_P_ID + "=?",
                new String[]{String.valueOf(item.getId())});
    }

    /** DELETE: removes an item by id. Returns rows affected. */
    public int deletePantryItem(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PANTRY, COL_P_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    private PantryItem cursorToPantryItem(Cursor c) {
        return new PantryItem(
                c.getInt(c.getColumnIndexOrThrow(COL_P_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_P_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(COL_P_QTY)),
                c.getString(c.getColumnIndexOrThrow(COL_P_UNIT)),
                c.getString(c.getColumnIndexOrThrow(COL_P_EXPIRY)));
    }

    // ===================== RECIPE READS =====================

    /** Returns all recipes, each with its ingredient list filled in. */
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_RECIPES, null, null, null, null, null,
                COL_R_NAME + " ASC")) {
            while (c.moveToNext()) {
                Recipe r = new Recipe(
                        c.getInt(c.getColumnIndexOrThrow(COL_R_ID)),
                        c.getString(c.getColumnIndexOrThrow(COL_R_NAME)),
                        c.getString(c.getColumnIndexOrThrow(COL_R_STEPS)));
                r.setIngredients(getIngredientsForRecipe(db, r.getId()));
                recipes.add(r);
            }
        }
        return recipes;
    }

    /** Returns one recipe (with ingredients) by id, or null if not found. */
    public Recipe getRecipeById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_RECIPES, null, COL_R_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                Recipe r = new Recipe(
                        c.getInt(c.getColumnIndexOrThrow(COL_R_ID)),
                        c.getString(c.getColumnIndexOrThrow(COL_R_NAME)),
                        c.getString(c.getColumnIndexOrThrow(COL_R_STEPS)));
                r.setIngredients(getIngredientsForRecipe(db, r.getId()));
                return r;
            }
        }
        return null;
    }

    private List<RecipeIngredient> getIngredientsForRecipe(SQLiteDatabase db, int recipeId) {
        List<RecipeIngredient> list = new ArrayList<>();
        try (Cursor c = db.query(TABLE_RI, null, COL_RI_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)}, null, null, null)) {
            while (c.moveToNext()) {
                list.add(new RecipeIngredient(
                        c.getInt(c.getColumnIndexOrThrow(COL_RI_ID)),
                        c.getInt(c.getColumnIndexOrThrow(COL_RI_RECIPE_ID)),
                        c.getString(c.getColumnIndexOrThrow(COL_RI_NAME)),
                        c.getDouble(c.getColumnIndexOrThrow(COL_RI_QTY)),
                        c.getString(c.getColumnIndexOrThrow(COL_RI_UNIT))));
            }
        }
        return list;
    }

    // ===================== SEED DATA =====================

    /** Inserts one recipe. Each ingredient is written as "name|quantity|unit". */
    private void seedRecipe(SQLiteDatabase db, String name, String steps,
                            String... ingredients) {
        ContentValues rv = new ContentValues();
        rv.put(COL_R_NAME, name);
        rv.put(COL_R_STEPS, steps);
        long recipeId = db.insert(TABLE_RECIPES, null, rv);

        for (String line : ingredients) {
            String[] p = line.split("\\|");
            ContentValues iv = new ContentValues();
            iv.put(COL_RI_RECIPE_ID, recipeId);
            iv.put(COL_RI_NAME, p[0]);
            iv.put(COL_RI_QTY, Double.parseDouble(p[1]));
            iv.put(COL_RI_UNIT, p[2]);
            db.insert(TABLE_RI, null, iv);
        }
    }

    private void seedRecipes(SQLiteDatabase db) {
        seedRecipe(db, "Scrambled Eggs",
                "1. Beat the eggs with salt.\n2. Melt butter in a pan.\n3. Pour in eggs and stir gently until set.",
                "egg|3|pcs", "butter|10|g", "salt|1|tsp");

        seedRecipe(db, "Cheese Omelette",
                "1. Beat the eggs.\n2. Melt butter in a pan and pour in eggs.\n3. Add grated cheese, fold, and cook until golden.",
                "egg|3|pcs", "cheese|50|g", "butter|10|g");

        seedRecipe(db, "Tomato Pasta",
                "1. Boil the pasta until soft.\n2. Fry chopped onion in olive oil, add chopped tomatoes and salt.\n3. Simmer 10 minutes, then mix with pasta.",
                "pasta|200|g", "tomato|4|pcs", "onion|1|pcs", "olive oil|2|tbsp", "salt|1|tsp");

        seedRecipe(db, "Egg Fried Rice",
                "1. Cook the rice and let it cool.\n2. Fry onion in oil, push aside and scramble the eggs.\n3. Add rice and soy sauce and stir-fry for 3 minutes.",
                "rice|200|g", "egg|2|pcs", "onion|1|pcs", "soy sauce|2|tbsp", "oil|1|tbsp");

        seedRecipe(db, "Pancakes",
                "1. Mix flour, sugar, egg and milk into a smooth batter.\n2. Melt butter in a pan.\n3. Cook spoonfuls of batter until golden on both sides.",
                "flour|200|g", "egg|1|pcs", "milk|300|ml", "sugar|2|tbsp", "butter|20|g");

        seedRecipe(db, "Cheese Toastie",
                "1. Butter the outside of the bread.\n2. Place cheese between the slices.\n3. Toast in a pan until golden and melted.",
                "bread|4|pcs", "cheese|80|g", "butter|20|g");

        seedRecipe(db, "French Toast",
                "1. Whisk eggs, milk and sugar.\n2. Dip the bread slices in the mixture.\n3. Fry until golden on both sides.",
                "bread|4|pcs", "egg|2|pcs", "milk|100|ml", "sugar|1|tbsp");

        seedRecipe(db, "Mashed Potatoes",
                "1. Peel and boil the potatoes until soft.\n2. Drain and mash.\n3. Stir in butter, milk and salt.",
                "potato|500|g", "butter|30|g", "milk|100|ml", "salt|1|tsp");

        seedRecipe(db, "Garlic Butter Pasta",
                "1. Boil the pasta.\n2. Melt butter and fry crushed garlic for 1 minute.\n3. Toss the pasta in the garlic butter with salt.",
                "pasta|200|g", "garlic|3|pcs", "butter|30|g", "salt|1|tsp");

        seedRecipe(db, "Tuna Salad",
                "1. Drain the tuna.\n2. Mix with chopped onion and mayonnaise.\n3. Add a squeeze of lemon juice and serve.",
                "tuna|1|pcs", "mayonnaise|2|tbsp", "onion|1|pcs", "lemon|1|pcs");

        seedRecipe(db, "Loaded Baked Potatoes",
                "1. Prick the potatoes and bake at 200C for 50 minutes.\n2. Cut open and add butter and salt.\n3. Top with grated cheese and return to the oven for 5 minutes.",
                "potato|4|pcs", "butter|20|g", "salt|1|tsp", "cheese|50|g");

        seedRecipe(db, "Vegetable Stir-Fry",
                "1. Slice the carrots, onion and cabbage.\n2. Heat oil in a pan and fry the vegetables on high heat.\n3. Add soy sauce and cook for 2 more minutes.",
                "carrot|2|pcs", "onion|1|pcs", "cabbage|200|g", "soy sauce|2|tbsp", "oil|1|tbsp");

        seedRecipe(db, "Banana Pancakes",
                "1. Mash the bananas.\n2. Mix with eggs and flour into a batter.\n3. Fry small pancakes until golden on both sides.",
                "banana|2|pcs", "egg|2|pcs", "flour|100|g");

        seedRecipe(db, "Rice and Beans",
                "1. Fry onion in oil, add chopped tomatoes and salt.\n2. Add cooked beans and simmer for 10 minutes.\n3. Serve over cooked rice.",
                "rice|200|g", "bean|200|g", "onion|1|pcs", "tomato|2|pcs", "oil|1|tbsp", "salt|1|tsp");

        seedRecipe(db, "Chicken and Rice",
                "1. Season the chicken with salt and fry in oil until browned.\n2. Add chopped onion and garlic and cook 5 minutes.\n3. Add rice and enough water, cover, and cook until the rice is soft.",
                "chicken|300|g", "rice|200|g", "onion|1|pcs", "garlic|2|pcs", "oil|2|tbsp", "salt|1|tsp");

        seedRecipe(db, "Tomato Soup",
                "1. Fry chopped onion and garlic in butter.\n2. Add chopped tomatoes and salt and simmer for 20 minutes.\n3. Blend until smooth.",
                "tomato|6|pcs", "onion|1|pcs", "garlic|2|pcs", "butter|20|g", "salt|1|tsp");

        seedRecipe(db, "Mac and Cheese",
                "1. Boil the pasta.\n2. Melt butter, stir in flour, then slowly add milk to make a sauce.\n3. Stir in the cheese, then mix in the pasta.",
                "pasta|250|g", "cheese|150|g", "milk|200|ml", "butter|30|g", "flour|2|tbsp");

        seedRecipe(db, "Egg Sandwich",
                "1. Boil the eggs for 10 minutes, then peel and mash.\n2. Mix with mayonnaise and salt.\n3. Spread on the bread and close the sandwich.",
                "bread|2|pcs", "egg|2|pcs", "mayonnaise|1|tbsp", "salt|1|tsp");

        seedRecipe(db, "Banana Smoothie",
                "1. Peel the bananas.\n2. Blend with milk and honey until smooth.\n3. Serve cold.",
                "banana|2|pcs", "milk|250|ml", "honey|1|tbsp");
    }
}
