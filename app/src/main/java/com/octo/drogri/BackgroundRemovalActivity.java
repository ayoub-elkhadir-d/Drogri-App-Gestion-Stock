package com.octo.drogri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.octo.drogri.adapters.BackgroundRemovalAdapter;
import com.octo.drogri.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dedicated AI Background Removal Preview Screen:
 * - Automatically begins on-device AI subject segmentation immediately upon selection.
 * - Displays side-by-side Before/After comparison on pure white (#FFFFFF) background.
 * - Provides individual and bulk choices: "استخدام الصور بعد إزالة الخلفية" or "استخدام الصور الأصلية".
 * - Resilient: preserves original images and handles individual failures gracefully.
 */
public class BackgroundRemovalActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_URIS = "extra_image_uris";
    public static final String EXTRA_RESULT_PATHS = "extra_result_paths";
    public static final String EXTRA_BG_REMOVED_FLAGS = "extra_bg_removed_flags";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ProgressBar pbOverall;
    private TextView tvOverallStatus;
    private RecyclerView rvPreviews;
    private BackgroundRemovalAdapter adapter;
    private MaterialButton btnAcceptProcessed;
    private MaterialButton btnUseOriginals;

    private final List<BackgroundRemovalAdapter.Item> items = new ArrayList<>();
    private boolean isFinishedProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_removal);

        ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra(EXTRA_IMAGE_URIS);
        if (uris == null || uris.isEmpty()) {
            Toast.makeText(this, "لم يتم اختيار أي صور", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupList(uris);
        startAutomaticProcessing();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        pbOverall = findViewById(R.id.pb_overall);
        tvOverallStatus = findViewById(R.id.tv_overall_status);
        rvPreviews = findViewById(R.id.rv_removal_previews);
        btnAcceptProcessed = findViewById(R.id.btn_accept_processed);
        btnUseOriginals = findViewById(R.id.btn_use_originals);

        btnAcceptProcessed.setOnClickListener(v -> acceptProcessedImages());
        btnUseOriginals.setOnClickListener(v -> acceptOriginalImages());
    }

    private void setupList(List<Uri> uris) {
        rvPreviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BackgroundRemovalAdapter(this);
        rvPreviews.setAdapter(adapter);

        items.clear();
        for (Uri u : uris) {
            items.add(new BackgroundRemovalAdapter.Item(u));
        }
        adapter.setItems(items);

        tvOverallStatus.setText("جاري معالجة " + items.size() + " صور بالذكاء الاصطناعي وعزل الخلفية…");
    }

    /**
     * Automatically initiates on-device AI background removal for all selected images immediately.
     */
    private void startAutomaticProcessing() {
        executor.execute(() -> {
            int completedCount = 0;
            int successBgCount = 0;

            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                BackgroundRemovalAdapter.Item item = items.get(index);

                // Run AI segmentation & optimization off main thread
                ImageUtils.ProcessedImageResult res = ImageUtils.processAndSaveProductImage(
                        BackgroundRemovalActivity.this,
                        item.originalUri,
                        true
                );

                if (res.success) {
                    item.isProcessing = false;
                    item.backgroundRemoved = res.backgroundRemoved;
                    item.processedSavedPath = res.processedPath;
                    item.originalSavedPath = res.originalPath;
                    item.userSelectedProcessed = res.backgroundRemoved;
                    item.statusMessage = res.statusMessage;

                    if (res.backgroundRemoved) {
                        successBgCount++;
                    }
                } else {
                    item.isProcessing = false;
                    item.backgroundRemoved = false;
                    item.originalSavedPath = res.originalPath;
                    item.processedSavedPath = res.originalPath;
                    item.userSelectedProcessed = false;
                    item.statusMessage = res.statusMessage;
                }

                completedCount++;
                final int currentCompleted = completedCount;
                final int finalSuccess = successBgCount;

                mainHandler.post(() -> {
                    adapter.notifyItemChanged(index);
                    tvOverallStatus.setText("جاري المعالجة: " + currentCompleted + " من " + items.size() + " مكتملة…");
                });
            }

            final int finalSuccessCount = successBgCount;
            mainHandler.post(() -> {
                isFinishedProcessing = true;
                pbOverall.setVisibility(View.GONE);
                if (finalSuccessCount == items.size()) {
                    tvOverallStatus.setText("اكتملت المعالجة: تم عزل جميع الصور بنجاح ووضعها على خلفية بيضاء نقية");
                } else if (finalSuccessCount > 0) {
                    tvOverallStatus.setText("اكتملت المعالجة: تم عزل " + finalSuccessCount + " من " + items.size() + " صور بنجاح");
                } else {
                    tvOverallStatus.setText("اكتملت المعالجة: تم حفظ الصور الأصلية بجودة كاملة للمنتج");
                }
            });
        });
    }

    /**
     * Primary action: Uses the processed background-removed images (or user's per-item selection).
     */
    private void acceptProcessedImages() {
        ArrayList<String> resultPaths = new ArrayList<>();
        boolean[] bgFlags = new boolean[items.size()];

        for (int i = 0; i < items.size(); i++) {
            BackgroundRemovalAdapter.Item it = items.get(i);
            String selectedPath = (it.userSelectedProcessed && it.processedSavedPath != null) ?
                    it.processedSavedPath :
                    (it.originalSavedPath != null ? it.originalSavedPath : it.processedSavedPath);

            if (selectedPath != null) {
                resultPaths.add(selectedPath);
                bgFlags[i] = it.userSelectedProcessed && it.backgroundRemoved;
            }
        }

        if (resultPaths.isEmpty()) {
            Toast.makeText(this, "لم تكتمل معالجة أي صورة", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_RESULT_PATHS, resultPaths);
        data.putExtra(EXTRA_BG_REMOVED_FLAGS, bgFlags);
        setResult(RESULT_OK, data);
        finish();
    }

    /**
     * Secondary action: Uses the original images without background removal.
     */
    private void acceptOriginalImages() {
        ArrayList<String> resultPaths = new ArrayList<>();
        boolean[] bgFlags = new boolean[items.size()];

        for (int i = 0; i < items.size(); i++) {
            BackgroundRemovalAdapter.Item it = items.get(i);
            String origPath = it.originalSavedPath != null ? it.originalSavedPath : it.processedSavedPath;
            if (origPath != null) {
                resultPaths.add(origPath);
                bgFlags[i] = false;
            }
        }

        if (resultPaths.isEmpty()) {
            Toast.makeText(this, "لم تكتمل معالجة أي صورة", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_RESULT_PATHS, resultPaths);
        data.putExtra(EXTRA_BG_REMOVED_FLAGS, bgFlags);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
