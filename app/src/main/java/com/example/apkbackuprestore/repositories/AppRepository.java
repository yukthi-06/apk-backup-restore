package com.example.apkbackuprestore.repositories;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import com.example.apkbackuprestore.models.AppInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppRepository {

    public interface LoadCallback {
        void onStart();
        void onLoaded(List<AppInfo> apps);
    }

    private final Context context;
    private final ExecutorService executorService;

    public AppRepository(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void loadInstalledApps(boolean showSystemApps, String sortType, String query, LoadCallback callback) {
        executorService.execute(() -> {
            callback.onStart();
            List<AppInfo> appList = new ArrayList<>();
            PackageManager pm = context.getPackageManager();
            
            try {
                List<PackageInfo> packages = pm.getInstalledPackages(0);
                String currentPackageName = context.getPackageName();

                for (PackageInfo packageInfo : packages) {
                    // Skip current app
                    if (packageInfo.packageName.equals(currentPackageName)) continue;

                    boolean isSystem = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                    
                    // Filter system apps if requested
                    if (isSystem && !showSystemApps) continue;

                    String appLabel = packageInfo.applicationInfo.loadLabel(pm).toString();
                    
                    // Filter by search query if applicable
                    if (query != null && !query.trim().isEmpty()) {
                        if (!appLabel.toLowerCase().contains(query.toLowerCase()) &&
                            !packageInfo.packageName.toLowerCase().contains(query.toLowerCase())) {
                            continue;
                        }
                    }

                    String apkPath = packageInfo.applicationInfo.publicSourceDir;
                    File file = new File(apkPath);
                    long apkSize = file.exists() ? file.length() : 0;
                    Drawable icon = packageInfo.applicationInfo.loadIcon(pm);

                    AppInfo info = new AppInfo(
                            appLabel,
                            packageInfo.packageName,
                            packageInfo.versionName != null ? packageInfo.versionName : "1.0",
                            packageInfo.versionCode,
                            apkPath,
                            apkSize,
                            icon,
                            isSystem,
                            packageInfo.firstInstallTime,
                            packageInfo.lastUpdateTime
                    );
                    appList.add(info);
                }

                // Sort the list
                sortAppList(appList, sortType);

            } catch (Exception e) {
                e.printStackTrace();
            }
            callback.onLoaded(appList);
        });
    }

    private void sortAppList(List<AppInfo> list, String sortType) {
        if (sortType == null) return;
        
        switch (sortType) {
            case "size":
                Collections.sort(list, (a, b) -> Long.compare(b.getApkSize(), a.getApkSize())); // Descending
                break;
            case "install_date":
                Collections.sort(list, (a, b) -> Long.compare(b.getInstallDate(), a.getInstallDate())); // Descending
                break;
            case "update_date":
                Collections.sort(list, (a, b) -> Long.compare(b.getUpdateDate(), a.getUpdateDate())); // Descending
                break;
            case "name":
            default:
                Collections.sort(list, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName())); // Ascending
                break;
        }
    }
}
