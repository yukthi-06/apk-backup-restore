package com.example.apkbackuprestore.adapters;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
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
import com.example.apkbackuprestore.models.BackupInfo;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.BackupViewHolder> {

    public interface BackupActionListener {
        void onInstall(BackupInfo backupInfo);
        void onShare(BackupInfo backupInfo);
        void onDelete(BackupInfo backupInfo);
        void onViewDetails(BackupInfo backupInfo);
    }

    private final Context context;
    private final List<BackupInfo> backupList = new ArrayList<>();
    private final BackupActionListener listener;

    public BackupListAdapter(Context context, BackupActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBackups(List<BackupInfo> newBackups) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return backupList.size(); }
            @Override
            public int getNewListSize() { return newBackups.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return backupList.get(oldItemPosition).getFileName().equals(newBackups.get(newItemPosition).getFileName());
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                BackupInfo oldBackup = backupList.get(oldItemPosition);
                BackupInfo newBackup = newBackups.get(newItemPosition);
                return oldBackup.getAppName().equals(newBackup.getAppName()) &&
                       oldBackup.getVersionName().equals(newBackup.getVersionName()) &&
                       oldBackup.getFileSize() == newBackup.getFileSize() &&
                       oldBackup.getBackupDate() == newBackup.getBackupDate();
            }
        });
        backupList.clear();
        backupList.addAll(newBackups);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public BackupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_backup, parent, false);
        return new BackupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BackupViewHolder holder, int position) {
        BackupInfo backupInfo = backupList.get(position);
        holder.bind(backupInfo);
    }

    @Override
    public int getItemCount() {
        return backupList.size();
    }

    class BackupViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvAppName, tvFileName, tvVersion, tvSize, tvDate;
        Button btnInstall;
        ImageButton btnOptions;

        public BackupViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_backup_icon);
            tvAppName = itemView.findViewById(R.id.tv_backup_name);
            tvFileName = itemView.findViewById(R.id.tv_backup_filename);
            tvVersion = itemView.findViewById(R.id.tv_backup_version);
            tvSize = itemView.findViewById(R.id.tv_backup_size);
            tvDate = itemView.findViewById(R.id.tv_backup_date);
            btnInstall = itemView.findViewById(R.id.btn_backup_install);
            btnOptions = itemView.findViewById(R.id.btn_backup_options);
        }

        public void bind(BackupInfo backupInfo) {
            if (backupInfo.getIcon() != null) {
                imgIcon.setImageDrawable(backupInfo.getIcon());
            } else {
                imgIcon.setImageResource(R.drawable.ic_installed_apps);
            }
            
            tvAppName.setText(backupInfo.getAppName());
            tvFileName.setText(backupInfo.getFileName());
            tvVersion.setText("v" + backupInfo.getVersionName());
            tvSize.setText(Formatter.formatFileSize(context, backupInfo.getFileSize()));
            
            String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(backupInfo.getBackupDate()));
            tvDate.setText(formattedDate);

            btnInstall.setOnClickListener(v -> listener.onInstall(backupInfo));

            btnOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, btnOptions);
                popup.getMenuInflater().inflate(R.menu.backup_options_menu, popup.getMenu());
                
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_backup_install) {
                        listener.onInstall(backupInfo);
                        return true;
                    } else if (id == R.id.menu_backup_share) {
                        listener.onShare(backupInfo);
                        return true;
                    } else if (id == R.id.menu_backup_delete) {
                        listener.onDelete(backupInfo);
                        return true;
                    } else if (id == R.id.menu_backup_details) {
                        listener.onViewDetails(backupInfo);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
