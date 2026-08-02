package com.bibliadiaria.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends Activity {
    // Light mode colors
    private static final int COLOR_BG_LIGHT = Color.rgb(247, 248, 246);
    private static final int COLOR_CARD_LIGHT = Color.WHITE;
    private static final int COLOR_INK_LIGHT = Color.rgb(25, 31, 31);
    private static final int COLOR_MUTED_LIGHT = Color.rgb(91, 99, 98);
    private static final int COLOR_STROKE_LIGHT = Color.rgb(219, 226, 222);

    // Dark mode colors
    private static final int COLOR_BG_DARK = Color.rgb(26, 28, 30);
    private static final int COLOR_CARD_DARK = Color.rgb(38, 41, 44);
    private static final int COLOR_INK_DARK = Color.rgb(224, 226, 219);
    private static final int COLOR_MUTED_DARK = Color.rgb(158, 163, 160);
    private static final int COLOR_STROKE_DARK = Color.rgb(58, 62, 65);

    private static final int COLOR_ACCENT = Color.rgb(0, 107, 90);
    private static final int COLOR_WARM = Color.rgb(217, 75, 61);

    private boolean isDarkMode;
    private ExecutorService executor;
    private TextView statusText;
    private TextView installedVersionText;
    private TextView checkButton;
    private TextView downloadButton;
    private TextView voiceSubtitleText;
    private TextView voiceButton;
    private TextView ttsProviderButton;
    private TextView mistralApiKeyButton;
    private UpdateManager.UpdateInfo latestInfo;
    private boolean isEnglish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(bg());
            getWindow().setNavigationBarColor(bg());
        }

        executor = Executors.newSingleThreadExecutor();
        isEnglish = UpdateManager.isEnglish(this);
        isDarkMode = UpdateManager.isDarkMode(this);
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
        root.setBackgroundColor(bg());

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

        TextView title = textView("Settings", 34, ink(), Typeface.BOLD);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(18), 0, dp(18));
        content.addView(title, titleParams);

        content.addView(createLanguageCard());
        content.addView(createTtsProviderCard());
        content.addView(createVoiceCard());
        content.addView(createMistralApiKeyCard());
        content.addView(createVersionCard());

        checkButton = actionButton(isEnglish ? "Check now" : "Comprobar", COLOR_ACCENT, Color.WHITE);
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

        TextView back = textView(isEnglish ? "Extras" : "Extras", 15, COLOR_ACCENT, Typeface.BOLD);
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

        TextView title = textView("Language", 20, ink(), Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        TextView subtitle = textView("Select the language for daily readings", 14, muted(), Typeface.NORMAL);
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

        TextView langToggle = actionButton(UpdateManager.isEnglish(this) ? "English" : "Español", COLOR_ACCENT, Color.WHITE);
        langToggle.setOnClickListener(view -> {
            boolean current = UpdateManager.isEnglish(this);
            UpdateManager.setEnglish(this, !current);
            langToggle.setText(!current ? "English" : "Español");
            isEnglish = !current;
            recreate();
            updateVoiceCard();
        });
        card.addView(langToggle, new LinearLayout.LayoutParams(dp(100), dp(40)));

        return card;
    }

    private View createTtsProviderCard() {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("TTS Provider", 20, ink(), Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        TextView subtitle = textView("Choose between Gleez and Mistral TTS", 14, muted(), Typeface.NORMAL);
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

        ttsProviderButton = actionButton(getTtsProviderLabel(), COLOR_ACCENT, Color.WHITE);
        ttsProviderButton.setOnClickListener(view -> showTtsProviderPicker());
        card.addView(ttsProviderButton, new LinearLayout.LayoutParams(dp(140), dp(40)));

        return card;
    }

    private View createVoiceCard() {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("Voice", 20, ink(), Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        voiceSubtitleText = textView("", 14, muted(), Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(6), 0, 0);
        copy.addView(voiceSubtitleText, subtitleParams);

        card.addView(copy, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        voiceButton = actionButton("", COLOR_ACCENT, Color.WHITE);
        voiceButton.setOnClickListener(view -> showVoicePicker());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        buttonParams.setMargins(0, dp(12), 0, 0);
        card.addView(voiceButton, buttonParams);

        updateVoiceCard();
        return card;
    }

    private View createMistralApiKeyCard() {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("Mistral API Key", 20, ink(), Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        TextView subtitle = textView("Required for Mistral TTS (get from mistral.ai)", 14, muted(), Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.setMargins(0, dp(6), 0, 0);
        copy.addView(subtitle, subtitleParams);

        card.addView(copy, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        mistralApiKeyButton = actionButton(getMistralApiKeyLabel(), COLOR_ACCENT, Color.WHITE);
        mistralApiKeyButton.setOnClickListener(view -> showMistralApiKeyDialog());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        buttonParams.setMargins(0, dp(12), 0, 0);
        card.addView(mistralApiKeyButton, buttonParams);

        return card;
    }

    private View createVersionCard() {
        LinearLayout card = createCard();
        card.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("Version", 20, ink(), Typeface.BOLD);
        title.setIncludeFontPadding(false);
        card.addView(title);

        installedVersionText = textView("", 15, muted(), Typeface.BOLD);
        LinearLayout.LayoutParams installedParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        installedParams.setMargins(0, dp(10), 0, 0);
        card.addView(installedVersionText, installedParams);

        statusText = textView("Ready to check for updates.", 15, muted(), Typeface.NORMAL);
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

    private void showVoicePicker() {
        boolean english = UpdateManager.isEnglish(this);
        String provider = UpdateManager.getTtsProvider(this);
        UpdateManager.VoiceOption[] options = UpdateManager.getTtsVoiceOptions(english, provider);
        String[] labels = new String[options.length];
        for (int index = 0; index < options.length; index++) {
            labels[index] = options[index].label;
        }

        int selectedIndex = UpdateManager.getTtsVoiceIndex(
                english,
                provider,
                UpdateManager.getTtsVoice(this, english, provider)
        );

        new AlertDialog.Builder(this)
                .setTitle(english ? "English voice" : "Spanish voice")
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    UpdateManager.setTtsVoice(this, english, provider, options[which].id);
                    updateVoiceCard();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTtsProviderPicker() {
        String[] providers = {"Gleez", "Mistral"};
        String currentProvider = UpdateManager.getTtsProvider(this);
        int selectedIndex = currentProvider.equals(UpdateManager.TTS_PROVIDER_MISTRAL) ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle("Select TTS Provider")
                .setSingleChoiceItems(providers, selectedIndex, (dialog, which) -> {
                    String provider = which == 1 ? UpdateManager.TTS_PROVIDER_MISTRAL : UpdateManager.TTS_PROVIDER_GLEEZE;
                    UpdateManager.setTtsProvider(this, provider);
                    ttsProviderButton.setText(getTtsProviderLabel());
                    updateVoiceCard();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMistralApiKeyDialog() {
        String currentKey = UpdateManager.getMistralApiKey(this);
        String displayKey = currentKey.isEmpty() ? "" : "••••••••";

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Enter your Mistral API key");
        input.setText(currentKey);
        input.setSelection(currentKey.length());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(20), dp(20), dp(20));
        layout.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        new AlertDialog.Builder(this)
                .setTitle("Mistral API Key")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    UpdateManager.setMistralApiKey(this, input.getText().toString().trim());
                    mistralApiKeyButton.setText(getMistralApiKeyLabel());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getTtsProviderLabel() {
        String provider = UpdateManager.getTtsProvider(this);
        return provider.equals(UpdateManager.TTS_PROVIDER_MISTRAL) ? "Mistral" : "Gleez";
    }

    private String getMistralApiKeyLabel() {
        String apiKey = UpdateManager.getMistralApiKey(this);
        if (apiKey.isEmpty()) {
            return "Not configured";
        }
        return "••••••••";
    }

    private void updateVoiceCard() {
        if (voiceSubtitleText == null || voiceButton == null) {
            return;
        }

        boolean english = UpdateManager.isEnglish(this);
        String provider = UpdateManager.getTtsProvider(this);
        String voiceId = UpdateManager.getTtsVoice(this, english);
        
        // Update subtitle based on provider
        if (provider.equals(UpdateManager.TTS_PROVIDER_MISTRAL)) {
            voiceSubtitleText.setText(english
                    ? "Mistral voices (requires API key)"
                    : "Voces de Mistral (requiere clave API)");
        } else {
            voiceSubtitleText.setText(english
                    ? "Gleez voices are shown while English is selected"
                    : "Las voces de Gleez se muestran mientras el idioma esta seleccionado");
        }
        
        voiceButton.setText(UpdateManager.getTtsVoiceLabel(english, provider, voiceId));
        
        // If Mistral is selected but API key is not configured, show a warning
        if (provider.equals(UpdateManager.TTS_PROVIDER_MISTRAL) && !UpdateManager.isMistralConfigured(this)) {
            voiceSubtitleText.setText(english
                    ? "Mistral voices (requires API key - not configured!)"
                    : "Voces de Mistral (requiere clave API - ¡no configurada!)");
        }
    }

    private void updateInstalledVersionText() {
        try {
            installedVersionText.setText(
                    (isEnglish ? "Installed: " : "Instalada: ")
                            + UpdateManager.getInstalledVersionName(this)
                            + " (" + UpdateManager.getInstalledVersionCode(this) + ")");
        } catch (Exception error) {
            installedVersionText.setText(isEnglish ? "Installed version unavailable." : "Versión instalada no disponible.");
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
        card.setBackground(roundedRect(card(), dp(8), Color.rgb(229, 232, 230), dp(1)));
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

    // --- Dark mode aware color helpers ---
    private int bg() { return isDarkMode ? COLOR_BG_DARK : COLOR_BG_LIGHT; }
    private int card() { return isDarkMode ? COLOR_CARD_DARK : COLOR_CARD_LIGHT; }
    private int ink() { return isDarkMode ? COLOR_INK_DARK : COLOR_INK_LIGHT; }
    private int muted() { return isDarkMode ? COLOR_MUTED_DARK : COLOR_MUTED_LIGHT; }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}