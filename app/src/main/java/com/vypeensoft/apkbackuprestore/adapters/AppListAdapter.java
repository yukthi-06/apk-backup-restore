package com.example.apkbackuprestore.adapters;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.apkbackuprestore.R;
import com.example.apkbackuprestore.models.AppInfo;
import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    public interface AppActionListener {
        void onBackup(AppInfo appInfo);
        void onShare(AppInfo appInfo);
        void onOpenInfo(AppInfo appInfo);
        void onLaunch(AppInfo appInfo);
        void onUninstall(AppInfo appInfo);
    }

    private final Context context;
    private final List<AppInfo> appList = new ArrayList<>();
    private final AppActionListener listener;

    public AppListAdapter(Context context, AppActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setApps(List<AppInfo> newApps) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return appList.size(); }
            @Override
            public int getNewListSize() { return newApps.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return appList.get(oldItemPosition).getPackageName().equals(newApps.get(newItemPosition).getPackageName());
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                AppInfo oldApp = appList.get(oldItemPosition);
                AppInfo newApp = newApps.get(newItemPosition);
                return oldApp.getAppName().equals(newApp.getAppName()) &&
                       oldApp.getVersionName().equals(newApp.getVersionName()) &&
                       oldApp.getApkSize() == newApp.getApkSize();
            }
        });
        appList.clear();
        appList.addAll(newApps);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.bind(appInfo);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName, tvPackage, tvVersion, tvSize, tvSystemBadge;
        Button btnBackup;
        ImageButton btnOptions;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_app_icon);
            tvName = itemView.findViewById(R.id.tv_app_name);
            tvPackage = itemView.findViewById(R.id.tv_package_name);
            tvVersion = itemView.findViewById(R.id.tv_app_version);
            tvSize = itemView.findViewById(R.id.tv_app_size);
            tvSystemBadge = itemView.findViewById(R.id.tv_system_badge);
            btnBackup = itemView.findViewById(R.id.btn_app_backup);
            btnOptions = itemView.findViewById(R.id.btn_app_options);
        }

        public void bind(AppInfo appInfo) {
            imgIcon.setImageDrawable(appInfo.getIcon());
            tvName.setText(appInfo.getAppName());
            tvPackage.setText(appInfo.getPackageName());
            tvVersion.setText("v" + appInfo.getVersionName());
            tvSize.setText(Formatter.formatFileSize(context, appInfo.getApkSize()));

            if (appInfo.isSystemApp()) {
                tvSystemBadge.setVisibility(View.VISIBLE);
            } else {
                tvSystemBadge.setVisibility(View.GONE);
            }

            btnBackup.setOnClickListener(v -> listener.onBackup(appInfo));
            
            btnOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, btnOptions);
                popup.getMenuInflater().inflate(R.menu.app_options_menu, popup.getMenu());
                
                // Disable launch if not launchable or if package uninstalled
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_launch) {
                        listener.onLaunch(appInfo);
                        return true;
                    } else if (id == R.id.menu_info) {
                        listener.onOpenInfo(appInfo);
                        return true;
                    } else if (id == R.id.menu_share) {
                        listener.onShare(appInfo);
                        return true;
                    } else if (id == R.id.menu_uninstall) {
                        listener.onUninstall(appInfo);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
