package com.octo.drogri.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model class representing a product in inventory with Moroccan Dirham pricing,
 * multiple product images, and optional physical measurements.
 */
public class Product {
    private long id;
    private String name;
    private double purchasePrice;
    private double sellingPrice;
    private String color;
    private String size;
    private String imagePath; // Primary image path
    private long categoryId;
    private String categoryName;

    // Multiple images list
    private List<ProductImage> images = new ArrayList<>();

    // Optional measurement fields
    private double lengthValue;
    private String lengthUnit;

    private double widthValue;
    private String widthUnit;

    private double heightValue;
    private String heightUnit;

    private double weightValue;
    private String weightUnit;

    private double volumeValue;
    private String volumeUnit;

    public Product() {
    }

    public Product(long id, String name, double purchasePrice, double sellingPrice,
                   String color, String size, String imagePath, long categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.color = color;
        this.size = size;
        this.imagePath = imagePath;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getImagePath() {
        return getPrimaryImagePath();
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images != null ? images : new ArrayList<>();
        if (!this.images.isEmpty() && (this.imagePath == null || this.imagePath.isEmpty())) {
            this.imagePath = getPrimaryImagePath();
        }
    }

    public void addImage(ProductImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
        if (image.isPrimary() || this.imagePath == null) {
            this.imagePath = image.getImagePath();
        }
    }

    public String getPrimaryImagePath() {
        if (images != null && !images.isEmpty()) {
            for (ProductImage img : images) {
                if (img.isPrimary()) {
                    return img.getImagePath();
                }
            }
            return images.get(0).getImagePath();
        }
        return imagePath;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getLengthValue() {
        return lengthValue;
    }

    public void setLengthValue(double lengthValue) {
        this.lengthValue = lengthValue;
    }

    public String getLengthUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(String lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    public double getWidthValue() {
        return widthValue;
    }

    public void setWidthValue(double widthValue) {
        this.widthValue = widthValue;
    }

    public String getWidthUnit() {
        return widthUnit;
    }

    public void setWidthUnit(String widthUnit) {
        this.widthUnit = widthUnit;
    }

    public double getHeightValue() {
        return heightValue;
    }

    public void setHeightValue(double heightValue) {
        this.heightValue = heightValue;
    }

    public String getHeightUnit() {
        return heightUnit;
    }

    public void setHeightUnit(String heightUnit) {
        this.heightUnit = heightUnit;
    }

    public double getWeightValue() {
        return weightValue;
    }

    public void setWeightValue(double weightValue) {
        this.weightValue = weightValue;
    }

    public String getWeightUnit() {
        return weightUnit;
    }

    public void setWeightUnit(String weightUnit) {
        this.weightUnit = weightUnit;
    }

    public double getVolumeValue() {
        return volumeValue;
    }

    public void setVolumeValue(double volumeValue) {
        this.volumeValue = volumeValue;
    }

    public String getVolumeUnit() {
        return volumeUnit;
    }

    public void setVolumeUnit(String volumeUnit) {
        this.volumeUnit = volumeUnit;
    }

    /**
     * Calculates net profit in Moroccan Dirham.
     */
    public double getProfit() {
        return sellingPrice - purchasePrice;
    }

    /**
     * Calculates profit margin percentage.
     */
    public double getProfitPercentage() {
        if (purchasePrice <= 0) return 0;
        return ((sellingPrice - purchasePrice) / purchasePrice) * 100.0;
    }

    /**
     * Formatted Moroccan Dirham currency strings (e.g. "150.00 د.م").
     */
    public String getFormattedPurchasePrice() {
        return getFormattedPurchasePrice("د.م");
    }

    public String getFormattedPurchasePrice(String currencySymbol) {
        String sym = (currencySymbol != null && !currencySymbol.trim().isEmpty()) ? currencySymbol.trim() : "د.م";
        return String.format(Locale.US, "%.2f %s", purchasePrice, sym);
    }

    public String getFormattedSellingPrice() {
        return getFormattedSellingPrice("د.م");
    }

    public String getFormattedSellingPrice(String currencySymbol) {
        String sym = (currencySymbol != null && !currencySymbol.trim().isEmpty()) ? currencySymbol.trim() : "د.م";
        return String.format(Locale.US, "%.2f %s", sellingPrice, sym);
    }

    public String getFormattedProfit() {
        return getFormattedProfit("د.م");
    }

    public String getFormattedProfit(String currencySymbol) {
        String sym = (currencySymbol != null && !currencySymbol.trim().isEmpty()) ? currencySymbol.trim() : "د.م";
        return String.format(Locale.US, "+%.2f %s (%.1f%%)", getProfit(), sym, getProfitPercentage());
    }

    // Measurement format helpers
    public boolean hasLength() {
        return lengthValue > 0;
    }

    public String getFormattedLength() {
        if (lengthValue <= 0) return "";
        String unit = lengthUnit != null ? lengthUnit : "سم";
        return formatMeasurementNumber(lengthValue) + " " + unit;
    }

    public boolean hasWidth() {
        return widthValue > 0;
    }

    public String getFormattedWidth() {
        if (widthValue <= 0) return "";
        String unit = widthUnit != null ? widthUnit : "سم";
        return formatMeasurementNumber(widthValue) + " " + unit;
    }

    public boolean hasHeight() {
        return heightValue > 0;
    }

    public String getFormattedHeight() {
        if (heightValue <= 0) return "";
        String unit = heightUnit != null ? heightUnit : "سم";
        return formatMeasurementNumber(heightValue) + " " + unit;
    }

    public boolean hasWeight() {
        return weightValue > 0;
    }

    public String getFormattedWeight() {
        if (weightValue <= 0) return "";
        String unit = weightUnit != null ? weightUnit : "غ";
        return formatMeasurementNumber(weightValue) + " " + unit;
    }

    public boolean hasVolume() {
        return volumeValue > 0;
    }

    public String getFormattedVolume() {
        if (volumeValue <= 0) return "";
        String unit = volumeUnit != null ? volumeUnit : "مل";
        return formatMeasurementNumber(volumeValue) + " " + unit;
    }

    private String formatMeasurementNumber(double val) {
        if (val == (long) val) {
            return String.format(Locale.US, "%d", (long) val);
        } else {
            return String.format(Locale.US, "%.2f", val);
        }
    }
}
