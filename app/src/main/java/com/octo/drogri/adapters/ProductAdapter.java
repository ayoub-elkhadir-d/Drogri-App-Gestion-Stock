package com.octo.drogri.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.octo.drogri.R;
import com.octo.drogri.models.Product;
import com.octo.drogri.utils.ImageUtils;
import com.octo.drogri.utils.SecurityManager;
import com.octo.drogri.utils.SettingsManager;
import com.octo.drogri.utils.ThemeManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying products in a clean 2-column grid.
 * Respects user preferences from SettingsManager for card display, fields, and currency.
 * Supports multi-selection mode with custom card highlights and smart aspect-ratio image display.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnSelectionListener {
        void onSelectionModeStarted();
        void onSelectionChanged(int count);
    }

    public interface OnCategoryBadgeClickListener {
        void onCategoryBadgeClick(long categoryId, String categoryName);
    }

    private final Context context;
    private final List<Product> products = new ArrayList<>();
    private final OnProductClickListener listener;
    private OnSelectionListener selectionListener;
    private OnCategoryBadgeClickListener categoryBadgeClickListener;
    private final SettingsManager settingsManager;

    // Selection mode state
    private boolean isSelectionMode = false;
    private final Set<Long> selectedProductIds = new HashSet<>();

    public ProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.settingsManager = SettingsManager.getInstance(context);
    }

    public void setOnSelectionListener(OnSelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    public void setOnCategoryBadgeClickListener(OnCategoryBadgeClickListener categoryBadgeClickListener) {
        this.categoryBadgeClickListener = categoryBadgeClickListener;
    }

    public void setProducts(List<Product> newProducts) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) {
            selectedProductIds.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(long productId) {
        if (selectedProductIds.contains(productId)) {
            selectedProductIds.remove(productId);
        } else {
            selectedProductIds.add(productId);
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedProductIds.size());
        }
    }

    public void selectAll() {
        selectedProductIds.clear();
        for (Product p : products) {
            selectedProductIds.add(p.getId());
        }
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedProductIds.size());
        }
    }

    public void clearSelection() {
        selectedProductIds.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(0);
        }
    }

    public Set<Long> getSelectedProductIds() {
        return new HashSet<>(selectedProductIds);
    }

    public int getSelectedCount() {
        return selectedProductIds.size();
    }

    public int getTotalCount() {
        return products.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        String currencySymbol = settingsManager.getCurrencySymbol();
        float density = context.getResources().getDisplayMetrics().density;

        // Base container height & padding
        int baseHeight = settingsManager.isCardCompact() ? 105 : 140;
        int targetHeight = baseHeight;

        if (holder.flImageContainer.getLayoutParams() != null) {
            if (settingsManager.isCardCompact()) {
                int p = (int) (8 * density);
                holder.layoutProductInfo.setPadding(p, (int) (6 * density), p, (int) (6 * density));
            } else {
                int p = (int) (12 * density);
                holder.layoutProductInfo.setPadding(p, p, p, p);
            }
        }

        // Product Name
        holder.tvProductName.setText(product.getName());

        // Image Container Display & Smart Aspect Ratio calculation
        if (settingsManager.showCardImage()) {
            holder.flImageContainer.setVisibility(View.VISIBLE);
            String imagePath = product.getImagePath();
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                Bitmap bitmap = ImageUtils.loadBitmap(imagePath);
                if (bitmap != null) {
                    holder.ivProductImage.setVisibility(View.VISIBLE);
                    holder.ivProductImage.setImageBitmap(bitmap);
                    holder.ivPlaceholder.setVisibility(View.GONE);

                    // Adaptive height for tall products to display without cropping
                    float aspectRatio = (float) bitmap.getWidth() / (float) Math.max(1, bitmap.getHeight());
                    if (aspectRatio < 0.75f && !settingsManager.isCardCompact()) {
                        targetHeight = 160;
                    }
                } else {
                    holder.ivProductImage.setVisibility(View.GONE);
                    holder.ivPlaceholder.setVisibility(View.VISIBLE);
                }
            } else {
                holder.ivProductImage.setVisibility(View.GONE);
                holder.ivPlaceholder.setVisibility(View.VISIBLE);
            }
        } else {
            holder.flImageContainer.setVisibility(View.GONE);
        }

        if (holder.flImageContainer.getLayoutParams() != null) {
            holder.flImageContainer.getLayoutParams().height = (int) (targetHeight * density);
        }

        // Multi-Selection Checkbox & Card Highlight
        if (isSelectionMode) {
            holder.cbSelectProduct.setVisibility(View.VISIBLE);
            boolean isSelected = selectedProductIds.contains(product.getId());
            holder.cbSelectProduct.setChecked(isSelected);

            if (isSelected) {
                int primaryColor = ThemeManager.getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
                holder.cardProduct.setStrokeColor(primaryColor);
                holder.cardProduct.setStrokeWidth((int) (2.5f * density));
            } else {
                holder.cardProduct.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
                holder.cardProduct.setStrokeWidth((int) (1.0f * density));
            }
        } else {
            holder.cbSelectProduct.setVisibility(View.GONE);
            holder.cardProduct.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
            holder.cardProduct.setStrokeWidth((int) (1.0f * density));
        }

        // Category Badge
        if (settingsManager.showCardCategory() && product.getCategoryName() != null && !product.getCategoryName().trim().isEmpty()) {
            holder.tvCategoryBadge.setVisibility(View.VISIBLE);
            holder.tvCategoryBadge.setText(product.getCategoryName());
            holder.tvCategoryBadge.setOnClickListener(v -> {
                if (!isSelectionMode && categoryBadgeClickListener != null) {
                    categoryBadgeClickListener.onCategoryBadgeClick(product.getCategoryId(), product.getCategoryName());
                }
            });
        } else {
            holder.tvCategoryBadge.setVisibility(View.GONE);
            holder.tvCategoryBadge.setOnClickListener(null);
        }

        // Color Indicator
        String color = product.getColor();
        if (settingsManager.showCardColor() && color != null && !color.trim().isEmpty()) {
            holder.tvColorIndicator.setVisibility(View.VISIBLE);
            holder.tvColorIndicator.setText(color.trim());
        } else {
            holder.tvColorIndicator.setVisibility(View.GONE);
        }

        // Size Indicator
        String size = product.getSize();
        if (settingsManager.showCardSize() && size != null && !size.trim().isEmpty()) {
            holder.tvSizeIndicator.setVisibility(View.VISIBLE);
            holder.tvSizeIndicator.setText(size.trim());
        } else {
            holder.tvSizeIndicator.setVisibility(View.GONE);
        }

        // Selling Price
        if (settingsManager.showCardSellingPrice()) {
            holder.tvSellingPrice.setVisibility(View.VISIBLE);
            holder.tvSellingPrice.setText(product.getFormattedSellingPrice(currencySymbol));
        } else {
            holder.tvSellingPrice.setVisibility(View.GONE);
        }

        // Purchase Price
        if (settingsManager.showCardPurchasePrice()) {
            holder.tvPurchasePrice.setVisibility(View.VISIBLE);
            SecurityManager securityManager = SecurityManager.getInstance(context);
            if (securityManager.isMaskPurchasePrice()) {
                holder.tvPurchasePrice.setText(context.getString(R.string.purchase_price_format, "••••••"));
            } else {
                holder.tvPurchasePrice.setText(context.getString(R.string.purchase_price_format, product.getFormattedPurchasePrice(currencySymbol)));
            }
        } else {
            holder.tvPurchasePrice.setVisibility(View.GONE);
        }

        // Click handling (selection toggle vs normal details opening)
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(product.getId());
            } else if (listener != null) {
                listener.onProductClick(product);
            }
        });

        // Long click handling (enter selection mode)
        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                setSelectionMode(true);
                toggleSelection(product.getId());
                if (selectionListener != null) {
                    selectionListener.onSelectionModeStarted();
                }
            } else {
                toggleSelection(product.getId());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardProduct;
        View flImageContainer;
        View layoutProductInfo;
        ImageView ivProductImage;
        ImageView ivPlaceholder;
        CheckBox cbSelectProduct;
        TextView tvCategoryBadge;
        TextView tvColorIndicator;
        TextView tvSizeIndicator;
        TextView tvProductName;
        TextView tvSellingPrice;
        TextView tvPurchasePrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardProduct = itemView.findViewById(R.id.card_product);
            flImageContainer = itemView.findViewById(R.id.fl_image_container);
            layoutProductInfo = itemView.findViewById(R.id.layout_product_info);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            ivPlaceholder = itemView.findViewById(R.id.iv_image_placeholder);
            cbSelectProduct = itemView.findViewById(R.id.cb_select_product);
            tvCategoryBadge = itemView.findViewById(R.id.tv_category_badge);
            tvColorIndicator = itemView.findViewById(R.id.tv_color_indicator);
            tvSizeIndicator = itemView.findViewById(R.id.tv_size_indicator);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvSellingPrice = itemView.findViewById(R.id.tv_selling_price);
            tvPurchasePrice = itemView.findViewById(R.id.tv_purchase_price);
        }
    }
}
