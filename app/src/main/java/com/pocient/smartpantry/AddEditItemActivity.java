package com.pocient.smartpantry;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pocient.smartpantry.adapter.PantryAdapter;
import com.pocient.smartpantry.database.DatabaseHelper;
import com.pocient.smartpantry.model.PantryItem;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Used for both adding a new pantry item and editing an existing one.
 * If an item id is passed in the Intent, the screen is in edit mode.
 */
public class AddEditItemActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "item_id";

    private DatabaseHelper dbHelper;
    private TextInputLayout tilName, tilQuantity;
    private TextInputEditText etName, etQuantity, etExpiry;
    private Spinner spUnit;
    private int editingId = -1; // -1 means "add mode"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        dbHelper = new DatabaseHelper(this);

        tilName = findViewById(R.id.tilName);
        tilQuantity = findViewById(R.id.tilQuantity);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        spUnit = findViewById(R.id.spUnit);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnClearDate = findViewById(R.id.btnClearDate);

        // Fill the unit spinner from the string-array in strings.xml
        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        // Edit mode: an id was passed in by the Intent
        editingId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
        if (editingId != -1) {
            setTitle("Edit Ingredient");
            loadItemIntoForm();
        } else {
            setTitle("Add Ingredient");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etExpiry.setOnClickListener(v -> showDatePicker());
        btnClearDate.setOnClickListener(v -> etExpiry.setText(""));
        btnSave.setOnClickListener(v -> saveItem());
        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Edit mode: reads the item from the database and fills the form. */
    private void loadItemIntoForm() {
        PantryItem item = dbHelper.getPantryItem(editingId);
        if (item == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etName.setText(item.getName());
        etQuantity.setText(PantryAdapter.formatQuantity(item.getQuantity()));
        etExpiry.setText(item.getExpiryDate());

        // Select the matching unit in the spinner
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spUnit.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(item.getUnit())) {
                spUnit.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                etExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d",
                        year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** Validates the form, then creates or updates the item. */
    private void saveItem() {
        tilName.setError(null);
        tilQuantity.setError(null);

        String name = etName.getText().toString().trim();
        String qtyText = etQuantity.getText().toString().trim();
        String unit = spUnit.getSelectedItem().toString();
        String expiry = etExpiry.getText().toString().trim();

        // ---- Validation ----
        boolean valid = true;

        if (name.isEmpty()) {
            tilName.setError("Ingredient name is required");
            valid = false;
        } else if (name.length() < 2) {
            tilName.setError("Name must be at least 2 letters");
            valid = false;
        } else if (!name.matches("[\\p{L} '\\-]+")) {
            tilName.setError("Use letters only (no numbers or symbols)");
            valid = false;
        } else if (isDuplicate(name, unit)) {
            tilName.setError("Already in your pantry - edit that item instead");
            valid = false;
        }

        double quantity = 0;
        if (qtyText.isEmpty()) {
            tilQuantity.setError("Quantity is required");
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(qtyText);
                if (quantity <= 0) {
                    tilQuantity.setError("Quantity must be greater than 0");
                    valid = false;
                } else if (quantity > 100000) {
                    tilQuantity.setError("Quantity is too large");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                tilQuantity.setError("Enter a valid number");
                valid = false;
            }
        }

        if (!valid) {
            return; // stop here and show the errors
        }

        // ---- Save ----
        if (editingId == -1) {
            PantryItem newItem = new PantryItem(0, name, quantity, unit, expiry);
            long result = dbHelper.addPantryItem(newItem);
            if (result == -1) {
                Toast.makeText(this, "Could not save item", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
        } else {
            PantryItem updated = new PantryItem(editingId, name, quantity, unit, expiry);
            dbHelper.updatePantryItem(updated);
            Toast.makeText(this, name + " updated", Toast.LENGTH_SHORT).show();
        }
        finish(); // go back to the list
    }

    /** True if another pantry item has the same name and unit. */
    private boolean isDuplicate(String name, String unit) {
        List<PantryItem> all = dbHelper.getAllPantryItems();
        for (PantryItem p : all) {
            boolean sameItem = (p.getId() == editingId);
            if (!sameItem && p.getName().equalsIgnoreCase(name)
                    && p.getUnit().equals(unit)) {
                return true;
            }
        }
        return false;
    }
}
