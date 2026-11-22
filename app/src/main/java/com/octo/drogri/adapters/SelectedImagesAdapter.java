package com.octo.drogri.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octo.drogri.R;
import com.octo.drogri.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for managing multiple selected images in the Add/Edit Product form.
 */
public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder> {

    public static class ImageEntry {
        public long imageId = -1;
        public Uri uri;
        public String localPath;
        public boolean isPrimary;

        public ImageEntry(Uri uri, boolean isPrimary) {
            this.uri = uri;
            this.isPrimary = isPrimary;
        }

        public ImageEntry(String localPath, boolean isPrimary) {
            this.localPath = localPath;
            this.isPrimary = isPrimary;
        }

        public ImageEntry(long imageId, String localPath, boolean isPrimary) {
            this.imageId = imageId;
            this.localPath = localPath;
            this.isPrimary = isPrimary;
        }
    }

    public interface OnImageActionListener {
        void onRemove(int position);
        void onSetPrimary(int position);
    }

    private final Context context;
    private final List<ImageEntry> items = new ArrayList<>();
    private final OnImageActionListener listener;

    public SelectedImagesAdapter(Context context, OnImageActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ImageEntry> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addEntry(ImageEntry entry) {
        if (items.isEmpty()) {
            entry.isPrimary = true;
        }
        this.items.add(entry);
        notifyItemInserted(items.size() - 1);
    }

    public void removeEntry(int position) {
        if (position >= 0 && position < items.size()) {
            boolean wasPrimary = items.get(position).isPrimary;
            items.remove(position);
            if (wasPrimary && !items.isEmpty()) {
                items.get(0).isPrimary = true;
            }
            notifyDataSetChanged();
        }
    }

    public void setPrimaryIndex(int position) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).isPrimary = (i == position);
        }
        notifyDataSetChanged();
    }

    public List<ImageEntry> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageEntry entry = items.get(position);

        if (entry.uri != null) {
            holder.ivThumbnail.setImageURI(entry.uri);
        } else if (entry.localPath != null) {
            Bitmap bmp = ImageUtils.loadBitmap(entry.localPath);
            if (bmp != null) {
                holder.ivThumbnail.setImageBitmap(bmp);
            }
        }

        if (entry.isPrimary) {
            holder.tvPrimaryBadge.setVisibility(View.VISIBLE);
            holder.btnMakePrimary.setVisibility(View.GONE);
        } else {
            holder.tvPrimaryBadge.setVisibility(View.GONE);
            holder.btnMakePrimary.setVisibility(View.VISIBLE);
        }

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRemove(pos);
            }
        });

        holder.btnMakePrimary.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onSetPrimary(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvPrimaryBadge;
        ImageButton btnRemove;
        TextView btnMakePrimary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
            btnRemove = itemView.findViewById(R.id.btn_remove_image);
            btnMakePrimary = itemView.findViewById(R.id.btn_make_primary);
        }
    }
}
