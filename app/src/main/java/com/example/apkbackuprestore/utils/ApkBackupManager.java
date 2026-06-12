package com.example.apkbackuprestore.utils;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import com.example.apkbackuprestore.models.AppInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkBackupManager {

    public interface BackupListener {
        void onStart();
        void onProgress(int progress);
        void onSuccess(String filePath);
        void onError(String message);
    }

    private final Context context;
    private final StorageManager storageManager;
    private final ExecutorService executorService;

    public ApkBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.storageManager = new StorageManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Helper to get the standardized filename.
     */
    public String getBackupFileName(AppInfo appInfo) {
        // Replace illegal filename characters
        String name = appInfo.getAppName().replaceAll("[\\\\/:*?\"<>|]", "_");
        String version = appInfo.getVersionName().replaceAll("[\\\\/:*?\"<>|]", "_");
        return name + "_" + version + ".apk";
    }

    /**
     * Checks if backup already exists for this app.
     */
    public boolean isBackupExisting(AppInfo appInfo) {
        String fileName = getBackupFileName(appInfo);
        DocumentFile destFolder = storageManager.getBackupDocumentFolder();
        if (destFolder == null) return false;
        
        DocumentFile existingFile = destFolder.findFile(fileName);
        return existingFile != null && existingFile.exists();
    }

    /**
     * Backs up an installed app to the backup directory.
     */
    public void backupApp(AppInfo appInfo, boolean overwrite, BackupListener listener) {
        executorService.execute(() -> {
            listener.onStart();
            try {
                String fileName = getBackupFileName(appInfo);
                File sourceApk = new File(appInfo.getApkPath());
                
                if (!sourceApk.exists()) {
                    listener.onError("Source APK not found.");
                    return;
                }

                DocumentFile destFolder = storageManager.getBackupDocumentFolder();
                if (destFolder == null) {
                    listener.onError("Backup directory unavailable.");
                    return;
                }

                // If folder is not writable
                if (!destFolder.canWrite()) {
                    listener.onError("Backup directory is read-only. Select another directory in Settings.");
                    return;
                }

                DocumentFile destFile = destFolder.findFile(fileName);
                if (destFile != null && destFile.exists()) {
                    if (!overwrite) {
                        listener.onError("Backup already exists.");
                        return;
                    }
                    destFile.delete();
                }

                destFile = destFolder.createFile("application/vnd.android.package-archive", fileName);
                if (destFile == null) {
                    listener.onError("Failed to create backup file.");
                    return;
                }

                // Copy stream
                try (InputStream in = new FileInputStream(sourceApk);
                     OutputStream out = context.getContentResolver().openOutputStream(destFile.getUri())) {
                    
                    if (out == null) {
                        listener.onError("Unable to write backup file stream.");
                        return;
                    }

                    byte[] buffer = new byte[8192];
                    long totalBytes = sourceApk.length();
                    long copiedBytes = 0;
                    int bytesRead;
                    int lastPercent = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        copiedBytes += bytesRead;
                        
                        if (totalBytes > 0) {
                            int percent = (int) ((copiedBytes * 100) / totalBytes);
                            if (percent > lastPercent) {
                                lastPercent = percent;
                                listener.onProgress(percent);
                            }
                        }
                    }
                    
                    listener.onProgress(100);
                    listener.onSuccess(destFile.getUri().toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                listener.onError("Backup failed: " + e.getMessage());
            }
        });
    }
}
