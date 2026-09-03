package com.cumarull.apksignaturekiller.update;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.cumarull.apksignaturekiller.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdaterHelper {

    public static final class UpdateInfo {
        public int currentVersion;
        public String downloadUrl;
        public String changelog;
    }

    private UpdaterHelper() {
    }

    public static void checkForUpdates(@NonNull Context context, @NonNull String jsonUrl) {
        checkForUpdates(context, jsonUrl, "stable");
    }

    public static void checkForUpdates(@NonNull Context context, @NonNull String jsonUrl,
                                       @NonNull String channel) {
        String url = jsonUrl.trim();
        if (url.isEmpty()) {
            Toast.makeText(context, R.string.update_no_url, Toast.LENGTH_LONG).show();
            return;
        }
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, R.string.update_no_internet, Toast.LENGTH_LONG).show();
            return;
        }
        new CheckTask(context, url, channel).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static boolean isNetworkAvailable(Context context) {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo net = cm == null ? null : cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    private static final class CheckTask extends AsyncTask<Void, Void, UpdateInfo> {
        private final WeakReference<Context> contextRef;
        private final String url;
        private final String channel;

        CheckTask(Context context, String url, String channel) {
            this.contextRef = new WeakReference<>(context);
            this.url = url;
            this.channel = channel == null || channel.isEmpty() ? "stable" : channel;
        }

        @Override
        protected UpdateInfo doInBackground(Void... voids) {
            try {
                URL target = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) target.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                connection.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                JSONObject channelObj = json.optJSONObject(channel);
                if (channelObj == null) {
                    channelObj = json;
                }
                UpdateInfo info = new UpdateInfo();
                info.currentVersion = channelObj.optInt("current_version", 0);
                info.downloadUrl = channelObj.optString("download_url", "");
                info.changelog = channelObj.optString("changelog", "");
                return info;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(UpdateInfo info) {
            Context context = contextRef.get();
            if (context == null) {
                return;
            }
            if (info == null || info.downloadUrl.isEmpty()) {
                Toast.makeText(context, R.string.update_not_found, Toast.LENGTH_LONG).show();
                return;
            }
            int localVersion;
            try {
                localVersion = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionCode;
            } catch (Exception ignored) {
                localVersion = 0;
            }
            if (info.currentVersion <= localVersion) {
                Toast.makeText(context, R.string.update_not_found, Toast.LENGTH_LONG).show();
                return;
            }

            new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_AlertDialog_Rounded)
                    .setTitle(R.string.update_title)
                    .setMessage(info.changelog)
                    .setPositiveButton(R.string.update_download, (d, w) ->
                            UpdateDownloader.download(context, info.downloadUrl))
                    .setNegativeButton(R.string.update_close, (d, w) -> d.cancel())
                    .show();
        }
    }
}
