package com.octo.drogri.models;

/**
 * Model representing an image associated with a product.
 */
public class ProductImage {
    private long id;
    private long productId;
    private String imagePath;
    private boolean isPrimary;
    private String createdAt;

    public ProductImage() {
    }

    public ProductImage(long id, long productId, String imagePath, boolean isPrimary) {
        this.id = id;
        this.productId = productId;
        this.imagePath = imagePath;
        this.isPrimary = isPrimary;
    }

    public ProductImage(long productId, String imagePath, boolean isPrimary) {
        this.productId = productId;
        this.imagePath = imagePath;
        this.isPrimary = isPrimary;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
