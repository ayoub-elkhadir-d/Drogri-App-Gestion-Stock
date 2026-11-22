package com.octo.drogri.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.octo.drogri.R;
import com.octo.drogri.utils.ImageUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the AI Background Removal Preview screen.
 * Displays side-by-side comparison (Before/After), live processing state, and choice toggle.
 */
public class BackgroundRemovalAdapter extends RecyclerView.Adapter<BackgroundRemovalAdapter.ViewHolder> {

    public static class Item {
        public Uri originalUri;
        public Bitmap originalThumb;
        public Bitmap processedThumb;
        public String originalSavedPath;
        public String processedSavedPath;
        public boolean isProcessing = true;
        public boolean backgroundRemoved = false;
        public boolean userSelectedProcessed = true;
        public String statusMessage = "جاري الكشف عن المنتج وعزل الخلفية…";

        public Item(Uri uri) {
            this.originalUri = uri;
        }
    }

    private final Context context;
    private final List<Item> items = new ArrayList<>();

    public BackgroundRemovalAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<Item> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<Item> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_background_removal_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);

        String indexText = "الصورة " + (position + 1) + " من " + items.size();
        holder.tvItemIndex.setText(indexText);

        // Load original thumbnail if not loaded yet
        if (item.originalThumb == null && item.originalUri != null) {
            item.originalThumb = loadSampledBitmapFromUri(context, item.originalUri, 300, 300);
        }
        if (item.originalThumb != null) {
            holder.ivOriginal.setImageBitmap(item.originalThumb);
        }

        if (item.isProcessing) {
            holder.pbItemStatus.setVisibility(View.VISIBLE);
            holder.tvItemStatus.setText("جاري المعالجة…");
            holder.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.ivProcessed.setVisibility(View.GONE);
            holder.layoutProcessingPlaceholder.setVisibility(View.VISIBLE);
            holder.rgChoice.setVisibility(View.GONE);
        } else {
            holder.pbItemStatus.setVisibility(View.GONE);
            holder.layoutProcessingPlaceholder.setVisibility(View.GONE);
            holder.ivProcessed.setVisibility(View.VISIBLE);
            holder.rgChoice.setVisibility(View.VISIBLE);

            if (item.backgroundRemoved) {
                holder.tvItemStatus.setText(R.string.bg_removed_success);
                holder.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_green));
            } else {
                holder.tvItemStatus.setText(R.string.bg_removed_failed);
                holder.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_orange));
            }

            // Display processed bitmap or load from saved path
            if (item.processedThumb == null && item.processedSavedPath != null) {
                item.processedThumb = ImageUtils.loadBitmap(item.processedSavedPath);
            }

            if (item.processedThumb != null) {
                holder.ivProcessed.setImageBitmap(item.processedThumb);
            } else if (item.originalThumb != null) {
                holder.ivProcessed.setImageBitmap(item.originalThumb);
            }

            holder.rgChoice.setOnCheckedChangeListener(null);
            holder.rbUseProcessed.setChecked(item.userSelectedProcessed);
            holder.rbUseOriginal.setChecked(!item.userSelectedProcessed);

            holder.rgChoice.setOnCheckedChangeListener((group, checkedId) -> {
                item.userSelectedProcessed = (checkedId == R.id.rb_use_processed);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static Bitmap loadSampledBitmapFromUri(Context context, Uri uri, int reqW, int reqH) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, options);
            }

            int inSampleSize = 1;
            while ((options.outWidth / inSampleSize) > reqW || (options.outHeight / inSampleSize) > reqH) {
                inSampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                return BitmapFactory.decodeStream(is, null, options);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemIndex;
        LinearLayout layoutStatusBadge;
        ProgressBar pbItemStatus;
        TextView tvItemStatus;

        ImageView ivProcessed;
        LinearLayout layoutProcessingPlaceholder;
        ImageView ivOriginal;

        RadioGroup rgChoice;
        RadioButton rbUseProcessed;
        RadioButton rbUseOriginal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemIndex = itemView.findViewById(R.id.tv_item_index);
            layoutStatusBadge = itemView.findViewById(R.id.layout_status_badge);
            pbItemStatus = itemView.findViewById(R.id.pb_item_status);
            tvItemStatus = itemView.findViewById(R.id.tv_item_status);

            ivProcessed = itemView.findViewById(R.id.iv_processed);
            layoutProcessingPlaceholder = itemView.findViewById(R.id.layout_processing_placeholder);
            ivOriginal = itemView.findViewById(R.id.iv_original);

            rgChoice = itemView.findViewById(R.id.rg_choice);
            rbUseProcessed = itemView.findViewById(R.id.rb_use_processed);
            rbUseOriginal = itemView.findViewById(R.id.rb_use_original);
        }
    }
}
