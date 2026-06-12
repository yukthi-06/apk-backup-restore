package com.vypeensoft.apkbackuprestore.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.vypeensoft.apkbackuprestore.R;
import com.vypeensoft.apkbackuprestore.fragments.BackupsFragment;
import com.vypeensoft.apkbackuprestore.fragments.InstalledAppsFragment;
import com.vypeensoft.apkbackuprestore.utils.PermissionManager;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private final Fragment installedFragment = new InstalledAppsFragment();
    private final Fragment backupsFragment = new BackupsFragment();
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment activeFragment = installedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check/Request storage permissions on startup
        if (!PermissionManager.hasStoragePermission(this)) {
            PermissionManager.requestStoragePermission(this);
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navDrawer = findViewById(R.id.nav_drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navDrawer.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_main) {
                // Already on main screen, do nothing
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (itemId == R.id.nav_help) {
                startActivity(new Intent(MainActivity.this, HelpActivity.class));
            } else if (itemId == R.id.nav_about) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Set Main checked by default
        navDrawer.setCheckedItem(R.id.nav_main);

        // Preload and hide backups fragment to preserve state, 
        // showing the installed fragment by default.
        fragmentManager.beginTransaction().add(R.id.nav_host_fragment, backupsFragment, "backups").hide(backupsFragment).commit();
        fragmentManager.beginTransaction().add(R.id.nav_host_fragment, installedFragment, "installed").commit();

        toolbar.setTitle(R.string.title_installed);
    }

    public void showBackupsFragment() {
        fragmentManager.beginTransaction().hide(activeFragment).show(backupsFragment).commit();
        activeFragment = backupsFragment;
        toolbar.setTitle(R.string.title_backups);
        
        // Show back button on toolbar
        if (getSupportActionBar() != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> showInstalledFragment());
        }
        
        // Trigger reload of backups when switching tabs
        ((BackupsFragment) backupsFragment).onResume();
    }

    public void showInstalledFragment() {
        fragmentManager.beginTransaction().hide(activeFragment).show(installedFragment).commit();
        activeFragment = installedFragment;
        toolbar.setTitle(R.string.title_installed);
        
        // Restore drawer toggle on toolbar
        if (getSupportActionBar() != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep "Main" selected in drawer when returning to main screen
        NavigationView navDrawer = findViewById(R.id.nav_drawer);
        if (navDrawer != null) {
            navDrawer.setCheckedItem(R.id.nav_main);
        }
        // Re-check permissions
        if (!PermissionManager.hasStoragePermission(this)) {
            PermissionManager.requestStoragePermission(this);
        }
    }
}
