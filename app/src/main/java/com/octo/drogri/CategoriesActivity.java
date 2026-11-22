package com.octo.drogri;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.octo.drogri.adapters.CategoryManageAdapter;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.dialogs.PasswordDialog;
import com.octo.drogri.models.Category;
import com.octo.drogri.utils.AuthHelper;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.SecurityManager;

import java.util.List;

/**
 * Activity for managing product categories (Add, View, Edit, Delete) in Arabic.
 */
public class CategoriesActivity extends BaseActivity implements CategoryManageAdapter.OnCategoryActionListener {

    public static final String EXTRA_SELECTED_CATEGORY_ID = "extra_selected_category_id";
    public static final String EXTRA_SELECTED_CATEGORY_NAME = "extra_selected_category_name";

    private DatabaseHelper databaseHelper;
    private CategoryManageAdapter adapter;
    private TextInputLayout tilCategoryName;
    private EditText etCategoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        databaseHelper = DatabaseHelper.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tilCategoryName = findViewById(R.id.til_category_name);
        etCategoryName = findViewById(R.id.et_category_name);
        MaterialButton btnAddCategory = findViewById(R.id.btn_add_category);

        btnAddCategory.setOnClickListener(v -> handleAddCategory());

        RecyclerView rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryManageAdapter(this, this);
        rvCategories.setAdapter(adapter);

        loadCategories();
    }

    private void loadCategories() {
        List<Category> categories = databaseHelper.getAllCategoriesWithCount();
        adapter.setCategories(categories);
    }

    private void handleAddCategory() {
        String name = etCategoryName.getText().toString().trim();

        if (name.isEmpty()) {
            tilCategoryName.setError(getString(R.string.error_category_name_empty));
            return;
        }

        if (databaseHelper.categoryExists(name, -1)) {
            tilCategoryName.setError(getString(R.string.error_category_exists));
            return;
        }

        tilCategoryName.setError(null);
        long newId = databaseHelper.insertCategory(name);

        if (newId != -1) {
            etCategoryName.setText("");
            Toast.makeText(this, R.string.success_category_added, Toast.LENGTH_SHORT).show();
            loadCategories();
            BackupManager.onCategoryChanged(this);
            setResult(RESULT_OK);
        } else {
            Toast.makeText(this, "فشل إضافة القسم", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCategoryClick(Category category) {
        if (category == null) return;
        android.content.Intent resultIntent = new android.content.Intent();
        resultIntent.putExtra(EXTRA_SELECTED_CATEGORY_ID, category.getId());
        resultIntent.putExtra(EXTRA_SELECTED_CATEGORY_NAME, category.getName());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onEditCategory(Category category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_edit, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextInputLayout tilEditName = dialogView.findViewById(R.id.til_category_name);
        EditText etEditName = dialogView.findViewById(R.id.et_category_name);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        tvDialogTitle.setText(R.string.edit_category);
        etEditName.setText(category.getName());
        etEditName.setSelection(category.getName().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etEditName.getText().toString().trim();
            if (newName.isEmpty()) {
                tilEditName.setError(getString(R.string.error_category_name_empty));
                return;
            }

            if (databaseHelper.categoryExists(newName, category.getId())) {
                tilEditName.setError(getString(R.string.error_category_exists));
                return;
            }

            boolean updated = databaseHelper.updateCategory(category.getId(), newName);
            if (updated) {
                Toast.makeText(this, R.string.success_category_updated, Toast.LENGTH_SHORT).show();
                loadCategories();
                BackupManager.onCategoryChanged(this);
                setResult(RESULT_OK);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "فشل تحديث القسم", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onDeleteCategory(Category category) {
        int productCount = databaseHelper.getProductCountByCategory(category.getId());

        // Safety check: Prevent deletion if products are assigned
        if (productCount > 0) {
            String warningMessage = getString(R.string.category_has_products_warning, productCount);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_delete_category_title)
                    .setMessage(warningMessage)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_category_title)
                .setMessage("هل أنت متأكد من رغبتك في حذف قسم \"" + category.getName() + "\"؟")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AuthHelper.authenticate(CategoriesActivity.this, getString(R.string.confirm_password_dialog_msg), () -> performDeleteCategory(category));
                })
                .show();
    }

    private void performDeleteCategory(Category category) {
        boolean deleted = databaseHelper.deleteCategory(category.getId());
        if (deleted) {
            Toast.makeText(this, R.string.success_category_deleted, Toast.LENGTH_SHORT).show();
            loadCategories();
            BackupManager.onCategoryChanged(this);
            setResult(RESULT_OK);
        } else {
            Toast.makeText(this, "فشل حذف القسم", Toast.LENGTH_SHORT).show();
        }
    }
}
