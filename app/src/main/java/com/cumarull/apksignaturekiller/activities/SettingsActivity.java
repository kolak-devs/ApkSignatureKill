package com.cumarull.apksignaturekiller.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.cumarull.apksignaturekiller.App;
import com.cumarull.apksignaturekiller.BuildConfig;
import com.cumarull.apksignaturekiller.R;

import java.io.File;
public class SettingsActivity extends AppCompatActivity {

    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_SIGNING = "signing_version";
    private static final String KEY_KILL = "kill_mode";
    private static final String KEY_OUTPUT = "output_dir";
    private static final String KEY_CHANNEL = "update_channel";

    private SharedPreferences prefs;
    private LinearLayout container;
    private TextView outputSubtitleTv;
    private TextView channelSubtitleTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = App.getPreferences();
        applyThemeMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        container = findViewById(R.id.settings_container);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        buildAppearance();
        buildSigning();
        buildKill();
        buildOutput();
        buildAbout();
    }

    private void buildAppearance() {
        addCategoryHeader(R.string.pref_cat_appearance);
        MaterialCardView card = startCard();
        addListRow(card,
                R.drawable.ic_theme_light,
                R.string.pref_theme_title,
                R.string.pref_theme_summary,
                KEY_THEME,
                R.array.theme_entries,
                R.array.theme_values,
                this::applyThemeMode);
    }

    private void buildSigning() {
        addCategoryHeader(R.string.pref_cat_signing);
        MaterialCardView card = startCard();
        addListRow(card,
                R.drawable.ic_settings,
                R.string.pref_signing_title,
                R.string.pref_signing_summary,
                KEY_SIGNING,
                R.array.signing_entries,
                R.array.signing_values,
                null);
    }

    private void buildKill() {
        addCategoryHeader(R.string.pref_cat_kill);
        MaterialCardView card = startCard();
        addListRow(card,
                R.drawable.ic_settings,
                R.string.pref_kill_mode_title,
                R.string.pref_kill_mode_summary,
                KEY_KILL,
                R.array.kill_mode_entries,
                R.array.kill_mode_values,
                null);
    }

    private void buildOutput() {
        addCategoryHeader(R.string.pref_cat_output);
        MaterialCardView card = startCard();
        addOutputRow(card,
                R.drawable.ic_folder_open,
                R.string.pref_output_dir_title,
                R.string.pref_output_dir_summary,
                KEY_OUTPUT);
    }

    private void buildAbout() {
        addCategoryHeader(R.string.about_updates);

        MaterialCardView card1 = startCard();
        View versionRow = inflateRow(card1,
                R.drawable.ic_info,
                R.string.about_version,
                0);
        ((TextView) versionRow.findViewById(R.id.item_title))
                .setText(getString(R.string.about_version, getString(R.string.app_name)));
        ((TextView) versionRow.findViewById(R.id.item_subtitle))
                .setText(BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ")");
        versionRow.setClickable(false);

        addCategoryHeader(R.string.updates_title);

        MaterialCardView card2 = startCard();

        View channelRow = inflateRow(card2,
                R.drawable.ic_update,
                R.string.pref_update_channel,
                R.string.pref_update_channel_summary);
        channelSubtitleTv = channelRow.findViewById(R.id.item_subtitle);
        channelSubtitleTv.setVisibility(View.VISIBLE);
        String[] channelLabels = getResources().getStringArray(R.array.update_channel_entries);
        String[] channelValues = getResources().getStringArray(R.array.update_channel_values);
        String currentChannel = prefs.getString(KEY_CHANNEL, "stable");
        channelSubtitleTv.setText(getChannelLabel(currentChannel, channelLabels, channelValues));
        channelRow.setOnClickListener(v -> {
            String cur = prefs.getString(KEY_CHANNEL, "stable");
            int checked = getChannelIndex(cur, channelValues);
            new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AlertDialog_Rounded)
                    .setTitle(R.string.pref_update_channel)
                    .setSingleChoiceItems(channelLabels, checked, (d, which) -> {
                        d.dismiss();
                        String value = channelValues[which];
                        prefs.edit().putString(KEY_CHANNEL, value).apply();
                        channelSubtitleTv.setText(channelLabels[which]);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });

        MaterialCardView card3 = startCard();
        View updateRow = inflateRow(card3,
                R.drawable.ic_update,
                R.string.about_check_updates,
                R.string.about_check_updates_sub);
        updateRow.setOnClickListener(v ->
                com.cumarull.apksignaturekiller.update.UpdaterHelper.checkForUpdates(this,
                        getString(R.string.update_url),
                        prefs.getString(KEY_CHANNEL, "stable")));
    }

    private String getChannelLabel(String value, String[] labels, String[] values) {
        int idx = getChannelIndex(value, values);
        return idx >= 0 ? labels[idx] : labels[0];
    }

    private int getChannelIndex(String value, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) return i;
        }
        return 0;
    }

    private void addCategoryHeader(int titleRes) {
        TextView tv = new TextView(this);
        tv.setText(titleRes);
        tv.setTextColor(ContextCompat.getColor(this, R.color.m3_primary));
        tv.setTextSize(14);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setAllCaps(true);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(8, dp(24), 8, dp(8));
        tv.setLayoutParams(lp);
        container.addView(tv);
    }

    private MaterialCardView startCard() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);
        card.setRadius(dp(20));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_surface));
        card.setStrokeWidth(0);
        card.setCardElevation(dp(1));
        container.addView(card);
        return card;
    }

    private void addListRow(MaterialCardView card, int iconRes, int titleRes, int subtitleRes,
                            final String key, int entriesRes, int valuesRes,
                            final Runnable onSaved) {
        View row = inflateRow(card, iconRes, titleRes, subtitleRes);
        final String[] entries = getResources().getStringArray(entriesRes);
        final String[] values = getResources().getStringArray(valuesRes);
        final TextView valueTv = row.findViewById(R.id.item_value);
        valueTv.setText(displayFor(key, entries, values));

        row.setOnClickListener(v -> {
            String cur = prefs.getString(key, null);
            int checked = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(cur)) {
                    checked = i;
                    break;
                }
            }
            new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AlertDialog_Rounded)
                    .setTitle(titleRes)
                    .setSingleChoiceItems(entries, checked, (dialog, which) -> {
                        prefs.edit().putString(key, values[which]).apply();
                        valueTv.setText(entries[which]);
                        dialog.dismiss();
                        if (onSaved != null) onSaved.run();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private static final int REQ_PICK_DIR = 1001;

    private void addOutputRow(MaterialCardView card, int iconRes, int titleRes, int subtitleRes,
                              final String key) {
        View row = inflateRow(card, iconRes, titleRes, 0);
        outputSubtitleTv = row.findViewById(R.id.item_subtitle);
        outputSubtitleTv.setVisibility(View.VISIBLE);
        row.findViewById(R.id.item_value).setVisibility(View.GONE);
        final String defaultValue = getString(R.string.pref_output_dir_default);
        boolean custom = isCustomPath();
        outputSubtitleTv.setText(custom ? R.string.pref_output_custom : R.string.pref_output_default);

        row.setOnClickListener(v -> {
            String[] labels = {
                    getString(R.string.pref_output_default),
                    getString(R.string.pref_output_custom)
            };
            int checked = isCustomPath() ? 1 : 0;
            new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AlertDialog_Rounded)
                    .setTitle(R.string.pref_output_dir_title)
                    .setSingleChoiceItems(labels, checked, (d, which) -> {
                        d.dismiss();
                        if (which == 1) {
                            openFolderPicker();
                        } else {
                            prefs.edit().putString(key, defaultValue).apply();
                            outputSubtitleTv.setText(R.string.pref_output_default);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private boolean isCustomPath() {
        String cur = prefs.getString(KEY_OUTPUT, "");
        return cur != null && !cur.isEmpty()
                && !cur.equals(getString(R.string.pref_output_dir_default));
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_PICK_DIR);
        } catch (Exception e) {
            Toast.makeText(this, R.string.pref_output_pick_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_DIR && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri == null) return;
            try {
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            String path = pathFromTreeUri(treeUri);
            if (path != null && !path.isEmpty()) {
                prefs.edit().putString(KEY_OUTPUT, path).apply();
                if (outputSubtitleTv != null) {
                    outputSubtitleTv.setText(R.string.pref_output_custom);
                }
                Toast.makeText(this, getString(R.string.pref_output_picked, path),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.pref_output_pick_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private String pathFromTreeUri(Uri treeUri) {
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            String[] split = docId.split(":");
            String type = split[0];
            String sub = split.length > 1 ? split[1] : "";
            if ("primary".equals(type)) {
                return new File(Environment.getExternalStorageDirectory(), sub).getAbsolutePath();
            }
            return "/storage/" + type + (sub.isEmpty() ? "" : "/" + sub);
        } catch (Exception e) {
            return null;
        }
    }

    private View inflateRow(MaterialCardView card, int iconRes, int titleRes, int subtitleRes) {
        if (card.getChildCount() > 0) {
            card.addView(makeDivider());
        }
        View row = LayoutInflater.from(this).inflate(R.layout.row_settings_item, card, false);

        ImageView icon = row.findViewById(R.id.item_icon);
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.m3_on_primary_container));

        ((FrameLayout) row.findViewById(R.id.icon_box)).setBackground(makeIconBox());

        TextView titleView = row.findViewById(R.id.item_title);
        if (titleRes != 0) titleView.setText(titleRes);
        TextView subtitleView = row.findViewById(R.id.item_subtitle);
        if (subtitleRes != 0) subtitleView.setText(subtitleRes);

        ImageView chevron = row.findViewById(R.id.item_chevron);
        chevron.setImageResource(R.drawable.ic_chevron_right);

        TextView title = row.findViewById(R.id.item_title);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);

        TextView subtitle = row.findViewById(R.id.item_subtitle);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);

        TextView valueTv = row.findViewById(R.id.item_value);
        valueTv.setSingleLine(true);
        valueTv.setEllipsize(TextUtils.TruncateAt.END);
        valueTv.setMaxWidth(dp(160));

        card.addView(row);
        return row;
    }

    private GradientDrawable makeIconBox() {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(dp(12));
        gd.setColor(ContextCompat.getColor(this, R.color.m3_primary_container));
        return gd;
    }

    private void addSimpleRow(MaterialCardView card, String text) {
        if (card.getChildCount() > 0) {
            card.addView(makeDivider());
        }
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.m3_on_surface));
        tv.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(14), dp(16), dp(14));
        tv.setLayoutParams(lp);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(tv);
    }

    private View makeDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1));
        lp.setMargins(dp(16), 0, dp(16), 0);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.m3_outline_variant));
        return divider;
    }

    private String displayFor(String key, String[] entries, String[] values) {
        String cur = prefs.getString(key, null);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(cur)) {
                return entries[i];
            }
        }
        return entries.length > 0 ? entries[0] : "";
    }

    private void applyThemeMode() {
        String mode = prefs == null
                ? "system"
                : prefs.getString(KEY_THEME, "system");
        int value;
        switch (mode == null ? "system" : mode) {
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
        AppCompatDelegate.setDefaultNightMode(value);
    }

    private int dp(int d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
