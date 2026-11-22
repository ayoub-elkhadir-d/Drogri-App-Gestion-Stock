package com.octo.drogri.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.octo.drogri.R;
import com.octo.drogri.models.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for horizontal category chip selector on the dashboard.
 */
public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.ViewHolder> {

    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }

    private final Context context;
    private final List<Category> categories = new ArrayList<>();
    private long selectedCategoryId = -1; // -1 represents "All Products"
    private final OnCategorySelectedListener listener;

    public CategoryChipAdapter(Context context, OnCategorySelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setCategories(List<Category> newCategories) {
        this.categories.clear();
        // Add "All Products" pseudo-category at the beginning
        this.categories.add(new Category(-1, context.getString(R.string.category_all)));
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    public void setSelectedCategoryId(long categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }

    public long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public int getPositionForCategoryId(long categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == categoryId) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvCategoryName.setText(category.getName());

        boolean isSelected = (category.getId() == selectedCategoryId);

        if (isSelected) {
            int primaryColor = com.octo.drogri.utils.ThemeManager.getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(24 * context.getResources().getDisplayMetrics().density);
            drawable.setColor(primaryColor);
            holder.tvCategoryName.setBackground(drawable);
            holder.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvCategoryName.setBackgroundResource(R.drawable.bg_chip_unselected);
            holder.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        View.OnClickListener clickListener = v -> {
            selectedCategoryId = category.getId();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onCategorySelected(category);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.tvCategoryName.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
        }
    }
}
