package com.octo.drogri.models;

import java.util.Locale;

/**
 * Model representing a deleted product archived in the local backup history.
 */
public class DeletedProduct {
    private long id;
    private long originalProductId;
    private String name;
    private double purchasePrice;
    private double sellingPrice;
    private String color;
    private String size;
    private String imagePath;
    private String categoryName;
    private String measurementsJson;
    private String imagesJson;
    private String deletedAt;

    public DeletedProduct() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOriginalProductId() {
        return originalProductId;
    }

    public void setOriginalProductId(long originalProductId) {
        this.originalProductId = originalProductId;
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
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getMeasurementsJson() {
        return measurementsJson;
    }

    public void setMeasurementsJson(String measurementsJson) {
        this.measurementsJson = measurementsJson;
    }

    public String getImagesJson() {
        return imagesJson;
    }

    public void setImagesJson(String imagesJson) {
        this.imagesJson = imagesJson;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getFormattedSellingPrice() {
        return String.format(Locale.US, "%.2f د.م", sellingPrice);
    }
}
