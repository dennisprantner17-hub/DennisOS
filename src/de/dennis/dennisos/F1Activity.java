package de.dennis.dennisos;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class F1Activity extends AppBaseActivity {

    private static final String API = "https://api.jolpi.ca/ergast/f1/";

    @Override protected String appTitle() { return "Formel 1"; }

    @Override protected void buildApp() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        addTab(tabs, "Übersicht", 0);
        addTab(tabs, "Live", 1);
        addTab(tabs, "Kalender", 2);
        addTab(tabs, "WM-Stand", 3);
        content.addView(tabs);
        showOverview();
    }

    private void addTab(LinearLayout row, String title, final int page) {
        TextView tab = actionButton(title);
        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                while (content.getChildCount() > 1) content.removeViewAt(1);
                if (page == 0) showOverview();
                else if (page == 1) showLive();
                else if (page == 2) showCalendar();
                else showStandings();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 58, 1);
        params.setMargins(3, 0, 3, 0);
        row.addView(tab, params);
    }

    private void showOverview() {
        content.addView(sectionTitle("Aktuelles Rennwochenende"));
        showLoading("F1-Daten werden geladen …");
        loadText(API + "current.json", new TextCallback() {
            @Override public void finished(String text) {
                removeLastLoading();
                try {
                    JSONArray races = new JSONObject(text).getJSONObject("MRData")
                            .getJSONObject("RaceTable").getJSONArray("Races");
                    JSONObject selected = null;
                    long now = System.currentTimeMillis();
                    for (int i = 0; i < races.length(); i++) {
                        JSONObject race = races.getJSONObject(i);
                        long start = parseUtc(race.optString("date"), race.optString("time"));
                        if (start + 12L * 60L * 60L * 1000L >= now) {
                            selected = race;
                            break;
                        }
                    }
                    if (selected == null && races.length() > 0) {
                        selected = races.getJSONObject(races.length() - 1);
                    }
                    if (selected == null) throw new Exception("Kein Rennen gefunden");
                    content.addView(label(selected.optString("raceName"), 27, Color.BLACK));
                    JSONObject circuit = selected.optJSONObject("Circuit");
                    String place = circuit == null ? "" : circuit.optString("circuitName");
                    content.addView(label(place + "\n" + localDate(
                            selected.optString("date"), selected.optString("time")),
                            20, Color.DKGRAY));
                    addSession(selected, "FirstPractice", "Training 1");
                    addSession(selected, "SecondPractice", "Training 2");
                    addSession(selected, "ThirdPractice", "Training 3");
                    addSession(selected, "Sprint", "Sprint");
                    addSession(selected, "Qualifying", "Qualifying");
                    content.addView(label("Rennen · " + localDate(
                            selected.optString("date"), selected.optString("time")),
                            19, Color.BLACK));
                } catch (Exception error) {
                    showError("F1-Übersicht konnte nicht gelesen werden.");
                }
            }
            @Override public void failed(Exception error) { removeLastLoading(); showError("F1-Daten momentan nicht verfügbar."); }
        });
    }

    private void showLive() {
        content.addView(sectionTitle("Live & letzte Sitzung"));
        content.addView(label("Während einer laufenden Sitzung aktualisiert DennisOS "
                + "diese Ansicht automatisch. Die kostenlose Live-Brücke wird als "
                + "nächster Teil von 4.0 zugeschaltet.", 17, Color.DKGRAY));
        showLoading("Letztes Rennergebnis wird geladen …");
        loadText(API + "current/last/results.json", new TextCallback() {
            @Override public void finished(String text) {
                removeLastLoading();
                try {
                    JSONArray races = new JSONObject(text).getJSONObject("MRData")
                            .getJSONObject("RaceTable").getJSONArray("Races");
                    JSONObject race = races.getJSONObject(0);
                    content.addView(label(race.optString("raceName"), 24, Color.BLACK));
                    JSONArray results = race.getJSONArray("Results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONObject driver = result.getJSONObject("Driver");
                        String line = result.optString("positionText") + ".  "
                                + driver.optString("givenName") + " "
                                + driver.optString("familyName") + "   "
                                + result.optString("points") + " Pkt.";
                        content.addView(resultRow(line, i));
                    }
                } catch (Exception error) { showError("Noch kein Rennergebnis verfügbar."); }
            }
            @Override public void failed(Exception error) { removeLastLoading(); showError("Ergebnis momentan nicht verfügbar."); }
        });
    }

    private void showCalendar() {
        content.addView(sectionTitle("F1-Kalender · österreichische Zeit"));
        showLoading("Kalender wird geladen …");
        loadText(API + "current.json", new TextCallback() {
            @Override public void finished(String text) {
                removeLastLoading();
                try {
                    JSONArray races = new JSONObject(text).getJSONObject("MRData")
                            .getJSONObject("RaceTable").getJSONArray("Races");
                    for (int i = 0; i < races.length(); i++) {
                        JSONObject race = races.getJSONObject(i);
                        String title = race.optString("round") + ".  " + race.optString("raceName");
                        String detail = localDate(race.optString("date"), race.optString("time"));
                        TextView card = label(title + "\n" + detail, 19, Color.BLACK);
                        card.setBackground(border(i % 2 == 0 ? Color.WHITE : Color.rgb(242,242,242), Color.LTGRAY, 1));
                        content.addView(card);
                    }
                } catch (Exception error) { showError("Kalender konnte nicht gelesen werden."); }
            }
            @Override public void failed(Exception error) { removeLastLoading(); showError("Kalender momentan nicht verfügbar."); }
        });
    }

    private void showStandings() {
        content.addView(sectionTitle("Fahrer-WM"));
        showLoading("WM-Stand wird geladen …");
        loadText(API + "current/driverStandings.json", new TextCallback() {
            @Override public void finished(String text) {
                removeLastLoading();
                try {
                    JSONArray lists = new JSONObject(text).getJSONObject("MRData")
                            .getJSONObject("StandingsTable").getJSONArray("StandingsLists");
                    JSONArray drivers = lists.getJSONObject(0).getJSONArray("DriverStandings");
                    for (int i = 0; i < drivers.length(); i++) {
                        JSONObject item = drivers.getJSONObject(i);
                        JSONObject driver = item.getJSONObject("Driver");
                        String team = item.getJSONArray("Constructors").getJSONObject(0).optString("name");
                        content.addView(resultRow(item.optString("position") + ".  "
                                + driver.optString("givenName") + " " + driver.optString("familyName")
                                + " · " + team + " · " + item.optString("points") + " Pkt.", i));
                    }
                } catch (Exception error) { showError("WM-Stand ist noch nicht verfügbar."); }
            }
            @Override public void failed(Exception error) { removeLastLoading(); showError("WM-Stand momentan nicht verfügbar."); }
        });
    }

    private TextView resultRow(String text, int index) {
        TextView row = label(text, 18, Color.BLACK);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(index % 2 == 0 ? Color.WHITE : Color.rgb(240,240,240));
        return row;
    }

    private void addSession(JSONObject race, String key, String title) {
        JSONObject session = race.optJSONObject(key);
        if (session != null) content.addView(label(title + " · "
                + localDate(session.optString("date"), session.optString("time")), 18, Color.BLACK));
    }

    private long parseUtc(String date, String time) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            return parser.parse(date + "T" + (time.length() == 0 ? "12:00:00Z" : time)).getTime();
        } catch (Exception ignored) { return 0L; }
    }

    private String localDate(String date, String time) {
        long value = parseUtc(date, time);
        if (value == 0L) return date;
        return new SimpleDateFormat("EEE, dd.MM.yyyy · HH:mm", Locale.GERMAN).format(new Date(value));
    }

    private void removeLastLoading() {
        if (content.getChildCount() > 1) content.removeViewAt(content.getChildCount() - 1);
    }

    private void showError(String value) { content.addView(label(value, 18, Color.DKGRAY)); }
}
