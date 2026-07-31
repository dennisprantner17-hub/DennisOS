package de.dennis.dennisos;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HoroscopeActivity extends AppBaseActivity {

    private static final String[] SIGNS = {
            "Widder", "Stier", "Zwillinge", "Krebs", "Löwe", "Jungfrau",
            "Waage", "Skorpion", "Schütze", "Steinbock", "Wassermann", "Fische"
    };

    private static final String[] GENERAL = {
            "Heute lohnt es sich, eine Sache bewusst zu Ende zu bringen.",
            "Ein ruhiger Blick auf die Details bringt überraschende Klarheit.",
            "Eine spontane Idee kann heute neuen Schwung in den Alltag bringen.",
            "Nimm dir Zeit für das, was dir wirklich wichtig ist.",
            "Ein offenes Gespräch sorgt heute für gute Stimmung.",
            "Heute ist ein guter Tag, Ordnung in eine offene Angelegenheit zu bringen."
    };

    private static final String[] LOVE = {
            "Ehrliche Aufmerksamkeit zählt heute mehr als große Worte.",
            "Gemeinsame Zeit stärkt eine wichtige Verbindung.",
            "Mit etwas Geduld lässt sich ein Missverständnis leicht lösen.",
            "Ein freundlicher erster Schritt wird besonders gut aufgenommen."
    };

    private static final String[] WORK = {
            "Konzentriere dich auf eine Aufgabe nach der anderen.",
            "Deine praktische Lösung kann heute andere überzeugen.",
            "Ein guter Zeitpunkt, um eine neue Idee festzuhalten.",
            "Plane genug Ruhe ein, bevor du eine Entscheidung triffst."
    };

    private static final String[] HEALTH = {
            "Eine kurze Pause und frische Luft bringen neue Energie.",
            "Achte heute besonders auf einen ruhigen Tagesrhythmus.",
            "Bewegung tut gut, aber ohne unnötigen Leistungsdruck.",
            "Genügend Wasser und ein früher Feierabend wirken wohltuend."
    };

    @Override protected String appTitle() { return "Tageshoroskop"; }

    @Override protected void buildApp() {
        content.addView(label("Wähle dein Sternzeichen", 20, Color.DKGRAY));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        for (int i = 0; i < SIGNS.length; i++) addSign(grid, i);
        content.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(label("Horoskope dienen der Unterhaltung.", 13, Color.GRAY));
    }

    private void addSign(GridLayout grid, final int index) {
        TextView sign = actionButton(symbol(index) + "  " + SIGNS[index]);
        sign.setGravity(Gravity.CENTER);
        sign.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showHoroscope(index); }
        });
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = Math.max(140,
                (getResources().getDisplayMetrics().widthPixels - 80) / 3);
        params.height = 74;
        params.columnSpec = GridLayout.spec(index % 3);
        params.rowSpec = GridLayout.spec(index / 3);
        params.setMargins(5, 5, 5, 5);
        grid.addView(sign, params);
    }

    private void showHoroscope(final int index) {
        content.removeAllViews();
        TextView back = actionButton("‹ Alle Sternzeichen");
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { refreshApp(); }
        });
        content.addView(back, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 60));

        String date = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN)
                .format(new Date());
        content.addView(label(symbol(index) + "  " + SIGNS[index], 31, Color.BLACK));
        content.addView(label(date, 16, Color.DKGRAY));

        int day = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date()));
        int seed = day + index * 17;
        addPart("Heute", GENERAL[Math.abs(seed) % GENERAL.length]);
        addPart("Liebe", LOVE[Math.abs(seed + 3) % LOVE.length]);
        addPart("Beruf", WORK[Math.abs(seed + 7) % WORK.length]);
        addPart("Gesundheit", HEALTH[Math.abs(seed + 11) % HEALTH.length]);
        content.addView(label("Glückszahl: " + ((Math.abs(seed) % 49) + 1), 19, Color.BLACK));
    }

    private void addPart(String title, String text) {
        content.addView(sectionTitle(title));
        content.addView(label(text, 20, Color.BLACK));
    }

    private String symbol(int index) {
        String[] symbols = {"♈", "♉", "♊", "♋", "♌", "♍",
                "♎", "♏", "♐", "♑", "♒", "♓"};
        return symbols[index];
    }
}
