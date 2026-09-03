package com.cumarull.apksignaturekiller.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.MaterialToolbar;
import com.cumarull.apksignaturekiller.App;
import com.cumarull.apksignaturekiller.R;
import com.cumarull.apksignaturekiller.adapters.ViewPagerAdapter;
import com.cumarull.apksignaturekiller.fragments.HomeFragment;
import com.cumarull.apksignaturekiller.utils.DensityUtil;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    private ViewPager viewPager;

    @Override
    public void onCreate(Bundle bundle) {
        applyThemeMode();
        super.onCreate(bundle);
        setContentView(R.layout.main);

        setupToolbar(getString(R.string.app_name));
        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        requestStorageAccess();
    }

    private void applyThemeMode() {
        SharedPreferences prefs = App.getPreferences();
        String themeMode = prefs.getString("theme_mode", null);
        int value;
        if (themeMode == null) {
            // Legacy: fall back to the old night_mode boolean toggle.
            value = prefs.getBoolean("night_mode", false)
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            switch (themeMode) {
                case "light":
                    value = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case "dark":
                    value = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                default:
                    value = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    break;
            }
        }
        AppCompatDelegate.setDefaultNightMode(value);
    }

    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 30) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this)
                    .setTitle("Storage access")
                    .setMessage("Allow access to all files to save the kill result to /ApkSignatureKill/output")
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startActivity(new Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception e) {
                                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        boolean dark = isDarkThemeMode();
        menu.findItem(R.id.action_night_mode).setIcon(dark
                ? R.drawable.ic_theme_dark
                : R.drawable.ic_theme_light);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean isDarkThemeMode() {
        SharedPreferences prefs = App.getPreferences();
        String themeMode = prefs.getString("theme_mode", null);
        if (themeMode == null) {
            return prefs.getBoolean("night_mode", false);
        }
        if ("dark".equals(themeMode)) return true;
        if ("light".equals(themeMode)) return false;
        int uiMode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("WrongConstant")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_night_mode) {
            SharedPreferences prefs = App.getPreferences();
            boolean dark = isDarkThemeMode();
            // Toggle between dark and light.
            prefs.edit().putString("theme_mode", dark ? "light" : "dark").apply();
            AppCompatDelegate.setDefaultNightMode(dark
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES);
            getDelegate().applyDayNight();
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void about() {
        Dialog bottomDialog = new Dialog(this, R.style.BottomDialog);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout contentView = new LinearLayout(this);
        contentView.setBackgroundResource(R.drawable.shape_dialog);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setPadding(40, 0, 40, 0);
        contentView.setLayoutParams(layoutParams);
        final AppCompatTextView msg = new AppCompatTextView(this);
        msg.setText(R.string.about);
        contentView.addView(msg);

        bottomDialog.setContentView(contentView);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels - DensityUtil.dp2px(this, 16f);
        params.bottomMargin = DensityUtil.dp2px(this, 8f);
        contentView.setLayoutParams(params);
        bottomDialog.setCanceledOnTouchOutside(true);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
        bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        bottomDialog.show();
    }

    private void donate() {
        Dialog bottomDialog = new Dialog(this, R.style.BottomDialog);

        View contentView = LayoutInflater.from(this).inflate(R.layout.dialog_content_donate_circle, null);
        ClipboardManager cmb = (ClipboardManager)this.getSystemService ( Context.CLIPBOARD_SERVICE );

        (contentView.findViewById(R.id.qiwi)).setOnClickListener(p1 -> {
            cmb.setText("4693 9575 5605 5692");
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG ).show ( );
        });

        (contentView.findViewById(R.id.visa)).setOnClickListener(p1 -> {
            cmb.setText("4276 3200 1538 3012");
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG ).show ( );
        });

        (contentView.findViewById(R.id.paypal)).setOnClickListener(p1 -> {
            String url = "https://www.paypal.me/timscriptov";
            Intent intent1 = new Intent(Intent.ACTION_VIEW);
            intent1.setData(Uri.parse(url));
            startActivity(intent1);
        });

        bottomDialog.setContentView(contentView);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) contentView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels - DensityUtil.dp2px(this, 16f);
        params.bottomMargin = DensityUtil.dp2px(this, 8f);
        contentView.setLayoutParams(params);
        bottomDialog.setCanceledOnTouchOutside(true);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
        bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        bottomDialog.show();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(String title) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setSubtitle("Signature Kill");
    }

    private void setupViewPager(@NotNull ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment(), "MAIN");
        viewPager.setAdapter(adapter);
    }
}
