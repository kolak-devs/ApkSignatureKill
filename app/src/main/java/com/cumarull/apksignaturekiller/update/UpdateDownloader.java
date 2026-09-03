package com.cumarull.apksignaturekiller.update;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.cumarull.apksignaturekiller.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateDownloader {

    private static final String PROVIDER_AUTHORITY = "com.mcal.apkkiller.fileprovider";

    private UpdateDownloader() {
    }

    public static void download(@NonNull Context context, @NonNull String url) {
        new DownloadTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
    }

    private static final class DownloadTask extends AsyncTask<String, Integer, Boolean> {
        private final WeakReference<Context> contextRef;
        private final AlertDialog dialog;
        private final LinearProgressIndicator progressBar;
        private final CircularProgressIndicator spinner;
        private final TextView percentText;
        private String filePath;
        private String fileName;

        DownloadTask(Context context) {
            this.contextRef = new WeakReference<>(context);
            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_download_progress, null);
            this.progressBar = view.findViewById(R.id.downloadBar);
            this.spinner = view.findViewById(R.id.downloadSpin);
            this.percentText = view.findViewById(R.id.downloadPercent);
            this.dialog = new MaterialAlertDialogBuilder(context,
                    R.style.ThemeOverlay_AlertDialog_Rounded)
                    .setTitle(R.string.update_downloading)
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Context context = contextRef.get();
            if (context == null) {
                return;
            }
            try {
                dialog.show();
            } catch (Exception ignored) {
            }
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            String url = urls[0];
            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return false;
                }
                int fileLength = connection.getContentLength();
                String filename = url.substring(url.lastIndexOf('/') + 1);
                if (filename.isEmpty()) {
                    filename = "signaturekill_update.apk";
                }
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
                    return false;
                }
                File out = new File(dir, filename);
                filePath = out.getAbsolutePath();
                fileName = filename;
                publishProgress(0);

                input = connection.getInputStream();
                output = new FileOutputStream(out, false);
                byte[] buffer = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (isCancelled()) {
                        return false;
                    }
                    total += count;
                    if (fileLength > 0) {
                        publishProgress((int) ((100 * total) / fileLength));
                    }
                    output.write(buffer, 0, count);
                }
                output.flush();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (fileName != null) {
                dialog.setTitle(fileName);
            }
            spinner.setVisibility(View.GONE);
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgressCompat(values[0], false);
            progressBar.setVisibility(View.VISIBLE);
            percentText.setText(values[0] + "%");
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Context context = contextRef.get();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (context == null) {
                return;
            }
            if (success == null || !success || filePath == null) {
                Toast.makeText(context, R.string.update_no_url, Toast.LENGTH_LONG).show();
                return;
            }
            if (!(context instanceof Activity)) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uriFromFile(context, new File(filePath)),
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, R.string.update_no_url, Toast.LENGTH_LONG).show();
            }
        }

        private static Uri uriFromFile(Context context, File file) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file);
            }
            return Uri.fromFile(file);
        }
    }
}
