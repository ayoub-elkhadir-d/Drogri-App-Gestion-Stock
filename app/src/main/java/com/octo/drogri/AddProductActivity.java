package com.octo.drogri;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.octo.drogri.adapters.SelectedImagesAdapter;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.models.Category;
import com.octo.drogri.models.Product;
import com.octo.drogri.models.ProductImage;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for adding a new product or editing an existing product.
 * Supports multiple images (AVIF compressed), expandable measurement sections,
 * and automatic backup notifications.
 */
public class AddProductActivity extends BaseActivity implements SelectedImagesAdapter.OnImageActionListener {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private DatabaseHelper databaseHelper;
    private long editProductId = -1;
    private Product existingProduct = null;

    // Header & Images
    private TextView tvScreenTitle;
    private View layoutImagePickerButtons;
    private MaterialCardView cardPickGallery;
    private MaterialCardView cardPickCamera;
    private MaterialButton btnAddMoreGallery;
    private MaterialButton btnAddMoreCamera;
    private RecyclerView rvSelectedImages;
    private SelectedImagesAdapter imagesAdapter;
    private final List<SelectedImagesAdapter.ImageEntry> imageEntries = new ArrayList<>();
    private final List<String> removedFilePaths = new ArrayList<>();

    // Section 1: Basic Info
    private LinearLayout bodySectionBasic;
    private ImageView ivArrowBasic;
    private TextInputLayout tilProductName;
    private EditText etProductName;
    private TextInputLayout tilPurchasePrice;
    private EditText etPurchasePrice;
    private TextInputLayout tilSellingPrice;
    private EditText etSellingPrice;
    private Spinner spinnerCategory;

    // Section 2: Properties
    private LinearLayout bodySectionProperties;
    private ImageView ivArrowProperties;
    private EditText etColor;
    private Spinner spinnerSize;
    private TextInputLayout tilCustomSize;
    private EditText etCustomSize;

    // Section 3: Dimensions
    private LinearLayout bodySectionDimensions;
    private ImageView ivArrowDimensions;
    private EditText etLength;
    private Spinner spinnerLengthUnit;
    private EditText etWidth;
    private Spinner spinnerWidthUnit;
    private EditText etHeight;
    private Spinner spinnerHeightUnit;

    // Section 4: Weight & Volume
    private LinearLayout bodySectionWeight;
    private ImageView ivArrowWeight;
    private EditText etWeight;
    private Spinner spinnerWeightUnit;
    private EditText etVolume;
    private Spinner spinnerVolumeUnit;

    // Save button
    private MaterialButton btnSaveProduct;

    private List<Category> categoryList;

    // Standard unit options
    private final String[] lengthUnits = {"سم", "م", "ملم"};
    private final String[] weightUnits = {"غ", "كغ", "ملغ"};
    private final String[] volumeUnits = {"مل", "لتر"};
    private final String[] standardSizes = {"اختر المقاس", "XS", "S", "M", "L", "XL", "XXL", "مخصص"};

