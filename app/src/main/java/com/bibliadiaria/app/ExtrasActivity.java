package com.bibliadiaria.app;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;

public class ExtrasActivity extends Activity {
    private static final int COLOR_BACKGROUND = Color.rgb(247, 248, 246);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_INK = Color.rgb(25, 31, 31);
    private static final int COLOR_MUTED = Color.rgb(91, 99, 98);
    private static final int COLOR_ACCENT = Color.rgb(0, 107, 90);
    private static final int COLOR_WARM = Color.rgb(217, 75, 61);

    private static final String VENI_CREATOR_PRAYER =
            "Ven, Espíritu Creador,\n"
                    + "visita las almas de tus fieles\n"
                    + "y llena de la gracia divina\n"
                    + "los corazones que tú mismo creaste.\n\n"
                    + "Tú eres nuestro Consolador,\n"
                    + "don del Dios altísimo,\n"
                    + "fuente viva, fuego, caridad\n"
                    + "y espiritual unción.\n\n"
                    + "Tú derramas sobre nosotros\n"
                    + "los siete dones;\n"
                    + "tú eres el dedo de la diestra del Padre,\n"
                    + "promesa solemne del Padre,\n"
                    + "que pones en nuestros labios la palabra.\n\n"
                    + "Enciende tu luz en nuestras mentes,\n"
                    + "infunde tu amor en nuestros corazones\n"
                    + "y fortalece con tu fuerza constante\n"
                    + "la debilidad de nuestro cuerpo.\n\n"
                    + "Aleja de nosotros al enemigo,\n"
                    + "danos pronto la paz;\n"
                    + "siendo tú nuestro guía,\n"
                    + "evitaremos todo mal.\n\n"
                    + "Por ti conozcamos al Padre,\n"
                    + "y también al Hijo;\n"
                    + "y que en ti, Espíritu de ambos,\n"
                    + "creamos en todo tiempo.\n\n"
                    + "Gloria a Dios Padre,\n"
                    + "y al Hijo que resucitó,\n"
                    + "y al Espíritu Consolador,\n"
                    + "por los siglos de los siglos.\n\n"
                    + "Amén.";

    private ScrollView scrollView;
    private LinearLayout listContainer;
    private LinearLayout detailContainer;
    private TextView titleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BACKGROUND);
            getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        }

        setContentView(createScreen());
    }

    @Override
    public void onBackPressed() {
        if (detailContainer != null && detailContainer.getVisibility() == View.VISIBLE) {
            showPrayerList();
            return;
        }
        super.onBackPressed();
    }

    private View createScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BACKGROUND);

        scrollView = new ScrollView(this);
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

        titleText = textView("Extras", 34, COLOR_INK, Typeface.BOLD);
        titleText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(18), 0, dp(18));
        content.addView(titleText, titleParams);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.addView(createPrayerCard());
        content.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        detailContainer = createPrayerDetail();
        detailContainer.setVisibility(View.GONE);
        content.addView(detailContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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

        TextView label = textView("ORACIONES", 12, COLOR_ACCENT, Typeface.BOLD);
        topBar.addView(label, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView settings = textView("Ajustes", 15, COLOR_ACCENT, Typeface.BOLD);
        settings.setGravity(Gravity.CENTER);
        settings.setPadding(dp(14), 0, dp(14), 0);
        settings.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        settings.setOnClickListener(view -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
        );
        settingsParams.setMargins(0, 0, dp(8), 0);
        topBar.addView(settings, settingsParams);

        TextView home = textView("Inicio", 15, COLOR_ACCENT, Typeface.BOLD);
        home.setGravity(Gravity.CENTER);
        home.setPadding(dp(14), 0, dp(14), 0);
        home.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        home.setOnClickListener(view -> finish());
        topBar.addView(home, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
        ));

        return topBar;
    }

    private View createPrayerCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(16), dp(16));
        card.setBackground(roundedRect(COLOR_CARD, dp(8), Color.rgb(229, 232, 230), dp(1)));
        card.setOnClickListener(view -> showPrayerDetail());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView("Ven, Espíritu Creador", 20, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        TextView subtitle = textView("Veni Creator Spiritus", 14, COLOR_MUTED, Typeface.BOLD);
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

        TextView arrow = textView(">", 22, COLOR_ACCENT, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(
                dp(28),
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        return card;
    }

    private LinearLayout createPrayerDetail() {
        LinearLayout detail = new LinearLayout(this);
        detail.setOrientation(LinearLayout.VERTICAL);

        TextView subtitle = textView("Veni Creator Spiritus", 14, COLOR_WARM, Typeface.BOLD);
        detail.addView(subtitle);

        TextView prayer = textView(VENI_CREATOR_PRAYER, 21, COLOR_INK, Typeface.NORMAL);
        prayer.setLineSpacing(dp(5), 1.14f);
        LinearLayout.LayoutParams prayerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        prayerParams.setMargins(0, dp(16), 0, dp(18));
        detail.addView(prayer, prayerParams);

        TextView backToList = textView("Volver a extras", 15, COLOR_ACCENT, Typeface.BOLD);
        backToList.setGravity(Gravity.CENTER);
        backToList.setPadding(dp(14), 0, dp(14), 0);
        backToList.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        backToList.setOnClickListener(view -> showPrayerList());
        detail.addView(backToList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        ));

        return detail;
    }

    private void showPrayerDetail() {
        titleText.setText("Ven, Espíritu Creador");
        listContainer.setVisibility(View.GONE);
        detailContainer.setVisibility(View.VISIBLE);
        scrollToTop();
    }

    private void showPrayerList() {
        titleText.setText("Extras");
        detailContainer.setVisibility(View.GONE);
        listContainer.setVisibility(View.VISIBLE);
        scrollToTop();
    }

    private void scrollToTop() {
        if (scrollView != null) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
        }
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
