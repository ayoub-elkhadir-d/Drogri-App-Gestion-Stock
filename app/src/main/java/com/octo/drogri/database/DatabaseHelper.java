package com.octo.drogri.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.octo.drogri.models.BackupRecord;
import com.octo.drogri.models.Category;
import com.octo.drogri.models.DeletedProduct;
import com.octo.drogri.models.Product;
import com.octo.drogri.models.ProductImage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Database Helper for managing Categories, Products, Product Images,
 * Backup History, Deleted Products, and Change Log.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mystore.db";
    private static final int DATABASE_VERSION = 3;

    // Table: Categories
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";

    // Table: Products
    public static final String TABLE_PRODUCTS = "products";
    public static final String COL_PROD_ID = "id";
    public static final String COL_PROD_NAME = "name";
    public static final String COL_PROD_PURCHASE_PRICE = "purchase_price";
    public static final String COL_PROD_SELLING_PRICE = "selling_price";
    public static final String COL_PROD_COLOR = "color";
    public static final String COL_PROD_SIZE = "size";
    public static final String COL_PROD_IMAGE_PATH = "image_path";
    public static final String COL_PROD_CATEGORY_ID = "category_id";
    public static final String COL_PROD_LENGTH_VAL = "length_value";
    public static final String COL_PROD_LENGTH_UNIT = "length_unit";
    public static final String COL_PROD_WIDTH_VAL = "width_value";
    public static final String COL_PROD_WIDTH_UNIT = "width_unit";
    public static final String COL_PROD_HEIGHT_VAL = "height_value";
    public static final String COL_PROD_HEIGHT_UNIT = "height_unit";
    public static final String COL_PROD_WEIGHT_VAL = "weight_value";
    public static final String COL_PROD_WEIGHT_UNIT = "weight_unit";
    public static final String COL_PROD_VOLUME_VAL = "volume_value";
    public static final String COL_PROD_VOLUME_UNIT = "volume_unit";
    public static final String COL_PROD_CREATED_AT = "created_at";

    // Table: Product Images
    public static final String TABLE_PRODUCT_IMAGES = "product_images";
    public static final String COL_IMG_ID = "id";
    public static final String COL_IMG_PROD_ID = "product_id";
    public static final String COL_IMG_PATH = "image_path";
    public static final String COL_IMG_IS_PRIMARY = "is_primary";
    public static final String COL_IMG_CREATED_AT = "created_at";

    // Table: Deleted Products Archive
    public static final String TABLE_DELETED_PRODUCTS = "deleted_products";
    public static final String COL_DEL_ID = "id";
    public static final String COL_DEL_ORIGINAL_ID = "original_product_id";
    public static final String COL_DEL_NAME = "name";
    public static final String COL_DEL_PURCHASE_PRICE = "purchase_price";
    public static final String COL_DEL_SELLING_PRICE = "selling_price";
    public static final String COL_DEL_COLOR = "color";
    public static final String COL_DEL_SIZE = "size";
    public static final String COL_DEL_IMAGE_PATH = "image_path";
    public static final String COL_DEL_CATEGORY_NAME = "category_name";
    public static final String COL_DEL_MEASUREMENTS_JSON = "measurements_json";
    public static final String COL_DEL_IMAGES_JSON = "images_json";
    public static final String COL_DEL_DELETED_AT = "deleted_at";

    // Table: Backup History
    public static final String TABLE_BACKUP_HISTORY = "backup_history";
    public static final String COL_BK_ID = "id";
    public static final String COL_BK_NAME = "backup_name";
    public static final String COL_BK_TYPE = "backup_type"; // "LOCAL" or "GOOGLE_DRIVE"
    public static final String COL_BK_FILE_PATH = "file_path";
    public static final String COL_BK_FILE_SIZE = "file_size";
    public static final String COL_BK_PROD_COUNT = "product_count";
    public static final String COL_BK_CAT_COUNT = "category_count";
    public static final String COL_BK_CREATED_AT = "created_at";
    public static final String COL_BK_DRIVE_FILE_ID = "drive_file_id";
    public static final String COL_BK_IS_CLOUD = "is_cloud";

    // Table: Change Log
    public static final String TABLE_CHANGE_LOG = "change_log";
    public static final String COL_LOG_ID = "id";
    public static final String COL_LOG_ACTION = "action_type";
    public static final String COL_LOG_TABLE = "table_name";
    public static final String COL_LOG_RECORD_ID = "record_id";
    public static final String COL_LOG_DESC = "description";
    public static final String COL_LOG_TIMESTAMP = "timestamp";
    public static final String COL_LOG_IS_SYNCED = "is_synced";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Categories
        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " ("
                + COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CAT_NAME + " TEXT NOT NULL UNIQUE"
                + ");");

        // 2. Products
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " ("
                + COL_PROD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PROD_NAME + " TEXT NOT NULL, "
                + COL_PROD_PURCHASE_PRICE + " REAL NOT NULL, "
                + COL_PROD_SELLING_PRICE + " REAL NOT NULL, "
                + COL_PROD_COLOR + " TEXT, "
                + COL_PROD_SIZE + " TEXT, "
                + COL_PROD_IMAGE_PATH + " TEXT, "
                + COL_PROD_CATEGORY_ID + " INTEGER NOT NULL, "
                + COL_PROD_LENGTH_VAL + " REAL, "
                + COL_PROD_LENGTH_UNIT + " TEXT, "
                + COL_PROD_WIDTH_VAL + " REAL, "
                + COL_PROD_WIDTH_UNIT + " TEXT, "
                + COL_PROD_HEIGHT_VAL + " REAL, "
                + COL_PROD_HEIGHT_UNIT + " TEXT, "
                + COL_PROD_WEIGHT_VAL + " REAL, "
                + COL_PROD_WEIGHT_UNIT + " TEXT, "
                + COL_PROD_VOLUME_VAL + " REAL, "
                + COL_PROD_VOLUME_UNIT + " TEXT, "
                + COL_PROD_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (" + COL_PROD_CATEGORY_ID + ") REFERENCES "
                + TABLE_CATEGORIES + "(" + COL_CAT_ID + ") ON DELETE RESTRICT"
                + ");");

        // 3. Product Images
        db.execSQL("CREATE TABLE " + TABLE_PRODUCT_IMAGES + " ("
                + COL_IMG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_IMG_PROD_ID + " INTEGER NOT NULL, "
                + COL_IMG_PATH + " TEXT NOT NULL, "
                + COL_IMG_IS_PRIMARY + " INTEGER DEFAULT 0, "
                + COL_IMG_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (" + COL_IMG_PROD_ID + ") REFERENCES "
                + TABLE_PRODUCTS + "(" + COL_PROD_ID + ") ON DELETE CASCADE"
                + ");");

        // 4. Deleted Products Archive
        db.execSQL("CREATE TABLE " + TABLE_DELETED_PRODUCTS + " ("
                + COL_DEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_DEL_ORIGINAL_ID + " INTEGER, "
                + COL_DEL_NAME + " TEXT NOT NULL, "
                + COL_DEL_PURCHASE_PRICE + " REAL, "
                + COL_DEL_SELLING_PRICE + " REAL, "
                + COL_DEL_COLOR + " TEXT, "
                + COL_DEL_SIZE + " TEXT, "
                + COL_DEL_IMAGE_PATH + " TEXT, "
                + COL_DEL_CATEGORY_NAME + " TEXT, "
                + COL_DEL_MEASUREMENTS_JSON + " TEXT, "
                + COL_DEL_IMAGES_JSON + " TEXT, "
                + COL_DEL_DELETED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");");

        // 5. Backup History
        db.execSQL("CREATE TABLE " + TABLE_BACKUP_HISTORY + " ("
                + COL_BK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_BK_NAME + " TEXT, "
                + COL_BK_TYPE + " TEXT, "
                + COL_BK_FILE_PATH + " TEXT, "
                + COL_BK_FILE_SIZE + " INTEGER, "
                + COL_BK_PROD_COUNT + " INTEGER, "
                + COL_BK_CAT_COUNT + " INTEGER, "
                + COL_BK_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + COL_BK_DRIVE_FILE_ID + " TEXT, "
                + COL_BK_IS_CLOUD + " INTEGER DEFAULT 0"
                + ");");

        // 6. Change Log
        db.execSQL("CREATE TABLE " + TABLE_CHANGE_LOG + " ("
                + COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_LOG_ACTION + " TEXT, "
                + COL_LOG_TABLE + " TEXT, "
                + COL_LOG_RECORD_ID + " INTEGER, "
                + COL_LOG_DESC + " TEXT, "
                + COL_LOG_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + COL_LOG_IS_SYNCED + " INTEGER DEFAULT 0"
                + ");");

        seedDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_LENGTH_VAL + " REAL;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_LENGTH_UNIT + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_WIDTH_VAL + " REAL;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_WIDTH_UNIT + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_HEIGHT_VAL + " REAL;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_HEIGHT_UNIT + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_WEIGHT_VAL + " REAL;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_WEIGHT_UNIT + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_VOLUME_VAL + " REAL;");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COL_PROD_VOLUME_UNIT + " TEXT;");
        }
        if (oldVersion < 3) {
            // Create product_images, deleted_products, backup_history, change_log if missing
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCT_IMAGES + " ("
                    + COL_IMG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_IMG_PROD_ID + " INTEGER NOT NULL, "
                    + COL_IMG_PATH + " TEXT NOT NULL, "
                    + COL_IMG_IS_PRIMARY + " INTEGER DEFAULT 0, "
                    + COL_IMG_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DELETED_PRODUCTS + " ("
                    + COL_DEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_DEL_ORIGINAL_ID + " INTEGER, "
                    + COL_DEL_NAME + " TEXT NOT NULL, "
                    + COL_DEL_PURCHASE_PRICE + " REAL, "
                    + COL_DEL_SELLING_PRICE + " REAL, "
                    + COL_DEL_COLOR + " TEXT, "
                    + COL_DEL_SIZE + " TEXT, "
                    + COL_DEL_IMAGE_PATH + " TEXT, "
                    + COL_DEL_CATEGORY_NAME + " TEXT, "
                    + COL_DEL_MEASUREMENTS_JSON + " TEXT, "
                    + COL_DEL_IMAGES_JSON + " TEXT, "
                    + COL_DEL_DELETED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BACKUP_HISTORY + " ("
                    + COL_BK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_BK_NAME + " TEXT, "
                    + COL_BK_TYPE + " TEXT, "
                    + COL_BK_FILE_PATH + " TEXT, "
                    + COL_BK_FILE_SIZE + " INTEGER, "
                    + COL_BK_PROD_COUNT + " INTEGER, "
                    + COL_BK_CAT_COUNT + " INTEGER, "
                    + COL_BK_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + COL_BK_DRIVE_FILE_ID + " TEXT, "
                    + COL_BK_IS_CLOUD + " INTEGER DEFAULT 0"
                    + ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CHANGE_LOG + " ("
                    + COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_LOG_ACTION + " TEXT, "
                    + COL_LOG_TABLE + " TEXT, "
                    + COL_LOG_RECORD_ID + " INTEGER, "
                    + COL_LOG_DESC + " TEXT, "
                    + COL_LOG_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + COL_LOG_IS_SYNCED + " INTEGER DEFAULT 0"
                    + ");");
        }
    }

    private void seedDefaultCategories(SQLiteDatabase db) {
        String[] defaultCategories = {
                "إلكترونيات",
                "ملابس وأزياء",
                "إكسسوارات",
                "مواد غذائية",
                "مستحضرات تجميل",
                "منتجات عامة"
        };
        for (String catName : defaultCategories) {
            ContentValues cv = new ContentValues();
            cv.put(COL_CAT_NAME, catName);
            db.insert(TABLE_CATEGORIES, null, cv);
        }
    }

    // ==========================================
    // CATEGORY OPERATIONS
    // ==========================================

    public long insertCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            return -1;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name.trim());
        long id = db.insert(TABLE_CATEGORIES, null, values);
        if (id != -1) {
            logChange("INSERT_CATEGORY", TABLE_CATEGORIES, id, "إضافة قسم: " + name);
        }
        return id;
    }

    public boolean categoryExists(String name, long excludeId) {
        if (name == null) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_CATEGORIES
                + " WHERE LOWER(" + COL_CAT_NAME + ") = LOWER(?) AND " + COL_CAT_ID + " != ?";
        Cursor cursor = db.rawQuery(query, new String[]{name.trim(), String.valueOf(excludeId)});
        boolean exists = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
            cursor.close();
        }
        return exists;
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_CAT_ID + ", " + COL_CAT_NAME
                + " FROM " + TABLE_CATEGORIES
                + " ORDER BY " + COL_CAT_NAME + " ASC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(COL_CAT_ID);
            int nameIndex = cursor.getColumnIndex(COL_CAT_NAME);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String name = cursor.getString(nameIndex);
                list.add(new Category(id, name));
            }
            cursor.close();
        }
        return list;
    }

    public List<Category> getAllCategoriesWithCount() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT c." + COL_CAT_ID + ", c." + COL_CAT_NAME + ", COUNT(p." + COL_PROD_ID + ") AS prod_count "
                + " FROM " + TABLE_CATEGORIES + " c "
                + " LEFT JOIN " + TABLE_PRODUCTS + " p ON c." + COL_CAT_ID + " = p." + COL_PROD_CATEGORY_ID
                + " GROUP BY c." + COL_CAT_ID + ", c." + COL_CAT_NAME
                + " ORDER BY c." + COL_CAT_NAME + " ASC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(COL_CAT_ID);
            int nameIndex = cursor.getColumnIndex(COL_CAT_NAME);
            int countIndex = cursor.getColumnIndex("prod_count");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String name = cursor.getString(nameIndex);
                int count = cursor.getInt(countIndex);
                list.add(new Category(id, name, count));
            }
            cursor.close();
        }
        return list;
    }

    public int getProductCountByCategory(long categoryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_PRODUCTS + " WHERE " + COL_PROD_CATEGORY_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(categoryId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public boolean updateCategory(long id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, newName.trim());
        int rows = db.update(TABLE_CATEGORIES, values, COL_CAT_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            logChange("UPDATE_CATEGORY", TABLE_CATEGORIES, id, "تعديل قسم: " + newName);
        }
        return rows > 0;
    }

    public boolean deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_CATEGORIES, COL_CAT_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            logChange("DELETE_CATEGORY", TABLE_CATEGORIES, id, "حذف قسم ID: " + id);
        }
        return rows > 0;
    }

    // ==========================================
    // PRODUCT OPERATIONS
    // ==========================================

    public long insertProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        long insertedId = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COL_PROD_NAME, product.getName());
            values.put(COL_PROD_PURCHASE_PRICE, product.getPurchasePrice());
            values.put(COL_PROD_SELLING_PRICE, product.getSellingPrice());
            values.put(COL_PROD_COLOR, product.getColor());
            values.put(COL_PROD_SIZE, product.getSize());
            values.put(COL_PROD_IMAGE_PATH, product.getPrimaryImagePath());
            values.put(COL_PROD_CATEGORY_ID, product.getCategoryId());

            values.put(COL_PROD_LENGTH_VAL, product.getLengthValue());
            values.put(COL_PROD_LENGTH_UNIT, product.getLengthUnit());
            values.put(COL_PROD_WIDTH_VAL, product.getWidthValue());
            values.put(COL_PROD_WIDTH_UNIT, product.getWidthUnit());
            values.put(COL_PROD_HEIGHT_VAL, product.getHeightValue());
            values.put(COL_PROD_HEIGHT_UNIT, product.getHeightUnit());
            values.put(COL_PROD_WEIGHT_VAL, product.getWeightValue());
            values.put(COL_PROD_WEIGHT_UNIT, product.getWeightUnit());
            values.put(COL_PROD_VOLUME_VAL, product.getVolumeValue());
            values.put(COL_PROD_VOLUME_UNIT, product.getVolumeUnit());

            insertedId = db.insert(TABLE_PRODUCTS, null, values);

            // Insert multiple product images
            if (insertedId != -1 && product.getImages() != null) {
                for (ProductImage img : product.getImages()) {
                    ContentValues imgValues = new ContentValues();
                    imgValues.put(COL_IMG_PROD_ID, insertedId);
                    imgValues.put(COL_IMG_PATH, img.getImagePath());
                    imgValues.put(COL_IMG_IS_PRIMARY, img.isPrimary() ? 1 : 0);
                    db.insert(TABLE_PRODUCT_IMAGES, null, imgValues);
                }
            }

            db.setTransactionSuccessful();
            if (insertedId != -1) {
                logChange("INSERT_PRODUCT", TABLE_PRODUCTS, insertedId, "إضافة منتج: " + product.getName());
            }
        } finally {
            db.endTransaction();
        }
        return insertedId;
    }

    public boolean updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues values = new ContentValues();
            values.put(COL_PROD_NAME, product.getName());
            values.put(COL_PROD_PURCHASE_PRICE, product.getPurchasePrice());
            values.put(COL_PROD_SELLING_PRICE, product.getSellingPrice());
            values.put(COL_PROD_COLOR, product.getColor());
            values.put(COL_PROD_SIZE, product.getSize());
            values.put(COL_PROD_IMAGE_PATH, product.getPrimaryImagePath());
            values.put(COL_PROD_CATEGORY_ID, product.getCategoryId());

            values.put(COL_PROD_LENGTH_VAL, product.getLengthValue());
            values.put(COL_PROD_LENGTH_UNIT, product.getLengthUnit());
            values.put(COL_PROD_WIDTH_VAL, product.getWidthValue());
            values.put(COL_PROD_WIDTH_UNIT, product.getWidthUnit());
            values.put(COL_PROD_HEIGHT_VAL, product.getHeightValue());
            values.put(COL_PROD_HEIGHT_UNIT, product.getHeightUnit());
            values.put(COL_PROD_WEIGHT_VAL, product.getWeightValue());
            values.put(COL_PROD_WEIGHT_UNIT, product.getWeightUnit());
            values.put(COL_PROD_VOLUME_VAL, product.getVolumeValue());
            values.put(COL_PROD_VOLUME_UNIT, product.getVolumeUnit());

            int rows = db.update(TABLE_PRODUCTS, values, COL_PROD_ID + " = ?",
                    new String[]{String.valueOf(product.getId())});

            // Update images
            db.delete(TABLE_PRODUCT_IMAGES, COL_IMG_PROD_ID + " = ?", new String[]{String.valueOf(product.getId())});
            if (product.getImages() != null) {
                for (ProductImage img : product.getImages()) {
                    ContentValues imgValues = new ContentValues();
                    imgValues.put(COL_IMG_PROD_ID, product.getId());
                    imgValues.put(COL_IMG_PATH, img.getImagePath());
                    imgValues.put(COL_IMG_IS_PRIMARY, img.isPrimary() ? 1 : 0);
                    db.insert(TABLE_PRODUCT_IMAGES, null, imgValues);
                }
            }

            db.setTransactionSuccessful();
            success = rows > 0;
            if (success) {
                logChange("UPDATE_PRODUCT", TABLE_PRODUCTS, product.getId(), "تعديل منتج: " + product.getName());
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    /**
     * Deletes product after archiving full details in DeletedProducts table.
     */
    public boolean deleteProduct(long id) {
        Product product = getProductById(id);
        if (product != null) {
            // Archive to deleted products table FIRST
            archiveDeletedProduct(product);
        }

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PRODUCT_IMAGES, COL_IMG_PROD_ID + " = ?", new String[]{String.valueOf(id)});
        int rows = db.delete(TABLE_PRODUCTS, COL_PROD_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            logChange("DELETE_PRODUCT", TABLE_PRODUCTS, id, "حذف منتج: " + (product != null ? product.getName() : ""));
        }
        return rows > 0;
    }

    /**
     * Atomically deletes multiple products in a single database transaction,
     * archiving each to DeletedProducts table before removal.
     */
    public int bulkDeleteProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return 0;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int count = 0;
        try {
            for (Long id : productIds) {
                Product product = getProductById(id);
                if (product != null) {
                    archiveDeletedProduct(product);
                }
                db.delete(TABLE_PRODUCT_IMAGES, COL_IMG_PROD_ID + " = ?", new String[]{String.valueOf(id)});
                count += db.delete(TABLE_PRODUCTS, COL_PROD_ID + " = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            if (count > 0) {
                logChange("BULK_DELETE_PRODUCTS", TABLE_PRODUCTS, 0, "حذف " + count + " منتجات محددة");
            }
        } finally {
            db.endTransaction();
        }
        return count;
    }

    public Product getProductById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, c." + COL_CAT_NAME + " AS category_name "
                + " FROM " + TABLE_PRODUCTS + " p "
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON p." + COL_PROD_CATEGORY_ID + " = c." + COL_CAT_ID
                + " WHERE p." + COL_PROD_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});
        Product product = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                product = parseProductFromCursor(cursor);
                product.setImages(getImagesForProduct(id));
            }
            cursor.close();
        }
        return product;
    }

    public List<Product> searchProducts(String queryText, long categoryId) {
        List<Product> products = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.*, c.").append(COL_CAT_NAME).append(" AS category_name ");
        sql.append(" FROM ").append(TABLE_PRODUCTS).append(" p ");
        sql.append(" LEFT JOIN ").append(TABLE_CATEGORIES).append(" c ON p.").append(COL_PROD_CATEGORY_ID).append(" = c.").append(COL_CAT_ID);

        List<String> selectionArgs = new ArrayList<>();
        boolean hasCategory = categoryId > 0;
        boolean hasQuery = queryText != null && !queryText.trim().isEmpty();

        if (hasCategory || hasQuery) {
            sql.append(" WHERE ");
            if (hasCategory) {
                sql.append("p.").append(COL_PROD_CATEGORY_ID).append(" = ?");
                selectionArgs.add(String.valueOf(categoryId));
            }
            if (hasQuery) {
                if (hasCategory) {
                    sql.append(" AND ");
                }
                sql.append("(p.").append(COL_PROD_NAME).append(" LIKE ? OR c.").append(COL_CAT_NAME).append(" LIKE ?)");
                String pattern = "%" + queryText.trim() + "%";
                selectionArgs.add(pattern);
                selectionArgs.add(pattern);
            }
        }

        sql.append(" ORDER BY p.").append(COL_PROD_ID).append(" DESC");

        String[] argsArray = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);
        Cursor cursor = db.rawQuery(sql.toString(), argsArray);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Product p = parseProductFromCursor(cursor);
                products.add(p);
            }
            cursor.close();
        }

        return products;
    }

    public int getTotalProductCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCTS, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getTotalCategoryCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORIES, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private Product parseProductFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_NAME));
        double purchasePrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_PURCHASE_PRICE));
        double sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PROD_SELLING_PRICE));
        String color = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_COLOR));
        String size = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_SIZE));
        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_IMAGE_PATH));
        long catId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROD_CATEGORY_ID));
        String catName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));

        Product product = new Product(id, name, purchasePrice, sellingPrice, color, size, imagePath, catId, catName);

        int lenValIdx = cursor.getColumnIndex(COL_PROD_LENGTH_VAL);
        if (lenValIdx != -1 && !cursor.isNull(lenValIdx)) {
            product.setLengthValue(cursor.getDouble(lenValIdx));
            product.setLengthUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_LENGTH_UNIT)));
        }

        int widthValIdx = cursor.getColumnIndex(COL_PROD_WIDTH_VAL);
        if (widthValIdx != -1 && !cursor.isNull(widthValIdx)) {
            product.setWidthValue(cursor.getDouble(widthValIdx));
            product.setWidthUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_WIDTH_UNIT)));
        }

        int heightValIdx = cursor.getColumnIndex(COL_PROD_HEIGHT_VAL);
        if (heightValIdx != -1 && !cursor.isNull(heightValIdx)) {
            product.setHeightValue(cursor.getDouble(heightValIdx));
            product.setHeightUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_HEIGHT_UNIT)));
        }

        int weightValIdx = cursor.getColumnIndex(COL_PROD_WEIGHT_VAL);
        if (weightValIdx != -1 && !cursor.isNull(weightValIdx)) {
            product.setWeightValue(cursor.getDouble(weightValIdx));
            product.setWeightUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_WEIGHT_UNIT)));
        }

        int volValIdx = cursor.getColumnIndex(COL_PROD_VOLUME_VAL);
        if (volValIdx != -1 && !cursor.isNull(volValIdx)) {
            product.setVolumeValue(cursor.getDouble(volValIdx));
            product.setVolumeUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_PROD_VOLUME_UNIT)));
        }

        return product;
    }

    // ==========================================
    // PRODUCT IMAGES OPERATIONS
    // ==========================================

    public List<ProductImage> getImagesForProduct(long productId) {
        List<ProductImage> images = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCT_IMAGES, null,
                COL_IMG_PROD_ID + " = ?", new String[]{String.valueOf(productId)},
                null, null, COL_IMG_IS_PRIMARY + " DESC, " + COL_IMG_ID + " ASC");

        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(COL_IMG_ID);
            int pathIdx = cursor.getColumnIndex(COL_IMG_PATH);
            int primaryIdx = cursor.getColumnIndex(COL_IMG_IS_PRIMARY);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIdx);
                String path = cursor.getString(pathIdx);
                boolean isPrimary = cursor.getInt(primaryIdx) == 1;
                images.add(new ProductImage(id, productId, path, isPrimary));
            }
            cursor.close();
        }
        return images;
    }

    public boolean setPrimaryImage(long productId, long imageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues resetValues = new ContentValues();
            resetValues.put(COL_IMG_IS_PRIMARY, 0);
            db.update(TABLE_PRODUCT_IMAGES, resetValues, COL_IMG_PROD_ID + " = ?", new String[]{String.valueOf(productId)});

            ContentValues setValues = new ContentValues();
            setValues.put(COL_IMG_IS_PRIMARY, 1);
            db.update(TABLE_PRODUCT_IMAGES, setValues, COL_IMG_ID + " = ?", new String[]{String.valueOf(imageId)});

            // Also update product's main image_path column
            Cursor cursor = db.query(TABLE_PRODUCT_IMAGES, new String[]{COL_IMG_PATH},
                    COL_IMG_ID + " = ?", new String[]{String.valueOf(imageId)}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String path = cursor.getString(0);
                    ContentValues prodValues = new ContentValues();
                    prodValues.put(COL_PROD_IMAGE_PATH, path);
                    db.update(TABLE_PRODUCTS, prodValues, COL_PROD_ID + " = ?", new String[]{String.valueOf(productId)});
                }
                cursor.close();
            }

            db.setTransactionSuccessful();
            success = true;
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public long insertProductImage(long productId, String imagePath, boolean isPrimary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues imgValues = new ContentValues();
        imgValues.put(COL_IMG_PROD_ID, productId);
        imgValues.put(COL_IMG_PATH, imagePath);
        imgValues.put(COL_IMG_IS_PRIMARY, isPrimary ? 1 : 0);
        return db.insert(TABLE_PRODUCT_IMAGES, null, imgValues);
    }

    public List<ProductImage> getProductImages(long productId) {
        return getImagesForProduct(productId);
    }

    public boolean deleteSingleProductImage(long imageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCT_IMAGES, COL_IMG_ID + " = ?", new String[]{String.valueOf(imageId)});
        return rows > 0;
    }

    public List<ProductImage> getAllProductImages() {
        List<ProductImage> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCT_IMAGES, null, null, null, null, null, null);
        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(COL_IMG_ID);
            int prodIdIdx = cursor.getColumnIndex(COL_IMG_PROD_ID);
            int pathIdx = cursor.getColumnIndex(COL_IMG_PATH);
            int primaryIdx = cursor.getColumnIndex(COL_IMG_IS_PRIMARY);
            while (cursor.moveToNext()) {
                list.add(new ProductImage(
                        cursor.getLong(idIdx),
                        cursor.getLong(prodIdIdx),
                        cursor.getString(pathIdx),
                        cursor.getInt(primaryIdx) == 1
                ));
            }
            cursor.close();
        }
        return list;
    }

    public int getAllProductImagesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PRODUCT_IMAGES, null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public void updateProductImageRecordPath(long imageId, String newPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_IMG_PATH, newPath);
        db.update(TABLE_PRODUCT_IMAGES, cv, COL_IMG_ID + " = ?", new String[]{String.valueOf(imageId)});
    }

    public void updateProductPrimaryImagePath(long productId, String newPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PROD_IMAGE_PATH, newPath);
        db.update(TABLE_PRODUCTS, cv, COL_PROD_ID + " = ?", new String[]{String.valueOf(productId)});
    }

    public void updateDeletedProductImagePath(long deletedId, String newPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DEL_IMAGE_PATH, newPath);
        db.update(TABLE_DELETED_PRODUCTS, cv, COL_DEL_ID + " = ?", new String[]{String.valueOf(deletedId)});
    }

    // ==========================================
    // DELETED PRODUCTS ARCHIVE & RECOVERY
    // ==========================================

    public void archiveDeletedProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DEL_ORIGINAL_ID, product.getId());
        values.put(COL_DEL_NAME, product.getName());
        values.put(COL_DEL_PURCHASE_PRICE, product.getPurchasePrice());
        values.put(COL_DEL_SELLING_PRICE, product.getSellingPrice());
        values.put(COL_DEL_COLOR, product.getColor());
        values.put(COL_DEL_SIZE, product.getSize());
        values.put(COL_DEL_IMAGE_PATH, product.getPrimaryImagePath());
        values.put(COL_DEL_CATEGORY_NAME, product.getCategoryName());

        try {
            JSONObject measJson = new JSONObject();
            measJson.put("length_val", product.getLengthValue());
            measJson.put("length_unit", product.getLengthUnit());
            measJson.put("width_val", product.getWidthValue());
            measJson.put("width_unit", product.getWidthUnit());
            measJson.put("height_val", product.getHeightValue());
            measJson.put("height_unit", product.getHeightUnit());
            measJson.put("weight_val", product.getWeightValue());
            measJson.put("weight_unit", product.getWeightUnit());
            measJson.put("volume_val", product.getVolumeValue());
            measJson.put("volume_unit", product.getVolumeUnit());
            values.put(COL_DEL_MEASUREMENTS_JSON, measJson.toString());

            JSONArray imgArray = new JSONArray();
            List<ProductImage> images = getImagesForProduct(product.getId());
            for (ProductImage img : images) {
                JSONObject imgObj = new JSONObject();
                imgObj.put("path", img.getImagePath());
                imgObj.put("is_primary", img.isPrimary());
                imgArray.put(imgObj);
            }
            values.put(COL_DEL_IMAGES_JSON, imgArray.toString());
        } catch (Exception ignored) {
        }

        db.insert(TABLE_DELETED_PRODUCTS, null, values);
    }

    public List<DeletedProduct> getAllDeletedProducts() {
        List<DeletedProduct> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DELETED_PRODUCTS, null, null, null, null, null, COL_DEL_ID + " DESC");

        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(COL_DEL_ID);
            int origIdIdx = cursor.getColumnIndex(COL_DEL_ORIGINAL_ID);
            int nameIdx = cursor.getColumnIndex(COL_DEL_NAME);
            int purchaseIdx = cursor.getColumnIndex(COL_DEL_PURCHASE_PRICE);
            int sellingIdx = cursor.getColumnIndex(COL_DEL_SELLING_PRICE);
            int colorIdx = cursor.getColumnIndex(COL_DEL_COLOR);
            int sizeIdx = cursor.getColumnIndex(COL_DEL_SIZE);
            int imgIdx = cursor.getColumnIndex(COL_DEL_IMAGE_PATH);
            int catIdx = cursor.getColumnIndex(COL_DEL_CATEGORY_NAME);
            int measIdx = cursor.getColumnIndex(COL_DEL_MEASUREMENTS_JSON);
            int imgsIdx = cursor.getColumnIndex(COL_DEL_IMAGES_JSON);
            int dateIdx = cursor.getColumnIndex(COL_DEL_DELETED_AT);

            while (cursor.moveToNext()) {
                DeletedProduct dp = new DeletedProduct();
                dp.setId(cursor.getLong(idIdx));
                dp.setOriginalProductId(cursor.getLong(origIdIdx));
                dp.setName(cursor.getString(nameIdx));
                dp.setPurchasePrice(cursor.getDouble(purchaseIdx));
                dp.setSellingPrice(cursor.getDouble(sellingIdx));
                dp.setColor(cursor.getString(colorIdx));
                dp.setSize(cursor.getString(sizeIdx));
                dp.setImagePath(cursor.getString(imgIdx));
                dp.setCategoryName(cursor.getString(catIdx));
                dp.setMeasurementsJson(cursor.getString(measIdx));
                dp.setImagesJson(cursor.getString(imgsIdx));
                dp.setDeletedAt(cursor.getString(dateIdx));
                list.add(dp);
            }
            cursor.close();
        }
        return list;
    }

    public boolean restoreDeletedProduct(long deletedRecordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_DELETED_PRODUCTS, null, COL_DEL_ID + " = ?",
                new String[]{String.valueOf(deletedRecordId)}, null, null, null);

        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return false;
        }

        String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_NAME));
        double purchase = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_DEL_PURCHASE_PRICE));
        double selling = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_DEL_SELLING_PRICE));
        String color = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_COLOR));
        String size = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_SIZE));
        String imgPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_IMAGE_PATH));
        String catName = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_CATEGORY_NAME));
        String measJsonStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_MEASUREMENTS_JSON));
        String imagesJsonStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEL_IMAGES_JSON));
        cursor.close();

        // Find or create category
        long catId = 1;
        if (catName != null && !catName.isEmpty()) {
            Cursor catCursor = db.query(TABLE_CATEGORIES, new String[]{COL_CAT_ID},
                    COL_CAT_NAME + " = ?", new String[]{catName}, null, null, null);
            if (catCursor != null && catCursor.moveToFirst()) {
                catId = catCursor.getLong(0);
                catCursor.close();
            } else {
                if (catCursor != null) catCursor.close();
                ContentValues cv = new ContentValues();
                cv.put(COL_CAT_NAME, catName);
                catId = db.insert(TABLE_CATEGORIES, null, cv);
            }
        }

        Product restored = new Product(0, name, purchase, selling, color, size, imgPath, catId, catName);

        try {
            if (measJsonStr != null && !measJsonStr.isEmpty()) {
                JSONObject obj = new JSONObject(measJsonStr);
                restored.setLengthValue(obj.optDouble("length_val", 0));
                restored.setLengthUnit(obj.optString("length_unit", null));
                restored.setWidthValue(obj.optDouble("width_val", 0));
                restored.setWidthUnit(obj.optString("width_unit", null));
                restored.setHeightValue(obj.optDouble("height_val", 0));
                restored.setHeightUnit(obj.optString("height_unit", null));
                restored.setWeightValue(obj.optDouble("weight_val", 0));
                restored.setWeightUnit(obj.optString("weight_unit", null));
                restored.setVolumeValue(obj.optDouble("volume_val", 0));
                restored.setVolumeUnit(obj.optString("volume_unit", null));
            }

            if (imagesJsonStr != null && !imagesJsonStr.isEmpty()) {
                JSONArray arr = new JSONArray(imagesJsonStr);
                List<ProductImage> imgs = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject io = arr.getJSONObject(i);
                    imgs.add(new ProductImage(0, io.getString("path"), io.optBoolean("is_primary", false)));
                }
                restored.setImages(imgs);
            }
        } catch (Exception ignored) {
        }

        long newProdId = insertProduct(restored);
        if (newProdId != -1) {
            db.delete(TABLE_DELETED_PRODUCTS, COL_DEL_ID + " = ?", new String[]{String.valueOf(deletedRecordId)});
            logChange("RESTORE_PRODUCT", TABLE_PRODUCTS, newProdId, "استرجاع منتج: " + name);
            return true;
        }
        return false;
    }

    // ==========================================
    // BACKUP HISTORY OPERATIONS
    // ==========================================

    public long insertBackupRecord(BackupRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_BK_NAME, record.getBackupName());
        cv.put(COL_BK_TYPE, record.getBackupType());
        cv.put(COL_BK_FILE_PATH, record.getFilePath());
        cv.put(COL_BK_FILE_SIZE, record.getFileSize());
        cv.put(COL_BK_PROD_COUNT, record.getProductCount());
        cv.put(COL_BK_CAT_COUNT, record.getCategoryCount());
        cv.put(COL_BK_DRIVE_FILE_ID, record.getDriveFileId());
        cv.put(COL_BK_IS_CLOUD, record.isCloud() ? 1 : 0);

        return db.insert(TABLE_BACKUP_HISTORY, null, cv);
    }

    public List<BackupRecord> getAllBackupRecords() {
        List<BackupRecord> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BACKUP_HISTORY, null, null, null, null, null, COL_BK_ID + " DESC");

        if (cursor != null) {
            int idIdx = cursor.getColumnIndex(COL_BK_ID);
            int nameIdx = cursor.getColumnIndex(COL_BK_NAME);
            int typeIdx = cursor.getColumnIndex(COL_BK_TYPE);
            int pathIdx = cursor.getColumnIndex(COL_BK_FILE_PATH);
            int sizeIdx = cursor.getColumnIndex(COL_BK_FILE_SIZE);
            int prodIdx = cursor.getColumnIndex(COL_BK_PROD_COUNT);
            int catIdx = cursor.getColumnIndex(COL_BK_CAT_COUNT);
            int dateIdx = cursor.getColumnIndex(COL_BK_CREATED_AT);
            int driveIdx = cursor.getColumnIndex(COL_BK_DRIVE_FILE_ID);
            int cloudIdx = cursor.getColumnIndex(COL_BK_IS_CLOUD);

            while (cursor.moveToNext()) {
                BackupRecord br = new BackupRecord();
                br.setId(cursor.getLong(idIdx));
                br.setBackupName(cursor.getString(nameIdx));
                br.setBackupType(cursor.getString(typeIdx));
                br.setFilePath(cursor.getString(pathIdx));
                br.setFileSize(cursor.getLong(sizeIdx));
                br.setProductCount(cursor.getInt(prodIdx));
                br.setCategoryCount(cursor.getInt(catIdx));
                br.setCreatedAt(cursor.getString(dateIdx));
                br.setDriveFileId(cursor.getString(driveIdx));
                br.setCloud(cursor.getInt(cloudIdx) == 1);
                list.add(br);
            }
            cursor.close();
        }
        return list;
    }

    public boolean deleteBackupRecord(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_BACKUP_HISTORY, COL_BK_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // ==========================================
    // CHANGE LOG OPERATIONS
    // ==========================================

    public void logChange(String actionType, String tableName, long recordId, String description) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COL_LOG_ACTION, actionType);
            cv.put(COL_LOG_TABLE, tableName);
            cv.put(COL_LOG_RECORD_ID, recordId);
            cv.put(COL_LOG_DESC, description);
            cv.put(COL_LOG_IS_SYNCED, 0);
            db.insert(TABLE_CHANGE_LOG, null, cv);
        } catch (Exception ignored) {
        }
    }

    public int getUnsyncedChangesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CHANGE_LOG + " WHERE " + COL_LOG_IS_SYNCED + " = 0", null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public void markAllChangesSynced() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LOG_IS_SYNCED, 1);
        db.update(TABLE_CHANGE_LOG, cv, null, null);
    }
}
