package com.example.apkbackuprestore.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.example.apkbackuprestore.R;
import com.example.apkbackuprestore.utils.StorageManager;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private StorageManager storageManager;
    private TextView tvBackupPath;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Uri> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    storageManager.saveBackupUri(uri);
                    updatePathDisplay();
                    Toast.makeText(getContext(), "Backup directory updated!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        storageManager = new StorageManager(requireContext());
        prefs = requireContext().getSharedPreferences("apk_backup_prefs", Context.MODE_PRIVATE);

        tvBackupPath = view.findViewById(R.id.tv_backup_path);
        LinearLayout layoutSelectFolder = view.findViewById(R.id.layout_select_folder);
        Button btnReset = view.findViewById(R.id.btn_reset_directory);
        MaterialSwitch switchSystem = view.findViewById(R.id.switch_show_system);
        MaterialSwitch switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        updatePathDisplay();

        // Folder selection
        layoutSelectFolder.setOnClickListener(v -> folderPickerLauncher.launch(null));

        // Reset to default folder
        btnReset.setOnClickListener(v -> {
            storageManager.resetToDefault();
            updatePathDisplay();
            Toast.makeText(getContext(), "Restored default backup directory.", Toast.LENGTH_SHORT).show();
        });

        // Show System Apps setting
        boolean currentShowSystem = prefs.getBoolean("show_system_apps", false);
        switchSystem.setChecked(currentShowSystem);
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("show_system_apps", isChecked).apply();
        });

        // Dark Mode setting
        boolean currentDarkMode = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(currentDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        return view;
    }

    private void updatePathDisplay() {
        if (tvBackupPath != null) {
            tvBackupPath.setText(storageManager.getBackupDirectoryPathDescription());
        }
    }
}
