package com.octo.drogri;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.octo.drogri.adapters.ProductGalleryAdapter;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.dialogs.PasswordDialog;
import com.octo.drogri.models.Product;
import com.octo.drogri.models.ProductImage;
import com.octo.drogri.utils.AuthHelper;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.ImageUtils;
import com.octo.drogri.utils.SecurityManager;
import com.octo.drogri.utils.SettingsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for displaying complete product details, professional 2-column image gallery,
 * full-screen zoom viewer integration, profit margin in Moroccan Dirham, and specifications.
 */
public class ProductDetailsActivity extends BaseActivity implements ProductGalleryAdapter.OnGalleryInteractionListener {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DatabaseHelper databaseHelper;
    private long productId = -1;
    private Product product;

    // Purchase Price Privacy State
    private ImageButton btnTogglePurchasePrice;
    private boolean isPurchasePriceRevealed = false;

    // Gallery UI Views
    private RecyclerView rvProductGallery;
    private ProductGalleryAdapter galleryAdapter;
    private MaterialButton btnAddGalleryImage;
    private MaterialButton btnAddCameraImage;
    private LinearLayout layoutGallerySection;

    // Details Views
    private TextView tvDetailCategoryBadge;
    private TextView tvDetailName;
    private TextView tvDetailSellingPrice;
    private TextView tvDetailPurchasePrice;
    private TextView tvDetailProfit;

    // Specifications & Measurements
    private TextView tvDetailCategory;
    private LinearLayout rowDetailColor;
    private TextView tvDetailColor;
    private LinearLayout rowDetailSize;
    private TextView tvDetailSize;
    private LinearLayout rowDetailLength;
    private TextView tvDetailLength;
    private LinearLayout rowDetailWidth;
    private TextView tvDetailWidth;
    private LinearLayout rowDetailHeight;
    private TextView tvDetailHeight;
    private LinearLayout rowDetailWeight;
    private TextView tvDetailWeight;
    private LinearLayout rowDetailVolume;
    private TextView tvDetailVolume;

