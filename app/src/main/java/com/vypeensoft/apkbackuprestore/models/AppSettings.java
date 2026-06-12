package com.vypeensoft.apkbackuprestore.models;

import android.os.Environment;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AppSettings {
    public String backupDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vypeensoft/APK_Backup_Restore/backup/";
    public boolean showSystemApps = false;
    public String defaultSortPreference = "name";

    private static final String SETTINGS_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Vypeensoft/APK_Backup_Restore/settings/settings.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static AppSettings load() {
        File file = new File(SETTINGS_FILE_PATH);
        if (!file.exists()) {
            return new AppSettings();
        }
        try (FileReader reader = new FileReader(file)) {
            AppSettings settings = gson.fromJson(reader, AppSettings.class);
            return settings != null ? settings : new AppSettings();
        } catch (Exception e) {
            e.printStackTrace();
            return new AppSettings();
        }
    }

    public void save() {
        File file = new File(SETTINGS_FILE_PATH);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
