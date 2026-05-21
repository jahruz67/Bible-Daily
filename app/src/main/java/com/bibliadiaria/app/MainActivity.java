package com.bibliadiaria.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int COLOR_BACKGROUND = Color.rgb(247, 248, 246);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_INK = Color.rgb(25, 31, 31);
    private static final int COLOR_MUTED = Color.rgb(91, 99, 98);
    private static final int COLOR_ACCENT = Color.rgb(0, 107, 90);
    private static final int COLOR_WARM = Color.rgb(217, 75, 61);
    private static final int MIN_FONT_SP = 16;
    private static final int MAX_FONT_SP = 30;

    private final Locale spanishLocale = new Locale("es", "ES");
    private final List<TextView> resizableTextViews = new ArrayList<>();

    private ExecutorService executor;
    private SharedPreferences preferences;
    private LinearLayout sectionsLayout;
    private TextView dateText;
    private TextView dateChipPrimary;
    private TextView dateChipSecondary;
    private TextView liturgyText;
    private TextView statusText;
    private TextView sourceText;
    private LinearLayout statusBlock;
    private LinearLayout calendarPanel;
    private TextView calendarMonthText;
    private GridLayout calendarGrid;
    private ProgressBar progressBar;
    private Button retryButton;
    private LinearLayout fontPanel;
    private TextView fontValueText;
    private Calendar selectedDateCalendar;
    private Calendar visibleMonthCalendar;
    private int loadGeneration;
    private float readingFontSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BACKGROUND);
            getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        }

        preferences = getSharedPreferences("preferencias_lectura", MODE_PRIVATE);
        readingFontSp = preferences.getFloat("tamano_letra", 20f);
        selectedDateCalendar = startOfDay(Calendar.getInstance());
        visibleMonthCalendar = startOfMonth(selectedDateCalendar);
        executor = Executors.newSingleThreadExecutor();

        setContentView(createScreen());
        loadToday();
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
        content.setPadding(dp(20), dp(28), dp(20), dp(126));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        content.addView(createHeader());
        content.addView(createStatusBlock());

        sectionsLayout = new LinearLayout(this);
        sectionsLayout.setOrientation(LinearLayout.VERTICAL);
        content.addView(sectionsLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        sourceText = textView("Fuente: Vatican News", 13, COLOR_MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sourceParams.setMargins(0, dp(6), 0, 0);
        content.addView(sourceText, sourceParams);

        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        fontPanel = createFontPanel();
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                dp(286),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.BOTTOM
        );
        panelParams.setMargins(dp(16), dp(16), dp(16), dp(88));
        root.addView(fontPanel, panelParams);

        TextView fontButton = createFontButton();
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                dp(58),
                dp(58),
                Gravity.END | Gravity.BOTTOM
        );
        buttonParams.setMargins(0, 0, dp(18), dp(18));
        root.addView(fontButton, buttonParams);

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();

            content.setPadding(dp(20), dp(28) + topInset, dp(20), dp(126) + bottomInset);

            FrameLayout.LayoutParams currentPanelParams =
                    (FrameLayout.LayoutParams) fontPanel.getLayoutParams();
            currentPanelParams.setMargins(dp(16), dp(16), dp(16), dp(88) + bottomInset);
            fontPanel.setLayoutParams(currentPanelParams);

            FrameLayout.LayoutParams currentButtonParams =
                    (FrameLayout.LayoutParams) fontButton.getLayoutParams();
            currentButtonParams.setMargins(0, 0, dp(18), dp(18) + bottomInset);
            fontButton.setLayoutParams(currentButtonParams);

            return insets;
        });
        root.requestApplyInsets();

        return root;
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0, 0, 0, dp(24));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView source = textView("VATICAN NEWS", 12, COLOR_ACCENT, Typeface.BOLD);
        topRow.addView(source, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        topRow.addView(createDateChip());
        header.addView(topRow);

        TextView title = textView("Palabra del día", 34, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(8), 0, dp(10));
        header.addView(title, titleParams);

        LinearLayout.LayoutParams extrasParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
        );
        extrasParams.setMargins(0, 0, 0, dp(12));
        header.addView(createExtrasButton(), extrasParams);

        calendarPanel = createCalendarPanel();
        LinearLayout.LayoutParams calendarParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        calendarParams.setMargins(0, dp(2), 0, dp(14));
        header.addView(calendarPanel, calendarParams);

        dateText = textView("", 16, COLOR_MUTED, Typeface.BOLD);
        header.addView(dateText);

        liturgyText = textView("", 16, COLOR_INK, Typeface.NORMAL);
        liturgyText.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams liturgyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        liturgyParams.setMargins(0, dp(8), 0, 0);
        header.addView(liturgyText, liturgyParams);

        return header;
    }

    private View createExtrasButton() {
        TextView button = textView("Extras", 15, Color.WHITE, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setBackground(roundedRect(COLOR_ACCENT, dp(8), Color.TRANSPARENT, 0));
        button.setContentDescription("Abrir extras");
        button.setOnClickListener(view -> {
            calendarPanel.setVisibility(View.GONE);
            if (fontPanel != null) {
                fontPanel.setVisibility(View.GONE);
            }
            startActivity(new Intent(this, ExtrasActivity.class));
        });
        return button;
    }

    private View createDateChip() {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12), dp(8), dp(10), dp(8));
        chip.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        chip.setContentDescription("Elegir fecha");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            chip.setElevation(dp(1));
        }
        chip.setOnClickListener(view -> toggleCalendarPanel());

        LinearLayout labelGroup = new LinearLayout(this);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        labelGroup.setGravity(Gravity.CENTER_VERTICAL);

        dateChipPrimary = textView("", 20, COLOR_INK, Typeface.BOLD);
        dateChipPrimary.setIncludeFontPadding(false);
        labelGroup.addView(dateChipPrimary);

        dateChipSecondary = textView("", 11, COLOR_MUTED, Typeface.BOLD);
        dateChipSecondary.setIncludeFontPadding(false);
        labelGroup.addView(dateChipSecondary);

        chip.addView(labelGroup);

        TextView chevron = textView("v", 15, COLOR_ACCENT, Typeface.BOLD);
        chevron.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(
                dp(22),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        chevronParams.setMargins(dp(6), 0, 0, 0);
        chip.addView(chevron, chevronParams);

        updateDateChip();
        return chip;
    }

    private LinearLayout createCalendarPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(14));
        panel.setVisibility(View.GONE);
        panel.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            panel.setElevation(dp(4));
        }

        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setOrientation(LinearLayout.HORIZONTAL);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);

        monthRow.addView(calendarNavButton("<", view -> changeVisibleMonth(-1)));

        calendarMonthText = textView("", 18, COLOR_INK, Typeface.BOLD);
        calendarMonthText.setGravity(Gravity.CENTER);
        monthRow.addView(calendarMonthText, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        monthRow.addView(calendarNavButton(">", view -> changeVisibleMonth(1)));
        panel.addView(monthRow);

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        quickParams.setMargins(0, dp(10), 0, dp(6));

        quickRow.addView(smallCalendarButton("- Año", view -> changeVisibleYear(-1)));
        quickRow.addView(smallCalendarButton("Hoy", view -> selectDate(Calendar.getInstance())));
        quickRow.addView(smallCalendarButton("+ Año", view -> changeVisibleYear(1)));
        panel.addView(quickRow, quickParams);

        LinearLayout weekdays = new LinearLayout(this);
        weekdays.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"L", "M", "X", "J", "V", "S", "D"};
        for (String label : labels) {
            TextView dayLabel = textView(label, 12, COLOR_MUTED, Typeface.BOLD);
            dayLabel.setGravity(Gravity.CENTER);
            weekdays.addView(dayLabel, new LinearLayout.LayoutParams(
                    0,
                    dp(24),
                    1f
            ));
        }
        panel.addView(weekdays);

        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setRowCount(6);
        panel.addView(calendarGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        renderCalendar();
        return panel;
    }

    private TextView calendarNavButton(String text, View.OnClickListener listener) {
        TextView button = textView(text, 22, COLOR_ACCENT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundedRect(Color.rgb(229, 244, 240), dp(8), Color.TRANSPARENT, 0));
        button.setOnClickListener(listener);
        button.setContentDescription(text.equals("<") ? "Mes anterior" : "Mes siguiente");
        return button;
    }

    private TextView smallCalendarButton(String text, View.OnClickListener listener) {
        TextView button = textView(text, 13, COLOR_ACCENT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundedRect(Color.rgb(229, 244, 240), dp(8), Color.TRANSPARENT, 0));
        button.setOnClickListener(listener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(36),
                1f
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void toggleCalendarPanel() {
        int nextVisibility = calendarPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        calendarPanel.setVisibility(nextVisibility);
        if (nextVisibility == View.VISIBLE) {
            fontPanel.setVisibility(View.GONE);
            renderCalendar();
        }
    }

    private void changeVisibleMonth(int amount) {
        visibleMonthCalendar.add(Calendar.MONTH, amount);
        visibleMonthCalendar = startOfMonth(visibleMonthCalendar);
        renderCalendar();
    }

    private void changeVisibleYear(int amount) {
        visibleMonthCalendar.add(Calendar.YEAR, amount);
        visibleMonthCalendar = startOfMonth(visibleMonthCalendar);
        renderCalendar();
    }

    private void selectDate(Calendar calendar) {
        selectedDateCalendar = startOfDay(calendar);
        visibleMonthCalendar = startOfMonth(selectedDateCalendar);
        calendarPanel.setVisibility(View.GONE);
        updateDateChip();
        renderCalendar();
        loadSelectedDate();
    }

    private void renderCalendar() {
        if (calendarGrid == null || calendarMonthText == null || visibleMonthCalendar == null) {
            return;
        }

        calendarMonthText.setText(formatMonthTitle(visibleMonthCalendar.getTime()));
        calendarGrid.removeAllViews();

        Calendar monthStart = startOfMonth(visibleMonthCalendar);
        int firstDayOfWeek = monthStart.get(Calendar.DAY_OF_WEEK);
        int firstOffset = (firstDayOfWeek + 5) % 7;
        int daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = startOfDay(Calendar.getInstance());

        for (int cell = 0; cell < 42; cell++) {
            TextView dayCell = textView("", 16, COLOR_INK, Typeface.BOLD);
            dayCell.setGravity(Gravity.CENTER);
            dayCell.setIncludeFontPadding(false);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
            );
            params.width = 0;
            params.height = dp(42);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            dayCell.setLayoutParams(params);

            int dayNumber = cell - firstOffset + 1;
            if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                Calendar cellDate = startOfMonth(visibleMonthCalendar);
                cellDate.set(Calendar.DAY_OF_MONTH, dayNumber);

                dayCell.setText(String.valueOf(dayNumber));
                if (isSameDay(cellDate, selectedDateCalendar)) {
                    dayCell.setTextColor(Color.WHITE);
                    dayCell.setBackground(oval(COLOR_ACCENT));
                } else if (isSameDay(cellDate, today)) {
                    dayCell.setTextColor(COLOR_WARM);
                    dayCell.setBackground(roundedRect(Color.rgb(255, 239, 236), dp(8), Color.TRANSPARENT, 0));
                } else {
                    dayCell.setBackground(roundedRect(Color.TRANSPARENT, dp(8), Color.TRANSPARENT, 0));
                }

                Calendar dateForClick = (Calendar) cellDate.clone();
                dayCell.setOnClickListener(view -> selectDate(dateForClick));
            } else {
                dayCell.setText("");
                dayCell.setEnabled(false);
            }

            calendarGrid.addView(dayCell);
        }
    }

    private View createStatusBlock() {
        statusBlock = new LinearLayout(this);
        statusBlock.setOrientation(LinearLayout.VERTICAL);
        statusBlock.setPadding(dp(18), dp(16), dp(18), dp(16));
        statusBlock.setBackground(roundedRect(COLOR_CARD, dp(8), Color.TRANSPARENT, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statusBlock.setElevation(dp(1));
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        progressBar = new ProgressBar(this);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        progressParams.setMargins(0, 0, dp(12), 0);
        row.addView(progressBar, progressParams);

        statusText = textView("Cargando la Palabra de hoy...", 16, COLOR_INK, Typeface.NORMAL);
        statusText.setLineSpacing(0, 1.16f);
        row.addView(statusText, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        statusBlock.addView(row);

        retryButton = new Button(this);
        retryButton.setText("Reintentar");
        retryButton.setTextColor(Color.WHITE);
        retryButton.setTextSize(14);
        retryButton.setAllCaps(false);
        retryButton.setBackground(roundedRect(COLOR_ACCENT, dp(8), Color.TRANSPARENT, 0));
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(view -> loadSelectedDate());
        LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        retryParams.setMargins(0, dp(14), 0, 0);
        statusBlock.addView(retryButton, retryParams);

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        blockParams.setMargins(0, 0, 0, dp(18));
        statusBlock.setLayoutParams(blockParams);
        return statusBlock;
    }

    private LinearLayout createFontPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setVisibility(View.GONE);
        panel.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(223, 229, 226), dp(1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            panel.setElevation(dp(8));
        }

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = textView("Tamaño de letra", 15, COLOR_INK, Typeface.BOLD);
        labelRow.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        fontValueText = textView(Math.round(readingFontSp) + " sp", 14, COLOR_MUTED, Typeface.BOLD);
        labelRow.addView(fontValueText);
        panel.addView(labelRow);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(MAX_FONT_SP - MIN_FONT_SP);
        seekBar.setProgress(Math.round(readingFontSp) - MIN_FONT_SP);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                readingFontSp = MIN_FONT_SP + progress;
                preferences.edit().putFloat("tamano_letra", readingFontSp).apply();
                fontValueText.setText(Math.round(readingFontSp) + " sp");
                applyReadingFontSize();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        seekParams.setMargins(0, dp(8), 0, 0);
        panel.addView(seekBar, seekParams);

        return panel;
    }

    private TextView createFontButton() {
        TextView button = textView("Aa", 20, Color.WHITE, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription("Cambiar tamaño de letra");
        button.setBackground(oval(COLOR_ACCENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(dp(10));
        }
        button.setOnClickListener(view -> {
            int nextVisibility = fontPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            fontPanel.setVisibility(nextVisibility);
            if (nextVisibility == View.VISIBLE) {
                calendarPanel.setVisibility(View.GONE);
            }
        });
        return button;
    }

    private void loadToday() {
        selectedDateCalendar = startOfDay(Calendar.getInstance());
        visibleMonthCalendar = startOfMonth(selectedDateCalendar);
        updateDateChip();
        renderCalendar();
        loadSelectedDate();
    }

    private void loadSelectedDate() {
        loadDate(selectedDateCalendar.getTime());
    }

    private void loadDate(Date date) {
        String url = buildVaticanUrl(date);
        int generation = ++loadGeneration;

        showLoading(date, url);

        executor.submit(() -> {
            try {
                String html = download(url);
                DailyReading reading = parseDailyReading(html, date, url);
                runOnUiThread(() -> {
                    if (generation == loadGeneration) {
                        renderReading(reading);
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (generation == loadGeneration) {
                        showError(error, url);
                    }
                });
            }
        });
    }

    private void showLoading(Date date, String url) {
        resizableTextViews.clear();
        sectionsLayout.removeAllViews();
        statusBlock.setVisibility(View.VISIBLE);
        dateText.setText(formatDisplayDate(date));
        liturgyText.setText("Lecturas según la fecha del dispositivo.");
        statusText.setText("Cargando la Palabra de hoy...");
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        sourceText.setText("Fuente: Vatican News\n" + url);
    }

    private void renderReading(DailyReading reading) {
        statusBlock.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        statusText.setText("Actualizado desde Vatican News.");

        resizableTextViews.clear();
        sectionsLayout.removeAllViews();

        dateText.setText(reading.displayDate);
        if (reading.liturgicalTitle.isEmpty()) {
            liturgyText.setText("Palabra del día");
        } else {
            liturgyText.setText(reading.liturgicalTitle);
        }

        addScriptureCard("Palabra de Dios", "Evangelio del Día", reading.gospel, true);
        addScriptureCard("Lectura del Día", null, reading.firstReading, false);
        addReflectionCard(reading.papalWords);

        sourceText.setText("Fuente: Vatican News\n" + reading.sourceUrl);
        applyReadingFontSize();
    }

    private void showError(Exception error, String url) {
        statusBlock.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        sectionsLayout.removeAllViews();
        statusText.setText("No se pudo cargar la lectura de hoy. Revisa la conexión e intenta de nuevo.\n\n"
                + cleanErrorMessage(error));
        sourceText.setText("Fuente prevista: Vatican News\n" + url);
    }

    private void addScriptureCard(String title, String overline, DailySection section, boolean featured) {
        if (section == null || section.body.isEmpty()) {
            return;
        }

        LinearLayout card = createCard(featured ? dp(22) : dp(18));

        if (overline != null && !overline.isEmpty()) {
            TextView overlineText = textView(overline.toUpperCase(spanishLocale), 12, COLOR_WARM, Typeface.BOLD);
            card.addView(overlineText);
        }

        TextView titleText = textView(title, featured ? 28 : 22, COLOR_INK, Typeface.BOLD);
        titleText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, overline == null ? 0 : dp(8), 0, dp(12));
        card.addView(titleText, titleParams);

        if (!section.introduction.isEmpty()) {
            TextView introText = textView(section.introduction, 16, COLOR_MUTED, Typeface.BOLD);
            introText.setLineSpacing(0, 1.14f);
            resizableTextViews.add(introText);
            card.addView(introText);
        }

        if (!section.reference.isEmpty()) {
            TextView referenceText = textView(section.reference, 15, COLOR_ACCENT, Typeface.BOLD);
            referenceText.setGravity(Gravity.CENTER_VERTICAL);
            referenceText.setBackground(roundedRect(Color.rgb(229, 244, 240), dp(8), Color.TRANSPARENT, 0));
            referenceText.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams referenceParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            referenceParams.setMargins(0, dp(12), 0, dp(14));
            card.addView(referenceText, referenceParams);
        }

        TextView bodyText = textView(section.body, Math.round(readingFontSp), COLOR_INK, Typeface.NORMAL);
        bodyText.setLineSpacing(dp(4), 1.16f);
        resizableTextViews.add(bodyText);
        card.addView(bodyText);

        addCardToSections(card);
    }

    private void addReflectionCard(DailySection section) {
        if (section == null || section.body.isEmpty()) {
            return;
        }

        LinearLayout card = createCard(dp(18));

        TextView title = textView("Las palabras de los Papas", 22, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        card.addView(title);

        TextView body = textView(section.body, Math.round(readingFontSp), COLOR_INK, Typeface.NORMAL);
        body.setLineSpacing(dp(4), 1.16f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyParams.setMargins(0, dp(14), 0, 0);
        card.addView(body, bodyParams);
        resizableTextViews.add(body);

        addCardToSections(card);
    }

    private LinearLayout createCard(int padding) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(padding, padding, padding, padding);
        card.setBackground(roundedRect(COLOR_CARD, dp(8), Color.rgb(229, 232, 230), dp(1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }
        return card;
    }

    private void addCardToSections(View card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(16));
        sectionsLayout.addView(card, params);
    }

    private void applyReadingFontSize() {
        for (TextView textView : resizableTextViews) {
            textView.setTextSize(readingFontSp);
        }
    }

    private String download(String urlText) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("User-Agent", "BibliaDiaria/1.0 Android");
        connection.setRequestProperty("Accept-Language", "es-ES,es;q=0.9");

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("Vatican News respondió con código " + responseCode + ".");
        }

        try (InputStream stream = connection.getInputStream()) {
            return readStream(stream);
        } finally {
            connection.disconnect();
        }
    }

    private String readStream(InputStream stream) throws IOException {
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

    private DailyReading parseDailyReading(String html, Date date, String url) throws IOException {
        String firstReadingText = extractHtmlContentByHeading(html, "Lectura del Día");
        String gospelText = extractHtmlContentByHeading(html, "Evangelio del Día");
        String papalText = extractHtmlContentByHeading(html, "Las palabras de los Papas");
        String plainText = htmlToPlainText(html);

        if (firstReadingText.isEmpty()) {
            firstReadingText = extractSection(plainText, "Lectura del Día", "Evangelio del Día");
        }
        if (gospelText.isEmpty()) {
            gospelText = extractSection(plainText, "Evangelio del Día", "Las palabras de los Papas");
        }
        if (papalText.isEmpty()) {
            papalText = extractSection(
                    plainText,
                    "Las palabras de los Papas",
                    "Su contribución",
                    "Los textos de la Sagrada Escritura",
                    "Enviar",
                    "Otros eventos programados"
            );
        }

        DailySection firstReading = DailySection.fromScripture(firstReadingText);
        DailySection gospel = DailySection.fromScripture(gospelText);
        DailySection papalWords = DailySection.reflection(cleanSectionText(papalText));

        if (gospel.body.isEmpty() && firstReading.body.isEmpty() && papalWords.body.isEmpty()) {
            throw new IOException("No encontré las secciones esperadas en la página.");
        }

        return new DailyReading(
                formatDisplayDate(date),
                extractLiturgicalTitle(html, plainText, date),
                url,
                firstReading,
                gospel,
                papalWords
        );
    }

    private String extractHtmlContentByHeading(String html, String heading) {
        String targetKey = headingKey(heading);
        String lowerHtml = html.toLowerCase(Locale.US);
        int searchFrom = 0;

        while (searchFrom < html.length()) {
            int h2Start = lowerHtml.indexOf("<h2", searchFrom);
            if (h2Start < 0) {
                return "";
            }

            int h2OpenEnd = html.indexOf('>', h2Start);
            int h2Close = lowerHtml.indexOf("</h2>", h2OpenEnd + 1);
            if (h2OpenEnd < 0 || h2Close < 0) {
                return "";
            }

            String h2Text = htmlFragmentToText(html.substring(h2OpenEnd + 1, h2Close));
            if (headingKey(h2Text).equals(targetKey)) {
                int sectionStart = lowerHtml.lastIndexOf("<section", h2Start);
                int sectionEnd = lowerHtml.indexOf("</section>", h2Close);
                if (sectionStart < 0 || sectionEnd < 0) {
                    return "";
                }
                sectionEnd += "</section>".length();

                int contentTagStart = findTagWithClass(
                        html,
                        "div",
                        "section__content",
                        h2Close,
                        sectionEnd
                );
                if (contentTagStart < 0) {
                    return "";
                }

                int contentOpenEnd = html.indexOf('>', contentTagStart);
                int contentClose = findMatchingClosingTag(html, contentTagStart, "div", sectionEnd);
                if (contentOpenEnd < 0 || contentClose < 0 || contentClose <= contentOpenEnd) {
                    return "";
                }

                return cleanSectionText(htmlFragmentToText(html.substring(contentOpenEnd + 1, contentClose)));
            }

            searchFrom = h2Close + "</h2>".length();
        }

        return "";
    }

    private int findTagWithClass(String html, String tagName, String className, int start, int end) {
        String lowerHtml = html.toLowerCase(Locale.US);
        String tagStart = "<" + tagName.toLowerCase(Locale.US);
        String classKey = className.toLowerCase(Locale.US);
        int searchFrom = start;

        while (searchFrom >= 0 && searchFrom < end) {
            int tagIndex = lowerHtml.indexOf(tagStart, searchFrom);
            if (tagIndex < 0 || tagIndex >= end) {
                return -1;
            }

            int tagEnd = html.indexOf('>', tagIndex);
            if (tagEnd < 0 || tagEnd >= end) {
                return -1;
            }

            String openingTag = lowerHtml.substring(tagIndex, tagEnd + 1);
            if (openingTag.contains("class=") && openingTag.contains(classKey)) {
                return tagIndex;
            }

            searchFrom = tagEnd + 1;
        }

        return -1;
    }

    private int findMatchingClosingTag(String html, int openingTagStart, String tagName, int limit) {
        String lowerHtml = html.toLowerCase(Locale.US);
        String openToken = "<" + tagName.toLowerCase(Locale.US);
        String closeToken = "</" + tagName.toLowerCase(Locale.US) + ">";
        int depth = 0;
        int searchFrom = openingTagStart;

        while (searchFrom >= 0 && searchFrom < limit) {
            int nextOpen = lowerHtml.indexOf(openToken, searchFrom);
            int nextClose = lowerHtml.indexOf(closeToken, searchFrom);

            if (nextClose < 0 || nextClose >= limit) {
                return -1;
            }

            if (nextOpen >= 0 && nextOpen < nextClose && nextOpen < limit) {
                int openEnd = html.indexOf('>', nextOpen);
                if (openEnd < 0 || openEnd >= limit) {
                    return -1;
                }
                String openingTag = html.substring(nextOpen, openEnd + 1).trim();
                if (!openingTag.endsWith("/>")) {
                    depth++;
                }
                searchFrom = openEnd + 1;
            } else {
                depth--;
                if (depth == 0) {
                    return nextClose;
                }
                searchFrom = nextClose + closeToken.length();
            }
        }

        return -1;
    }

    private String htmlFragmentToText(String html) {
        return htmlToPlainText(html);
    }

    private String htmlToPlainText(String html) {
        String scrubbed = html
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<noscript[^>]*>.*?</noscript>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)</h[1-6]>", "\n");

        Spanned spanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(scrubbed, Html.FROM_HTML_MODE_LEGACY);
        } else {
            spanned = Html.fromHtml(scrubbed);
        }

        return normalizeWhitespace(spanned.toString());
    }

    private String extractSection(String text, String startHeading, String... endHeadings) {
        int start = text.indexOf(startHeading);
        if (start < 0) {
            return "";
        }
        start += startHeading.length();

        int end = text.length();
        for (String endHeading : endHeadings) {
            int candidate = text.indexOf(endHeading, start);
            if (candidate >= 0 && candidate < end) {
                end = candidate;
            }
        }

        return cleanSectionText(text.substring(start, end));
    }

    private String extractLiturgicalTitle(String html, String text, Date date) {
        String htmlTitle = extractLiturgicalTitleFromHtml(html);
        if (!htmlTitle.isEmpty()) {
            return htmlTitle;
        }

        String dateToken = new SimpleDateFormat("dd/MM/yyyy", spanishLocale).format(date);
        int start = text.indexOf("Fecha" + dateToken);
        if (start >= 0) {
            start += ("Fecha" + dateToken).length();
        } else {
            start = text.indexOf("Fecha");
            if (start < 0) {
                return "";
            }
            start += "Fecha".length();
        }

        int end = text.indexOf("La Palabra del día es", start);
        if (end < 0) {
            end = text.indexOf("Lectura del Día", start);
        }
        if (end < 0 || end <= start) {
            return "";
        }

        String title = text.substring(start, end)
                .replaceFirst("^\\s*\\d{1,2}/\\d{1,2}/\\d{4}\\s*", "");
        return cleanSectionText(title);
    }

    private String extractLiturgicalTitleFromHtml(String html) {
        int containerStart = findTagWithClass(
                html,
                "div",
                "indicazioneLiturgica",
                0,
                html.length()
        );
        if (containerStart < 0) {
            return "";
        }

        int containerOpenEnd = html.indexOf('>', containerStart);
        int containerClose = findMatchingClosingTag(html, containerStart, "div", html.length());
        if (containerOpenEnd < 0 || containerClose < 0) {
            return "";
        }

        return cleanSectionText(htmlFragmentToText(html.substring(containerOpenEnd + 1, containerClose)));
    }

    private String cleanSectionText(String text) {
        String cleaned = normalizeWhitespace(text)
                .replace("Cookie Policy", "")
                .replace("I AGREE", "")
                .trim();
        cleaned = cleaned.replaceAll("(?m)^\\s*[•*]\\s*", "");
        return cleaned.trim();
    }

    private String normalizeWhitespace(String text) {
        String normalized = text.replace('\u00A0', ' ')
                .replace("\r", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ");
        return normalized.trim();
    }

    private String headingKey(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalizeWhitespace(normalized).toLowerCase(Locale.US);
    }

    private String cleanErrorMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Error desconocido.";
        }
        return message.trim();
    }

    private String buildVaticanUrl(Date date) {
        String pathDate = new SimpleDateFormat("yyyy/MM/dd", Locale.US).format(date);
        return "https://www.vaticannews.va/es/evangelio-de-hoy/" + pathDate + ".html";
    }

    private String formatDisplayDate(Date date) {
        return new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", spanishLocale).format(date);
    }

    private String formatMonthTitle(Date date) {
        return new SimpleDateFormat("MMMM yyyy", spanishLocale).format(date);
    }

    private void updateDateChip() {
        if (dateChipPrimary == null || dateChipSecondary == null || selectedDateCalendar == null) {
            return;
        }

        Date selectedDate = selectedDateCalendar.getTime();
        dateChipPrimary.setText(new SimpleDateFormat("EEE d", spanishLocale).format(selectedDate));
        dateChipSecondary.setText(new SimpleDateFormat("MMM yyyy", spanishLocale).format(selectedDate));
    }

    private Calendar startOfDay(Calendar calendar) {
        Calendar copy = (Calendar) calendar.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy;
    }

    private Calendar startOfMonth(Calendar calendar) {
        Calendar copy = startOfDay(calendar);
        copy.set(Calendar.DAY_OF_MONTH, 1);
        return copy;
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first != null
                && second != null
                && first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private TextView textView(String text, int sizeSp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setIncludeFontPadding(true);
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

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class DailyReading {
        final String displayDate;
        final String liturgicalTitle;
        final String sourceUrl;
        final DailySection firstReading;
        final DailySection gospel;
        final DailySection papalWords;

        DailyReading(
                String displayDate,
                String liturgicalTitle,
                String sourceUrl,
                DailySection firstReading,
                DailySection gospel,
                DailySection papalWords
        ) {
            this.displayDate = displayDate;
            this.liturgicalTitle = liturgicalTitle;
            this.sourceUrl = sourceUrl;
            this.firstReading = firstReading;
            this.gospel = gospel;
            this.papalWords = papalWords;
        }
    }

    private static class DailySection {
        final String introduction;
        final String reference;
        final String body;

        DailySection(String introduction, String reference, String body) {
            this.introduction = introduction;
            this.reference = reference;
            this.body = body;
        }

        static DailySection fromScripture(String sectionText) {
            List<String> lines = nonEmptyLines(sectionText);
            if (lines.size() >= 3) {
                String introduction = lines.get(0);
                String reference = lines.get(1);
                StringBuilder body = new StringBuilder();
                for (int i = 2; i < lines.size(); i++) {
                    if (body.length() > 0) {
                        body.append("\n\n");
                    }
                    body.append(lines.get(i));
                }
                return new DailySection(introduction, reference, body.toString().trim());
            }
            return new DailySection("", "", sectionText.trim());
        }

        static DailySection reflection(String body) {
            return new DailySection("", "", body.trim());
        }

        private static List<String> nonEmptyLines(String text) {
            List<String> lines = new ArrayList<>();
            String[] rawLines = text.split("\\n+");
            for (String rawLine : rawLines) {
                String line = rawLine.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            return lines;
        }
    }
}
