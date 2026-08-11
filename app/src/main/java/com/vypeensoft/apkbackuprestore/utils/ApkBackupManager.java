package com.vypeensoft.apkbackuprestore.utils;

import android.content.Context;
import com.vypeensoft.apkbackuprestore.models.AppInfo;
import com.vypeensoft.apkbackuprestore.models.AppSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    private final ExecutorService executorService;

    public ApkBackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Helper to get the standardized filename.
     */
    public String getBackupFileName(AppInfo appInfo) {
        // Replace illegal filename characters
        String name = appInfo.getAppName().replaceAll("[\\\\/:*?\"<>|]", "_");
        String version = appInfo.getVersionName().replaceAll("[\\\\/:*?\"<>|]", "_");
        String packageName = appInfo.getPackageName().replaceAll("[\\\\/:*?\"<>|]", "_");
        return name + "_(" + packageName + ")_" + version + ".apk";
    }

    /**
     * Checks if backup already exists for this app.
     */
    public boolean isBackupExisting(AppInfo appInfo) {
        AppSettings settings = AppSettings.load();
        File destFolder = new File(settings.backupDirPath);
        if (!destFolder.exists()) return false;
        
        File existingFile = new File(destFolder, getBackupFileName(appInfo));
        return existingFile.exists();
    }

    /**
     * Backs up an installed app to the backup directory.
     */
    public void backupApp(AppInfo appInfo, boolean overwrite, BackupListener listener) {
        executorService.execute(() -> {
            listener.onStart();
            try {
                AppSettings settings = AppSettings.load();
                File destFolder = new File(settings.backupDirPath);
                if (!destFolder.exists()) {
                    destFolder.mkdirs();
                }

                String fileName = getBackupFileName(appInfo);
                File sourceApk = new File(appInfo.getApkPath());
                
                if (!sourceApk.exists()) {
                    listener.onError("Source APK not found.");
                    return;
                }

                File destFile = new File(destFolder, fileName);
                if (destFile.exists()) {
                    if (!overwrite) {
                        listener.onError("Backup already exists.");
                        return;
                    }
                    destFile.delete();
                }

                // Copy stream
                try (InputStream in = new FileInputStream(sourceApk);
                     OutputStream out = new FileOutputStream(destFile)) {

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
                    listener.onSuccess(destFile.getAbsolutePath());
                }

            } catch (Exception e) {
                e.printStackTrace();
                listener.onError("Backup failed: " + e.getMessage());
            }
        });
    }
}
