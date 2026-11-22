package com.octo.drogri.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.models.BackupRecord;
import com.octo.drogri.models.DeletedProduct;
import com.octo.drogri.models.ProductImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Manages local automatic and manual backup ZIP generation, validation, retention, and restoration.
 * Adheres strictly to the comprehensive MyStore_Backup.zip structure.
 */
public class LocalBackupManager {

    public static final String TAG = "MyStoreBackup";
    public static final String BACKUP_DIR_NAME = "backups";
    public static final String IMAGES_DIR_NAME = "product_images";

    public static File getBackupDirectory(Context context) {
        File dir = new File(context.getFilesDir(), BACKUP_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getImagesDirectory(Context context) {
        File dir = new File(context.getFilesDir(), IMAGES_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Creates a complete compressed ZIP backup containing:
     * - database.sqlite
     * - images/product_<productId>/image_<imageId>.<ext>
     * - deleted_products/images/<filename>
     * - deleted_products/deleted_products.json
     * - backup_info.json
     * - backup_history.json
     */
    public static File createBackupZip(Context context, String triggerType) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Backup started. Trigger: " + triggerType);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        File backupDir = getBackupDirectory(context);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String zipFileName = "MyStore_Backup_" + timeStamp + ".zip";
        File zipFile = new File(backupDir, zipFileName);

        Log.i(TAG, "ZIP creation started: " + zipFile.getAbsolutePath());

        File currentDbFile = context.getDatabasePath("mystore.db");

        // Flush WAL checkpoint to ensure database.sqlite is fully written and standalone
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = db.rawQuery("PRAGMA wal_checkpoint(FULL);", null);
            c.close();
        } catch (Exception e) {
            Log.w(TAG, "WAL checkpoint warning: " + e.getMessage());
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {

            // 1. Add database.sqlite
            if (currentDbFile.exists()) {
                addFileToZip(zos, currentDbFile, "database.sqlite");
                Log.i(TAG, "Database file added: " + currentDbFile.length() + " bytes");
            } else {
                Log.e(TAG, "Database file does not exist: " + currentDbFile.getAbsolutePath());
            }

            // 2. Read and add all product images
            List<ProductImage> allImages = dbHelper.getAllProductImages();
            JSONArray imagesManifest = new JSONArray();
            List<String> missingImages = new ArrayList<>();
            Set<String> addedZipEntries = new HashSet<>();
            int imagesAddedCount = 0;

            for (ProductImage img : allImages) {
                String path = img.getImagePath();
                if (path == null || path.trim().isEmpty()) {
                    continue;
                }
                File imgFile = new File(path);
                if (imgFile.exists() && imgFile.isFile()) {
                    String relativePath = "images/product_" + img.getProductId() + "/" + imgFile.getName();
                    if (!addedZipEntries.contains(relativePath)) {
                        addFileToZip(zos, imgFile, relativePath);
                        addedZipEntries.add(relativePath);
                        imagesAddedCount++;
                        Log.i(TAG, "Product image added: " + relativePath + " (" + imgFile.length() + " bytes)");
                    }
                    JSONObject item = new JSONObject();
                    item.put("product_id", img.getProductId());
                    item.put("image_id", img.getId());
                    item.put("is_primary", img.isPrimary());
                    item.put("file_name", imgFile.getName());
                    item.put("relative_path", relativePath);
                    imagesManifest.put(item);
                } else {
                    Log.w(TAG, "Missing image file: " + path + " for product ID " + img.getProductId());
                    missingImages.add(path);
                }
            }

            // 3. Read and add deleted product images & data
            List<DeletedProduct> deletedList = dbHelper.getAllDeletedProducts();
            JSONArray delArray = new JSONArray();
            JSONArray delImagesManifest = new JSONArray();

            for (DeletedProduct dp : deletedList) {
                JSONObject obj = new JSONObject();
                obj.put("id", dp.getId());
                obj.put("original_id", dp.getOriginalProductId());
                obj.put("name", dp.getName());
                obj.put("purchase_price", dp.getPurchasePrice());
                obj.put("selling_price", dp.getSellingPrice());
                obj.put("category", dp.getCategoryName());
                obj.put("deleted_at", dp.getDeletedAt());
                obj.put("image_path", dp.getImagePath());
                delArray.put(obj);

                String dpImg = dp.getImagePath();
                if (dpImg != null && !dpImg.trim().isEmpty()) {
                    File dpFile = new File(dpImg);
                    if (dpFile.exists() && dpFile.isFile()) {
                        String relDelPath = "deleted_products/images/" + dpFile.getName();
                        if (!addedZipEntries.contains(relDelPath)) {
                            addFileToZip(zos, dpFile, relDelPath);
                            addedZipEntries.add(relDelPath);
                            Log.i(TAG, "Deleted product image added: " + relDelPath);
                        }
                        JSONObject delImgObj = new JSONObject();
                        delImgObj.put("deleted_id", dp.getId());
                        delImgObj.put("file_name", dpFile.getName());
                        delImgObj.put("relative_path", relDelPath);
                        delImagesManifest.put(delImgObj);
                    }
                }
            }
            addBytesToZip(zos, delArray.toString().getBytes(StandardCharsets.UTF_8), "deleted_products/deleted_products.json");

            // 4. Add backup_history.json
            List<BackupRecord> records = dbHelper.getAllBackupRecords();
            JSONArray histArray = new JSONArray();
            for (BackupRecord rec : records) {
                JSONObject obj = new JSONObject();
                obj.put("name", rec.getBackupName());
                obj.put("type", rec.getBackupType());
                obj.put("date", rec.getCreatedAt());
                obj.put("size", rec.getFileSize());
                histArray.put(obj);
            }
            addBytesToZip(zos, histArray.toString().getBytes(StandardCharsets.UTF_8), "backup_history.json");

            // 5. Add backup_info.json
            int prodCount = dbHelper.getTotalProductCount();
            int catCount = dbHelper.getTotalCategoryCount();
            JSONObject infoJson = new JSONObject();
            infoJson.put("app_name", "MyStore");
            infoJson.put("app_version", "1.0");
            infoJson.put("database_version", 3);
            infoJson.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            infoJson.put("trigger_type", triggerType);
            infoJson.put("product_count", prodCount);
            infoJson.put("category_count", catCount);
            infoJson.put("images_count", imagesAddedCount);
            infoJson.put("missing_images_count", missingImages.size());
            infoJson.put("missing_images", new JSONArray(missingImages));
            infoJson.put("images_manifest", imagesManifest);
            infoJson.put("deleted_images_manifest", delImagesManifest);
            infoJson.put("version_id", "v3_" + timeStamp);

            addBytesToZip(zos, infoJson.toString().getBytes(StandardCharsets.UTF_8), "backup_info.json");

            zos.finish();

            long fileSize = zipFile.length();
            Log.i(TAG, "ZIP creation completed: path=" + zipFile.getAbsolutePath() + ", size=" + fileSize + " bytes (" + (fileSize / 1024) + " KB) in " + (System.currentTimeMillis() - startTime) + "ms");

            // Record in BackupHistory table
            BackupRecord record = new BackupRecord();
            record.setBackupName(zipFileName);
            record.setBackupType(triggerType);
            record.setFilePath(zipFile.getAbsolutePath());
            record.setFileSize(fileSize);
            record.setProductCount(prodCount);
            record.setCategoryCount(catCount);
            record.setCloud(false);
            dbHelper.insertBackupRecord(record);

            // Update settings
            SettingsManager settings = SettingsManager.getInstance(context);
            settings.setLastLocalBackupTime(System.currentTimeMillis());
            settings.setLastLocalBackupName(zipFileName);
            settings.setLastLocalBackupSize(fileSize);

            // Export persistent copy to SAF directory (survives app uninstall)
            exportBackupToSaf(context, zipFile);

            // Enforce retention policy
            enforceLocalRetentionPolicy(context);

            return zipFile;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create local backup", e);
            if (zipFile.exists()) {
                zipFile.delete();
            }
            return null;
        }
    }

    /**
     * Validates that the ZIP file is a well-formed MyStore backup.
     */
    public static boolean validateBackupZip(File zipFile) {
        if (zipFile == null || !zipFile.exists() || zipFile.length() == 0) {
            return false;
        }
        boolean hasDatabase = false;
        boolean hasInfo = false;

        try (ZipFile zf = new ZipFile(zipFile)) {
            ZipEntry dbEntry = zf.getEntry("database.sqlite");
            if (dbEntry != null) hasDatabase = true;

            ZipEntry infoEntry = zf.getEntry("backup_info.json");
            if (infoEntry != null) hasInfo = true;

            return hasDatabase && hasInfo;
        } catch (Exception e) {
            Log.e(TAG, "Zip validation failed", e);
            return false;
        }
    }

    /**
     * Safely restores database and product images from a backup ZIP.
     * 1. Creates safety backup of current database.
     * 2. Validates backup file.
     * 3. Extracts database and all images into internal storage.
     * 4. Updates SQLite image records to point to the new absolute paths.
     * 5. Verifies every restored image exists.
     * 6. Cleans up safety backup on success, or rolls back on error.
     */
    public static boolean restoreFromZip(Context context, File backupZip) {
        Log.i(TAG, "Restoration started from ZIP: " + (backupZip != null ? backupZip.getAbsolutePath() : "null"));

        if (!validateBackupZip(backupZip)) {
            Log.e(TAG, "Cannot restore: backup file is invalid or corrupted");
            return false;
        }

        File currentDbFile = context.getDatabasePath("mystore.db");
        File safetyDbFile = new File(currentDbFile.getParentFile(), "mystore.db.safety");

        // 1. Create safety backup
        try {
            if (currentDbFile.exists()) {
                copyFile(currentDbFile, safetyDbFile);
                Log.i(TAG, "Safety backup created at: " + safetyDbFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create safety backup", e);
            return false;
        }

        // 2. Close active database connections
        DatabaseHelper.getInstance(context).close();

        File imagesDir = getImagesDirectory(context);
        ByteArrayOutputStream infoJsonBytes = new ByteArrayOutputStream();

        // 3. Extract contents
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(backupZip)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.equals("database.sqlite")) {
                    try (FileOutputStream fos = new FileOutputStream(currentDbFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    Log.i(TAG, "Extracted database.sqlite to " + currentDbFile.getAbsolutePath());
                } else if (name.equals("backup_info.json")) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        infoJsonBytes.write(buffer, 0, len);
                    }
                } else if ((name.startsWith("images/") || name.startsWith("deleted_products/images/")) && !entry.isDirectory()) {
                    // Extract image filename
                    String fileName = new File(name).getName();
                    File imgDest = new File(imagesDir, fileName);
                    try (FileOutputStream fos = new FileOutputStream(imgDest)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    Log.i(TAG, "Extracted image: " + imgDest.getName() + " (" + imgDest.length() + " bytes)");
                }
                zis.closeEntry();
            }

            // 4. Update SQLite paths to point to the new absolute paths in current storage
            DatabaseHelper newHelper = DatabaseHelper.getInstance(context);

            if (infoJsonBytes.size() > 0) {
                try {
                    String infoStr = infoJsonBytes.toString(StandardCharsets.UTF_8.name());
                    JSONObject infoJson = new JSONObject(infoStr);

                    // Rebuild product images paths
                    JSONArray manifest = infoJson.optJSONArray("images_manifest");
                    if (manifest != null) {
                        for (int i = 0; i < manifest.length(); i++) {
                            JSONObject item = manifest.getJSONObject(i);
                            long imageId = item.optLong("image_id", -1);
                            long productId = item.optLong("product_id", -1);
                            boolean isPrimary = item.optBoolean("is_primary", false);
                            String fileName = item.optString("file_name", "");

                            if (!fileName.isEmpty()) {
                                File restoredFile = new File(imagesDir, fileName);
                                if (restoredFile.exists()) {
                                    String newPath = restoredFile.getAbsolutePath();
                                    if (imageId != -1) {
                                        newHelper.updateProductImageRecordPath(imageId, newPath);
                                    }
                                    if (isPrimary && productId != -1) {
                                        newHelper.updateProductPrimaryImagePath(productId, newPath);
                                    }
                                }
                            }
                        }
                    }

                    // Rebuild deleted products image paths
                    JSONArray delManifest = infoJson.optJSONArray("deleted_images_manifest");
                    if (delManifest != null) {
                        for (int i = 0; i < delManifest.length(); i++) {
                            JSONObject item = delManifest.getJSONObject(i);
                            long deletedId = item.optLong("deleted_id", -1);
                            String fileName = item.optString("file_name", "");
                            if (deletedId != -1 && !fileName.isEmpty()) {
                                File restoredFile = new File(imagesDir, fileName);
                                if (restoredFile.exists()) {
                                    newHelper.updateDeletedProductImagePath(deletedId, restoredFile.getAbsolutePath());
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.w(TAG, "Warning parsing backup_info.json during restore: " + e.getMessage());
                }
            }

            int restoredProdCount = newHelper.getTotalProductCount();
            int restoredImgCount = newHelper.getAllProductImagesCount();
            Log.i(TAG, "Restored successfully. Product count: " + restoredProdCount + ", Images count: " + restoredImgCount);

            // Clean up safety backup
            if (safetyDbFile.exists()) {
                safetyDbFile.delete();
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restoration failed, rolling back safety backup", e);
            // Roll back
            if (safetyDbFile.exists()) {
                try {
                    copyFile(safetyDbFile, currentDbFile);
                    Log.i(TAG, "Safety rollback completed");
                } catch (Exception ignored) {
                }
            }
            return false;
        }
    }

    /**
     * Atomically exports the backup ZIP into the user-selected persistent SAF directory (e.g. Documents/MyStore Backups/).
     * This backup file survives application uninstallation.
     */
    public static boolean exportBackupToSaf(Context context, File sourceZipFile) {
        if (context == null || sourceZipFile == null || !sourceZipFile.exists()) {
            return false;
        }
        SettingsManager settings = SettingsManager.getInstance(context);
        String treeUriStr = settings.getSafBackupTreeUri();
        if (treeUriStr == null || treeUriStr.trim().isEmpty()) {
            return false;
        }

        try {
            Uri treeUri = Uri.parse(treeUriStr);
            DocumentFile treeDoc = DocumentFile.fromTreeUri(context, treeUri);
            if (treeDoc == null || !treeDoc.canWrite()) {
                Log.w(TAG, "Cannot write to SAF directory or permission revoked");
                return false;
            }

            String finalName = "MyStore_Backup.zip";
            DocumentFile existingFile = treeDoc.findFile(finalName);
            if (existingFile != null && existingFile.exists()) {
                existingFile.delete();
            }

            DocumentFile targetDoc = treeDoc.createFile("application/zip", finalName);
            if (targetDoc == null) {
                Log.e(TAG, "Failed to create target file in SAF tree");
                return false;
            }

            try (InputStream in = new BufferedInputStream(new FileInputStream(sourceZipFile));
                 OutputStream out = new BufferedOutputStream(context.getContentResolver().openOutputStream(targetDoc.getUri()))) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }

            Log.i(TAG, "Exported persistent SAF backup successfully to: " + targetDoc.getUri() + " (" + sourceZipFile.length() + " bytes)");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception exporting persistent SAF backup", e);
            return false;
        }
    }

    /**
     * Restores database and assets from a persistent SAF backup URI.
     */
    public static boolean restoreFromSafUri(Context context, Uri safUri) {
        if (context == null || safUri == null) return false;
        File tempFile = new File(context.getCacheDir(), "saf_restore_temp.zip");
        try {
            try (InputStream in = new BufferedInputStream(context.getContentResolver().openInputStream(safUri));
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            boolean success = restoreFromZip(context, tempFile);
            if (tempFile.exists()) tempFile.delete();
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed reading backup from SAF URI", e);
            if (tempFile.exists()) tempFile.delete();
            return false;
        }
    }

    /**
     * Enforces the backup retention policy by deleting the oldest backups.
     */
    public static void enforceLocalRetentionPolicy(Context context) {
        int maxRetained = SettingsManager.getInstance(context).getMaxRetainedBackups();
        File backupDir = getBackupDirectory(context);
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));

        if (files != null && files.length > maxRetained) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            int toDelete = files.length - maxRetained;
            for (int i = 0; i < toDelete; i++) {
                files[i].delete();
                Log.d(TAG, "Deleted old backup for retention: " + files[i].getName());
            }
        }
    }

    private static void addFileToZip(ZipOutputStream zos, File file, String entryName) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(file.lastModified());
        zos.putNextEntry(entry);
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        zos.closeEntry();
    }

    private static void addBytesToZip(ZipOutputStream zos, byte[] data, String entryName) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }
}
