package com.octo.drogri;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.octo.drogri.adapters.BackupRecordAdapter;
import com.octo.drogri.adapters.DeletedProductAdapter;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.dialogs.PasswordDialog;
import com.octo.drogri.models.BackupRecord;
import com.octo.drogri.models.DeletedProduct;
import com.octo.drogri.utils.AuthHelper;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.LocalBackupManager;
import com.octo.drogri.utils.SecurityManager;

import java.io.File;
import java.util.List;

/**
 * Activity for viewing backup records and recovering accidentally deleted products.
 */
public class BackupHistoryActivity extends BaseActivity implements
        BackupRecordAdapter.OnBackupRestoreListener,
        DeletedProductAdapter.OnProductRestoreListener {

    private DatabaseHelper databaseHelper;

    private TextView tabBackups;
    private TextView tabDeletedProducts;
    private RecyclerView rvBackups;
    private RecyclerView rvDeletedProducts;
    private TextView tvEmpty;

    private BackupRecordAdapter backupAdapter;
    private DeletedProductAdapter deletedProductAdapter;

    private boolean isBackupsTabActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_history);

        databaseHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupRecyclerViews();
        loadActiveTab();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tabBackups = findViewById(R.id.tab_backups);
        tabDeletedProducts = findViewById(R.id.tab_deleted_products);
        rvBackups = findViewById(R.id.rv_backups);
        rvDeletedProducts = findViewById(R.id.rv_deleted_products);
        tvEmpty = findViewById(R.id.tv_empty_history);

        tabBackups.setOnClickListener(v -> {
            if (!isBackupsTabActive) {
                isBackupsTabActive = true;
                updateTabUI();
                loadActiveTab();
            }
        });

        tabDeletedProducts.setOnClickListener(v -> {
            if (isBackupsTabActive) {
                isBackupsTabActive = false;
                updateTabUI();
                loadActiveTab();
            }
        });
    }

    private void setupRecyclerViews() {
        rvBackups.setLayoutManager(new LinearLayoutManager(this));
        backupAdapter = new BackupRecordAdapter(this, this);
        rvBackups.setAdapter(backupAdapter);

        rvDeletedProducts.setLayoutManager(new LinearLayoutManager(this));
        deletedProductAdapter = new DeletedProductAdapter(this, this);
        rvDeletedProducts.setAdapter(deletedProductAdapter);
    }

    private void updateTabUI() {
        if (isBackupsTabActive) {
            tabBackups.setBackgroundResource(R.drawable.bg_chip_selected);
            tabBackups.setTextColor(ContextCompat.getColor(this, R.color.white));

            tabDeletedProducts.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            tabDeletedProducts.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            rvBackups.setVisibility(View.VISIBLE);
            rvDeletedProducts.setVisibility(View.GONE);
        } else {
            tabDeletedProducts.setBackgroundResource(R.drawable.bg_chip_selected);
            tabDeletedProducts.setTextColor(ContextCompat.getColor(this, R.color.white));

            tabBackups.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            tabBackups.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            rvDeletedProducts.setVisibility(View.VISIBLE);
            rvBackups.setVisibility(View.GONE);
        }
    }

    private void loadActiveTab() {
        if (isBackupsTabActive) {
            List<BackupRecord> records = databaseHelper.getAllBackupRecords();
            backupAdapter.setRecords(records);
            if (records.isEmpty()) {
                tvEmpty.setText(R.string.empty_backups);
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        } else {
            List<DeletedProduct> deletedList = databaseHelper.getAllDeletedProducts();
            deletedProductAdapter.setProducts(deletedList);
            if (deletedList.isEmpty()) {
                tvEmpty.setText(R.string.empty_deleted_products);
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onRestore(BackupRecord record) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_backup_confirm_title)
                .setMessage(R.string.restore_backup_confirm_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    AuthHelper.authenticate(BackupHistoryActivity.this, getString(R.string.confirm_password_dialog_msg), () -> performRestoreBackupRecord(record));
                })
                .show();
    }

    private void performRestoreBackupRecord(BackupRecord record) {
        File file = new File(record.getFilePath());
        if (!file.exists()) {
            Toast.makeText(this, "الملف غير موجود محلياً", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean restored = LocalBackupManager.restoreFromZip(this, file);
        if (restored) {
            Toast.makeText(this, R.string.success_restore, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "فشلت استعادة النسخة الاحتياطية", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestore(DeletedProduct product) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_product)
                .setMessage("هل ترغب في استرجاع المنتج \"" + product.getName() + "\" إلى قائمة المنتجات النشطة؟")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    boolean success = databaseHelper.restoreDeletedProduct(product.getId());
                    if (success) {
                        BackupManager.onProductAdded(this);
                        Toast.makeText(this, R.string.success_product_restored, Toast.LENGTH_SHORT).show();
                        loadActiveTab();
                        setResult(RESULT_OK);
                    } else {
                        Toast.makeText(this, "فشل استرجاع المنتج", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
