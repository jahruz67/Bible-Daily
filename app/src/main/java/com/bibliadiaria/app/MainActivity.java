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
    private static final int COLOR_HOLIDAY_BG = Color.rgb(255, 246, 229);
    private static final int COLOR_HOLIDAY_BORDER = Color.rgb(229, 166, 72);
    private static final int COLOR_HOLIDAY_TEXT = Color.rgb(143, 76, 22);
    private static final int MIN_FONT_SP = 16;
    private static final int MAX_FONT_SP = 30;

    private static final int RANK_HOLY_DAY = 0;
    private static final int RANK_MEMORIAL = 1;
    private static final int RANK_FEAST = 2;
    private static final int RANK_SOLEMNITY = 3;

    private final Locale spanishLocale = new Locale("es", "ES");
    private final List<TextView> resizableTextViews = new ArrayList<>();

    private ExecutorService executor;
    private SharedPreferences preferences;
    private LinearLayout sectionsLayout;
    private TextView dateText;
    private TextView dateChipPrimary;
    private TextView dateChipSecondary;
    private TextView liturgyText;
    private LinearLayout holidayTagsRow;
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
    private boolean isEnglish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BACKGROUND);
            getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        }

        preferences = getSharedPreferences("preferencias_lectura", MODE_PRIVATE);
        readingFontSp = preferences.getFloat("tamano_letra", 20f);
        isEnglish = preferences.getBoolean("is_english", false);
        selectedDateCalendar = startOfDay(Calendar.getInstance());
        visibleMonthCalendar = startOfMonth(selectedDateCalendar);
        executor = Executors.newSingleThreadExecutor();

        setContentView(createScreen());
        loadToday();
        checkForUpdateOnStartup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean currentLanguage = UpdateManager.isEnglish(this);
        if (currentLanguage != isEnglish) {
            isEnglish = currentLanguage;
            recreate();
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

        TextView title = textView(isEnglish ? "Word of the day" : "Palabra del día", 34, COLOR_INK, Typeface.BOLD);
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

        holidayTagsRow = new LinearLayout(this);
        holidayTagsRow.setOrientation(LinearLayout.HORIZONTAL);
        holidayTagsRow.setGravity(Gravity.CENTER_VERTICAL);
        holidayTagsRow.setVisibility(View.GONE);
        LinearLayout.LayoutParams tagsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tagsParams.setMargins(0, dp(8), 0, 0);
        header.addView(holidayTagsRow, tagsParams);

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
        button.setContentDescription(isEnglish ? "Open extras" : "Abrir extras");
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
        chip.setContentDescription(isEnglish ? "Choose date" : "Elegir fecha");
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
                LiturgicalDay holiday = knownCatholicHoliday(cellDate);

                dayCell.setText(String.valueOf(dayNumber));
                if (isSameDay(cellDate, selectedDateCalendar)) {
                    dayCell.setTextColor(Color.WHITE);
                    dayCell.setBackground(oval(holiday == null ? COLOR_ACCENT : COLOR_WARM));
                } else if (holiday != null) {
                    dayCell.setTextColor(COLOR_HOLIDAY_TEXT);
                    dayCell.setBackground(roundedRect(COLOR_HOLIDAY_BG, dp(8), COLOR_HOLIDAY_BORDER, dp(1)));
                } else if (isSameDay(cellDate, today)) {
                    dayCell.setTextColor(COLOR_WARM);
                    dayCell.setBackground(roundedRect(Color.rgb(255, 239, 236), dp(8), Color.TRANSPARENT, 0));
                } else {
                    dayCell.setBackground(roundedRect(Color.TRANSPARENT, dp(8), Color.TRANSPARENT, 0));
                }
                if (holiday != null) {
                    dayCell.setContentDescription(dayNumber + ", " + holiday.displayName(isEnglish));
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

    private void updateHolidayTags(LiturgicalDay holiday) {
        if (holidayTagsRow == null) {
            return;
        }

        holidayTagsRow.removeAllViews();
        if (holiday == null) {
            holidayTagsRow.setVisibility(View.GONE);
            return;
        }

        holidayTagsRow.setVisibility(View.VISIBLE);
        holidayTagsRow.addView(createHolidayTag(holiday.rankLabel(isEnglish)));
        holidayTagsRow.addView(createHolidayTag(isEnglish ? "Catholic holiday" : "Fiesta cat\u00f3lica"));
    }

    private TextView createHolidayTag(String text) {
        Locale locale = isEnglish ? Locale.US : spanishLocale;
        TextView tag = textView(text.toUpperCase(locale), 11, COLOR_HOLIDAY_TEXT, Typeface.BOLD);
        tag.setGravity(Gravity.CENTER);
        tag.setIncludeFontPadding(false);
        tag.setPadding(dp(10), dp(6), dp(10), dp(6));
        tag.setBackground(roundedRect(COLOR_HOLIDAY_BG, dp(8), COLOR_HOLIDAY_BORDER, dp(1)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, dp(6), 0);
        tag.setLayoutParams(params);
        return tag;
    }

    private LiturgicalDay holidayFromLiturgicalTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        String key = headingKey(title);
        int rank = classifyLiturgicalTitle(key);
        if (rank < 0) {
            return null;
        }
        return new LiturgicalDay(title, title, rank);
    }

    private int classifyLiturgicalTitle(String key) {
        if (key.contains("solemnity") || key.contains("solemnidad")) {
            return RANK_SOLEMNITY;
        }
        if (key.contains("feast") || key.contains("fiesta")) {
            return RANK_FEAST;
        }
        if (key.contains("memorial") || key.contains("memoria")) {
            return RANK_MEMORIAL;
        }
        if (key.contains("ash wednesday")
                || key.contains("miercoles de ceniza")
                || key.contains("palm sunday")
                || key.contains("domingo de ramos")
                || key.contains("holy thursday")
                || key.contains("jueves santo")
                || key.contains("good friday")
                || key.contains("viernes santo")
                || key.contains("holy saturday")
                || key.contains("sabado santo")
                || key.contains("easter sunday")
                || key.contains("domingo de pascua")
                || key.contains("octave of easter")
                || key.contains("octava de pascua")
                || key.contains("pentecost")
                || key.contains("pentecostes")
                || key.contains("first sunday of advent")
                || key.contains("i domingo de adviento")) {
            return RANK_HOLY_DAY;
        }
        return -1;
    }

    private LiturgicalDay knownCatholicHoliday(Calendar date) {
        if (date == null) {
            return null;
        }

        int year = date.get(Calendar.YEAR);
        Calendar easter = easterSunday(year);

        LiturgicalDay movable = holidayOnOffset(date, easter, -46,
                "Ash Wednesday",
                "Mi\u00e9rcoles de Ceniza",
                RANK_HOLY_DAY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, -7,
                "Palm Sunday of the Passion of the Lord",
                "Domingo de Ramos de la Pasi\u00f3n del Se\u00f1or",
                RANK_HOLY_DAY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, -3,
                "Holy Thursday",
                "Jueves Santo",
                RANK_HOLY_DAY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, -2,
                "Good Friday",
                "Viernes Santo",
                RANK_HOLY_DAY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, -1,
                "Holy Saturday",
                "S\u00e1bado Santo",
                RANK_HOLY_DAY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 0,
                "Easter Sunday of the Resurrection of the Lord",
                "Domingo de Pascua de la Resurrecci\u00f3n del Se\u00f1or",
                RANK_SOLEMNITY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 7,
                "Second Sunday of Easter",
                "II Domingo de Pascua",
                RANK_HOLY_DAY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 39,
                "Solemnity of the Ascension of the Lord",
                "Solemnidad de la Ascensi\u00f3n del Se\u00f1or",
                RANK_SOLEMNITY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 49,
                "Pentecost",
                "Pentecost\u00e9s",
                RANK_SOLEMNITY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 50,
                "Memorial of the Blessed Virgin Mary, Mother of the Church",
                "Memoria de la Bienaventurada Virgen Mar\u00eda, Madre de la Iglesia",
                RANK_MEMORIAL);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 56,
                "Solemnity of the Most Holy Trinity",
                "Solemnidad de la Sant\u00edsima Trinidad",
                RANK_SOLEMNITY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 60,
                "Solemnity of the Most Holy Body and Blood of Christ",
                "Solemnidad del Sant\u00edsimo Cuerpo y Sangre de Cristo",
                RANK_SOLEMNITY);
        if (movable != null) {
            return movable;
        }
        movable = holidayOnOffset(date, easter, 68,
                "Solemnity of the Most Sacred Heart of Jesus",
                "Solemnidad del Sagrado Coraz\u00f3n de Jes\u00fas",
                RANK_SOLEMNITY);
        if (movable != null) {
            return movable;
        }

        Calendar advent = firstSundayOfAdvent(year);
        if (isSameDay(date, shiftedDate(advent, -7))) {
            return new LiturgicalDay(
                    "Solemnity of Our Lord Jesus Christ, King of the Universe",
                    "Solemnidad de Nuestro Se\u00f1or Jesucristo, Rey del Universo",
                    RANK_SOLEMNITY
            );
        }
        if (isSameDay(date, advent)) {
            return new LiturgicalDay(
                    "First Sunday of Advent",
                    "I Domingo de Adviento",
                    RANK_HOLY_DAY
            );
        }

        Calendar baptism = sundayOnOrAfter(year, Calendar.JANUARY, 7);
        if (isSameDay(date, baptism)) {
            return new LiturgicalDay(
                    "Feast of the Baptism of the Lord",
                    "Fiesta del Bautismo del Se\u00f1or",
                    RANK_FEAST
            );
        }

        Calendar holyFamily = holyFamilySunday(year);
        if (isSameDay(date, holyFamily)) {
            return new LiturgicalDay(
                    "Feast of the Holy Family",
                    "Fiesta de la Sagrada Familia",
                    RANK_FEAST
            );
        }

        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DAY_OF_MONTH);

        if (month == Calendar.JANUARY && day == 1) {
            return new LiturgicalDay(
                    "Solemnity of Mary, the Holy Mother of God",
                    "Solemnidad de Santa Mar\u00eda, Madre de Dios",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.JANUARY && day == 6) {
            return new LiturgicalDay(
                    "Solemnity of the Epiphany of the Lord",
                    "Solemnidad de la Epifan\u00eda del Se\u00f1or",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.FEBRUARY && day == 2) {
            return new LiturgicalDay(
                    "Feast of the Presentation of the Lord",
                    "Fiesta de la Presentaci\u00f3n del Se\u00f1or",
                    RANK_FEAST
            );
        }
        if (month == Calendar.MARCH && day == 19) {
            return new LiturgicalDay(
                    "Solemnity of Saint Joseph",
                    "Solemnidad de San Jos\u00e9",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.MARCH && day == 25) {
            return new LiturgicalDay(
                    "Solemnity of the Annunciation of the Lord",
                    "Solemnidad de la Anunciaci\u00f3n del Se\u00f1or",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.MAY && day == 31) {
            return new LiturgicalDay(
                    "Feast of the Visitation of the Blessed Virgin Mary",
                    "Fiesta de la Visitaci\u00f3n de la Bienaventurada Virgen Mar\u00eda",
                    RANK_FEAST
            );
        }
        if (month == Calendar.JUNE && day == 24) {
            return new LiturgicalDay(
                    "Solemnity of the Nativity of Saint John the Baptist",
                    "Solemnidad de la Natividad de San Juan Bautista",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.JUNE && day == 29) {
            return new LiturgicalDay(
                    "Solemnity of Saints Peter and Paul",
                    "Solemnidad de San Pedro y San Pablo",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.AUGUST && day == 6) {
            return new LiturgicalDay(
                    "Feast of the Transfiguration of the Lord",
                    "Fiesta de la Transfiguraci\u00f3n del Se\u00f1or",
                    RANK_FEAST
            );
        }
        if (month == Calendar.AUGUST && day == 15) {
            return new LiturgicalDay(
                    "Solemnity of the Assumption of the Blessed Virgin Mary",
                    "Solemnidad de la Asunci\u00f3n de la Bienaventurada Virgen Mar\u00eda",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.SEPTEMBER && day == 14) {
            return new LiturgicalDay(
                    "Feast of the Exaltation of the Holy Cross",
                    "Fiesta de la Exaltaci\u00f3n de la Santa Cruz",
                    RANK_FEAST
            );
        }
        if (month == Calendar.NOVEMBER && day == 1) {
            return new LiturgicalDay(
                    "Solemnity of All Saints",
                    "Solemnidad de Todos los Santos",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.NOVEMBER && day == 2) {
            return new LiturgicalDay(
                    "Commemoration of All the Faithful Departed",
                    "Conmemoraci\u00f3n de Todos los Fieles Difuntos",
                    RANK_HOLY_DAY
            );
        }
        if (month == Calendar.NOVEMBER && day == 9) {
            return new LiturgicalDay(
                    "Feast of the Dedication of the Lateran Basilica",
                    "Fiesta de la Dedicaci\u00f3n de la Bas\u00edlica de Letr\u00e1n",
                    RANK_FEAST
            );
        }
        if (month == Calendar.DECEMBER && day == 8) {
            return new LiturgicalDay(
                    "Solemnity of the Immaculate Conception",
                    "Solemnidad de la Inmaculada Concepci\u00f3n",
                    RANK_SOLEMNITY
            );
        }
        if (month == Calendar.DECEMBER && day == 12) {
            return new LiturgicalDay(
                    "Feast of Our Lady of Guadalupe",
                    "Fiesta de Nuestra Se\u00f1ora de Guadalupe",
                    RANK_FEAST
            );
        }
        if (month == Calendar.DECEMBER && day == 25) {
            return new LiturgicalDay(
                    "Solemnity of the Nativity of the Lord",
                    "Solemnidad de la Natividad del Se\u00f1or",
                    RANK_SOLEMNITY
            );
        }

        return null;
    }

    private LiturgicalDay holidayOnOffset(
            Calendar date,
            Calendar baseDate,
            int dayOffset,
            String englishName,
            String spanishName,
            int rank
    ) {
        if (isSameDay(date, shiftedDate(baseDate, dayOffset))) {
            return new LiturgicalDay(englishName, spanishName, rank);
        }
        return null;
    }

    private Calendar calendarFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return startOfDay(calendar);
    }

    private Calendar shiftedDate(Calendar date, int dayOffset) {
        Calendar shifted = startOfDay(date);
        shifted.add(Calendar.DAY_OF_YEAR, dayOffset);
        return shifted;
    }

    private Calendar easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        Calendar easter = Calendar.getInstance();
        easter.clear();
        easter.set(year, month - 1, day);
        return startOfDay(easter);
    }

    private Calendar firstSundayOfAdvent(int year) {
        return sundayOnOrAfter(year, Calendar.NOVEMBER, 27);
    }

    private Calendar sundayOnOrAfter(int year, int month, int day) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(year, month, day);
        date = startOfDay(date);
        while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            date.add(Calendar.DAY_OF_YEAR, 1);
        }
        return date;
    }

    private Calendar holyFamilySunday(int year) {
        Calendar date = calendarDate(year, Calendar.DECEMBER, 26);
        while (date.get(Calendar.DAY_OF_MONTH) <= 31) {
            if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                return date;
            }
            date.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendarDate(year, Calendar.DECEMBER, 30);
    }

    private Calendar calendarDate(int year, int month, int day) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(year, month, day);
        return startOfDay(date);
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

    private void checkForUpdateOnStartup() {
        // Auto updates are disabled as per user request. 
        // We only keep the manual check in SettingsActivity.
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
        LiturgicalDay localHoliday = knownCatholicHoliday(calendarFromDate(date));
        updateHolidayTags(localHoliday);
        if (localHoliday == null) {
            liturgyText.setText(isEnglish ? "Readings according to device date." : "Lecturas según la fecha del dispositivo.");
        } else {
            liturgyText.setText(localHoliday.displayName(isEnglish));
        }
        statusText.setText(isEnglish ? "Loading today's Word..." : "Cargando la Palabra de hoy...");
        progressBar.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        sourceText.setText((isEnglish ? "Source: " : "Fuente: ") + "Vatican News\n" + url);
    }

    private void renderReading(DailyReading reading) {
        statusBlock.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        statusText.setText(isEnglish ? "Updated from Vatican News." : "Actualizado desde Vatican News.");

        resizableTextViews.clear();
        sectionsLayout.removeAllViews();

        dateText.setText(reading.displayDate);
        if (reading.liturgicalTitle.isEmpty()) {
            liturgyText.setText(isEnglish ? "Word of the day" : "Palabra del día");
        } else {
            liturgyText.setText(reading.liturgicalTitle);
        }
        LiturgicalDay websiteHoliday = holidayFromLiturgicalTitle(reading.liturgicalTitle);
        updateHolidayTags(websiteHoliday == null ? knownCatholicHoliday(selectedDateCalendar) : websiteHoliday);

        if (isEnglish) {
            addScriptureCard("Word of God", "Gospel of the Day", reading.gospel, true);
            addScriptureCard("Reading of the Day", null, reading.firstReading, false);
            addReflectionCard(reading.papalWords);
            sourceText.setText("Source: Vatican News\n" + reading.sourceUrl);
        } else {
            addScriptureCard("Palabra de Dios", "Evangelio del Día", reading.gospel, true);
            addScriptureCard("Lectura del Día", null, reading.firstReading, false);
            addReflectionCard(reading.papalWords);
            sourceText.setText("Fuente: Vatican News\n" + reading.sourceUrl);
        }
        applyReadingFontSize();
    }

    private void showError(Exception error, String url) {
        statusBlock.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        retryButton.setText(isEnglish ? "Retry" : "Reintentar");
        sectionsLayout.removeAllViews();
        statusText.setText(isEnglish ? "Could not load today's reading. Check your connection and try again.\n\n" : "No se pudo cargar la lectura de hoy. Revisa la conexión e intenta de nuevo.\n\n"
                + cleanErrorMessage(error));
        sourceText.setText((isEnglish ? "Intended source: " : "Fuente prevista: ") + "Vatican News\n" + url);
    }

    private void addScriptureCard(String title, String overline, DailySection section, boolean featured) {
        if (section == null || section.body.isEmpty()) {
            return;
        }

        LinearLayout card = createCard(featured ? dp(22) : dp(18));

        if (overline != null && !overline.isEmpty()) {
            TextView overlineText = textView(overline.toUpperCase(isEnglish ? Locale.US : spanishLocale), 12, COLOR_WARM, Typeface.BOLD);
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

        TextView title = textView(isEnglish ? "The Pope's words" : "Las palabras de los Papas", 22, COLOR_INK, Typeface.BOLD);
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
        connection.setRequestProperty("Accept-Language", isEnglish ? "en-US,en;q=0.9" : "es-ES,es;q=0.9");

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException(isEnglish ? "Vatican News responded with code " + responseCode + "." : "Vatican News respondió con código " + responseCode + ".");
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
        String firstReadingHeading = isEnglish ? "Reading of the Day" : "Lectura del Día";
        String gospelHeading = isEnglish ? "Gospel of the Day" : "Evangelio del Día";
        String papalHeading = isEnglish ? "The Pope's words" : "Las palabras de los Papas";

        String firstReadingText = extractHtmlContentByHeading(html, firstReadingHeading);
        String gospelText = extractHtmlContentByHeading(html, gospelHeading);
        String papalText = extractHtmlContentByHeading(html, papalHeading);
        String plainText = htmlToPlainText(html);

        if (firstReadingText.isEmpty()) {
            firstReadingText = extractSection(plainText, firstReadingHeading, gospelHeading);
        }
        if (gospelText.isEmpty()) {
            gospelText = extractSection(plainText, gospelHeading, papalHeading);
        }
        if (papalText.isEmpty()) {
            if (isEnglish) {
                papalText = extractSection(
                        plainText,
                        papalHeading,
                        "His contribution",
                        "The texts of the Holy Scripture",
                        "Send",
                        "Other scheduled events"
                );
            } else {
                papalText = extractSection(
                        plainText,
                        papalHeading,
                        "Su contribución",
                        "Los textos de la Sagrada Escritura",
                        "Enviar",
                        "Otros eventos programados"
                );
            }
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

        String dateToken = new SimpleDateFormat("dd/MM/yyyy", isEnglish ? Locale.US : spanishLocale).format(date);
        String dateLabel = isEnglish ? "Date" : "Fecha";
        int start = text.indexOf(dateLabel + dateToken);
        if (start >= 0) {
            start += (dateLabel + dateToken).length();
        } else {
            start = text.indexOf(dateLabel);
            if (start < 0) {
                return "";
            }
            start += dateLabel.length();
        }

        int end = text.indexOf(isEnglish ? "Today's Word is" : "La Palabra del día es", start);
        if (end < 0) {
            end = text.indexOf(isEnglish ? "Reading of the Day" : "Lectura del Día", start);
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
        String langPath = isEnglish ? "en" : "es";
        String pageName = isEnglish ? "word-of-the-day" : "evangelio-de-hoy";
        return "https://www.vaticannews.va/" + langPath + "/" + pageName + "/" + pathDate + ".html";
    }

    private String formatDisplayDate(Date date) {
        if (isEnglish) {
            return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(date);
        }
        return new SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", spanishLocale).format(date);
    }

    private String formatMonthTitle(Date date) {
        return new SimpleDateFormat("MMMM yyyy", isEnglish ? Locale.US : spanishLocale).format(date);
    }

    private void updateDateChip() {
        if (dateChipPrimary == null || dateChipSecondary == null || selectedDateCalendar == null) {
            return;
        }

        Date selectedDate = selectedDateCalendar.getTime();
        Locale locale = isEnglish ? Locale.US : spanishLocale;
        dateChipPrimary.setText(new SimpleDateFormat("EEE d", locale).format(selectedDate));
        dateChipSecondary.setText(new SimpleDateFormat("MMM yyyy", locale).format(selectedDate));
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

    private static class LiturgicalDay {
        final String englishName;
        final String spanishName;
        final int rank;

        LiturgicalDay(String englishName, String spanishName, int rank) {
            this.englishName = englishName;
            this.spanishName = spanishName;
            this.rank = rank;
        }

        String displayName(boolean isEnglish) {
            return isEnglish ? englishName : spanishName;
        }

        String rankLabel(boolean isEnglish) {
            switch (rank) {
                case RANK_SOLEMNITY:
                    return isEnglish ? "Solemnity" : "Solemnidad";
                case RANK_FEAST:
                    return isEnglish ? "Feast" : "Fiesta";
                case RANK_MEMORIAL:
                    return isEnglish ? "Memorial" : "Memoria";
                default:
                    return isEnglish ? "Holy day" : "D\u00eda santo";
            }
        }
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
