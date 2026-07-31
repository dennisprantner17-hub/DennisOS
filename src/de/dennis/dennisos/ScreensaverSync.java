package de.dennis.dennisos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public final class ScreensaverSync {
    private static final String MANIFEST_URL =
            "https://raw.githubusercontent.com/dennisprantner17-hub/"
                    + "DennisOS/main/screensaver.json";

    public interface Callback {
        void onFinished(Bitmap bitmap);
        void onError(Exception exception);
    }

    private ScreensaverSync() {
    }

    public static void loadRandom(final Callback callback) {
        new AsyncTask<Void, Void, Result>() {
            @Override
            protected Result doInBackground(Void... ignored) {
                try {
                    ArrayList<String> urls = loadUrls();
                    if (urls.size() == 0) {
                        throw new Exception("Im Album sind keine Bilder verfügbar.");
                    }

                    String selected = urls.get(new Random().nextInt(urls.size()));
                    HttpURLConnection connection = openConnection(selected);
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty("User-Agent", "DennisOS/3.0");
                    InputStream input = new BufferedInputStream(connection.getInputStream());
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    connection.disconnect();

                    if (bitmap == null) {
                        throw new Exception("Bild konnte nicht geöffnet werden.");
                    }
                    return new Result(bitmap, null);
                } catch (Exception exception) {
                    return new Result(null, exception);
                }
            }

            @Override
            protected void onPostExecute(Result result) {
                if (result.error == null) {
                    callback.onFinished(result.bitmap);
                } else {
                    callback.onError(result.error);
                }
            }
        }.execute();
    }

    private static ArrayList<String> loadUrls() throws Exception {
        HttpURLConnection connection = openConnection(
                MANIFEST_URL + "?t=" + System.currentTimeMillis()
        );
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "DennisOS/3.0");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8")
        );
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        reader.close();
        connection.disconnect();

        JSONArray images = new JSONObject(json.toString()).getJSONArray("images");
        ArrayList<String> urls = new ArrayList<String>();
        for (int index = 0; index < images.length(); index++) {
            String value = images.optString(index, "").trim();
            if (value.length() > 0) {
                urls.add(value);
            }
        }
        return urls;
    }

    private static HttpURLConnection openConnection(String address)
            throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(true);

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

    private static final class Result {
        private final Bitmap bitmap;
        private final Exception error;

        private Result(Bitmap bitmap, Exception error) {
            this.bitmap = bitmap;
            this.error = error;
        }
    }
}
