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
            "Ven, Esp\u00edritu Creador,\n"
                    + "visita las almas de tus fieles\n"
                    + "y llena de la gracia divina\n"
                    + "los corazones que t\u00fa mismo creaste.\n\n"
                    + "T\u00fa eres nuestro Consolador,\n"
                    + "don del Dios alt\u00edsimo,\n"
                    + "fuente viva, fuego, caridad\n"
                    + "y espiritual unci\u00f3n.\n\n"
                    + "T\u00fa derramas sobre nosotros\n"
                    + "los siete dones;\n"
                    + "t\u00fa eres el dedo de la diestra del Padre,\n"
                    + "promesa solemne del Padre,\n"
                    + "que pones en nuestros labios la palabra.\n\n"
                    + "Enciende tu luz en nuestras mentes,\n"
                    + "infunde tu amor en nuestros corazones\n"
                    + "y fortalece con tu fuerza constante\n"
                    + "la debilidad de nuestro cuerpo.\n\n"
                    + "Aleja de nosotros al enemigo,\n"
                    + "danos pronto la paz;\n"
                    + "siendo t\u00fa nuestro gu\u00eda,\n"
                    + "evitaremos todo mal.\n\n"
                    + "Por ti conozcamos al Padre,\n"
                    + "y tambi\u00e9n al Hijo;\n"
                    + "y que en ti, Esp\u00edritu de ambos,\n"
                    + "creamos en todo tiempo.\n\n"
                    + "Gloria a Dios Padre,\n"
                    + "y al Hijo que resucit\u00f3,\n"
                    + "y al Esp\u00edritu Consolador,\n"
                    + "por los siglos de los siglos.\n\n"
                    + "Am\u00e9n.";

    private static final String ANGELUS_PRAYER =
            "El \u00c1ngel del Se\u00f1or anunci\u00f3 a Mar\u00eda.\n"
                    + "Y concibi\u00f3 por obra y gracia del Esp\u00edritu Santo.\n\n"
                    + "Dios te salve, Mar\u00eda, llena eres de gracia,\n"
                    + "el Se\u00f1or es contigo.\n"
                    + "Bendita t\u00fa eres entre todas las mujeres,\n"
                    + "y bendito es el fruto de tu vientre, Jes\u00fas.\n"
                    + "Santa Mar\u00eda, Madre de Dios,\n"
                    + "ruega por nosotros, pecadores,\n"
                    + "ahora y en la hora de nuestra muerte.\n"
                    + "Am\u00e9n.\n\n"
                    + "He aqu\u00ed la esclava del Se\u00f1or.\n"
                    + "H\u00e1gase en m\u00ed seg\u00fan tu palabra.\n\n"
                    + "Dios te salve, Mar\u00eda...\n\n"
                    + "Y el Verbo se hizo carne.\n"
                    + "Y habit\u00f3 entre nosotros.\n\n"
                    + "Dios te salve, Mar\u00eda...\n\n"
                    + "Ruega por nosotros, Santa Madre de Dios.\n"
                    + "Para que seamos dignos de alcanzar las promesas de Cristo.\n\n"
                    + "Infunde, Se\u00f1or, tu gracia en nuestros corazones,\n"
                    + "para que quienes hemos conocido por el anuncio del \u00c1ngel\n"
                    + "la encarnaci\u00f3n de tu Hijo Jesucristo,\n"
                    + "por su pasi\u00f3n y cruz seamos llevados\n"
                    + "a la gloria de su resurrecci\u00f3n.\n"
                    + "Por Jesucristo nuestro Se\u00f1or.\n\n"
                    + "Am\u00e9n.";

    private static final String SALVE_REGINA_PRAYER =
            "Dios te salve, Reina y Madre de misericordia,\n"
                    + "vida, dulzura y esperanza nuestra;\n"
                    + "Dios te salve.\n\n"
                    + "A ti llamamos los desterrados hijos de Eva;\n"
                    + "a ti suspiramos, gimiendo y llorando\n"
                    + "en este valle de l\u00e1grimas.\n\n"
                    + "Ea, pues, Se\u00f1ora, abogada nuestra,\n"
                    + "vuelve a nosotros esos tus ojos misericordiosos;\n"
                    + "y despu\u00e9s de este destierro,\n"
                    + "mu\u00e9stranos a Jes\u00fas,\n"
                    + "fruto bendito de tu vientre.\n\n"
                    + "\u00a1Oh clemente, oh piadosa,\n"
                    + "oh dulce Virgen Mar\u00eda!\n\n"
                    + "Ruega por nosotros, Santa Madre de Dios,\n"
                    + "para que seamos dignos de alcanzar\n"
                    + "las promesas de nuestro Se\u00f1or Jesucristo.\n\n"
                    + "Am\u00e9n.";

    private static final String MEMORARE_PRAYER =
            "Acordaos, oh piados\u00edsima Virgen Mar\u00eda,\n"
                    + "que jam\u00e1s se ha o\u00eddo decir\n"
                    + "que ninguno de los que han acudido a vuestra protecci\u00f3n,\n"
                    + "implorado vuestra asistencia\n"
                    + "y reclamado vuestro socorro,\n"
                    + "haya sido abandonado de vos.\n\n"
                    + "Animado con esta confianza,\n"
                    + "a vos tambi\u00e9n acudo,\n"
                    + "oh Madre, Virgen de las v\u00edrgenes;\n"
                    + "y aunque gimiendo bajo el peso de mis pecados,\n"
                    + "me atrevo a comparecer ante vuestra presencia soberana.\n\n"
                    + "No desech\u00e9is, oh Madre de Dios,\n"
                    + "mis humildes s\u00faplicas;\n"
                    + "antes bien, escuchadlas y acogedlas benignamente.\n\n"
                    + "Am\u00e9n.";

    private static final PrayerItem[] PRAYERS = {
            new PrayerItem("Ven, Esp\u00edritu Creador", "Veni Creator Spiritus", VENI_CREATOR_PRAYER),
            new PrayerItem("\u00c1ngelus", "Oraci\u00f3n de la Encarnaci\u00f3n", ANGELUS_PRAYER),
            new PrayerItem("Salve Regina", "Dios te salve, Reina", SALVE_REGINA_PRAYER),
            new PrayerItem("Acordaos", "Memorare", MEMORARE_PRAYER)
    };

    private ScrollView scrollView;
    private LinearLayout listContainer;
    private LinearLayout detailContainer;
    private TextView titleText;
    private PrayerItem selectedPrayer = PRAYERS[0];

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
        for (PrayerItem prayer : PRAYERS) {
            listContainer.addView(createPrayerCard(prayer));
        }
        content.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        detailContainer = new LinearLayout(this);
        detailContainer.setOrientation(LinearLayout.VERTICAL);
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

    private View createPrayerCard(PrayerItem prayer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(16), dp(16));
        card.setBackground(roundedRect(COLOR_CARD, dp(8), Color.rgb(229, 232, 230), dp(1)));
        card.setOnClickListener(view -> showPrayerDetail(prayer));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(1));
        }

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);

        TextView title = textView(prayer.title, 20, COLOR_INK, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        copy.addView(title);

        TextView subtitle = textView(prayer.subtitle, 14, COLOR_MUTED, Typeface.BOLD);
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

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        return card;
    }

    private void renderPrayerDetail() {
        detailContainer.removeAllViews();

        TextView subtitle = textView(selectedPrayer.subtitle, 14, COLOR_WARM, Typeface.BOLD);
        detailContainer.addView(subtitle);

        TextView prayer = textView(selectedPrayer.body, 21, COLOR_INK, Typeface.NORMAL);
        prayer.setLineSpacing(dp(5), 1.14f);
        LinearLayout.LayoutParams prayerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        prayerParams.setMargins(0, dp(16), 0, dp(18));
        detailContainer.addView(prayer, prayerParams);

        TextView backToList = textView("Volver a extras", 15, COLOR_ACCENT, Typeface.BOLD);
        backToList.setGravity(Gravity.CENTER);
        backToList.setPadding(dp(14), 0, dp(14), 0);
        backToList.setBackground(roundedRect(Color.WHITE, dp(8), Color.rgb(219, 226, 222), dp(1)));
        backToList.setOnClickListener(view -> showPrayerList());
        detailContainer.addView(backToList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(42)
        ));
    }

    private void showPrayerDetail(PrayerItem prayer) {
        selectedPrayer = prayer;
        titleText.setText(prayer.title);
        renderPrayerDetail();
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

    private static class PrayerItem {
        final String title;
        final String subtitle;
        final String body;

        PrayerItem(String title, String subtitle, String body) {
            this.title = title;
            this.subtitle = subtitle;
            this.body = body;
        }
    }
}
