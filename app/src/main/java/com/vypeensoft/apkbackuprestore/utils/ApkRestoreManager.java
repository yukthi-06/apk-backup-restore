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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkRestoreManager {

    private final Context context;
    private final ExecutorService executorService;

    public ApkRestoreManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
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
        executorService.execute(() -> {
            try {
                // To avoid permission denial issues from package installer,
                // we copy the backup APK into the app's cache folder,
                // and then serve it via FileProvider.
                File tempApk = new File(context.getCacheDir(), "install_temp.apk");
                if (tempApk.exists()) {
                    tempApk.delete();
                }

                Uri sourceUri = backupInfo.getFileUri();
                try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                     OutputStream out = new FileOutputStream(tempApk)) {
                    
                    if (in == null) {
                        listener.onError("Cannot read backup APK.");
                        return;
                    }

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // Get FileProvider URI
                Uri apkUri = FileProvider.getUriForFile(context, 
                        "com.vypeensoft.apkbackuprestore.fileprovider", tempApk);

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
        });
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