    private final ActivityResultLauncher<Intent> editProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadProductDetails();
                    setResult(RESULT_OK);
                }
            });

    private final ActivityResultLauncher<Intent> bgRemovalLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> paths = result.getData().getStringArrayListExtra(BackgroundRemovalActivity.EXTRA_RESULT_PATHS);
                    if (paths != null && !paths.isEmpty()) {
                        addAcceptedPathsToProduct(paths);
                    }
                }
            });

    private final ActivityResultLauncher<String> addMoreImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    Intent intent = new Intent(this, BackgroundRemovalActivity.class);
                    intent.putParcelableArrayListExtra(BackgroundRemovalActivity.EXTRA_IMAGE_URIS, new ArrayList<>(uris));
                    bgRemovalLauncher.launch(intent);
                }
            });

    private Uri currentCameraPhotoUri;
    private File currentCameraPhotoFile;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && currentCameraPhotoUri != null && currentCameraPhotoFile != null && currentCameraPhotoFile.exists()) {
                    Intent intent = new Intent(this, BackgroundRemovalActivity.class);
                    ArrayList<Uri> uris = new ArrayList<>();
                    uris.add(currentCameraPhotoUri);
                    intent.putParcelableArrayListExtra(BackgroundRemovalActivity.EXTRA_IMAGE_URIS, uris);
                    bgRemovalLauncher.launch(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        databaseHelper = DatabaseHelper.getInstance(this);

        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
        if (productId == -1) {
            Toast.makeText(this, "منتج غير صالح", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupGalleryRecyclerView();
        loadProductDetails();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        ImageButton btnEditTop = findViewById(R.id.btn_edit_top);
        ImageButton btnDeleteTop = findViewById(R.id.btn_delete_top);
        MaterialButton btnEditBottom = findViewById(R.id.btn_edit_product);
        MaterialButton btnDeleteBottom = findViewById(R.id.btn_delete_product);

        layoutGallerySection = findViewById(R.id.layout_gallery_section);
        rvProductGallery = findViewById(R.id.rv_product_gallery);
        btnAddGalleryImage = findViewById(R.id.btn_add_gallery_image);
        btnAddCameraImage = findViewById(R.id.btn_add_camera_image);

        btnAddGalleryImage.setOnClickListener(v -> addMoreImagesLauncher.launch("image/*"));
        if (btnAddCameraImage != null) {
            btnAddCameraImage.setOnClickListener(v -> launchCameraCapture());
        }

        tvDetailCategoryBadge = findViewById(R.id.tv_detail_category_badge);
        tvDetailName = findViewById(R.id.tv_detail_name);
        tvDetailSellingPrice = findViewById(R.id.tv_detail_selling_price);
        tvDetailPurchasePrice = findViewById(R.id.tv_detail_purchase_price);
        tvDetailProfit = findViewById(R.id.tv_detail_profit);
        btnTogglePurchasePrice = findViewById(R.id.btn_toggle_purchase_price);
        if (btnTogglePurchasePrice != null) {
            btnTogglePurchasePrice.setOnClickListener(v -> togglePurchasePriceVisibility());
        }

        tvDetailCategory = findViewById(R.id.tv_detail_category);
        rowDetailColor = findViewById(R.id.row_detail_color);
        tvDetailColor = findViewById(R.id.tv_detail_color);
        rowDetailSize = findViewById(R.id.row_detail_size);
        tvDetailSize = findViewById(R.id.tv_detail_size);
        rowDetailLength = findViewById(R.id.row_detail_length);
        tvDetailLength = findViewById(R.id.tv_detail_length);
        rowDetailWidth = findViewById(R.id.row_detail_width);
        tvDetailWidth = findViewById(R.id.tv_detail_width);
        rowDetailHeight = findViewById(R.id.row_detail_height);
        tvDetailHeight = findViewById(R.id.tv_detail_height);
        rowDetailWeight = findViewById(R.id.row_detail_weight);
        tvDetailWeight = findViewById(R.id.tv_detail_weight);
        rowDetailVolume = findViewById(R.id.row_detail_volume);
        tvDetailVolume = findViewById(R.id.tv_detail_volume);

        View.OnClickListener editListener = v -> openEditProduct();
        btnEditTop.setOnClickListener(editListener);
        btnEditBottom.setOnClickListener(editListener);

        View.OnClickListener deleteListener = v -> confirmDeleteProduct();
        btnDeleteTop.setOnClickListener(deleteListener);
        btnDeleteBottom.setOnClickListener(deleteListener);
    }

    private void setupGalleryRecyclerView() {
        galleryAdapter = new ProductGalleryAdapter(this, this);
        rvProductGallery.setAdapter(galleryAdapter);
    }

    private void loadProductDetails() {
        product = databaseHelper.getProductById(productId);
        if (product == null) {
            finish();
            return;
        }

        tvDetailName.setText(product.getName());

        String categoryName = product.getCategoryName();
        tvDetailCategoryBadge.setText(categoryName);
        tvDetailCategory.setText(categoryName);

        // Clicking category badge filters main page to this category
        tvDetailCategoryBadge.setOnClickListener(v -> {
            if (product != null && product.getCategoryId() > 0) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(CategoriesActivity.EXTRA_SELECTED_CATEGORY_ID, product.getCategoryId());
                resultIntent.putExtra(CategoriesActivity.EXTRA_SELECTED_CATEGORY_NAME, product.getCategoryName());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // Prices & Profit (Moroccan Dirham) & Privacy Masking
        updatePriceDisplay();

        // Specifications
        String color = product.getColor();
        if (color != null && !color.trim().isEmpty()) {
            rowDetailColor.setVisibility(View.VISIBLE);
            tvDetailColor.setText(color.trim());
        } else {
            rowDetailColor.setVisibility(View.GONE);
        }

        String size = product.getSize();
        if (size != null && !size.trim().isEmpty()) {
            rowDetailSize.setVisibility(View.VISIBLE);
            tvDetailSize.setText(size.trim());
        } else {
            rowDetailSize.setVisibility(View.GONE);
        }

        if (product.hasLength()) {
            rowDetailLength.setVisibility(View.VISIBLE);
            tvDetailLength.setText(product.getFormattedLength());
        } else {
            rowDetailLength.setVisibility(View.GONE);
        }

        if (product.hasWidth()) {
            rowDetailWidth.setVisibility(View.VISIBLE);
            tvDetailWidth.setText(product.getFormattedWidth());
        } else {
            rowDetailWidth.setVisibility(View.GONE);
        }

        if (product.hasHeight()) {
            rowDetailHeight.setVisibility(View.VISIBLE);
            tvDetailHeight.setText(product.getFormattedHeight());
        } else {
            rowDetailHeight.setVisibility(View.GONE);
        }

        if (product.hasWeight()) {
            rowDetailWeight.setVisibility(View.VISIBLE);
            tvDetailWeight.setText(product.getFormattedWeight());
        } else {
            rowDetailWeight.setVisibility(View.GONE);
        }

        if (product.hasVolume()) {
            rowDetailVolume.setVisibility(View.VISIBLE);
            tvDetailVolume.setText(product.getFormattedVolume());
        } else {
            rowDetailVolume.setVisibility(View.GONE);
        }

        // Professional Gallery Setup
        List<ProductImage> images = product.getImages();
        if (images != null && !images.isEmpty()) {
            int spanCount = images.size() == 1 ? 1 : 2;
            rvProductGallery.setLayoutManager(new GridLayoutManager(this, spanCount));
            galleryAdapter.setImages(images);
            rvProductGallery.setVisibility(View.VISIBLE);
        } else {
            galleryAdapter.setImages(new ArrayList<>());
            rvProductGallery.setVisibility(View.GONE);
        }
    }

    /**
     * Tapping any image in the gallery opens the dedicated full-screen zoom viewer.
     */
    @Override
    public void onImageClick(ProductImage image, int position) {
        openFullScreenViewer(position);
    }

    @Override
    public void onImageOptionsClick(ProductImage image, int position) {
        String[] options;
        if (image.isPrimary()) {
            options = new String[]{"تكبير في ملء الشاشة", "حذف هذه الصورة"};
        } else {
            options = new String[]{"تكبير في ملء الشاشة", "تعيين كصورة رئيسية للمنتج", "حذف هذه الصورة"};
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("خيارات الصورة")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openFullScreenViewer(position);
                    } else if (!image.isPrimary() && which == 1) {
                        // Set as primary
                        databaseHelper.setPrimaryImage(productId, image.getId());
                        BackupManager.onProductEdited(this);
                        Toast.makeText(this, "تم تعيين الصورة كرئيسية", Toast.LENGTH_SHORT).show();
                        loadProductDetails();
                        setResult(RESULT_OK);
                    } else {
                        // Delete image
                        confirmDeleteImage(image);
                    }
                })
                .show();
    }

    private void openFullScreenViewer(int position) {
        List<ProductImage> images = galleryAdapter.getImages();
        if (images.isEmpty()) return;

        ArrayList<String> paths = new ArrayList<>();
        for (ProductImage img : images) {
            paths.add(img.getImagePath());
        }

        Intent intent = new Intent(this, ImageViewerActivity.class);
        intent.putStringArrayListExtra(ImageViewerActivity.EXTRA_IMAGE_PATHS, paths);
        intent.putExtra(ImageViewerActivity.EXTRA_INITIAL_POSITION, position);
        if (product != null) {
            intent.putExtra(ImageViewerActivity.EXTRA_PRIMARY_PATH, product.getPrimaryImagePath());
        }
        startActivity(intent);
    }

    private void confirmDeleteImage(ProductImage image) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_product)
                .setMessage("هل ترغب في حذف هذه الصورة نهائياً؟")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    databaseHelper.deleteSingleProductImage(image.getId());
                    ImageUtils.deleteImageFile(image.getImagePath());

                    // If deleted image was primary, assign primary to first remaining image
                    List<ProductImage> remaining = databaseHelper.getProductImages(productId);
                    if (!remaining.isEmpty()) {
                        boolean hasPrimary = false;
                        for (ProductImage pi : remaining) {
                            if (pi.isPrimary()) {
                                hasPrimary = true;
                                break;
                            }
                        }
                        if (!hasPrimary) {
                            databaseHelper.setPrimaryImage(productId, remaining.get(0).getId());
                        }
                    } else {
                        databaseHelper.updateProductPrimaryImagePath(productId, null);
                    }

                    BackupManager.onProductEdited(this);
                    Toast.makeText(this, "تم حذف الصورة", Toast.LENGTH_SHORT).show();
                    loadProductDetails();
                    setResult(RESULT_OK);
                })
                .show();
    }

    private void addAcceptedPathsToProduct(List<String> paths) {
        List<ProductImage> existing = databaseHelper.getProductImages(productId);
        boolean hadNoImages = existing == null || existing.isEmpty();
        int countAdded = 0;

        for (String path : paths) {
            boolean isPrimary = hadNoImages && countAdded == 0;
            long imgId = databaseHelper.insertProductImage(productId, path, isPrimary);
            if (isPrimary && imgId != -1) {
                databaseHelper.updateProductPrimaryImagePath(productId, path);
            }
            countAdded++;
        }

        if (countAdded > 0) {
            BackupManager.onProductEdited(ProductDetailsActivity.this);
            loadProductDetails();
            setResult(RESULT_OK);
            Toast.makeText(ProductDetailsActivity.this, "تمت إضافة " + countAdded + " صورة بنجاح", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCameraCapture() {
        try {
            File storageDir = new File(getCacheDir(), "camera");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            String fileName = "IMG_CAM_" + System.currentTimeMillis() + ".jpg";
            currentCameraPhotoFile = new File(storageDir, fileName);
            currentCameraPhotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    currentCameraPhotoFile
            );
            takePictureLauncher.launch(currentCameraPhotoUri);
        } catch (Exception e) {
            Toast.makeText(this, "فشل تشغيل الكاميرا: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePurchasePriceVisibility() {
        isPurchasePriceRevealed = !isPurchasePriceRevealed;
        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        if (product == null) return;
        String currencySymbol = SettingsManager.getInstance(this).getCurrencySymbol();
        tvDetailSellingPrice.setText(product.getFormattedSellingPrice(currencySymbol));

        SecurityManager securityManager = SecurityManager.getInstance(this);
        boolean shouldMask = securityManager.isMaskPurchasePrice() && !isPurchasePriceRevealed;

        if (shouldMask) {
            tvDetailPurchasePrice.setText("•••••• " + currencySymbol);
            tvDetailProfit.setText("•••••• " + currencySymbol);
            if (btnTogglePurchasePrice != null) {
                btnTogglePurchasePrice.setVisibility(View.VISIBLE);
                btnTogglePurchasePrice.setImageResource(R.drawable.ic_visibility_off);
            }
        } else {
            tvDetailPurchasePrice.setText(product.getFormattedPurchasePrice(currencySymbol));
            tvDetailProfit.setText(product.getFormattedProfit(currencySymbol));
            if (btnTogglePurchasePrice != null) {
                if (securityManager.isMaskPurchasePrice()) {
                    btnTogglePurchasePrice.setVisibility(View.VISIBLE);
                    btnTogglePurchasePrice.setImageResource(R.drawable.ic_visibility);
                } else {
                    btnTogglePurchasePrice.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPurchasePriceRevealed = false;
        updatePriceDisplay();
    }

    private void openEditProduct() {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra(AddProductActivity.EXTRA_PRODUCT_ID, productId);
        editProductLauncher.launch(intent);
    }

    private void confirmDeleteProduct() {
        boolean confirmEnabled = SettingsManager.getInstance(this).isConfirmOnDelete();
        Runnable proceedWithDelete = () -> AuthHelper.authenticate(ProductDetailsActivity.this, getString(R.string.confirm_password_dialog_msg), this::performDeleteProduct);

        if (confirmEnabled) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_delete_title)
                    .setMessage(R.string.confirm_delete_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> proceedWithDelete.run())
                    .show();
        } else {
            proceedWithDelete.run();
        }
    }

    private void performDeleteProduct() {
        boolean deleted = databaseHelper.deleteProduct(productId);
        if (deleted) {
            BackupManager.onProductDeleted(this);
            Toast.makeText(this, R.string.success_product_deleted, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "فشل حذف المنتج", Toast.LENGTH_SHORT).show();
        }
    }
}
