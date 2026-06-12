package com.example.apkbackuprestore.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.apkbackuprestore.R;
import com.example.apkbackuprestore.fragments.BackupsFragment;
import com.example.apkbackuprestore.fragments.InstalledAppsFragment;
import com.example.apkbackuprestore.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private final Fragment installedFragment = new InstalledAppsFragment();
    private final Fragment backupsFragment = new BackupsFragment();
    private final Fragment settingsFragment = new SettingsFragment();
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment activeFragment = installedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved dark mode preference before super.onCreate
        applySavedNightMode();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Preload and hide backup and settings fragments to preserve state, 
        // showing the installed fragment by default.
        fragmentManager.beginTransaction().add(R.id.nav_host_fragment, settingsFragment, "settings").hide(settingsFragment).commit();
        fragmentManager.beginTransaction().add(R.id.nav_host_fragment, backupsFragment, "backups").hide(backupsFragment).commit();
        fragmentManager.beginTransaction().add(R.id.nav_host_fragment, installedFragment, "installed").commit();

        toolbar.setTitle(R.string.title_installed);

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_installed) {
                fragmentManager.beginTransaction().hide(activeFragment).show(installedFragment).commit();
                activeFragment = installedFragment;
                toolbar.setTitle(R.string.title_installed);
                return true;
            } else if (itemId == R.id.navigation_backups) {
                fragmentManager.beginTransaction().hide(activeFragment).show(backupsFragment).commit();
                activeFragment = backupsFragment;
                toolbar.setTitle(R.string.title_backups);
                // Trigger reload of backups when switching tabs
                ((BackupsFragment) backupsFragment).onResume();
                return true;
            } else if (itemId == R.id.navigation_settings) {
                fragmentManager.beginTransaction().hide(activeFragment).show(settingsFragment).commit();
                activeFragment = settingsFragment;
                toolbar.setTitle(R.string.title_settings);
                return true;
            }
            return false;
        });
    }

    private void applySavedNightMode() {
        SharedPreferences prefs = getSharedPreferences("apk_backup_prefs", Context.MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
