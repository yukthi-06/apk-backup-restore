package com.vypeensoft.apkbackuprestore.repositories;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import com.vypeensoft.apkbackuprestore.R;
import com.vypeensoft.apkbackuprestore.models.AppSettings;
import com.vypeensoft.apkbackuprestore.models.BackupInfo;
import java.io.File;
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
    private final ExecutorService executorService;

    public BackupRepository(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void loadBackups(String sortType, String query, LoadCallback callback) {
        executorService.execute(() -> {
            callback.onStart();
            List<BackupInfo> backupList = new ArrayList<>();
            AppSettings settings = AppSettings.load();
            File backupFolder = new File(settings.backupDirPath);

            if (backupFolder.exists() && backupFolder.isDirectory()) {
                File[] files = backupFolder.listFiles();
                PackageManager pm = context.getPackageManager();

                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName() != null && file.getName().endsWith(".apk")) {
                            
                            // Perform name filtering if query is present
                            if (query != null && !query.trim().isEmpty()) {
                                if (!file.getName().toLowerCase().contains(query.toLowerCase())) {
                                    continue;
                                }
                            }

                            BackupInfo info = parseApkMetadata(file, pm);
                            if (info != null) {
                                backupList.add(info);
                            }
                        }
                    }
                }
            }

            // Sort backups
            sortBackupList(backupList, sortType);
            callback.onLoaded(backupList);
        });
    }

    private BackupInfo parseApkMetadata(File file, PackageManager pm) {
        try {
            // Parse APK directly
            PackageInfo packageInfo = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            if (packageInfo != null) {
                // Important to load label/icon correctly
                packageInfo.applicationInfo.sourceDir = file.getAbsolutePath();
                packageInfo.applicationInfo.publicSourceDir = file.getAbsolutePath();
                
                String appLabel = packageInfo.applicationInfo.loadLabel(pm).toString();
                Drawable rawIcon = packageInfo.applicationInfo.loadIcon(pm);
                Drawable icon = null;
                if (rawIcon != null) {
                    try {
                        int width = rawIcon.getIntrinsicWidth();
                        int height = rawIcon.getIntrinsicHeight();
                        if (width <= 0) width = 100;
                        if (height <= 0) height = 100;
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        rawIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        rawIcon.draw(canvas);
                        icon = new BitmapDrawable(context.getResources(), bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        icon = rawIcon;
                    }
                }

                return new BackupInfo(
                        file.getName(),
                        file.getAbsolutePath(),
                        Uri.fromFile(file),
                        appLabel,
                        packageInfo.packageName,
                        packageInfo.versionName != null ? packageInfo.versionName : "1.0",
                        packageInfo.versionCode,
                        file.lastModified(),
                        file.length(),
                        icon
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Return a fallback backup info if parsing fails
        return new BackupInfo(
                file.getName(),
                file.getAbsolutePath(),
                Uri.fromFile(file),
                file.getName().replace(".apk", ""),
                "unknown.package",
                "1.0",
                1,
                file.lastModified(),
                file.length(),
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
