package com.vypeensoft.apkbackuprestore.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.vypeensoft.apkbackuprestore.models.BackupInfo;
import java.io.File;

public class ApkRestoreManager {

    private final Context context;

    public ApkRestoreManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Installs a backed up APK.
     */
    public void installApk(BackupInfo backupInfo, InstallListener listener) {
        if (!PermissionManager.canInstallPackages(context)) {
            listener.onPermissionRequired();
            return;
        }

        listener.onStart();
        try {
            File apkFile = new File(backupInfo.getFilePath());
            if (!apkFile.exists()) {
                listener.onError("Backup APK file not found at " + apkFile.getAbsolutePath());
                return;
            }

            // Get FileProvider URI directly from external storage
            Uri apkUri = FileProvider.getUriForFile(context, 
                    "com.vypeensoft.apkbackuprestore.fileprovider", apkFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);
            listener.onSuccess();

        } catch (Exception e) {
            e.printStackTrace();
            listener.onError("Installation failed: " + e.getMessage());
        }
    }

    /**
     * Guides the user to the system settings page for unknown app installations.
     */
    public void requestInstallPermissionSetting(Context activityContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri packageUri = Uri.parse("package:" + context.getPackageName());
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activityContext.startActivity(intent);
            Toast.makeText(context, "Enable 'Allow from this source' to restore APKs", Toast.LENGTH_LONG).show();
        }
    }

    public interface InstallListener {
        void onStart();
        void onPermissionRequired();
        void onSuccess();
        void onError(String message);
    }
}
