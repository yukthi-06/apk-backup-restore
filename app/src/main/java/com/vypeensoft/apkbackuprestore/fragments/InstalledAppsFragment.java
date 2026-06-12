package com.vypeensoft.apkbackuprestore.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.vypeensoft.apkbackuprestore.R;
import com.vypeensoft.apkbackuprestore.adapters.AppListAdapter;
import com.vypeensoft.apkbackuprestore.models.AppInfo;
import com.vypeensoft.apkbackuprestore.models.AppSettings;
import com.vypeensoft.apkbackuprestore.utils.ApkBackupManager;
import com.vypeensoft.apkbackuprestore.viewmodels.InstalledAppsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InstalledAppsFragment extends Fragment implements AppListAdapter.AppActionListener {

    private InstalledAppsViewModel viewModel;
    private AppListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ApkBackupManager backupManager;
    private ExecutorService shareExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_installed_apps, container, false);
        
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        
        SearchView searchView = view.findViewById(R.id.search_view);
        ImageButton btnSort = view.findViewById(R.id.btn_sort);

        backupManager = new ApkBackupManager(requireContext());
        shareExecutor = Executors.newSingleThreadExecutor();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AppListAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(InstalledAppsViewModel.class);

        // Observers
        viewModel.getInstalledApps().observe(getViewLifecycleOwner(), apps -> {
            adapter.setApps(apps);
            tvEmpty.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading && !swipeRefresh.isRefreshing() ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                swipeRefresh.setRefreshing(false);
            }
        });

        // Pull to refresh
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadApps());

        // Search filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });

        // Sort Dialog
        btnSort.setOnClickListener(v -> showSortDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Automatically check Settings and reload apps
        AppSettings settings = AppSettings.load();
        viewModel.setShowSystemApps(settings.showSystemApps);
        viewModel.loadApps();
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_name),
                getString(R.string.sort_size),
                getString(R.string.sort_install_date),
                getString(R.string.sort_update_date)
        };
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort_by)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: viewModel.setSortType("name"); break;
                        case 1: viewModel.setSortType("size"); break;
                        case 2: viewModel.setSortType("install_date"); break;
                        case 3: viewModel.setSortType("update_date"); break;
                    }
                })
                .show();
    }

    // Callback Actions from Adapter
    @Override
    public void onBackup(AppInfo appInfo) {
        if (backupManager.isBackupExisting(appInfo)) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Overwrite Backup?")
                    .setMessage("A backup already exists for " + appInfo.getAppName() + ". Do you want to replace it?")
                    .setPositiveButton("Overwrite", (dialog, which) -> startBackupProcess(appInfo, true))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            startBackupProcess(appInfo, false);
        }
    }

    private void startBackupProcess(AppInfo appInfo, boolean overwrite) {
        // Show progress dialog
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Backing up " + appInfo.getAppName())
                .setMessage("Extracting APK, please wait...")
                .setCancelable(false)
                .create();
        
        progressDialog.show();

        backupManager.backupApp(appInfo, overwrite, new ApkBackupManager.BackupListener() {
            @Override
            public void onStart() {}

            @Override
            public void onProgress(int progress) {
                // Update on UI thread
                requireActivity().runOnUiThread(() -> 
                    progressDialog.setMessage("Copying file: " + progress + "%")
                );
            }

            @Override
            public void onSuccess(String filePath) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Successfully backed up: " + appInfo.getAppName(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onShare(AppInfo appInfo) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preparing File")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        shareExecutor.execute(() -> {
            try {
                // Copy base APK to cache to share via FileProvider
                File cacheFile = new File(requireContext().getCacheDir(), appInfo.getAppName() + ".apk");
                if (cacheFile.exists()) cacheFile.delete();

                try (InputStream in = new FileInputStream(new File(appInfo.getApkPath()));
                     OutputStream out = new FileOutputStream(cacheFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                Uri shareUri = FileProvider.getUriForFile(requireContext(),
                        "com.vypeensoft.apkbackuprestore.fileprovider", cacheFile);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/vnd.android.package-archive");
                intent.putExtra(Intent.EXTRA_STREAM, shareUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    startActivity(Intent.createChooser(intent, "Share APK via"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to prepare APK: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onOpenInfo(AppInfo appInfo) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + appInfo.getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot open App Info.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLaunch(AppInfo appInfo) {
        try {
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(appInfo.getPackageName());
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "App is not launchable.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot launch App.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUninstall(AppInfo appInfo) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Uninstall App")
                .setMessage("Are you sure you want to uninstall " + appInfo.getAppName() + "?")
                .setPositiveButton("Uninstall", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        intent.setData(Uri.parse("package:" + appInfo.getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Cannot uninstall.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shareExecutor.shutdown();
    }
}
