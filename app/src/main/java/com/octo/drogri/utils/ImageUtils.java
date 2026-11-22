package com.octo.drogri.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Professional Image Processing Utility:
 * - High-resolution scaling up to 1200 x 1200 maintaining aspect ratio.
 * - On-device ML Kit Subject Segmentation to isolate product on pure white (#FFFFFF) background.
 * - Automatic padding and centering of the segmented product silhouette.
 * - Target 100-250 KB compression via AVIF (API 31+) and WebP (API 24+) without visible quality loss.
 * - Recovery preservation of original unprocessed image.
 */
public class ImageUtils {

    public static final String TAG = "ImageUtils";
    public static final int MAX_DIMENSION = 1200; // Maximum dimension per specification
    public static final long TARGET_MAX_BYTES = 250 * 1024; // 250 KB max target
    public static final long TARGET_MIN_BYTES = 100 * 1024; // 100 KB target

    public static class ProcessedImageResult {
        public boolean success;
        public boolean backgroundRemoved;
        public String processedPath;
        public String originalPath;
        public String statusMessage;
        public long fileSize;
        public int width;
        public int height;

        public ProcessedImageResult(boolean success) {
            this.success = success;
        }
    }

    /**
     * Complete professional image processing workflow:
     * 1. Decodes and rotates original image from Uri.
     * 2. Preserves original copy locally as recoverable source.
     * 3. Executes on-device ML Kit Subject Segmentation to extract product silhouette.
     * 4. Places product on pure white background (#FFFFFF) with balanced padding.
     * 5. If segmentation is unavailable or fails, preserves complete original image cleanly without distortion.
     * 6. Compresses using AVIF (Android 12+) or WebP targeting 100-250 KB.
     */
    public static ProcessedImageResult processAndSaveProductImage(Context context, Uri sourceUri, boolean attemptBackgroundRemoval) {
        if (sourceUri == null) {
            ProcessedImageResult res = new ProcessedImageResult(false);
            res.statusMessage = "مسار الصورة غير صالح";
            return res;
        }

        Bitmap originalBitmap = null;
        Bitmap processedBitmap = null;

        try {
            // 1. Decode bounds to determine sampling size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                if (is == null) {
                    ProcessedImageResult res = new ProcessedImageResult(false);
                    res.statusMessage = "تعذر قراءة ملف الصورة";
                    return res;
                }
                BitmapFactory.decodeStream(is, null, options);
            }

            int origWidth = options.outWidth;
            int origHeight = options.outHeight;
            if (origWidth <= 0 || origHeight <= 0) {
                ProcessedImageResult res = new ProcessedImageResult(false);
                res.statusMessage = "أبعاد الصورة غير صالحة";
                return res;
            }

            // Downsample only if original image is extremely large (e.g. > 3600px)
            int sampleSize = 1;
            while ((origWidth / sampleSize) > (MAX_DIMENSION * 3) || (origHeight / sampleSize) > (MAX_DIMENSION * 3)) {
                sampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                if (is == null) {
                    ProcessedImageResult res = new ProcessedImageResult(false);
                    res.statusMessage = "تعذر قراءة بيانات الصورة";
                    return res;
                }
                originalBitmap = BitmapFactory.decodeStream(is, null, options);
            }

            if (originalBitmap == null) {
                ProcessedImageResult res = new ProcessedImageResult(false);
                res.statusMessage = "فشل فك تشفير الصورة";
                return res;
            }

            // 2. Correct EXIF camera orientation
            int rotationAngle = getOrientationAngle(context, sourceUri);
            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                Bitmap rotated = Bitmap.createBitmap(
                        originalBitmap, 0, 0,
                        originalBitmap.getWidth(), originalBitmap.getHeight(),
                        matrix, true
                );
                if (rotated != originalBitmap) {
                    originalBitmap.recycle();
                    originalBitmap = rotated;
                }
            }

            // Scale to max 1200x1200 preserving aspect ratio
            int curW = originalBitmap.getWidth();
            int curH = originalBitmap.getHeight();
            float scale = Math.min((float) MAX_DIMENSION / curW, (float) MAX_DIMENSION / curH);
            Bitmap workingBitmap;
            if (scale < 1.0f) {
                int targetW = Math.max(1, Math.round(curW * scale));
                int targetH = Math.max(1, Math.round(curH * scale));
                workingBitmap = Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true);
            } else {
                workingBitmap = originalBitmap;
            }

            File imagesDir = new File(context.getFilesDir(), "product_images");
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            String baseId = UUID.randomUUID().toString();

            // Save original image as recoverable backup
            String origFilename = "prod_" + baseId + "_orig.webp";
            File origFile = new File(imagesDir, origFilename);
            try (FileOutputStream fos = new FileOutputStream(origFile)) {
                workingBitmap.compress(Bitmap.CompressFormat.WEBP, 85, fos);
            }

            boolean bgRemoved = false;
            String statusMsg;

            if (attemptBackgroundRemoval) {
                try {
                    Log.i(TAG, "Starting on-device ML Kit Subject Segmentation...");
                    SubjectSegmenterOptions segmenterOptions = new SubjectSegmenterOptions.Builder()
                            .enableForegroundBitmap()
                            .enableForegroundConfidenceMask()
                            .build();

                    SubjectSegmenter segmenter = SubjectSegmentation.getClient(segmenterOptions);
                    InputImage inputImage = InputImage.fromBitmap(workingBitmap, 0);

                    // Execute subject segmentation synchronously on current thread (must be called off main thread)
                    SubjectSegmentationResult segResult = Tasks.await(segmenter.process(inputImage), 15, TimeUnit.SECONDS);

                    Bitmap foreground = segResult != null ? segResult.getForegroundBitmap() : null;
                    if (foreground != null) {
                        Log.i(TAG, "ML Kit Subject Segmentation succeeded. Creating pure white background canvas.");
                        processedBitmap = placeOnWhiteBackgroundWithPadding(foreground);
                        bgRemoved = true;
                        statusMsg = "تم عزل خلفية المنتج ووضعه على خلفية بيضاء نقية بنجاح";
                    } else {
                        Log.w(TAG, "Subject segmentation returned null foreground. Keeping original image.");
                        processedBitmap = workingBitmap;
                        statusMsg = "لم يتمكن نموذج العزل من تحديد حدود المنتج. تم حفظ الصورة الأصلية بجودة عالية.";
                    }
                } catch (Exception segEx) {
                    Log.w(TAG, "On-device ML Kit Subject Segmentation failed or model downloading: " + segEx.getMessage());
                    processedBitmap = workingBitmap;
                    statusMsg = "تعذر عزل الخلفية تلقائياً. تم حفظ الصورة الأصلية بجودة كاملة.";
                }
            } else {
                processedBitmap = workingBitmap;
                statusMsg = "تم حفظ الصورة بنجاح";
            }

            if (processedBitmap == null) {
                processedBitmap = workingBitmap;
            }

            // 5. Select optimal compression format: AVIF (Android 12+) -> WEBP
            Bitmap.CompressFormat format = null;
            String extension = ".webp";

            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    format = Bitmap.CompressFormat.valueOf("AVIF");
                    extension = ".avif";
                } catch (Exception ignored) {
                    format = null;
                }
            }

            if (format == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    format = Bitmap.CompressFormat.WEBP_LOSSY;
                } else {
                    format = Bitmap.CompressFormat.WEBP;
                }
                extension = ".webp";
            }

            // Target 100-250 KB compression without sacrificing visible quality
            byte[] compressedBytes = compressTargetingSize(processedBitmap, format);
            if (compressedBytes == null || compressedBytes.length == 0) {
                // Fallback to WebP
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    format = Bitmap.CompressFormat.WEBP_LOSSY;
                } else {
                    format = Bitmap.CompressFormat.WEBP;
                }
                extension = ".webp";
                compressedBytes = compressTargetingSize(processedBitmap, format);
            }

            String finalFilename = "prod_" + baseId + extension;
            File outputFile = new File(imagesDir, finalFilename);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(compressedBytes);
                fos.flush();
            }

            ProcessedImageResult result = new ProcessedImageResult(true);
            result.processedPath = outputFile.getAbsolutePath();
            result.originalPath = origFile.getAbsolutePath();
            result.backgroundRemoved = bgRemoved;
            result.statusMessage = statusMsg;
            result.fileSize = outputFile.length();
            result.width = processedBitmap.getWidth();
            result.height = processedBitmap.getHeight();

            Log.i(TAG, "Product image processed: " + result.processedPath
                    + " (" + (result.fileSize / 1024) + " KB, " + result.width + "x" + result.height
                    + ", bgRemoved=" + bgRemoved + ")");

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error in processAndSaveProductImage", e);
            ProcessedImageResult res = new ProcessedImageResult(false);
            res.statusMessage = "خطأ أثناء معالجة الصورة: " + e.getMessage();
            return res;
        } finally {
            if (originalBitmap != null && !originalBitmap.isRecycled()) {
                originalBitmap.recycle();
            }
            if (processedBitmap != null && !processedBitmap.isRecycled() && processedBitmap != originalBitmap) {
                processedBitmap.recycle();
            }
        }
    }

    /**
     * Composites foreground product on a pure white background (#FFFFFF) with balanced padding.
     * Preserves the full silhouette, handles, cables, edges, and corners without clipping.
     */
    private static Bitmap placeOnWhiteBackgroundWithPadding(Bitmap foreground) {
        int width = foreground.getWidth();
        int height = foreground.getHeight();

        // 1. Scan for non-transparent bounding box
        int minX = width;
        int maxX = -1;
        int minY = height;
        int maxY = -1;

        int[] rowPixels = new int[width];
        for (int y = 0; y < height; y++) {
            foreground.getPixels(rowPixels, 0, width, 0, y, width, 1);
            for (int x = 0; x < width; x++) {
                int alpha = Color.alpha(rowPixels[x]);
                if (alpha > 20) { // Threshold for visible product pixel
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // If no product pixels detected, fall back to entire bitmap
        if (maxX <= minX || maxY <= minY) {
            minX = 0;
            maxX = width - 1;
            minY = 0;
            maxY = height - 1;
        }

        int objW = (maxX - minX) + 1;
        int objH = (maxY - minY) + 1;

        // 2. Add 6% white padding around product to ensure no details are cramped or clipped
        int padX = Math.max(16, (int) (objW * 0.06f));
        int padY = Math.max(16, (int) (objH * 0.06f));

        int targetCanvasW = objW + (padX * 2);
        int targetCanvasH = objH + (padY * 2);

        // Make canvas clean and proportional
        Bitmap finalBitmap = Bitmap.createBitmap(targetCanvasW, targetCanvasH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);

        // Fill background with pure white #FFFFFF
        canvas.drawColor(Color.WHITE);

        // Draw cropped foreground onto white canvas with smoothing
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Rect srcRect = new Rect(minX, minY, maxX + 1, maxY + 1);
        Rect dstRect = new Rect(padX, padY, padX + objW, padY + objH);

        canvas.drawBitmap(foreground, srcRect, dstRect, paint);

        return finalBitmap;
    }

    /**
     * Compresses bitmap targeting 100 KB - 250 KB without aggressive loss.
     */
    private static byte[] compressTargetingSize(Bitmap bitmap, Bitmap.CompressFormat format) {
        int quality = 90;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        boolean success = bitmap.compress(format, quality, stream);
        if (!success) {
            return null;
        }

        byte[] bytes = stream.toByteArray();

        // If file is larger than 250 KB, reduce quality gently down to 70 (never degrade below 70)
        while (bytes.length > TARGET_MAX_BYTES && quality > 70) {
            quality -= 5;
            stream.reset();
            bitmap.compress(format, quality, stream);
            bytes = stream.toByteArray();
        }

        return bytes;
    }

    /**
     * Backward compatible wrapper for legacy calls.
     */
    public static String optimizeAndSaveImage(Context context, Uri sourceUri) {
        ProcessedImageResult res = processAndSaveProductImage(context, sourceUri, true);
        return res.success ? res.processedPath : null;
    }

    /**
     * Reads image EXIF metadata to determine camera rotation.
     */
    private static int getOrientationAngle(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return 0;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Safely loads a bitmap from a local file path.
     */
    public static Bitmap loadBitmap(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * Deletes a local image file if it exists.
     */
    public static void deleteImageFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete image file: " + filePath, e);
        }
    }
}
