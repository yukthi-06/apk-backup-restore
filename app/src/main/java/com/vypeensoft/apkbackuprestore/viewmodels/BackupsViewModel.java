package com.vypeensoft.apkbackuprestore.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.vypeensoft.apkbackuprestore.models.BackupInfo;
import com.vypeensoft.apkbackuprestore.repositories.BackupRepository;
import java.util.List;

public class BackupsViewModel extends AndroidViewModel {

    private final BackupRepository repository;
    private final MutableLiveData<List<BackupInfo>> backupApps = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    private String currentSort = "date";
    private String currentQuery = "";

    public BackupsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new BackupRepository(application);
    }

    public LiveData<List<BackupInfo>> getBackupApps() {
        return backupApps;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void setSortType(String sortType) {
        this.currentSort = sortType;
        loadBackups();
    }

    public void setSearchQuery(String query) {
        this.currentQuery = query;
        loadBackups();
    }

    public void loadBackups() {
        repository.loadBackups(currentSort, currentQuery, new BackupRepository.LoadCallback() {
            @Override
            public void onStart() {
                isLoading.postValue(true);
            }

            @Override
            public void onLoaded(List<BackupInfo> backups) {
                backupApps.postValue(backups);
                isLoading.postValue(false);
            }
        });
    }
}
