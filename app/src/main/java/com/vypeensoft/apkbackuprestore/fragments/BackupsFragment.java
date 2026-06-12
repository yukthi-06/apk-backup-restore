package com.vypeensoft.apkbackuprestore.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.vypeensoft.apkbackuprestore.R;
import com.vypeensoft.apkbackuprestore.adapters.BackupListAdapter;
import com.vypeensoft.apkbackuprestore.models.BackupInfo;
import com.vypeensoft.apkbackuprestore.utils.ApkRestoreManager;
import com.vypeensoft.apkbackuprestore.viewmodels.BackupsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupsFragment extends Fragment implements BackupListAdapter.BackupActionListener {

    private BackupsViewModel viewModel;
    private BackupListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    
    private ApkRestoreManager restoreManager;
    private ExecutorService shareExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backups, container, false);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        SearchView searchView = view.findViewById(R.id.search_view);
        ImageButton btnSort = view.findViewById(R.id.btn_sort);

        restoreManager = new ApkRestoreManager(requireContext());
        shareExecutor = Executors.newSingleThreadExecutor();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BackupListAdapter(requireContext(), this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(BackupsViewModel.class);

        // Observers
        viewModel.getBackupApps().observe(getViewLifecycleOwner(), backups -> {
            adapter.setBackups(backups);
            layoutEmpty.setVisibility(backups.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading && !swipeRefresh.isRefreshing() ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                swipeRefresh.setRefreshing(false);
            }
        });

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadBackups());

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

        btnSort.setOnClickListener(v -> showSortDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadBackups();
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_name),
                getString(R.string.sort_size),
                getString(R.string.sort_date)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort_by)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: viewModel.setSortType("name"); break;
                        case 1: viewModel.setSortType("size"); break;
                        case 2: viewModel.setSortType("date"); break;
                    }
                })
                .show();
    }

    // Callback Actions from Adapter
    @Override
    public void onInstall(BackupInfo backupInfo) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preparing Installation")
                .setMessage("Extracting package to temporary path...")
                .setCancelable(false)
                .create();

        restoreManager.installApk(backupInfo, new ApkRestoreManager.InstallListener() {
            @Override
            public void onStart() {
                requireActivity().runOnUiThread(progressDialog::show);
            }

            @Override
            public void onPermissionRequired() {
                requireActivity().runOnUiThread(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    showInstallPermissionDialog();
                });
            }

            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showInstallPermissionDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Permission Required")
                .setMessage("To install backed up applications, this app needs the 'Install Unknown Apps' permission. Enable this in Settings?")
                .setPositiveButton("Settings", (dialog, which) -> restoreManager.requestInstallPermissionSetting(requireContext()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onShare(BackupInfo backupInfo) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Preparing File")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        shareExecutor.execute(() -> {
            try {
                // Copy base APK from document provider to cache to share via FileProvider
                File cacheFile = new File(requireContext().getCacheDir(), backupInfo.getFileName());
                if (cacheFile.exists()) cacheFile.delete();

                try (InputStream in = requireContext().getContentResolver().openInputStream(backupInfo.getFileUri());
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
                    startActivity(Intent.createChooser(intent, "Share Backup APK via"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDelete(BackupInfo backupInfo) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Backup?")
                .setMessage("Are you sure you want to permanently delete the backup file: " + backupInfo.getFileName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        DocumentFile file = DocumentFile.fromSingleUri(requireContext(), backupInfo.getFileUri());
                        if (file != null && file.exists()) {
                            file.delete();
                            Toast.makeText(getContext(), "Backup deleted.", Toast.LENGTH_SHORT).show();
                            viewModel.loadBackups();
                        } else {
                            Toast.makeText(getContext(), "Backup file not found.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewDetails(BackupInfo backupInfo) {
        String dateStr = DateFormat.getDateTimeInstance().format(new Date(backupInfo.getBackupDate()));
        String sizeStr = Formatter.formatFileSize(requireContext(), backupInfo.getFileSize());
        
        StringBuilder details = new StringBuilder();
        details.append("Application Label: ").append(backupInfo.getAppName()).append("\n");
        details.append("Package Name: ").append(backupInfo.getPackageName()).append("\n");
        details.append("Version Name: ").append(backupInfo.getVersionName()).append("\n");
        details.append("Version Code: ").append(backupInfo.getVersionCode()).append("\n");
        details.append("File Size: ").append(sizeStr).append("\n");
        details.append("Backup Date: ").append(dateStr).append("\n");
        details.append("Filename: ").append(backupInfo.getFileName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Backup Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shareExecutor.shutdown();
    }
}
