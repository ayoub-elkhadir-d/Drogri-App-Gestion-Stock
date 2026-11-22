package com.octo.drogri;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.octo.drogri.adapters.CategoryChipAdapter;
import com.octo.drogri.adapters.ProductAdapter;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.dialogs.PasswordDialog;
import com.octo.drogri.models.Category;
import com.octo.drogri.models.Product;
import com.octo.drogri.utils.AuthHelper;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.NetworkUtils;
import com.octo.drogri.utils.SecurityManager;
import com.octo.drogri.utils.SettingsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Dashboard Activity:
 * Displays product inventory in a 2-column grid, horizontal category filter,
 * real-time search by product and category name, multiple product selection,
 * atomic bulk deletion, and quick access to settings and backups.
 */
public class MainActivity extends BaseActivity implements
        CategoryChipAdapter.OnCategorySelectedListener,
        ProductAdapter.OnProductClickListener,
        ProductAdapter.OnSelectionListener {

    private DatabaseHelper databaseHelper;

    // Adapters
    private CategoryChipAdapter categoryChipAdapter;
    private ProductAdapter productAdapter;

    // Header & Contextual Selection Toolbar
    private LinearLayout layoutHeader;
    private LinearLayout layoutSelectionToolbar;
    private ImageButton btnCloseSelection;
    private TextView tvSelectionCount;
    private MaterialButton btnSelectAll;
    private ImageButton btnDeleteSelected;
    private FloatingActionButton fabAddProduct;

    // Search and Empty State UI Elements
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    // Backup Warning Banner (Feature 7)
    private View cardBackupWarning;
    private TextView tvBackupWarningText;
    private MaterialButton btnBannerBackupNow;

    // Filter state
    private long selectedCategoryId = -1; // -1 represents All Products
    private String currentSearchQuery = "";
    private RecyclerView rvCategories;

    // Activity Result Launchers
    private final ActivityResultLauncher<Intent> addProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshData();
                }
            });

    private final ActivityResultLauncher<Intent> productDetailsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    long catId = -1;
                    if (result.getData() != null) {
                        catId = result.getData().getLongExtra(CategoriesActivity.EXTRA_SELECTED_CATEGORY_ID, -1);
                    }
                    if (catId > 0) {
                        selectCategoryById(catId);
                    } else {
                        refreshData();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> categoriesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    long catId = -1;
                    if (result.getData() != null) {
                        catId = result.getData().getLongExtra(CategoriesActivity.EXTRA_SELECTED_CATEGORY_ID, -1);
                    }
                    if (catId > 0) {
                        selectCategoryById(catId);
                    } else {
                        refreshData();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> backupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshData();
                }
            });

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                refreshData();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsManager.applyTheme(SettingsManager.getInstance(this).getThemeMode());
        super.onCreate(savedInstanceState);

        SecurityManager securityManager = SecurityManager.getInstance(this);
        if (!securityManager.isPasswordSet()) {
            Intent intent = new Intent(this, PasswordActivity.class);
            intent.putExtra(PasswordActivity.EXTRA_MODE, PasswordActivity.MODE_SETUP);
            startActivity(intent);
            finish();
            return;
        } else if (!SecurityManager.isAppUnlocked()) {
            Intent intent = new Intent(this, PasswordActivity.class);
            intent.putExtra(PasswordActivity.EXTRA_MODE, PasswordActivity.MODE_UNLOCK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        databaseHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupRecyclerViews();
        setupSearch();
        setupBackPressHandler();

        // Initial Data Load
        refreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void initViews() {
        layoutHeader = findViewById(R.id.layout_header);
        layoutSelectionToolbar = findViewById(R.id.layout_selection_toolbar);
        btnCloseSelection = findViewById(R.id.btn_close_selection);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        fabAddProduct = findViewById(R.id.fab_add_product);

        ImageButton btnManageCategories = findViewById(R.id.btn_manage_categories);
        btnManageCategories.setOnClickListener(v -> {
            Intent intent = new Intent(this, CategoriesActivity.class);
            categoriesLauncher.launch(intent);
        });

        ImageButton btnBackup = findViewById(R.id.btn_backup);
        btnBackup.setOnClickListener(v -> {
            Intent intent = new Intent(this, BackupActivity.class);
            backupLauncher.launch(intent);
        });

        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProductActivity.class);
            addProductLauncher.launch(intent);
        });

        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        // Selection Toolbar buttons
        btnCloseSelection.setOnClickListener(v -> exitSelectionMode());

        btnSelectAll.setOnClickListener(v -> {
            if (productAdapter.getSelectedCount() == productAdapter.getTotalCount() && productAdapter.getTotalCount() > 0) {
                productAdapter.clearSelection();
            } else {
                productAdapter.selectAll();
            }
        });

        btnDeleteSelected.setOnClickListener(v -> handleBulkDelete());

        // Backup Warning Banner (Feature 7)
        cardBackupWarning = findViewById(R.id.card_backup_warning);
        tvBackupWarningText = findViewById(R.id.tv_backup_warning_text);
        btnBannerBackupNow = findViewById(R.id.btn_banner_backup_now);
        if (btnBannerBackupNow != null) {
            btnBannerBackupNow.setOnClickListener(v -> handleBannerBackupNow());
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (productAdapter != null && productAdapter.isSelectionMode()) {
                    exitSelectionMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupRecyclerViews() {
        // 1. Horizontal Categories RecyclerView
        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryChipAdapter = new CategoryChipAdapter(this, this);
        rvCategories.setAdapter(categoryChipAdapter);

        // 2. Products 2-column Grid RecyclerView
        RecyclerView rvProducts = findViewById(R.id.rv_products);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(this, this);
        productAdapter.setOnSelectionListener(this);
        productAdapter.setOnCategoryBadgeClickListener((categoryId, categoryName) -> selectCategoryById(categoryId));
        rvProducts.setAdapter(productAdapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                refreshProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }

    /**
     * Refreshes both categories and products, and updates backup warning status.
     */
    private void refreshData() {
        refreshCategories();
        refreshProducts();
        updateBackupWarningBanner();
    }

    private void updateBackupWarningBanner() {
        if (cardBackupWarning == null) return;

        SettingsManager settings = SettingsManager.getInstance(this);
        if (!settings.isShowBackupWarningBanner()) {
            cardBackupWarning.setVisibility(View.GONE);
            return;
        }

        boolean isCloudEnabled = settings.isGoogleDriveBackupEnabled();
        boolean hasPending = settings.isCloudBackupPending();
        boolean hasError = settings.getLastCloudError() != null;
        boolean hasUnsynced = databaseHelper != null && databaseHelper.getUnsyncedChangesCount() > 0;
        int pendingCount = settings.getProductsAddedSinceBackup();

        boolean showBanner = isCloudEnabled && (hasPending || hasError || hasUnsynced || pendingCount > 0);

        if (showBanner) {
            cardBackupWarning.setVisibility(View.VISIBLE);
            if (!NetworkUtils.isNetworkAvailable(this)) {
                tvBackupWarningText.setText(R.string.backup_pending_no_internet);
            } else if (hasError) {
                tvBackupWarningText.setText(getString(R.string.backup_failed_warning) + ": " + settings.getLastCloudError());
            } else {
                tvBackupWarningText.setText(R.string.backup_pending_warning_title);
            }
        } else {
            cardBackupWarning.setVisibility(View.GONE);
        }
    }

    private void handleBannerBackupNow() {
        if (btnBannerBackupNow == null) return;
        btnBannerBackupNow.setEnabled(false);
        btnBannerBackupNow.setText(R.string.backup_in_progress);

        BackupManager.performManualBackup(this, new BackupManager.ManualBackupCallback() {
            @Override
            public void onStarted() {
            }

            @Override
            public void onLocalSuccess(File backupFile) {
            }

            @Override
            public void onCloudUploading() {
            }

            @Override
            public void onCloudSuccess(File backupFile, String driveFileId) {
                runOnUiThread(() -> {
                    btnBannerBackupNow.setEnabled(true);
                    btnBannerBackupNow.setText(R.string.backup_now);
                    cardBackupWarning.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.backup_synced_success, Toast.LENGTH_SHORT).show();
                    refreshData();
                });
            }

            @Override
            public void onCloudFailed(File backupFile, String errorMessage) {
                runOnUiThread(() -> {
                    btnBannerBackupNow.setEnabled(true);
                    btnBannerBackupNow.setText(R.string.backup_now);
                    updateBackupWarningBanner();
                    Toast.makeText(MainActivity.this, errorMessage != null ? errorMessage : "فشل النسخ السحابي", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnBannerBackupNow.setEnabled(true);
                    btnBannerBackupNow.setText(R.string.backup_now);
                    updateBackupWarningBanner();
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Loads categories into the horizontal chip list.
     */
    private void refreshCategories() {
        List<Category> categories = databaseHelper.getAllCategories();
        categoryChipAdapter.setCategories(categories);
    }

    /**
     * Searches and filters products according to active search query and category.
     */
    private void refreshProducts() {
        List<Product> products = databaseHelper.searchProducts(currentSearchQuery, selectedCategoryId);
        productAdapter.setProducts(products);

        if (productAdapter.isSelectionMode()) {
            onSelectionChanged(productAdapter.getSelectedCount());
        }

        // Toggle Empty State View
        if (products.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (!currentSearchQuery.isEmpty() || selectedCategoryId > 0) {
                tvEmptyTitle.setText(R.string.empty_products_title);
                tvEmptyDesc.setText(R.string.empty_search_desc);
            } else {
                tvEmptyTitle.setText(R.string.empty_products_title);
                tvEmptyDesc.setText(R.string.empty_products_desc);
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Programmatically selects a category, updates the chip selector, scrolls it into view,
     * and filters the product grid.
     */
    public void selectCategoryById(long categoryId) {
        selectedCategoryId = categoryId;
        if (categoryChipAdapter != null) {
            categoryChipAdapter.setSelectedCategoryId(categoryId);
            int pos = categoryChipAdapter.getPositionForCategoryId(categoryId);
            if (pos >= 0 && rvCategories != null) {
                rvCategories.smoothScrollToPosition(pos);
            }
        }
        refreshProducts();
    }

    /**
     * Callback when a category is selected from the horizontal chip list.
     */
    @Override
    public void onCategorySelected(Category category) {
        selectedCategoryId = category.getId();
        refreshProducts();
    }

    /**
     * Callback when a product card is clicked.
     */
    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailsActivity.class);
        intent.putExtra(ProductDetailsActivity.EXTRA_PRODUCT_ID, product.getId());
        productDetailsLauncher.launch(intent);
    }

    /**
     * Multi-Selection Callbacks
     */
    @Override
    public void onSelectionModeStarted() {
        layoutHeader.setVisibility(View.GONE);
        layoutSelectionToolbar.setVisibility(View.VISIBLE);
        fabAddProduct.hide();
        onSelectionChanged(productAdapter.getSelectedCount());
    }

    @Override
    public void onSelectionChanged(int count) {
        tvSelectionCount.setText(getString(R.string.selected_count, count));
        if (count == productAdapter.getTotalCount() && count > 0) {
            btnSelectAll.setText(R.string.deselect_all);
        } else {
            btnSelectAll.setText(R.string.select_all);
        }
        btnDeleteSelected.setEnabled(count > 0);
        btnDeleteSelected.setAlpha(count > 0 ? 1.0f : 0.4f);
    }

    private void exitSelectionMode() {
        if (productAdapter != null) {
            productAdapter.setSelectionMode(false);
        }
        layoutSelectionToolbar.setVisibility(View.GONE);
        layoutHeader.setVisibility(View.VISIBLE);
        fabAddProduct.show();
    }

    private void handleBulkDelete() {
        int selectedCount = productAdapter.getSelectedCount();
        if (selectedCount == 0) {
            Toast.makeText(this, "يرجى تحديد منتجات للحذف", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_bulk_delete_title)
                .setMessage(getString(R.string.confirm_bulk_delete_message, selectedCount))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AuthHelper.authenticate(MainActivity.this, getString(R.string.confirm_password_dialog_msg), this::executeBulkDelete);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void executeBulkDelete() {
        List<Long> idsToDelete = new ArrayList<>(productAdapter.getSelectedProductIds());
        int deleted = databaseHelper.bulkDeleteProducts(idsToDelete);
        if (deleted > 0) {
            BackupManager.onProductsBulkDeleted(MainActivity.this, deleted);
            Toast.makeText(MainActivity.this, getString(R.string.bulk_delete_success, deleted), Toast.LENGTH_SHORT).show();
        }
        exitSelectionMode();
        refreshProducts();
    }
}
