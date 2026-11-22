package com.octo.drogri.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octo.drogri.R;
import com.octo.drogri.models.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for managing categories with edit, delete, and view products actions in Arabic.
 */
public class CategoryManageAdapter extends RecyclerView.Adapter<CategoryManageAdapter.ViewHolder> {

    public interface OnCategoryActionListener {
        void onCategoryClick(Category category);
        void onEditCategory(Category category);
        void onDeleteCategory(Category category);
    }

    private final Context context;
    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryActionListener listener;

    public CategoryManageAdapter(Context context, OnCategoryActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCategories(List<Category> newCategories) {
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvCategoryName.setText(category.getName());

        int count = category.getProductCount();
        String countText;
        if (count == 0) {
            countText = "لا توجد منتجات";
        } else if (count == 1) {
            countText = "منتج واحد";
        } else if (count == 2) {
            countText = "منتجان";
        } else if (count <= 10) {
            countText = count + " منتجات";
        } else {
            countText = count + " منتج";
        }
        holder.tvProductCount.setText(countText);

        // Click whole category card/name to show its products
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditCategory(category);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCategory(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvProductCount;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvProductCount = itemView.findViewById(R.id.tv_product_count);
            btnEdit = itemView.findViewById(R.id.btn_edit_category);
            btnDelete = itemView.findViewById(R.id.btn_delete_category);
        }
    }
}
