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
import com.vypeensoft.apkbackuprestore.models.BackupInfo;
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
                       oldBackup.getBackupDate() == newBackup.getBackupDate() &&
                       oldBackup.isSelected() == newBackup.isSelected();
            }
        });
        backupList.clear();
        backupList.addAll(newBackups);
        diffResult.dispatchUpdatesTo(this);
    }

    public List<BackupInfo> getSelectedBackups() {
        List<BackupInfo> selected = new ArrayList<>();
        for (BackupInfo b : backupList) {
            if (b.isSelected()) {
                selected.add(b);
            }
        }
        return selected;
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
        CheckBox cbSelect;
        ImageView imgIcon;
        TextView tvAppName, tvVersion, tvSize, tvDate;
        ImageButton btnInfo;

        public BackupViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_backup_select);
            imgIcon = itemView.findViewById(R.id.img_backup_icon);
            tvAppName = itemView.findViewById(R.id.tv_backup_name);
            tvVersion = itemView.findViewById(R.id.tv_backup_version);
            tvSize = itemView.findViewById(R.id.tv_backup_size);
            tvDate = itemView.findViewById(R.id.tv_backup_date);
            btnInfo = itemView.findViewById(R.id.btn_backup_info);
        }

        public void bind(BackupInfo backupInfo) {
            if (backupInfo.getIcon() != null) {
                imgIcon.setImageDrawable(backupInfo.getIcon());
            } else {
                imgIcon.setImageResource(R.drawable.ic_installed_apps);
            }
            
            tvAppName.setText(backupInfo.getAppName());
            tvVersion.setText("v" + backupInfo.getVersionName());
            tvSize.setText(Formatter.formatFileSize(context, backupInfo.getFileSize()));
            
            String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(backupInfo.getBackupDate()));
            tvDate.setText(formattedDate);

            // Remove listener before setting checked status to avoid side effects during recycling
            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(backupInfo.isSelected());
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> backupInfo.setSelected(isChecked));

            btnInfo.setOnClickListener(v -> listener.onViewDetails(backupInfo));
        }
    }
}
