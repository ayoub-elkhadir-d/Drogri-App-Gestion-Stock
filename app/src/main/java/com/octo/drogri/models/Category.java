package com.octo.drogri.models;

import androidx.annotation.NonNull;

/**
 * Model class representing a product category.
 */
public class Category {
    private long id;
    private String name;
    private int productCount; // Number of products currently assigned to this category

    public Category() {
    }

    public Category(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Category(long id, String name, int productCount) {
        this.id = id;
        this.name = name;
        this.productCount = productCount;
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

    public int getProductCount() {
        return productCount;
    }

    public void setProductCount(int productCount) {
        this.productCount = productCount;
    }

    @NonNull
    @Override
    public String toString() {
        // Returned when using ArrayAdapter in Spinners
        return name != null ? name : "";
    }
}
