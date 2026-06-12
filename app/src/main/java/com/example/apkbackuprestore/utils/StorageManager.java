package com.example.apkbackuprestore.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;

public class StorageManager {

    private static final String PREFS_NAME = "apk_backup_prefs";
    private static final String KEY_BACKUP_URI = "backup_directory_uri";
    private static final String DEFAULT_SUBDIR = "APK_Backups";

    private final Context context;
    private final SharedPreferences prefs;

    public StorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Stores the selected tree URI and requests persistable permissions.
     */
    public void saveBackupUri(Uri uri) {
        if (uri == null) {
            prefs.edit().remove(KEY_BACKUP_URI).apply();
            return;
        }

        try {
            // Request persistable permission
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            prefs.edit().putString(KEY_BACKUP_URI, uri.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the configured backup directory tree URI.
     */
    public Uri getBackupUri() {
        String uriStr = prefs.getString(KEY_BACKUP_URI, null);
        if (uriStr == null) return null;
        
        try {
            Uri uri = Uri.parse(uriStr);
            // Verify if permission is still valid
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            return uri;
        } catch (Exception e) {
            // Permission might have been revoked
            prefs.edit().remove(KEY_BACKUP_URI).apply();
            return null;
        }
    }

    /**
     * Resets directory to the default path.
     */
    public void resetToDefault() {
        prefs.edit().remove(KEY_BACKUP_URI).apply();
    }

    /**
     * Checks if a custom backup directory is currently configured.
     */
    public boolean isCustomDirectoryConfigured() {
        return getBackupUri() != null;
    }

    /**
     * Returns a human-readable description of the current backup directory.
     */
    public String getBackupDirectoryPathDescription() {
        Uri uri = getBackupUri();
        if (uri != null) {
            DocumentFile doc = DocumentFile.fromTreeUri(context, uri);
            if (doc != null && doc.getName() != null) {
                return "[SAF] " + doc.getName();
            }
            return uri.getPath();
        }
        
        // Fallback default
        File defaultDir = getDefaultBackupDir();
        return defaultDir.getAbsolutePath();
    }

    /**
     * Returns the default backup directory File object.
     * Fallback to external files directory if external storage is not writable.
     */
    public File getDefaultBackupDir() {
        File baseDir = Environment.getExternalStorageDirectory();
        File backupDir = new File(baseDir, DEFAULT_SUBDIR);
        
        // If external directory is not accessible directly (due to scoped storage limitations),
        // we use getExternalFilesDir which requires no permissions.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            backupDir = context.getExternalFilesDir(DEFAULT_SUBDIR);
        }
        
        if (backupDir != null && !backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }

    /**
     * Gets the DocumentFile representing the configured folder.
     */
    public DocumentFile getBackupDocumentFolder() {
        Uri uri = getBackupUri();
        if (uri != null) {
            return DocumentFile.fromTreeUri(context, uri);
        }
        
        File defaultDir = getDefaultBackupDir();
        if (defaultDir != null) {
            return DocumentFile.fromFile(defaultDir);
        }
        return null;
    }
}
