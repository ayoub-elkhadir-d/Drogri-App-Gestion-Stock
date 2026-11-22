package com.octo.drogri.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octo.drogri.R;
import com.octo.drogri.models.ProductImage;
import com.octo.drogri.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for 2-column equal-sized square image grid in ProductDetailsActivity.
 * Supports viewing full-screen zoom, setting primary image, and deleting images.
 */
public class ProductGalleryAdapter extends RecyclerView.Adapter<ProductGalleryAdapter.ViewHolder> {

    public interface OnGalleryInteractionListener {
        void onImageClick(ProductImage image, int position);
        void onImageOptionsClick(ProductImage image, int position);
    }

    private final Context context;
    private final List<ProductImage> images = new ArrayList<>();
    private final OnGalleryInteractionListener listener;

    public ProductGalleryAdapter(Context context, OnGalleryInteractionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setImages(List<ProductImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            this.images.addAll(newImages);
        }
        notifyDataSetChanged();
    }

    public List<ProductImage> getImages() {
        return images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gallery_grid_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductImage img = images.get(position);
        Bitmap bmp = ImageUtils.loadBitmap(img.getImagePath());
        if (bmp != null) {
            holder.ivImage.setImageBitmap(bmp);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_image);
        }

        holder.tvPrimaryBadge.setVisibility(img.isPrimary() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onImageClick(images.get(pos), pos);
            }
        });

        holder.btnOptions.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onImageOptionsClick(images.get(pos), pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvPrimaryBadge;
        ImageButton btnOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_grid_image);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_grid_primary_badge);
            btnOptions = itemView.findViewById(R.id.btn_grid_image_options);
        }
    }
}
