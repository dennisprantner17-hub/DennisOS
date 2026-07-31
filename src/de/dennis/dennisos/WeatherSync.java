package de.dennis.dennisos;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import java.net.HttpURLConnection;

public class WeatherSync {

    private static final String WEATHER_URL =
        "http://wttr.in/Graz?format=j1";


    public interface Callback {

        void onFinished(String weather);

        void onError(Exception e);
    }

    public static void sync(
            final Context context,
            final Callback callback
    ) {

        new AsyncTask<Void, Void, String>() {

            private Exception error;

            @Override
            protected String doInBackground(
                    Void... params
            ) {

                try {

                    URL url = new URL(
                            WEATHER_URL
                    );

                    HttpURLConnection connection =
        (HttpURLConnection)
                url.openConnection();
                    connection.setConnectTimeout(
                            10000
                    );

                    connection.setReadTimeout(
                            10000
                    );

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream()
                                    )
                            );

                    StringBuilder builder =
        new StringBuilder();

String line;

while ((line = reader.readLine()) != null) {

    builder.append(line);
}

reader.close();

String data =
        builder.toString();

String weather =
        data.contains("\"temp_C\": \"")
                ? data.split("\"temp_C\": \"")[1]
                      .split("\"")[0] + "°C"
                : "Kein Wetter";

StorageHelper.saveWeather(
        context,
        weather
);

return weather;

                } catch (Exception e) {

    error = e;

    return null;
}
            }

            @Override
protected void onPostExecute(
        String weather
) {

    if (weather != null) {

        callback.onFinished(
                weather
        );

    } else {

        callback.onFinished(
                "FEHLER: " + error
        );
    }
}

        }.execute();
    }
}