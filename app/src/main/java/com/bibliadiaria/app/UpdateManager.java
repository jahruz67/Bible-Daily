package com.bibliadiaria.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class UpdateManager {
    static final String PREFS_NAME = "preferencias_lectura";
    static final String KEY_AUTO_UPDATES = "auto_updates_enabled";
    static final String KEY_IS_ENGLISH = "is_english";
    static final String LATEST_JSON_URL =
            "https://github.com/jahruz67/Bible-Daily/releases/download/latest/latest.json";
    static final String APK_URL =
            "https://github.com/jahruz67/Bible-Daily/releases/download/latest/app-debug.apk";

    private UpdateManager() {
    }

    static boolean isAutoUpdatesEnabled(Context context) {
        return preferences(context).getBoolean(KEY_AUTO_UPDATES, true);
    }

    static void setAutoUpdatesEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_AUTO_UPDATES, enabled).apply();
    }

    static boolean isEnglish(Context context) {
        return preferences(context).getBoolean(KEY_IS_ENGLISH, false);
    }

    static void setEnglish(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_IS_ENGLISH, enabled).apply();
    }

    static UpdateInfo fetchLatestUpdate(Context context) throws Exception {
        String jsonText = download(LATEST_JSON_URL, "application/json");
        JSONObject json = new JSONObject(jsonText);

        int latestVersionCode = json.optInt("versionCode", 0);
        if (latestVersionCode <= 0) {
            throw new IOException("Update metadata did not include a valid versionCode.");
        }

        long currentVersionCode = getInstalledVersionCode(context);
        String latestVersionName = json.optString("versionName", "");
        String apkUrl = json.optString("apkUrl", APK_URL);
        String builtAt = json.optString("builtAt", "");
        String commit = json.optString("commit", "");

        return new UpdateInfo(
                currentVersionCode,
                getInstalledVersionName(context),
                latestVersionCode,
                latestVersionName,
                apkUrl.isEmpty() ? APK_URL : apkUrl,
                builtAt,
                commit,
                latestVersionCode > currentVersionCode
        );
    }

    static long getInstalledVersionCode(Context context) throws PackageManager.NameNotFoundException {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return info.getLongVersionCode();
        }
        return info.versionCode;
    }

    static String getInstalledVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (info.versionName == null || info.versionName.trim().isEmpty()) {
                return "unknown";
            }
            return info.versionName;
        } catch (PackageManager.NameNotFoundException error) {
            return "unknown";
        }
    }

    static void showUpdateDialog(Activity activity, UpdateInfo info) {
        if (activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            return;
        }

        String version = info.latestVersionName.isEmpty()
                ? String.valueOf(info.latestVersionCode)
                : info.latestVersionName + " (" + info.latestVersionCode + ")";

        new AlertDialog.Builder(activity)
                .setTitle("Update available")
                .setMessage("Version " + version + " is ready to download.")
                .setPositiveButton("Download", (dialog, which) -> openDownload(activity, info.apkUrl))
                .setNegativeButton("Later", null)
                .show();
    }

    static void openDownload(Context context, String apkUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String download(String urlText, String acceptHeader) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "BibliaDiaria/1.0 Android");
        connection.setRequestProperty("Accept", acceptHeader);

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Update server responded with code " + responseCode + ".");
        }

        try (InputStream stream = connection.getInputStream()) {
            return readStream(stream);
        } finally {
            connection.disconnect();
        }
    }

    private static String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    static final class UpdateInfo {
        final long currentVersionCode;
        final String currentVersionName;
        final int latestVersionCode;
        final String latestVersionName;
        final String apkUrl;
        final String builtAt;
        final String commit;
        final boolean updateAvailable;

        UpdateInfo(
                long currentVersionCode,
                String currentVersionName,
                int latestVersionCode,
                String latestVersionName,
                String apkUrl,
                String builtAt,
                String commit,
                boolean updateAvailable
        ) {
            this.currentVersionCode = currentVersionCode;
            this.currentVersionName = currentVersionName;
            this.latestVersionCode = latestVersionCode;
            this.latestVersionName = latestVersionName;
            this.apkUrl = apkUrl;
            this.builtAt = builtAt;
            this.commit = commit;
            this.updateAvailable = updateAvailable;
        }
    }
}
