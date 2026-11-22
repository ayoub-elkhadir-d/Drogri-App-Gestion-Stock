package com.octo.drogri.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.octo.drogri.R;
import com.octo.drogri.models.DeletedProduct;
import com.octo.drogri.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying deleted products in the recovery archive.
 */
public class DeletedProductAdapter extends RecyclerView.Adapter<DeletedProductAdapter.ViewHolder> {

    public interface OnProductRestoreListener {
        void onRestore(DeletedProduct product);
    }

    private final Context context;
    private final List<DeletedProduct> products = new ArrayList<>();
    private final OnProductRestoreListener listener;

    public DeletedProductAdapter(Context context, OnProductRestoreListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setProducts(List<DeletedProduct> newProducts) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_deleted_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeletedProduct p = products.get(position);
        holder.tvName.setText(p.getName());

        String details = p.getFormattedSellingPrice();
        if (p.getCategoryName() != null && !p.getCategoryName().isEmpty()) {
            details += " • " + p.getCategoryName();
        }
        if (p.getDeletedAt() != null) {
            details += " • حُذف: " + p.getDeletedAt();
        }
        holder.tvDetails.setText(details);

        if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
            Bitmap bmp = ImageUtils.loadBitmap(p.getImagePath());
            if (bmp != null) {
                holder.ivThumbnail.setImageBitmap(bmp);
                holder.ivThumbnail.setVisibility(View.VISIBLE);
                holder.ivPlaceholder.setVisibility(View.GONE);
            } else {
                holder.ivThumbnail.setVisibility(View.GONE);
                holder.ivPlaceholder.setVisibility(View.VISIBLE);
            }
        } else {
            holder.ivThumbnail.setVisibility(View.GONE);
            holder.ivPlaceholder.setVisibility(View.VISIBLE);
        }

        holder.btnRestore.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRestore(products.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        ImageView ivPlaceholder;
        TextView tvName;
        TextView tvDetails;
        MaterialButton btnRestore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_deleted_thumbnail);
            ivPlaceholder = itemView.findViewById(R.id.iv_deleted_placeholder);
            tvName = itemView.findViewById(R.id.tv_deleted_name);
            tvDetails = itemView.findViewById(R.id.tv_deleted_details);
            btnRestore = itemView.findViewById(R.id.btn_restore_product);
        }
    }
}
