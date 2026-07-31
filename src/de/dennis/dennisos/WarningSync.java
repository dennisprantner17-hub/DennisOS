package de.dennis.dennisos;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public final class WarningSync {

    private static final String WARNING_URL =
            "https://warnungen.zamg.at/wsapp/api/getWarningsForCoords"
                    + "?lon=15.39768&lat=47.02330&lang=de";

    public interface Callback {
        void onFinished(ArrayList<WeatherWarning> warnings);
        void onError(Exception exception);
    }

    private WarningSync() {
    }

    public static void sync(final Callback callback) {
        new AsyncTask<Void, Void, Result>() {
            @Override
            protected Result doInBackground(Void... ignored) {
                HttpURLConnection connection = null;

                try {
                    connection = openConnection(WARNING_URL);
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("User-Agent", "DennisOS/3.0");
                    connection.setRequestProperty("Accept", "application/json");

                    int status = connection.getResponseCode();
                    if (status < 200 || status >= 300) {
                        throw new Exception("Warnserver antwortet mit " + status);
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8")
                    );
                    StringBuilder json = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }
                    reader.close();

                    return new Result(parse(json.toString()), null);
                } catch (Exception exception) {
                    return new Result(null, exception);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(Result result) {
                if (result.error == null) {
                    callback.onFinished(result.warnings);
                } else {
                    callback.onError(result.error);
                }
            }
        }.execute();
    }

    private static HttpURLConnection openConnection(String address)
            throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();

        if (connection instanceof HttpsURLConnection
                && android.os.Build.VERSION.SDK_INT < 21) {
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);
            ((HttpsURLConnection) connection).setSSLSocketFactory(
                    new Tls12SocketFactory(context.getSocketFactory())
            );
        }

        return connection;
    }

    private static ArrayList<WeatherWarning> parse(String json)
            throws Exception {
        ArrayList<WeatherWarning> result = new ArrayList<WeatherWarning>();
        JSONObject properties = new JSONObject(json).getJSONObject("properties");
        JSONArray warnings = properties.optJSONArray("warnings");

        if (warnings == null) {
            return result;
        }

        long nowSeconds = System.currentTimeMillis() / 1000L;

        for (int index = 0; index < warnings.length(); index++) {
            JSONObject item = warnings.getJSONObject(index).getJSONObject("properties");
            JSONObject raw = item.optJSONObject("rawinfo");
            long start = raw == null ? 0L : parseLong(raw.optString("start"));
            long end = raw == null ? 0L : parseLong(raw.optString("end"));

            if (end > 0L && end < nowSeconds) {
                continue;
            }

            result.add(new WeatherWarning(
                    item.optInt("warnstufeid", raw == null ? 1 : raw.optInt("wlevel", 1)),
                    item.optInt("warntypid", raw == null ? 0 : raw.optInt("wtype", 0)),
                    start,
                    end,
                    clean(item.optString("begin")),
                    clean(item.optString("end")),
                    clean(item.optString("text")),
                    clean(item.optString("auswirkungen")),
                    clean(item.optString("empfehlungen")),
                    clean(item.optString("meteotext"))
            ));
        }

        return result;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String clean(String value) {
        if (value == null || "null".equalsIgnoreCase(value)) {
            return "";
        }
        return value.trim();
    }

    public static String typeName(int type) {
        switch (type) {
            case 1: return "Wind";
            case 2: return "Regen";
            case 3: return "Schnee";
            case 4: return "Glatteis";
            case 5: return "Gewitter";
            case 6: return "Hitze";
            case 7: return "Kälte";
            default: return "Unwetter";
        }
    }

    public static String levelName(int level) {
        if (level >= 3) return "ROT";
        if (level == 2) return "ORANGE";
        return "GELB";
    }

    private static final class Result {
        private final ArrayList<WeatherWarning> warnings;
        private final Exception error;

        private Result(ArrayList<WeatherWarning> warnings, Exception error) {
            this.warnings = warnings;
            this.error = error;
        }
    }

    public static final class WeatherWarning {
        private final int level;
        private final int type;
        private final long startSeconds;
        private final long endSeconds;
        private final String beginText;
        private final String endText;
        private final String text;
        private final String effects;
        private final String recommendations;
        private final String meteorologicalText;

        private WeatherWarning(int level, int type, long startSeconds,
                               long endSeconds, String beginText, String endText,
                               String text, String effects, String recommendations,
                               String meteorologicalText) {
            this.level = level;
            this.type = type;
            this.startSeconds = startSeconds;
            this.endSeconds = endSeconds;
            this.beginText = beginText;
            this.endText = endText;
            this.text = text;
            this.effects = effects;
            this.recommendations = recommendations;
            this.meteorologicalText = meteorologicalText;
        }

        public int getLevel() { return level; }
        public int getType() { return type; }
        public long getStartSeconds() { return startSeconds; }
        public long getEndSeconds() { return endSeconds; }
        public String getBeginText() { return beginText; }
        public String getEndText() { return endText; }
        public String getText() { return text; }
        public String getEffects() { return effects; }
        public String getRecommendations() { return recommendations; }
        public String getMeteorologicalText() { return meteorologicalText; }
    }
}
