package com.cumarull.apksignaturekiller.fragments;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.mcal.apksigner.ApkSigner;
import com.cumarull.apksignaturekiller.App;
import com.cumarull.apksignaturekiller.R;
import com.cumarull.apksignaturekiller.utils.BinSignatureTool;
import com.cumarull.apksignaturekiller.utils.MyAppInfo;
import com.cumarull.apksignaturekiller.utils.SuperSignatureTool;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HomeFragment extends Fragment {
    public static AppCompatEditText apkPath;
    private AppCompatImageView apkIcon;
    private AppCompatTextView apkName;
    private AppCompatTextView apkPack;

    View.OnClickListener radioButtonClickListener = v -> {
        int id = v.getId();
        if (id == R.id.binMtSignatureKill) {
            App.getPreferences().edit()
                    .putBoolean("getBinMtSignatureKill", true)
                    .putBoolean("getSuperSignatureKill", false)
                    .putString("kill_mode", "bin")
                    .apply();
        } else if (id == R.id.superSignatureKill) {
            App.getPreferences().edit()
                    .putBoolean("getSuperSignatureKill", true)
                    .putBoolean("getBinMtSignatureKill", false)
                    .putString("kill_mode", "super")
                    .apply();
        }
    };

    private final ActivityResultLauncher<String> openApkLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pickFile(uri);
                } else {
                    toast("Selection canceled");
                }
            });

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.activity_main, container, false);

        apkIcon = mView.findViewById(R.id.apkIcon);
        apkName = mView.findViewById(R.id.apkName);
        apkPack = mView.findViewById(R.id.apkPackage);
        apkPath = mView.findViewById(R.id.apkPath);

        apkPath.setText(App.getPreferences().getString("ApkPath", ""));
        apkPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void afterTextChanged(Editable p1) {
                if (!p1.toString().isEmpty()) {
                    File apk = new File(p1.toString());
                    if (apk.exists()) {
                        apkIcon.setImageDrawable(new MyAppInfo(getContext(), apk.getAbsolutePath()).getIcon());
                        apkName.setText(MyAppInfo.getAppName());
                        apkPack.setText(MyAppInfo.getPackage());
                    } else {
                        apkIcon.setImageResource(R.mipmap.ic_launcher);
                        apkName.setText("Select apk");
                        apkPack.setText("none");
                    }
                }
            }
        });

        (mView.findViewById(R.id.browseApk)).setOnClickListener(p1 -> {
            browseApk();
        });

        (mView.findViewById(R.id.hookRun)).setOnClickListener(p1 -> {
            hookRun();
        });

        com.google.android.material.radiobutton.MaterialRadioButton binRadio = mView.findViewById(R.id.binMtSignatureKill);
        com.google.android.material.radiobutton.MaterialRadioButton superRadio = mView.findViewById(R.id.superSignatureKill);

        boolean superChecked = "super".equals(App.getPreferences().getString("kill_mode", "bin"));
        binRadio.setChecked(!superChecked);
        superRadio.setChecked(superChecked);

        android.widget.CompoundButton.OnCheckedChangeListener toggleMode = (button, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (button.getId() == R.id.binMtSignatureKill) {
                App.getPreferences().edit()
                        .putBoolean("getBinMtSignatureKill", true)
                        .putBoolean("getSuperSignatureKill", false)
                        .putString("kill_mode", "bin")
                        .apply();
            } else if (button.getId() == R.id.superSignatureKill) {
                App.getPreferences().edit()
                        .putBoolean("getSuperSignatureKill", true)
                        .putBoolean("getBinMtSignatureKill", false)
                        .putString("kill_mode", "super")
                        .apply();
            }
        };
        binRadio.setOnCheckedChangeListener(toggleMode);
        superRadio.setOnCheckedChangeListener(toggleMode);

        return mView;
    }

    public void browseApk() {
        openApkLauncher.launch("application/vnd.android.package-archive");
    }

    private void pickFile(Uri uri) {
        try {
            String displayName = getDisplayName(uri);
            if (displayName == null || displayName.isEmpty()) {
                displayName = "selected.apk";
            }
            displayName = displayName.replaceAll("[/\\\\]", "_")
                    .replaceAll(":", "_");
            if (!displayName.toLowerCase().endsWith(".apk")) {
                displayName = displayName + ".apk";
            }
            File dest = new File(getContext().getCacheDir(), displayName);
            try (InputStream in = getContext().getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            apkPath.setText(dest.getAbsolutePath());
        } catch (Exception e) {
            toast("Error: " + e);
        }
    }

    private String getDisplayName(Uri uri) {
        try (Cursor cursor = getContext().getContentResolver().query(
                uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && cursor.getString(idx) != null) {
                    return cursor.getString(idx);
                }
            }
        } catch (Exception ignored) {
        }
        return uri.getLastPathSegment();
    }

    public void hookRun() {
        String srcApk = apkPath.getText().toString().trim();
        if (srcApk.isEmpty() || !new File(srcApk).exists()) {
            toast(getString(R.string.toast_select_apk_first));
            return;
        }

        AlertDialog progressDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Processing...")
                .setCancelable(false)
                .setView(R.layout.dialog_progress)
                .create();
        progressDialog.show();

        @SuppressLint("HandlerLeak")
        Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                progressDialog.dismiss();
            }
        };

        new Thread() {
            public void run() {
                File outputDir = getOutputDir();
                String baseName = new File(srcApk).getName();
                if (baseName.toLowerCase().endsWith(".apk")) {
                    baseName = baseName.substring(0, baseName.length() - 4);
                }
                String outApk = new File(outputDir, baseName + "_kill.apk").getAbsolutePath();
                String signApk = new File(outputDir, baseName + "_sign.apk").getAbsolutePath();

                Exception error = null;
                try {
                    if ("super".equals(App.getPreferences().getString("kill_mode", "bin"))) {
                        SuperSignatureTool signatureTool = new SuperSignatureTool(getContext());
                        signatureTool.setPath(srcApk, outApk);
                        signatureTool.Kill();
                    } else {
                        BinSignatureTool binSignatureTool = new BinSignatureTool(getContext());
                        binSignatureTool.setPath(srcApk, outApk);
                        binSignatureTool.Kill();
                    }
                    File killedFile = new File(outApk);
                    if (!killedFile.exists() || killedFile.length() == 0) {
                        throw new IOException("Kill() produced no output: " + outApk);
                    }
                    String[] keys = deployTestKey();
                    boolean signed = new ApkSigner().sign(outApk, signApk, keys[0], keys[1]);
                    File signFile = new File(signApk);
                    if (!signed || !signFile.exists() || signFile.length() == 0) {
                        throw new IOException("Signing failed: " + signApk + " was not produced (signed=" + signed + ")");
                    }
                } catch (Exception e) {
                    error = e;
                }

                final Exception finalError = error;
                final String finalSign = signApk;
                mHandler.post(() -> {
                    progressDialog.dismiss();
                    if (finalError != null) {
                        showError(finalError);
                    } else {
                        dialogFinished(finalSign);
                    }
                });
            }
        }.start();
    }

    private File getOutputDir() {
        String dir = App.getPreferences().getString("output_dir", null);
        File out;
        if (dir != null && !dir.trim().isEmpty()) {
            out = new File(dir.trim());
        } else {
            out = new File(Environment.getExternalStorageDirectory(), "ApkSignatureKill/output");
        }
        if (!out.exists()) {
            out.mkdirs();
        }
        return out;
    }

    public void toast(String str) {
        Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
    }

    private void showError(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 6) {
            sb.append(cur.getClass().getSimpleName()).append(": ").append(cur.getMessage()).append('\n');
            cur = cur.getCause();
            depth++;
        }
        String stack = null;
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            stack = sw.toString();
        } catch (Exception ignore) {
        }
        final String log = sb.toString() + "\n\n" + (stack == null ? "" : stack);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Processing failed")
                .setMessage(log)
                .setCancelable(true)
                .setPositiveButton("Ок", null)
                .setNeutralButton("Salin Log", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("ApkSignatureKill log", log));
                    Toast.makeText(requireContext(), "Log disalin", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String[] deployTestKey() throws IOException {
        File dir = new File(getContext().getFilesDir(), "bin");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String pk8 = new File(dir, "testkey.pk8").getAbsolutePath();
        String x509 = new File(dir, "testkey.x509.pem").getAbsolutePath();
        copyAsset("key/testkey.pk8", pk8);
        copyAsset("key/testkey.x509.pem", x509);
        return new String[]{pk8, x509};
    }

    private void copyAsset(String assetName, String destPath) throws IOException {
        try (InputStream in = getContext().getAssets().open(assetName);
             OutputStream out = new FileOutputStream(destPath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    public void dialogFinished(String outputPath) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Processing complete")
                .setMessage("Saved:\n" + outputPath)
                .setCancelable(true)
                .setPositiveButton("Ок", null)
                .show();
    }
}
