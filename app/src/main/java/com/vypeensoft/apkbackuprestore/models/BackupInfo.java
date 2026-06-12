package com.vypeensoft.apkbackuprestore.models;

import android.graphics.drawable.Drawable;
import android.net.Uri;

public class BackupInfo {
    private String fileName;
    private String filePath; // For standard File paths (fallback)
    private Uri fileUri;      // For Scoped Storage/SAF Document Uris
    private String appName;
    private String packageName;
    private String versionName;
    private int versionCode;
    private long backupDate;
    private long fileSize;
    private Drawable icon;
    private boolean isSelected;

    public BackupInfo(String fileName, String filePath, Uri fileUri, String appName, 
                      String packageName, String versionName, int versionCode, 
                      long backupDate, long fileSize, Drawable icon) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileUri = fileUri;
        this.appName = appName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.backupDate = backupDate;
        this.fileSize = fileSize;
        this.icon = icon;
        this.isSelected = false;
    }

    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public Uri getFileUri() { return fileUri; }
    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public String getVersionName() { return versionName; }
    public int getVersionCode() { return versionCode; }
    public long getBackupDate() { return backupDate; }
    public long getFileSize() { return fileSize; }
    public Drawable getIcon() { return icon; }
    public void setIcon(Drawable icon) { this.icon = icon; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }
}
