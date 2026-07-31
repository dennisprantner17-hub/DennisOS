package de.dennis.dennisos;

import android.graphics.Color;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LottoActivity extends AppBaseActivity {

    private static final String ROOT = "https://www.lotterien.at/api/";
    private LinearLayout lottoBox;
    private LinearLayout euroBox;

    @Override protected String appTitle() { return "Lotto Österreich"; }

    @Override protected void buildApp() {
        content.addView(label("Offizielle Angaben der Österreichischen Lotterien · ohne Gewähr",
                14, Color.DKGRAY));

        content.addView(sectionTitle("Lotto 6 aus 45"));
        lottoBox = new LinearLayout(this);
        lottoBox.setOrientation(LinearLayout.VERTICAL);
        lottoBox.addView(label("Daten werden geladen …", 18, Color.DKGRAY));
        content.addView(lottoBox);

        content.addView(sectionTitle("EuroMillionen"));
        euroBox = new LinearLayout(this);
        euroBox.setOrientation(LinearLayout.VERTICAL);
        euroBox.addView(label("Daten werden geladen …", 18, Color.DKGRAY));
        content.addView(euroBox);

        loadJackpots();
        loadResults("lotto", lottoBox, false);
        loadResults("euromillionen", euroBox, true);
    }

    private void loadJackpots() {
        loadText(ROOT + "jackpot/all", new TextCallback() {
            @Override public void finished(String text) {
                try {
                    JSONObject root = new JSONObject(text);
                    showJackpot(lottoBox, root.getJSONObject("lotto"));
                    showJackpot(euroBox, root.getJSONObject("euromillionen"));
                } catch (Exception ignored) { }
            }
            @Override public void failed(Exception error) { }
        });
    }

    private void showJackpot(LinearLayout box, JSONObject game) {
        long draw = game.optLong("drawTime", 0L) * 1000L;
        String next = draw > 0 ? new SimpleDateFormat(
                "EEEE, dd.MM.yyyy · HH:mm", Locale.GERMAN).format(new Date(draw)) : "unbekannt";
        double amount = 0;
        JSONObject jackpot = game.optJSONObject("jackpot");
        if (jackpot != null) amount = jackpot.optDouble("value", 0);
        if (amount <= 0) {
            JSONObject original = game.optJSONObject("__original__");
            if (original != null) {
                JSONObject prize = original.optJSONObject("announcedFirstRankPrize");
                if (prize != null) {
                    JSONObject value = prize.optJSONObject("amount");
                    if (value != null) {
                        amount = value.optDouble("value", 0);
                        if (value.optString("weight").contains("MILLIONS")) amount *= 1000000d;
                    }
                }
            }
        }
        String money = amount > 0 ? NumberFormat.getCurrencyInstance(Locale.GERMANY).format(amount)
                : "Betrag noch nicht bekannt";
        box.addView(label("Nächste Ziehung: " + next + "\nIm Topf: " + money,
                19, Color.BLACK), 0);
    }

    private void loadResults(String game, final LinearLayout box, final boolean stars) {
        loadText(ROOT + "results/" + game + "/3", new TextCallback() {
            @Override public void finished(String text) {
                removeLoading(box);
                try {
                    JSONArray draws = new JSONArray(text);
                    for (int i = 0; i < draws.length(); i++) {
                        JSONObject draw = draws.getJSONObject(i);
                        JSONObject result = draw.getJSONObject("results");
                        String numbers = join(result.getJSONArray("regular"));
                        String extra = stars
                                ? "Sterne: " + join(result.getJSONArray("star"))
                                : "Zusatzzahl: " + result.optString("bonus");
                        String date = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMAN)
                                .format(new Date(draw.optLong("drawTime") * 1000L));
                        box.addView(label(date + "\n" + numbers + "\n" + extra,
                                i == 0 ? 22 : 18, Color.BLACK));
                    }
                } catch (Exception error) {
                    box.addView(label("Ziehungsergebnisse konnten nicht gelesen werden.", 18, Color.DKGRAY));
                }
            }
            @Override public void failed(Exception error) {
                removeLoading(box);
                box.addView(label("Lotto-Daten momentan nicht verfügbar.", 18, Color.DKGRAY));
            }
        });
    }

    private void removeLoading(LinearLayout box) {
        for (int i = box.getChildCount() - 1; i >= 0; i--) {
            if (box.getChildAt(i) instanceof android.widget.TextView) {
                CharSequence value = ((android.widget.TextView) box.getChildAt(i)).getText();
                if (value != null && value.toString().contains("Daten werden geladen")) {
                    box.removeViewAt(i);
                }
            }
        }
    }

    private String join(JSONArray values) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length(); i++) {
            if (i > 0) result.append("   ");
            result.append(values.getInt(i));
        }
        return result.toString();
    }
}
