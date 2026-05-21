package com.bibliadiaria.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    private static final int COLOR_BACKGROUND = Color.rgb(247, 248, 246);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_INK = Color.rgb(25, 31, 31);
    private static final int COLOR_MUTED = Color.rgb(91, 99, 98);
    private static final int COLOR_ACCENT = Color.rgb(0, 107, 90);
    private static final int COLOR_WARM = Color.rgb(217, 75, 61);

    private ExecutorService executor;
    private TextView statusText;
    private TextView installedVersionText;
    private TextView checkButton;
    private TextView downloadButton;
    private UpdateManager.UpdateInfo latestInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BACKGROUND);
            getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        }

        executor = Executors.newSingleThreadExecutor();
        setContentView(createScreen());
        updateInstalledVersionText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private View createScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BACKGROUND);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(28));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        content.addView(createTopBar());

        TextView title = textView("Settings", 34, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(18), 0, dp(18));
        content.addView(title, titleParams);

        content.addView(createLanguageCard());
        content.addView(createVersionCard());

        checkButton = actionButton("Check now", COLOR_ACCENT, Color.WHITE);
        checkButton.setOnClickListener(view -> checkForUpdate());
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        checkParams.setMargins(0, dp(16), 0, 0);
        content.addView(checkButton, checkParams);

        downloadButton = actionButton("Download latest APK", COLOR_WARM, Color.WHITE);
        downloadButton.setVisibility(View.GONE);
        downloadButton.setOnClickListener(view -> {
            String url = latestInfo == null ? UpdateManager.APK_URL : latestInfo.apkUrl;
            UpdateManager.openDownload(this, url);
        });
        LinearLayout.LayoutParams downloadParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
        );
        downloadParams.setMargins(0, dp(10), 0, 0);
        content.addView(downloadButton, downloadParams);

        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(
                    dp(20),
                    dp(28) + insets.getSystemWindowInsetTop(),
                    dp(20),
                    dp(28) + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        root.requestApplyInsets();

        return root;
    }

    private View createTopBar() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = textView("APP", 12, COLOR_ACCENT, Typeface.BOLD);
        topBar.addView(label, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView back = textView("Extras", 15, COLOR_ACCENT, Typeface.BOLD);
        back.setGravity(Gravity.CENTER);
        back.setPadding(dp(14), 0, dp(14), 0);
        back.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        back.setOnClickListener(view -> finish());
        topBar.addView(back, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
        ));

        return topBar;
    }

    private View createLanguageCard() {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("Language", 20, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        TextView subtitle = textView("Select the language for daily readings", 14, COLOR_MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(6), 0, 0);
        copy.addView(subtitle, subtitleParams);

        card.addView(copy, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView langToggle = actionButton(UpdateManager.isEnglish(this) ? "English" : "Spanish", COLOR_ACCENT, Color.WHITE);
        langToggle.setOnClickListener(view -> {
            boolean current = UpdateManager.isEnglish(this);
            UpdateManager.setEnglish(this, !current);
            langToggle.setText(!current ? "English" : "Spanish");
        });
        card.addView(langToggle, new LinearLayout.LayoutParams(dp(100), dp(40)));

        return card;
    }

    private View createVersionCard() {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("Version", 20, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        card.addView(title);

        installedVersionText = textView("", 15, COLOR_MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams installedParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        installedParams.setMargins(0, dp(10), 0, 0);
        card.addView(installedVersionText, installedParams);

        statusText = textView("Ready to check for updates.", 15, COLOR_MUTED, Typeface.NORMAL);
        statusText.setLineSpacing(dp(3), 1.1f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, dp(8), 0, 0);
        card.addView(statusText, statusParams);

        return card;
    }

    private void checkForUpdate() {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        latestInfo = null;
        downloadButton.setVisibility(View.GONE);
        checkButton.setEnabled(false);
        checkButton.setAlpha(0.62f);
        statusText.setText("Checking for updates...");

        executor.submit(() -> {
            try {
                UpdateManager.UpdateInfo info = UpdateManager.fetchLatestUpdate(this);
                runOnUiThread(() -> showUpdateResult(info));
            } catch (Exception error) {
                runOnUiThread(() -> showUpdateError(error));
            }
        });
    }

    private void showUpdateResult(UpdateManager.UpdateInfo info) {
        latestInfo = info;
        checkButton.setEnabled(true);
        checkButton.setAlpha(1f);

        if (info.updateAvailable) {
            String version = info.latestVersionName.isEmpty()
                    ? String.valueOf(info.latestVersionCode)
                    : info.latestVersionName + " (" + info.latestVersionCode + ")";
            statusText.setText("Update available: " + version);
            downloadButton.setVisibility(View.VISIBLE);
            return;
        }

        statusText.setText("You are on the latest build: "
                + info.currentVersionName + " (" + info.currentVersionCode + ")");
    }

    private void showUpdateError(Exception error) {
        checkButton.setEnabled(true);
        checkButton.setAlpha(1f);
        statusText.setText("Could not check for updates. " + cleanMessage(error));
    }

    private void updateInstalledVersionText() {
        try {
            installedVersionText.setText("Installed: "
                    + UpdateManager.getInstalledVersionName(this)
                    + " (" + UpdateManager.getInstalledVersionCode(this) + ")");
        } catch (Exception error) {
            installedVersionText.setText("Installed version unavailable.");
        }
    }

    private String cleanMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Unknown error.";
        }
        return message.trim();
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(roundedRect(COLOR_CARD, dp(8), Color.rgb(229, 232, 230), dp(1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        return card;
    }

    private TextView actionButton(String text, int backgroundColor, int textColor) {
        TextView button = textView(text, 15, textColor, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackground(roundedRect(backgroundColor, dp(8), Color.TRANSPARENT, 0));
        return button;
    }

    private TextView textView(String text, int sizeSp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    private GradientDrawable roundedRect(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
