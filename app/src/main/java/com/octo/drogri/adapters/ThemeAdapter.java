package com.octo.drogri.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.octo.drogri.R;
import com.octo.drogri.utils.ThemeManager;

import java.util.List;

/**
 * Adapter for displaying the 10 custom application themes in horizontal or grid cards.
 */
public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {

    public interface OnThemeSelectedListener {
        void onThemeSelected(int themeIndex);
    }

    private final Context context;
    private final List<ThemeManager.ThemeItem> themes;
    private int selectedIndex;
    private final OnThemeSelectedListener listener;

    public ThemeAdapter(Context context, int selectedIndex, OnThemeSelectedListener listener) {
        this.context = context;
        this.themes = ThemeManager.getThemes();
        this.selectedIndex = selectedIndex;
        this.listener = listener;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_theme_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThemeManager.ThemeItem theme = themes.get(position);
        holder.tvThemeName.setText(theme.nameResId);

        // Preview circles
        setCircleColor(holder.viewPrimary, theme.primaryHex);
        setCircleColor(holder.viewSecondary, theme.secondaryHex);
        setCircleColor(holder.viewBg, theme.backgroundHex);

        boolean isSelected = (theme.id == selectedIndex);
        float density = context.getResources().getDisplayMetrics().density;

        if (isSelected) {
            int primaryColor = ThemeManager.getThemeColor(context, com.google.android.material.R.attr.colorPrimary);
            holder.cardTheme.setStrokeColor(primaryColor);
            holder.cardTheme.setStrokeWidth((int) (2.5f * density));
            holder.tvSelectedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.cardTheme.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
            holder.cardTheme.setStrokeWidth((int) (1.0f * density));
            holder.tvSelectedBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedIndex;
            selectedIndex = theme.id;
            notifyItemChanged(prev);
            notifyItemChanged(selectedIndex);
            if (listener != null) {
                listener.onThemeSelected(theme.id);
            }
        });
    }

    private void setCircleColor(View view, String hexColor) {
        try {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(hexColor));
            drawable.setStroke(1, Color.parseColor("#33000000"));
            view.setBackground(drawable);
        } catch (Exception ignored) {}
    }

    @Override
    public int getItemCount() {
        return themes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTheme;
        View viewPrimary;
        View viewSecondary;
        View viewBg;
        TextView tvThemeName;
        TextView tvSelectedBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTheme = itemView.findViewById(R.id.card_theme);
            viewPrimary = itemView.findViewById(R.id.view_color_primary);
            viewSecondary = itemView.findViewById(R.id.view_color_secondary);
            viewBg = itemView.findViewById(R.id.view_color_bg);
            tvThemeName = itemView.findViewById(R.id.tv_theme_name);
            tvSelectedBadge = itemView.findViewById(R.id.tv_theme_selected);
        }
    }
}
