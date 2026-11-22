package com.octo.drogri;

import com.octo.drogri.models.Category;
import com.octo.drogri.models.Product;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Product model with Moroccan Dirham currency and measurement formatting.
 */
public class ProductModelTest {

    @Test
    public void testProductProfitAndMoroccanDirhamFormatting() {
        Product product = new Product();
        product.setPurchasePrice(100.0);
        product.setSellingPrice(150.0);

        assertEquals(50.0, product.getProfit(), 0.001);
        assertEquals(50.0, product.getProfitPercentage(), 0.001);
        assertEquals("100.00 د.م", product.getFormattedPurchasePrice());
        assertEquals("150.00 د.م", product.getFormattedSellingPrice());
        assertEquals("+50.00 د.م (50.0%)", product.getFormattedProfit());
    }

    @Test
    public void testProductMeasurementsFormatting() {
        Product product = new Product();
        product.setLengthValue(20.0);
        product.setLengthUnit("سم");
        product.setWidthValue(15.5);
        product.setWidthUnit("سم");
        product.setWeightValue(250.0);
        product.setWeightUnit("غ");
        product.setVolumeValue(1.5);
        product.setVolumeUnit("لتر");

        assertTrue(product.hasLength());
        assertEquals("20 سم", product.getFormattedLength());

        assertTrue(product.hasWidth());
        assertEquals("15.50 سم", product.getFormattedWidth());

        assertTrue(product.hasWeight());
        assertEquals("250 غ", product.getFormattedWeight());

        assertTrue(product.hasVolume());
        assertEquals("1.50 لتر", product.getFormattedVolume());
    }

    @Test
    public void testCustomCurrencySymbolFormatting() {
        Product product = new Product();
        product.setPurchasePrice(200.0);
        product.setSellingPrice(300.0);

        assertEquals("200.00 MAD", product.getFormattedPurchasePrice("MAD"));
        assertEquals("300.00 MAD", product.getFormattedSellingPrice("MAD"));
        assertEquals("+100.00 MAD (50.0%)", product.getFormattedProfit("MAD"));
    }

    @Test
    public void testCategoryModel() {
        Category category = new Category(1, "ملابس وأزياء", 8);
        assertEquals(1, category.getId());
        assertEquals("ملابس وأزياء", category.getName());
        assertEquals(8, category.getProductCount());
        assertEquals("ملابس وأزياء", category.toString());
    }

    @Test
    public void testSingleDriveBackupFilenameConstant() {
        assertEquals("MyStore_Backup.zip", com.octo.drogri.utils.GoogleDriveManager.CLOUD_BACKUP_FILENAME);
    }

    @Test
    public void testImageProcessingTargets() {
        assertEquals(1200, com.octo.drogri.utils.ImageUtils.MAX_DIMENSION);
        assertEquals(250 * 1024, com.octo.drogri.utils.ImageUtils.TARGET_MAX_BYTES);
    }

    @Test
    public void testProductMultipleImagesAssociation() {
        Product product = new Product();
        product.setId(42);
        product.setName("حذاء رياضي");

        java.util.List<com.octo.drogri.models.ProductImage> images = new java.util.ArrayList<>();
        images.add(new com.octo.drogri.models.ProductImage(1, 42, "/path/img1.webp", true));
        images.add(new com.octo.drogri.models.ProductImage(2, 42, "/path/img2.webp", false));
        images.add(new com.octo.drogri.models.ProductImage(3, 42, "/path/img3.webp", false));
        product.setImages(images);
        product.setImagePath("/path/img1.webp");

        assertEquals(3, product.getImages().size());
        assertEquals("/path/img1.webp", product.getPrimaryImagePath());
        assertTrue(product.getImages().get(0).isPrimary());
    }

    @Test
    public void testThemeManager10ThemesDefinition() {
        java.util.List<com.octo.drogri.utils.ThemeManager.ThemeItem> themes = com.octo.drogri.utils.ThemeManager.getThemes();
        assertEquals(10, themes.size());

        for (int i = 0; i < 10; i++) {
            com.octo.drogri.utils.ThemeManager.ThemeItem item = themes.get(i);
            assertEquals(i, item.id);
            assertTrue(item.primaryHex.startsWith("#"));
            assertTrue(item.secondaryHex.startsWith("#"));
            assertTrue(item.backgroundHex.startsWith("#"));
            assertTrue(item.styleResId != 0);
            assertTrue(item.nameResId != 0);
        }

        // Check Ocean Blue is default theme at index 0
        assertEquals(R.style.Theme_Drogri_OceanBlue, com.octo.drogri.utils.ThemeManager.getTheme(0).styleResId);
    }
}
