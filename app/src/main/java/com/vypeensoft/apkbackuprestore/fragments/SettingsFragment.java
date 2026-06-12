package com.vypeensoft.apkbackuprestore.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.vypeensoft.apkbackuprestore.R;
import com.vypeensoft.apkbackuprestore.models.AppSettings;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private EditText editPath;
    private AppSettings settings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settings = AppSettings.load();

        editPath = view.findViewById(R.id.editPath);
        Button btnSave = view.findViewById(R.id.btnSaveSettings);
        Button btnReset = view.findViewById(R.id.btn_reset_directory);
        MaterialSwitch switchSystem = view.findViewById(R.id.switch_show_system);

        // Populate initial values
        editPath.setText(settings.backupDirPath);
        switchSystem.setChecked(settings.showSystemApps);

        // Save Custom Path
        btnSave.setOnClickListener(v -> {
            String newPath = editPath.getText().toString().trim();
            if (!newPath.isEmpty()) {
                if (!newPath.endsWith("/")) {
                    newPath += "/";
                }
                settings.backupDirPath = newPath;
                settings.save();
                Toast.makeText(getContext(), "Backup path saved to settings.json", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Path cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Reset to default folder path
        btnReset.setOnClickListener(v -> {
            AppSettings defaultSettings = new AppSettings();
            settings.backupDirPath = defaultSettings.backupDirPath;
            settings.save();
            editPath.setText(settings.backupDirPath);
            Toast.makeText(getContext(), "Restored default backup path.", Toast.LENGTH_SHORT).show();
        });

        // Show System Apps setting
        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.showSystemApps = isChecked;
            settings.save();
        });

        return view;
    }
}
