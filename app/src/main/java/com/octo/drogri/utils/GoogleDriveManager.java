package com.octo.drogri.utils;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.octo.drogri.models.BackupRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Google Drive API v3 operations: OAuth2 token generation,
 * SINGLE backup file management (MyStore_Backup.zip), in-place file updating via files.update,
 * multipart upload (<= 5MB), resumable upload (> 5MB), download, listing, and duplicate cleanup.
 */
public class GoogleDriveManager {

    public static final String TAG = "MyStoreBackup";
    public static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    public static final String CLOUD_BACKUP_FILENAME = "MyStore_Backup.zip";
    public static final long RESUMABLE_THRESHOLD = 5 * 1024 * 1024; // 5 MB

    public static class UploadResult {
        public boolean success;
        public String fileId;
        public String fileName;
        public long fileSize;
        public int httpStatusCode;
        public String errorMessage;
        public String userFriendlyMessage;
        public boolean wasUpdated;

        public UploadResult(boolean success) {
            this.success = success;
        }
    }

    public static GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DRIVE_SCOPE))
                .build();
        return GoogleSignIn.getClient(context, gso);
    }

    public static boolean isAccountConnected(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null && GoogleSignIn.hasPermissions(account, new Scope(DRIVE_SCOPE));
    }

    public static String getConnectedEmail(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null ? account.getEmail() : null;
    }

    public static String getAccessToken(Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null || account.getAccount() == null) {
            Log.w(TAG, "Access token acquisition failed: account is null");
            return null;
        }
        try {
            String token = GoogleAuthUtil.getToken(context, account.getAccount(), "oauth2:" + DRIVE_SCOPE);
            if (token != null && !token.trim().isEmpty()) {
                Log.i(TAG, "Access token acquisition result: SUCCESS (token withheld)");
                return token.trim();
            } else {
                Log.w(TAG, "Access token acquisition result: EMPTY");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Access token acquisition result: FAILED (" + e.getMessage() + ")");
            return null;
        }
    }

    public static void invalidateToken(Context context, String token) {
        if (token != null) {
            try {
                GoogleAuthUtil.clearToken(context, token);
                Log.i(TAG, "Google OAuth token invalidated successfully");
            } catch (Exception e) {
                Log.w(TAG, "Failed to invalidate token: " + e.getMessage());
            }
        }
    }

    /**
     * Verifies if a specific Google Drive file exists and is not in trash.
     */
    public static boolean verifyDriveFileExists(String token, String fileId) {
        if (token == null || fileId == null || fileId.trim().isEmpty()) return false;
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=id,name,trashed");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String res = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(res);
                return !json.optBoolean("trashed", false);
            }
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Failed verifying file " + fileId + ": " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Gets or creates the dedicated "MyStore Backups" folder in Google Drive.
     */
    public static String getOrCreateBackupFolderId(Context context, String token) {
        if (token == null) return null;
        HttpURLConnection conn = null;
        try {
            // 1. Search for existing folder
            String query = "name = 'MyStore Backups' and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
            URL searchUrl = new URL("https://www.googleapis.com/drive/v3/files?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&fields=files(id)");
            conn = (HttpURLConnection) searchUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String res = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(res);
                JSONArray files = json.optJSONArray("files");
                if (files != null && files.length() > 0) {
                    String folderId = files.getJSONObject(0).getString("id");
                    Log.i(TAG, "Found existing Drive folder ID: " + folderId);
                    return folderId;
                }
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                invalidateToken(context, token);
                return null;
            } else if (code == HttpURLConnection.HTTP_FORBIDDEN) {
                String err = readStream(conn.getErrorStream());
                Log.e(TAG, "Drive API access forbidden (" + code + "): " + err);
                return null;
            }
            conn.disconnect();

            // 2. Create folder if not found
            URL createUrl = new URL("https://www.googleapis.com/drive/v3/files");
            conn = (HttpURLConnection) createUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            JSONObject folderMeta = new JSONObject();
            folderMeta.put("name", "MyStore Backups");
            folderMeta.put("mimeType", "application/vnd.google-apps.folder");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(folderMeta.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int createCode = conn.getResponseCode();
            if (createCode == HttpURLConnection.HTTP_OK || createCode == HttpURLConnection.HTTP_CREATED) {
                String res = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(res);
                String newFolderId = json.has("id") ? json.getString("id") : null;
                Log.i(TAG, "Created new Drive folder ID: " + newFolderId);
                return newFolderId;
            } else {
                String err = readStream(conn.getErrorStream());
                Log.e(TAG, "Failed creating Drive folder (" + createCode + "): " + err);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get or create MyStore Backups folder: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Resolves the single valid Google Drive backup file ID.
     * Checks locally cached ID, searches Drive folder for MyStore_Backup.zip,
     * and automatically removes duplicate backup files if any were created previously.
     */
    public static String resolveSingleBackupFileId(Context context, String token, String folderId) {
        SettingsManager settings = SettingsManager.getInstance(context);
        String cachedId = settings.getLastCloudBackupId();

        // 1. Check if cached ID is still valid in Drive
        if (cachedId != null && !cachedId.trim().isEmpty()) {
            if (verifyDriveFileExists(token, cachedId)) {
                Log.i(TAG, "Using verified single cloud backup file ID: " + cachedId);
                return cachedId;
            } else {
                Log.w(TAG, "Cached cloud backup ID " + cachedId + " no longer exists or was trashed");
                settings.setLastCloudBackupId(null);
            }
        }

        // 2. Search Drive for existing files named MyStore_Backup.zip (or old backups)
        HttpURLConnection conn = null;
        try {
            String query;
            if (folderId != null) {
                query = "'" + folderId + "' in parents and name contains 'MyStore_Backup' and trashed = false";
            } else {
                query = "name contains 'MyStore_Backup' and trashed = false";
            }

            URL url = new URL("https://www.googleapis.com/drive/v3/files?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&fields=files(id,name,modifiedTime,createdTime)&orderBy=modifiedTime desc");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String res = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(res);
                JSONArray files = json.optJSONArray("files");

                if (files != null && files.length() > 0) {
                    // Pick the most recent one as our single official backup file
                    String singleFileId = files.getJSONObject(0).getString("id");
                    Log.i(TAG, "Identified single active backup file ID: " + singleFileId);
                    settings.setLastCloudBackupId(singleFileId);

                    // Duplicate cleanup: If multiple old backup files exist, delete redundant older duplicates
                    if (files.length() > 1) {
                        Log.i(TAG, "Found " + files.length() + " backup files in Drive. Cleaning up " + (files.length() - 1) + " old duplicates...");
                        for (int i = 1; i < files.length(); i++) {
                            String dupId = files.getJSONObject(i).getString("id");
                            deleteDriveBackup(context, dupId);
                            Log.i(TAG, "Cleaned up duplicate Drive backup file: " + dupId);
                        }
                    }

                    return singleFileId;
                }
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                invalidateToken(context, token);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving single backup file in Drive: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }

        return null; // Signals that no backup file exists yet in Google Drive
    }

    /**
     * Uploads the backup ZIP to Google Drive.
     * If a single backup file already exists, it UPDATES that file in-place using files.update.
     * If no backup file exists yet, it CREATES the single MyStore_Backup.zip using files.create.
     * Duplicate backup files are automatically prevented and cleaned up.
     */
    public static UploadResult uploadBackup(Context context, File zipFile, String description) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "Starting Google Drive backup process for: " + zipFile.getName() + " (" + zipFile.length() + " bytes)");

        UploadResult result = new UploadResult(false);
        result.fileName = CLOUD_BACKUP_FILENAME;
        result.fileSize = zipFile.length();

        String token = getAccessToken(context);
        if (token == null) {
            result.userFriendlyMessage = "فشل الحصول على تصريح الوصول لحساب Google";
            Log.e(TAG, "Final backup status: FAILED (missing token)");
            return result;
        }

        String folderId = getOrCreateBackupFolderId(context, token);
        if (folderId != null) {
            Log.i(TAG, "Target Google Drive Folder ID: " + folderId);
        } else {
            Log.w(TAG, "Google Drive Folder ID is null, attempting root search/upload");
        }

        // Check for existing single backup file ID to update
        String existingFileId = resolveSingleBackupFileId(context, token, folderId);
        boolean isUpdate = (existingFileId != null);
        result.wasUpdated = isUpdate;

        if (isUpdate) {
            Log.i(TAG, "Existing single cloud backup found (" + existingFileId + "). Updating in-place with files.update");
        } else {
            Log.i(TAG, "No existing cloud backup found. Creating first single backup with files.create");
        }

        boolean useResumable = zipFile.length() > RESUMABLE_THRESHOLD;
        Log.i(TAG, "Upload method: " + (useResumable ? "Resumable (> 5MB)" : "Multipart (<= 5MB)") + " | In-place update: " + isUpdate);

        if (useResumable) {
            result = uploadResumable(context, token, zipFile, description, folderId, existingFileId);
        } else {
            result = uploadMultipart(context, token, zipFile, description, folderId, existingFileId);
        }

        result.fileName = CLOUD_BACKUP_FILENAME;
        result.fileSize = zipFile.length();
        result.wasUpdated = isUpdate;

        long duration = System.currentTimeMillis() - startTime;
        Log.i(TAG, "Upload duration: " + duration + "ms");

        if (result.success && result.fileId != null) {
            Log.i(TAG, "Returned single file ID: " + result.fileId);
            Log.i(TAG, "Final backup status: SUCCESS (single file " + (isUpdate ? "updated" : "created") + ")");
            SettingsManager settings = SettingsManager.getInstance(context);
            settings.setLastCloudBackupTime(System.currentTimeMillis());
            settings.setLastCloudBackupName(CLOUD_BACKUP_FILENAME);
            settings.setLastCloudBackupId(result.fileId);
            settings.setLastCloudBackupSize(result.fileSize);
            settings.setLastCloudError(null);
            settings.resetProductsAddedSinceBackup();
        } else {
            Log.e(TAG, "Final backup status: FAILED (HTTP " + result.httpStatusCode + ": " + result.errorMessage + ")");
            SettingsManager.getInstance(context).setLastCloudError(result.userFriendlyMessage);
        }

        return result;
    }

    private static UploadResult uploadMultipart(Context context, String token, File zipFile, String description, String folderId, String existingFileId) {
        UploadResult result = new UploadResult(false);
        result.fileName = CLOUD_BACKUP_FILENAME;
        result.fileSize = zipFile.length();

        HttpURLConnection conn = null;
        try {
            String boundary = "-------MyStoreBoundary" + System.currentTimeMillis();
            URL url;
            boolean isUpdate = (existingFileId != null);

            if (isUpdate) {
                // files.update endpoint
                url = new URL("https://www.googleapis.com/upload/drive/v3/files/" + existingFileId + "?uploadType=multipart");
            } else {
                // files.create endpoint
                url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart");
            }

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            if (isUpdate) {
                // Use standard Google API method override to perform PATCH update safely
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);

            JSONObject metadata = new JSONObject();
            metadata.put("name", CLOUD_BACKUP_FILENAME);
            metadata.put("mimeType", "application/zip");
            metadata.put("description", description != null ? description : "MyStore Android Backup");
            if (!isUpdate && folderId != null) {
                JSONArray parents = new JSONArray();
                parents.put(folderId);
                metadata.put("parents", parents);
            }

            try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
                String part1 = "--" + boundary + "\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                        + metadata.toString() + "\r\n";
                os.write(part1.getBytes(StandardCharsets.UTF_8));

                String part2 = "--" + boundary + "\r\n"
                        + "Content-Type: application/zip\r\n\r\n";
                os.write(part2.getBytes(StandardCharsets.UTF_8));

                try (InputStream fis = new BufferedInputStream(new FileInputStream(zipFile))) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                }

                String end = "\r\n--" + boundary + "--\r\n";
                os.write(end.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            result.httpStatusCode = code;
            Log.i(TAG, "Multipart HTTP response code: " + code + " (isUpdate=" + isUpdate + ")");

            if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_CREATED) {
                String response = readStream(conn.getInputStream());
                JSONObject resObj = new JSONObject(response);
                result.fileId = resObj.getString("id");
                result.success = true;
                result.userFriendlyMessage = isUpdate ?
                        "تم تحديث النسخة الاحتياطية في Google Drive بنجاح" :
                        "تم إنشاء النسخة الاحتياطية في Google Drive بنجاح";
            } else {
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    invalidateToken(context, token);
                }
                String err = readStream(conn.getErrorStream());
                result.errorMessage = err;
                result.userFriendlyMessage = parseGoogleApiError(code, err);
                Log.e(TAG, "Google API error message: " + err);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in uploadMultipart", e);
            result.errorMessage = e.getMessage();
            result.userFriendlyMessage = "خطأ في الاتصال بالإنترنت: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    private static UploadResult uploadResumable(Context context, String token, File zipFile, String description, String folderId, String existingFileId) {
        UploadResult result = new UploadResult(false);
        result.fileName = CLOUD_BACKUP_FILENAME;
        result.fileSize = zipFile.length();

        HttpURLConnection conn = null;
        try {
            boolean isUpdate = (existingFileId != null);
            URL initUrl;
            if (isUpdate) {
                initUrl = new URL("https://www.googleapis.com/upload/drive/v3/files/" + existingFileId + "?uploadType=resumable");
            } else {
                initUrl = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable");
            }

            conn = (HttpURLConnection) initUrl.openConnection();
            conn.setRequestMethod("POST");
            if (isUpdate) {
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            }
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("X-Upload-Content-Type", "application/zip");
            conn.setRequestProperty("X-Upload-Content-Length", String.valueOf(zipFile.length()));

            JSONObject metadata = new JSONObject();
            metadata.put("name", CLOUD_BACKUP_FILENAME);
            metadata.put("mimeType", "application/zip");
            metadata.put("description", description != null ? description : "MyStore Android Backup");
            if (!isUpdate && folderId != null) {
                JSONArray parents = new JSONArray();
                parents.put(folderId);
                metadata.put("parents", parents);
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(metadata.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            result.httpStatusCode = code;
            Log.i(TAG, "Resumable init HTTP response code: " + code);

            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_CREATED) {
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    invalidateToken(context, token);
                }
                String err = readStream(conn.getErrorStream());
                result.errorMessage = err;
                result.userFriendlyMessage = parseGoogleApiError(code, err);
                Log.e(TAG, "Google API error message (resumable init): " + err);
                return result;
            }

            String sessionUri = conn.getHeaderField("Location");
            conn.disconnect();

            if (sessionUri == null || sessionUri.isEmpty()) {
                result.userFriendlyMessage = "فشل بدء جلسة الرفع مع Google Drive";
                return result;
            }

            // Step 2: Upload file bytes
            URL uploadUrl = new URL(sessionUri);
            conn = (HttpURLConnection) uploadUrl.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Content-Type", "application/zip");
            conn.setRequestProperty("Content-Length", String.valueOf(zipFile.length()));

            try (OutputStream os = new BufferedOutputStream(conn.getOutputStream());
                 InputStream fis = new BufferedInputStream(new FileInputStream(zipFile))) {
                byte[] buffer = new byte[16384];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }

            int uploadCode = conn.getResponseCode();
            result.httpStatusCode = uploadCode;
            Log.i(TAG, "Resumable upload HTTP response code: " + uploadCode);

            if (uploadCode == HttpURLConnection.HTTP_OK || uploadCode == HttpURLConnection.HTTP_CREATED) {
                String response = readStream(conn.getInputStream());
                JSONObject resObj = new JSONObject(response);
                result.fileId = resObj.getString("id");
                result.success = true;
                result.userFriendlyMessage = isUpdate ?
                        "تم تحديث النسخة الاحتياطية في Google Drive بنجاح" :
                        "تم إنشاء النسخة الاحتياطية في Google Drive بنجاح";
            } else {
                String err = readStream(conn.getErrorStream());
                result.errorMessage = err;
                result.userFriendlyMessage = parseGoogleApiError(uploadCode, err);
                Log.e(TAG, "Google API error message (resumable put): " + err);
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in uploadResumable", e);
            result.errorMessage = e.getMessage();
            result.userFriendlyMessage = "خطأ في الاتصال بالإنترنت أثناء الرفع: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    /**
     * Lists the single active MyStore backup present in the user's Google Drive.
     */
    public static List<BackupRecord> listDriveBackups(Context context) {
        List<BackupRecord> list = new ArrayList<>();
        String token = getAccessToken(context);
        if (token == null) return list;

        HttpURLConnection conn = null;
        try {
            String query = "name contains 'MyStore_Backup' and trashed = false";
            String urlStr = "https://www.googleapis.com/drive/v3/files?q="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&fields=files(id,name,size,modifiedTime,createdTime,description)&orderBy=modifiedTime desc";

            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                String response = readStream(conn.getInputStream());
                JSONObject resObj = new JSONObject(response);
                JSONArray files = resObj.optJSONArray("files");
                if (files != null) {
                    for (int i = 0; i < files.length(); i++) {
                        JSONObject f = files.getJSONObject(i);
                        BackupRecord rec = new BackupRecord();
                        rec.setDriveFileId(f.getString("id"));
                        rec.setBackupName(f.getString("name"));
                        rec.setFileSize(f.optLong("size", 0));
                        String timestamp = f.optString("modifiedTime", f.optString("createdTime", ""));
                        rec.setCreatedAt(timestamp);
                        rec.setBackupType("GOOGLE_DRIVE");
                        rec.setCloud(true);
                        list.add(rec);
                    }
                }
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                invalidateToken(context, token);
            } else {
                Log.e(TAG, "List drive backups failed (" + code + "): " + readStream(conn.getErrorStream()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing Google Drive backups", e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return list;
    }

    /**
     * Downloads a backup ZIP from Google Drive to a destination file on local disk.
     */
    public static boolean downloadDriveBackup(Context context, String driveFileId, File destFile) {
        String token = getAccessToken(context);
        if (token == null) return false;

        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://www.googleapis.com/drive/v3/files/" + driveFileId + "?alt=media");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = new BufferedInputStream(conn.getInputStream());
                     OutputStream os = new BufferedOutputStream(new FileOutputStream(destFile))) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }
                }
                return destFile.exists() && destFile.length() > 0;
            } else if (conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                invalidateToken(context, token);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading from Google Drive", e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Deletes a backup from Google Drive.
     */
    public static boolean deleteDriveBackup(Context context, String driveFileId) {
        String token = getAccessToken(context);
        if (token == null || driveFileId == null || driveFileId.isEmpty()) return false;

        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://www.googleapis.com/drive/v3/files/" + driveFileId);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            Log.i(TAG, "Delete file " + driveFileId + " response code: " + code);
            return code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file from Google Drive", e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Parses Google API error responses into clear, actionable Arabic messages for Moroccan users.
     */
    public static String parseGoogleApiError(int statusCode, String errorJson) {
        if (errorJson == null || errorJson.isEmpty()) {
            return "فشل الاتصال بـ Google Drive (رمز الخطأ: " + statusCode + ")";
        }
        try {
            JSONObject obj = new JSONObject(errorJson);
            JSONObject errObj = obj.optJSONObject("error");
            if (errObj != null) {
                String message = errObj.optString("message", "");
                String status = errObj.optString("status", "");

                if (statusCode == 403 && (message.contains("Google Drive API has not been used") || message.contains("disabled") || "PERMISSION_DENIED".equals(status))) {
                    return "خدمة Google Drive API غير مفعلة في مشروع Google Cloud (رقم المشروع: 383221348854). يرجى تفعيلها من Google Cloud Console.";
                }
                if (statusCode == 401) {
                    return "انتهت صلاحية جلسة تسجيل الدخول بحساب Google. يرجى إعادة ربط الحساب.";
                }
                if (!message.isEmpty()) {
                    return "خطأ Google Drive (" + statusCode + "): " + message;
                }
            }
        } catch (Exception ignored) {
        }
        return "فشل الرفع إلى Google Drive (رمز الخطأ: " + statusCode + ")";
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
