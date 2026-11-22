package com.octo.drogri;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.octo.drogri.utils.ImageUtils;
import com.octo.drogri.views.ZoomImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Full-Screen Image Viewer Activity:
 * - High resolution large display on dark background.
 * - Multi-touch pinch-to-zoom (up to 5x) and double-tap zoom (2.5x).
 * - Smooth horizontal swiping across multiple product images.
 * - Dynamic page indicator (e.g. 1 / 4).
 * - Preserves aspect ratio with zero distortion.
 */
public class ImageViewerActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATHS = "extra_image_paths";
    public static final String EXTRA_INITIAL_POSITION = "extra_initial_position";
    public static final String EXTRA_PRIMARY_PATH = "extra_primary_path";

    private ViewPager2 viewPager;
    private TextView tvCounter;
    private TextView tvBadge;
    private List<String> imagePaths = new ArrayList<>();
    private String primaryPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ArrayList<String> paths = getIntent().getStringArrayListExtra(EXTRA_IMAGE_PATHS);
        if (paths != null && !paths.isEmpty()) {
            imagePaths.addAll(paths);
        }

        int initialPos = getIntent().getIntExtra(EXTRA_INITIAL_POSITION, 0);
        primaryPath = getIntent().getStringExtra(EXTRA_PRIMARY_PATH);

        if (imagePaths.isEmpty()) {
            Toast.makeText(this, "لا توجد صور لعرضها", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewPager = findViewById(R.id.view_pager_images);
        tvCounter = findViewById(R.id.tv_image_counter);
        tvBadge = findViewById(R.id.tv_viewer_badge);

        ImageButton btnBack = findViewById(R.id.btn_viewer_back);
        btnBack.setOnClickListener(v -> finish());

        ImageViewerAdapter adapter = new ImageViewerAdapter(imagePaths);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(position);
            }
        });

        if (initialPos >= 0 && initialPos < imagePaths.size()) {
            viewPager.setCurrentItem(initialPos, false);
            updateCounter(initialPos);
        } else {
            updateCounter(0);
        }
    }

    private void updateCounter(int position) {
        String countText = (position + 1) + " / " + imagePaths.size();
        tvCounter.setText(countText);

        if (primaryPath != null && position < imagePaths.size()) {
            boolean isPrimary = primaryPath.equals(imagePaths.get(position));
            tvBadge.setVisibility(isPrimary ? View.VISIBLE : View.GONE);
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    private static class ImageViewerAdapter extends RecyclerView.Adapter<ImageViewerAdapter.ViewHolder> {

        private final List<String> paths;

        public ImageViewerAdapter(List<String> paths) {
            this.paths = paths;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_viewer_page, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String path = paths.get(position);
            Bitmap bmp = ImageUtils.loadBitmap(path);
            if (bmp != null) {
                holder.zoomImageView.setImageBitmap(bmp);
            }
        }

        @Override
        public int getItemCount() {
            return paths.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ZoomImageView zoomImageView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                zoomImageView = itemView.findViewById(R.id.zoom_image_view);
            }
        }
    }
}
