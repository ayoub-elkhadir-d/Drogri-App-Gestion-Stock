# ProGuard and R8 rules for Drogri / MyStore

-dontwarn androidx.room.**

# Keep RoomDatabase classes and their parameterless reflection constructors
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}

# Explicitly preserve WorkManager internal database implementation
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
    *;
}

# Preserve AutoMigrations
-keep class androidx.work.impl.WorkDatabase_AutoMigration_* {
    <init>();
    *;
}

# Keep WorkManager Worker reflection constructors
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
