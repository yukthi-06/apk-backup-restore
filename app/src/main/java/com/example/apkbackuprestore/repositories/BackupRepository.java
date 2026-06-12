package com.example.apkbackuprestore.repositories;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import com.example.apkbackuprestore.R;
import com.example.apkbackuprestore.models.BackupInfo;
import com.example.apkbackuprestore.utils.StorageManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupRepository {

    public interface LoadCallback {
        void onStart();
        void onLoaded(List<BackupInfo> backups);
    }

    private final Context context;
    private final StorageManager storageManager;
    private final ExecutorService executorService;

    public BackupRepository(Context context) {
        this.context = context.getApplicationContext();
        this.storageManager = new StorageManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void loadBackups(String sortType, String query, LoadCallback callback) {
        executorService.execute(() -> {
            callback.onStart();
            List<BackupInfo> backupList = new ArrayList<>();
            DocumentFile backupFolder = storageManager.getBackupDocumentFolder();

            if (backupFolder != null && backupFolder.exists() && backupFolder.isDirectory()) {
                DocumentFile[] files = backupFolder.listFiles();
                PackageManager pm = context.getPackageManager();

                for (DocumentFile docFile : files) {
                    if (docFile.isFile() && docFile.getName() != null && docFile.getName().endsWith(".apk")) {
                        
                        // Perform name filtering if query is present
                        if (query != null && !query.trim().isEmpty()) {
                            if (!docFile.getName().toLowerCase().contains(query.toLowerCase())) {
                                continue;
                            }
                        }

                        BackupInfo info = parseApkMetadata(docFile, pm);
                        if (info != null) {
                            backupList.add(info);
                        }
                    }
                }
            }

            // Sort backups
            sortBackupList(backupList, sortType);
            callback.onLoaded(backupList);
        });
    }

    private BackupInfo parseApkMetadata(DocumentFile docFile, PackageManager pm) {
        File tempFile = new File(context.getCacheDir(), "temp_parse_" + System.currentTimeMillis() + ".apk");
        try {
            // Copy file to cache for parsing
            try (InputStream in = context.getContentResolver().openInputStream(docFile.getUri());
                 OutputStream out = new FileOutputStream(tempFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            // Parse APK
            PackageInfo packageInfo = pm.getPackageArchiveInfo(tempFile.getAbsolutePath(), 0);
            if (packageInfo != null) {
                // Important to load label/icon correctly
                packageInfo.applicationInfo.sourceDir = tempFile.getAbsolutePath();
                packageInfo.applicationInfo.publicSourceDir = tempFile.getAbsolutePath();
                
                String appLabel = packageInfo.applicationInfo.loadLabel(pm).toString();
                Drawable icon = packageInfo.applicationInfo.loadIcon(pm);

                return new BackupInfo(
                        docFile.getName(),
                        docFile.getUri().getPath(),
                        docFile.getUri(),
                        appLabel,
                        packageInfo.packageName,
                        packageInfo.versionName != null ? packageInfo.versionName : "1.0",
                        packageInfo.versionCode,
                        docFile.lastModified(),
                        docFile.length(),
                        icon
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        
        // Return a fallback backup info if parsing fails
        return new BackupInfo(
                docFile.getName(),
                docFile.getUri().getPath(),
                docFile.getUri(),
                docFile.getName().replace(".apk", ""),
                "unknown.package",
                "1.0",
                1,
                docFile.lastModified(),
                docFile.length(),
                ContextCompat.getDrawable(context, R.drawable.ic_installed_apps)
        );
    }

    private void sortBackupList(List<BackupInfo> list, String sortType) {
        if (sortType == null) return;

        switch (sortType) {
            case "size":
                Collections.sort(list, (a, b) -> Long.compare(b.getFileSize(), a.getFileSize())); // Descending
                break;
            case "date":
                Collections.sort(list, (a, b) -> Long.compare(b.getBackupDate(), a.getBackupDate())); // Descending
                break;
            case "name":
            default:
                Collections.sort(list, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName())); // Ascending
                break;
        }
    }
}