    // Background Removal Launcher
    private final ActivityResultLauncher<Intent> bgRemovalLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> paths = result.getData().getStringArrayListExtra(BackgroundRemovalActivity.EXTRA_RESULT_PATHS);
                    if (paths != null && !paths.isEmpty()) {
                        for (String path : paths) {
                            boolean isFirst = imageEntries.isEmpty();
                            imageEntries.add(new SelectedImagesAdapter.ImageEntry(path, isFirst));
                        }
                        updateImagesUI();
                    }
                }
            });

    // Camera capture
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

    // Multiple image gallery picker
    private final ActivityResultLauncher<String> multipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    Intent intent = new Intent(this, BackgroundRemovalActivity.class);
                    intent.putParcelableArrayListExtra(BackgroundRemovalActivity.EXTRA_IMAGE_URIS, new ArrayList<>(uris));
                    bgRemovalLauncher.launch(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        databaseHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupImagesRecyclerView();
        setupSpinners();
        setupExpandableSections();

        if (getIntent().hasExtra(EXTRA_PRODUCT_ID)) {
            editProductId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, -1);
            if (editProductId != -1) {
                loadExistingProductData();
            }
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvScreenTitle = findViewById(R.id.tv_screen_title);

        // Multiple images views
        layoutImagePickerButtons = findViewById(R.id.layout_image_picker_buttons);
        cardPickGallery = findViewById(R.id.card_pick_gallery);
        cardPickCamera = findViewById(R.id.card_pick_camera);
        btnAddMoreGallery = findViewById(R.id.btn_add_more_gallery);
        btnAddMoreCamera = findViewById(R.id.btn_add_more_camera);
        rvSelectedImages = findViewById(R.id.rv_selected_images);

        View.OnClickListener galleryListener = v -> multipleImagesLauncher.launch("image/*");
        View.OnClickListener cameraListener = v -> launchCameraCapture();

        cardPickGallery.setOnClickListener(galleryListener);
        btnAddMoreGallery.setOnClickListener(galleryListener);

        cardPickCamera.setOnClickListener(cameraListener);
        btnAddMoreCamera.setOnClickListener(cameraListener);

        // Section 1 views
        bodySectionBasic = findViewById(R.id.body_section_basic);
        ivArrowBasic = findViewById(R.id.iv_arrow_basic);
        tilProductName = findViewById(R.id.til_product_name);
        etProductName = findViewById(R.id.et_product_name);
        tilPurchasePrice = findViewById(R.id.til_purchase_price);
        etPurchasePrice = findViewById(R.id.et_purchase_price);
        tilSellingPrice = findViewById(R.id.til_selling_price);
        etSellingPrice = findViewById(R.id.et_selling_price);
        spinnerCategory = findViewById(R.id.spinner_category);

        // Section 2 views
        bodySectionProperties = findViewById(R.id.body_section_properties);
        ivArrowProperties = findViewById(R.id.iv_arrow_properties);
        etColor = findViewById(R.id.et_color);
        spinnerSize = findViewById(R.id.spinner_size);
        tilCustomSize = findViewById(R.id.til_custom_size);
        etCustomSize = findViewById(R.id.et_custom_size);

        // Section 3 views
        bodySectionDimensions = findViewById(R.id.body_section_dimensions);
        ivArrowDimensions = findViewById(R.id.iv_arrow_dimensions);
        etLength = findViewById(R.id.et_length);
        spinnerLengthUnit = findViewById(R.id.spinner_length_unit);
        etWidth = findViewById(R.id.et_width);
        spinnerWidthUnit = findViewById(R.id.spinner_width_unit);
        etHeight = findViewById(R.id.et_height);
        spinnerHeightUnit = findViewById(R.id.spinner_height_unit);

        // Section 4 views
        bodySectionWeight = findViewById(R.id.body_section_weight);
        ivArrowWeight = findViewById(R.id.iv_arrow_weight);
        etWeight = findViewById(R.id.et_weight);
        spinnerWeightUnit = findViewById(R.id.spinner_weight_unit);
        etVolume = findViewById(R.id.et_volume);
        spinnerVolumeUnit = findViewById(R.id.spinner_volume_unit);

        btnSaveProduct = findViewById(R.id.btn_save_product);
        btnSaveProduct.setOnClickListener(v -> handleSaveProduct());
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

    private void setupImagesRecyclerView() {
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagesAdapter = new SelectedImagesAdapter(this, this);
        rvSelectedImages.setAdapter(imagesAdapter);
    }

    private void updateImagesUI() {
        imagesAdapter.setItems(imageEntries);
        if (imageEntries.isEmpty()) {
            rvSelectedImages.setVisibility(View.GONE);
            btnAddMoreGallery.setVisibility(View.GONE);
            btnAddMoreCamera.setVisibility(View.GONE);
            layoutImagePickerButtons.setVisibility(View.VISIBLE);
        } else {
            rvSelectedImages.setVisibility(View.VISIBLE);
            btnAddMoreGallery.setVisibility(View.VISIBLE);
            btnAddMoreCamera.setVisibility(View.VISIBLE);
            layoutImagePickerButtons.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRemove(int position) {
        if (position >= 0 && position < imageEntries.size()) {
            SelectedImagesAdapter.ImageEntry removed = imageEntries.remove(position);
            if (removed.localPath != null) {
                removedFilePaths.add(removed.localPath);
            }
            imagesAdapter.removeEntry(position);
            if (imageEntries.isEmpty()) {
                updateImagesUI();
            }
        }
    }

    @Override
    public void onSetPrimary(int position) {
        for (int i = 0; i < imageEntries.size(); i++) {
            imageEntries.get(i).isPrimary = (i == position);
        }
        imagesAdapter.setPrimaryIndex(position);
    }

    private void setupSpinners() {
        categoryList = databaseHelper.getAllCategories();
        ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categoryList
        );
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                standardSizes
        );
        spinnerSize.setAdapter(sizeAdapter);
        spinnerSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = standardSizes[position];
                if ("مخصص".equals(selected)) {
                    tilCustomSize.setVisibility(View.VISIBLE);
                } else {
                    tilCustomSize.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<String> dimAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                lengthUnits
        );
        spinnerLengthUnit.setAdapter(dimAdapter);
        spinnerWidthUnit.setAdapter(dimAdapter);
        spinnerHeightUnit.setAdapter(dimAdapter);

        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                weightUnits
        );
        spinnerWeightUnit.setAdapter(weightAdapter);

        ArrayAdapter<String> volumeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                volumeUnits
        );
        spinnerVolumeUnit.setAdapter(volumeAdapter);
    }

    private void setupExpandableSections() {
        findViewById(R.id.header_section_basic).setOnClickListener(v -> toggleSection(bodySectionBasic, ivArrowBasic));
        findViewById(R.id.header_section_properties).setOnClickListener(v -> toggleSection(bodySectionProperties, ivArrowProperties));
        findViewById(R.id.header_section_dimensions).setOnClickListener(v -> toggleSection(bodySectionDimensions, ivArrowDimensions));
        findViewById(R.id.header_section_weight).setOnClickListener(v -> toggleSection(bodySectionWeight, ivArrowWeight));
    }

    private void toggleSection(LinearLayout sectionBody, ImageView arrowIcon) {
        if (sectionBody.getVisibility() == View.VISIBLE) {
            sectionBody.setVisibility(View.GONE);
            arrowIcon.setImageResource(R.drawable.ic_expand_more);
        } else {
            sectionBody.setVisibility(View.VISIBLE);
            arrowIcon.setImageResource(R.drawable.ic_expand_less);
        }
    }

    private void expandSection(LinearLayout sectionBody, ImageView arrowIcon) {
        sectionBody.setVisibility(View.VISIBLE);
        arrowIcon.setImageResource(R.drawable.ic_expand_less);
    }

    private void loadExistingProductData() {
        existingProduct = databaseHelper.getProductById(editProductId);
        if (existingProduct == null) {
            Toast.makeText(this, "المنتج غير موجود", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvScreenTitle.setText(R.string.edit_product);
        btnSaveProduct.setText(R.string.update_product);

        etProductName.setText(existingProduct.getName());
        etPurchasePrice.setText(String.format(Locale.US, "%.2f", existingProduct.getPurchasePrice()));
        etSellingPrice.setText(String.format(Locale.US, "%.2f", existingProduct.getSellingPrice()));

        if (existingProduct.getColor() != null) {
            etColor.setText(existingProduct.getColor());
        }

        String size = existingProduct.getSize();
        if (size != null && !size.trim().isEmpty()) {
            int sizeIndex = Arrays.asList(standardSizes).indexOf(size);
            if (sizeIndex >= 0) {
                spinnerSize.setSelection(sizeIndex);
            } else {
                spinnerSize.setSelection(standardSizes.length - 1);
                tilCustomSize.setVisibility(View.VISIBLE);
                etCustomSize.setText(size);
            }
        }

        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getId() == existingProduct.getCategoryId()) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Dimensions
        boolean hasDimensions = false;
        if (existingProduct.hasLength()) {
            etLength.setText(String.valueOf(existingProduct.getLengthValue()));
            selectSpinnerItem(spinnerLengthUnit, existingProduct.getLengthUnit());
            hasDimensions = true;
        }
        if (existingProduct.hasWidth()) {
            etWidth.setText(String.valueOf(existingProduct.getWidthValue()));
            selectSpinnerItem(spinnerWidthUnit, existingProduct.getWidthUnit());
            hasDimensions = true;
        }
        if (existingProduct.hasHeight()) {
            etHeight.setText(String.valueOf(existingProduct.getHeightValue()));
            selectSpinnerItem(spinnerHeightUnit, existingProduct.getHeightUnit());
            hasDimensions = true;
        }
        if (hasDimensions) {
            expandSection(bodySectionDimensions, ivArrowDimensions);
        }

        // Weight & Volume
        boolean hasWeightVolume = false;
        if (existingProduct.hasWeight()) {
            etWeight.setText(String.valueOf(existingProduct.getWeightValue()));
            selectSpinnerItem(spinnerWeightUnit, existingProduct.getWeightUnit());
            hasWeightVolume = true;
        }
        if (existingProduct.hasVolume()) {
            etVolume.setText(String.valueOf(existingProduct.getVolumeValue()));
            selectSpinnerItem(spinnerVolumeUnit, existingProduct.getVolumeUnit());
            hasWeightVolume = true;
        }
        if (hasWeightVolume) {
            expandSection(bodySectionWeight, ivArrowWeight);
        }

        // Existing multiple images
        List<ProductImage> existingImages = databaseHelper.getImagesForProduct(editProductId);
        imageEntries.clear();
        removedFilePaths.clear();
        if (existingImages != null && !existingImages.isEmpty()) {
            for (ProductImage pi : existingImages) {
                imageEntries.add(new SelectedImagesAdapter.ImageEntry(pi.getId(), pi.getImagePath(), pi.isPrimary()));
            }
        } else if (existingProduct.getImagePath() != null && !existingProduct.getImagePath().isEmpty()) {
            imageEntries.add(new SelectedImagesAdapter.ImageEntry(-1, existingProduct.getImagePath(), true));
        }
        updateImagesUI();
    }

    private void selectSpinnerItem(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void handleSaveProduct() {
        String name = etProductName.getText().toString().trim();
        String purchaseStr = etPurchasePrice.getText().toString().trim();
        String sellingStr = etSellingPrice.getText().toString().trim();
        String color = etColor.getText().toString().trim();

        String size = "";
        String selectedSize = (String) spinnerSize.getSelectedItem();
        if ("مخصص".equals(selectedSize)) {
            size = etCustomSize.getText().toString().trim();
        } else if (spinnerSize.getSelectedItemPosition() > 0) {
            size = selectedSize;
        }

        boolean isValid = true;

        if (name.isEmpty()) {
            tilProductName.setError(getString(R.string.error_name_empty));
            isValid = false;
        } else {
            tilProductName.setError(null);
        }

        double purchasePrice = 0;
        try {
            purchasePrice = Double.parseDouble(purchaseStr);
            if (purchasePrice < 0) {
                tilPurchasePrice.setError(getString(R.string.error_invalid_purchase_price));
                isValid = false;
            } else {
                tilPurchasePrice.setError(null);
            }
        } catch (NumberFormatException e) {
            tilPurchasePrice.setError(getString(R.string.error_invalid_purchase_price));
            isValid = false;
        }

        double sellingPrice = 0;
        try {
            sellingPrice = Double.parseDouble(sellingStr);
            if (sellingPrice < 0) {
                tilSellingPrice.setError(getString(R.string.error_invalid_selling_price));
                isValid = false;
            } else {
                tilSellingPrice.setError(null);
            }
        } catch (NumberFormatException e) {
            tilSellingPrice.setError(getString(R.string.error_invalid_selling_price));
            isValid = false;
        }

        if (categoryList == null || categoryList.isEmpty() || spinnerCategory.getSelectedItem() == null) {
            Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        final double finalPurchasePrice = purchasePrice;
        final double finalSellingPrice = sellingPrice;
        final String finalSize = size;

        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        long categoryId = selectedCategory.getId();

        // Optional measurements
        double lengthVal = parseDoubleOrZero(etLength.getText().toString());
        String lengthUnit = lengthVal > 0 ? (String) spinnerLengthUnit.getSelectedItem() : null;

        double widthVal = parseDoubleOrZero(etWidth.getText().toString());
        String widthUnit = widthVal > 0 ? (String) spinnerWidthUnit.getSelectedItem() : null;

        double heightVal = parseDoubleOrZero(etHeight.getText().toString());
        String heightUnit = heightVal > 0 ? (String) spinnerHeightUnit.getSelectedItem() : null;

        double weightVal = parseDoubleOrZero(etWeight.getText().toString());
        String weightUnit = weightVal > 0 ? (String) spinnerWeightUnit.getSelectedItem() : null;

        double volumeVal = parseDoubleOrZero(etVolume.getText().toString());
        String volumeUnit = volumeVal > 0 ? (String) spinnerVolumeUnit.getSelectedItem() : null;

        // Check if there are newly picked images to process
        boolean hasNewUris = false;
        for (SelectedImagesAdapter.ImageEntry entry : imageEntries) {
            if (entry.uri != null) {
                hasNewUris = true;
                break;
            }
        }

        final boolean finalHasNewUris = hasNewUris;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري معالجة الصور وعزل الخلفية…");
        progressDialog.setCancelable(false);
        if (finalHasNewUris) {
            progressDialog.show();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<ProductImage> finalImages = new ArrayList<>();
            String primaryPath = null;

            for (SelectedImagesAdapter.ImageEntry entry : imageEntries) {
                String savedPath = null;
                if (entry.uri != null) {
                    ImageUtils.ProcessedImageResult res = ImageUtils.processAndSaveProductImage(AddProductActivity.this, entry.uri, true);
                    if (res.success) {
                        savedPath = res.processedPath;
                    }
                } else if (entry.localPath != null) {
                    savedPath = entry.localPath;
                }

                if (savedPath != null) {
                    ProductImage pi = new ProductImage(entry.imageId > 0 ? entry.imageId : 0, savedPath, entry.isPrimary);
                    finalImages.add(pi);
                    if (entry.isPrimary || primaryPath == null) {
                        primaryPath = savedPath;
                    }
                }
            }

            final String finalPrimaryPath = primaryPath;

            mainHandler.post(() -> {
                if (finalHasNewUris && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (editProductId != -1 && existingProduct != null) {
                    existingProduct.setName(name);
                    existingProduct.setPurchasePrice(finalPurchasePrice);
                    existingProduct.setSellingPrice(finalSellingPrice);
                    existingProduct.setColor(color);
                    existingProduct.setSize(finalSize);
                    existingProduct.setCategoryId(categoryId);
                    existingProduct.setImagePath(finalPrimaryPath);
                    existingProduct.setImages(finalImages);

                    existingProduct.setLengthValue(lengthVal);
                    existingProduct.setLengthUnit(lengthUnit);
                    existingProduct.setWidthValue(widthVal);
                    existingProduct.setWidthUnit(widthUnit);
                    existingProduct.setHeightValue(heightVal);
                    existingProduct.setHeightUnit(heightUnit);
                    existingProduct.setWeightValue(weightVal);
                    existingProduct.setWeightUnit(weightUnit);
                    existingProduct.setVolumeValue(volumeVal);
                    existingProduct.setVolumeUnit(volumeUnit);

                    boolean success = databaseHelper.updateProduct(existingProduct);
                    if (success) {
                        // Delete unreferenced removed files from storage
                        for (String path : removedFilePaths) {
                            boolean inUse = false;
                            for (ProductImage pi : finalImages) {
                                if (path.equals(pi.getImagePath())) {
                                    inUse = true;
                                    break;
                                }
                            }
                            if (!inUse) {
                                try {
                                    java.io.File f = new java.io.File(path);
                                    if (f.exists()) {
                                        f.delete();
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        BackupManager.onProductEdited(AddProductActivity.this);
                        Toast.makeText(AddProductActivity.this, R.string.success_product_updated, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddProductActivity.this, "فشل تحديث المنتج", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Product newProduct = new Product(
                            0,
                            name,
                            finalPurchasePrice,
                            finalSellingPrice,
                            color,
                            finalSize,
                            finalPrimaryPath,
                            categoryId,
                            selectedCategory.getName()
                    );

                    newProduct.setImages(finalImages);
                    newProduct.setLengthValue(lengthVal);
                    newProduct.setLengthUnit(lengthUnit);
                    newProduct.setWidthValue(widthVal);
                    newProduct.setWidthUnit(widthUnit);
                    newProduct.setHeightValue(heightVal);
                    newProduct.setHeightUnit(heightUnit);
                    newProduct.setWeightValue(weightVal);
                    newProduct.setWeightUnit(weightUnit);
                    newProduct.setVolumeValue(volumeVal);
                    newProduct.setVolumeUnit(volumeUnit);

                    long insertedId = databaseHelper.insertProduct(newProduct);
                    if (insertedId != -1) {
                        BackupManager.onProductAdded(AddProductActivity.this);
                        Toast.makeText(AddProductActivity.this, R.string.success_product_saved, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(AddProductActivity.this, "فشل حفظ المنتج", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private double parseDoubleOrZero(String str) {
        if (str == null || str.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
