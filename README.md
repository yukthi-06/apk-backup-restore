# APK Backup & Restore

A complete, modern Android application developed in **Java** utilizing Android SDK, AndroidX, and Material Design 3 UI components. This application allows users to inspect installed applications, extract/backup their APK files, manage/install backups, and uninstall applications.

## Features

- **Installed Apps Screen**:
  - View all user and system applications with their icons, package names, version names, sizes, and installation tags.
  - Search applications instantly by name or package name.
  - Sort applications by name, size, install date, or update date.
  - Quick options: open App Info, launch, share base APK, or uninstall.

- **APK Backup Functionality**:
  - Extract the APK of any installed app and save it in `AppName_VersionName.apk` format.
  - Features real-time extraction progress updates.
  - Built-in protection against accidental overwrites.

- **Backup Management Screen**:
  - Browse all backed-up APK files.
  - Install APKs directly from your backup repository.
  - Delete backups or share files with other devices.
  - Parse APK metadata directly to inspect package name, version name, size, and date.

- **Configurable Backup Folder**:
  - Select any directory on your device using the Storage Access Framework (SAF).
  - Persist directory read/write permissions across device reboots.
  - Toggle system apps visibility and dark theme appearance.

---

## Technical Details & Compatibility

- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Language**: Java 8 compatible
- **Architecture**: Model-View-ViewModel (MVVM)
- **Security**: Utilizes `FileProvider` to share and install APKs securely without requiring root permissions.
- **Storage Compatibility**: Fully compliant with Scoped Storage requirements on Android 10+ and package visibility requirements on Android 11+.

---

## Required Permissions

- `android.permission.QUERY_ALL_PACKAGES`: To retrieve the list of installed applications on API 30+.
- `android.permission.REQUEST_INSTALL_PACKAGES`: To request the package installer to restore backed-up APKs.
- `android.permission.REQUEST_DELETE_PACKAGES`: To allow trigger uninstall sequences.
- `android.permission.READ_EXTERNAL_STORAGE` & `android.permission.WRITE_EXTERNAL_STORAGE`: Handled dynamically with max SDK limits for legacy compatibility.

---

## How to Build and Run

1. Open the project root folder in **Android Studio**.
2. Wait for Gradle sync to complete.
3. Build the project using `Build` -> `Make Project` or run `./gradlew assembleDebug` from the terminal.
4. Deploy the application to an emulator or physical device.

---

## Workflows

### APK Backup Workflow
1. Select the **Installed Apps** tab.
2. Search or scroll to find your desired application.
3. Click the **Backup** button.
4. If a backup already exists for that version, you will be prompted to overwrite it.
5. The APK extraction runs in a background thread and shows copying progress.

### Restore / Install Workflow
1. Select the **Backups** tab.
2. Tap the **Install** button next to any backup file.
3. If this is the first install, the app will guide you to enable the **"Install Unknown Apps"** permission.
4. The APK is copied into a temporary application cache folder and securely passed to the package installer using `FileProvider` and `ACTION_VIEW` intent.
