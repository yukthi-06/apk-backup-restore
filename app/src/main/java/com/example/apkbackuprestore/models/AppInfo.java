package com.example.apkbackuprestore.models;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String appName;
    private String packageName;
    private String versionName;
    private int versionCode;
    private String apkPath;
    private long apkSize;
    private Drawable icon;
    private boolean isSystemApp;
    private long installDate;
    private long updateDate;

    public AppInfo(String appName, String packageName, String versionName, int versionCode, 
                   String apkPath, long apkSize, Drawable icon, boolean isSystemApp, 
                   long installDate, long updateDate) {
        this.appName = appName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.apkPath = apkPath;
        this.apkSize = apkSize;
        this.icon = icon;
        this.isSystemApp = isSystemApp;
        this.installDate = installDate;
        this.updateDate = updateDate;
    }

    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public String getVersionName() { return versionName; }
    public int getVersionCode() { return versionCode; }
    public String getApkPath() { return apkPath; }
    public long getApkSize() { return apkSize; }
    public Drawable getIcon() { return icon; }
    public boolean isSystemApp() { return isSystemApp; }
    public long getInstallDate() { return installDate; }
    public long getUpdateDate() { return updateDate; }
}
