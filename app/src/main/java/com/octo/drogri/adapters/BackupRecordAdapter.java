package com.octo.drogri.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.octo.drogri.R;
import com.octo.drogri.models.BackupRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying backup records with date, size, product count and restore button.
 */
public class BackupRecordAdapter extends RecyclerView.Adapter<BackupRecordAdapter.ViewHolder> {

    public interface OnBackupRestoreListener {
        void onRestore(BackupRecord record);
    }

    private final Context context;
    private final List<BackupRecord> records = new ArrayList<>();
    private final OnBackupRestoreListener listener;

    public BackupRecordAdapter(Context context, OnBackupRestoreListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRecords(List<BackupRecord> newRecords) {
        this.records.clear();
        if (newRecords != null) {
            this.records.addAll(newRecords);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_backup_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BackupRecord record = records.get(position);
        holder.tvName.setText(record.getBackupName());

        String typeLabel = record.isCloud() ? "Google Drive" : "نسخة محلية";
        String details = typeLabel + " • " + record.getFormattedSize() + " • " + record.getCreatedAt();
        if (record.getProductCount() > 0) {
            details = record.getProductCount() + " منتج • " + details;
        }
        holder.tvDetails.setText(details);

        holder.btnRestore.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onRestore(records.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvDetails;
        MaterialButton btnRestore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_backup_icon);
            tvName = itemView.findViewById(R.id.tv_backup_name);
            tvDetails = itemView.findViewById(R.id.tv_backup_details);
            btnRestore = itemView.findViewById(R.id.btn_restore_record);
        }
    }
}
