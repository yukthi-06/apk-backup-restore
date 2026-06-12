package com.vypeensoft.apkbackuprestore.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.vypeensoft.apkbackuprestore.models.AppInfo;
import com.vypeensoft.apkbackuprestore.repositories.AppRepository;
import java.util.List;

public class InstalledAppsViewModel extends AndroidViewModel {

    private final AppRepository repository;
    private final MutableLiveData<List<AppInfo>> installedApps = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    private String currentSort = "name";
    private String currentQuery = "";
    private boolean showSystem = false;

    public InstalledAppsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AppRepository(application);
    }

    public LiveData<List<AppInfo>> getInstalledApps() {
        return installedApps;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void setSortType(String sortType) {
        this.currentSort = sortType;
        loadApps();
    }

    public void setSearchQuery(String query) {
        this.currentQuery = query;
        loadApps();
    }

    public void setShowSystemApps(boolean show) {
        this.showSystem = show;
        loadApps();
    }

    public void loadApps() {
        repository.loadInstalledApps(showSystem, currentSort, currentQuery, new AppRepository.LoadCallback() {
            @Override
            public void onStart() {
                isLoading.postValue(true);
            }

            @Override
            public void onLoaded(List<AppInfo> apps) {
                installedApps.postValue(apps);
                isLoading.postValue(false);
            }
        });
    }
}
