package com.vypeensoft.apkbackuprestore.adapters;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.vypeensoft.apkbackuprestore.R;
import com.vypeensoft.apkbackuprestore.models.AppInfo;
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
                       oldApp.getApkSize() == newApp.getApkSize() &&
                       oldApp.isSelected() == newApp.isSelected();
            }
        });
        appList.clear();
        appList.addAll(newApps);
        diffResult.dispatchUpdatesTo(this);
    }

    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selected = new ArrayList<>();
        for (AppInfo app : appList) {
            if (app.isSelected()) {
                selected.add(app);
            }
        }
        return selected;
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
        CheckBox cbSelect;
        ImageView imgIcon;
        TextView tvName, tvVersion, tvSize, tvSystemBadge;
        ImageButton btnInfo;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_app_select);
            imgIcon = itemView.findViewById(R.id.img_app_icon);
            tvName = itemView.findViewById(R.id.tv_app_name);
            tvVersion = itemView.findViewById(R.id.tv_app_version);
            tvSize = itemView.findViewById(R.id.tv_app_size);
            tvSystemBadge = itemView.findViewById(R.id.tv_system_badge);
            btnInfo = itemView.findViewById(R.id.btn_app_info);
        }

        public void bind(AppInfo appInfo) {
            imgIcon.setImageDrawable(appInfo.getIcon());
            tvName.setText(appInfo.getAppName());
            tvVersion.setText("v" + appInfo.getVersionName());
            tvSize.setText(Formatter.formatFileSize(context, appInfo.getApkSize()));

            if (appInfo.isSystemApp()) {
                tvSystemBadge.setVisibility(View.VISIBLE);
            } else {
                tvSystemBadge.setVisibility(View.GONE);
            }

            // Remove listener before setting state to avoid side effects during recycling
            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(appInfo.isSelected());
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> appInfo.setSelected(isChecked));

            btnInfo.setOnClickListener(v -> listener.onOpenInfo(appInfo));
        }
    }
}
