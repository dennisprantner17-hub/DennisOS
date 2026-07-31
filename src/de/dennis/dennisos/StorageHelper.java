package de.dennis.dennisos;

import android.content.Context;
import android.content.SharedPreferences;

public class StorageHelper {

    private static final String PREFS =
            "DennisOS";

    public static void saveCalendar(
            Context context,
            String calendar
    ) {
        preferences(context)
                .edit()
                .putString(
                        "calendar_data",
                        calendar
                )
                .apply();
    }

    public static String loadCalendar(
            Context context
    ) {
        return preferences(context)
                .getString(
                        "calendar_data",
                        ""
                );
    }

    public static void saveWeather(
            Context context,
            String weather
    ) {
        preferences(context)
                .edit()
                .putString(
                        "weather_data",
                        weather
                )
                .apply();
    }

    public static String loadWeather(
            Context context
    ) {
        return preferences(context)
                .getString(
                        "weather_data",
                        "Wetter wird geladen …"
                );
    }

    public static void saveWeatherForecast(
            Context context,
            String json
    ) {
        preferences(context)
                .edit()
                .putString(
                        "weather_forecast_json",
                        json
                )
                .apply();
    }

    public static String loadWeatherForecast(
            Context context
    ) {
        return preferences(context)
                .getString(
                        "weather_forecast_json",
                        ""
                );
    }

    public static void saveSyncTime(
            Context context,
            long time
    ) {
        preferences(context)
                .edit()
                .putLong(
                        "last_sync",
                        time
                )
                .apply();
    }

    public static long loadSyncTime(
            Context context
    ) {
        return preferences(context)
                .getLong(
                        "last_sync",
                        0
                );
    }

    private static SharedPreferences preferences(
            Context context
    ) {
        return context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );
    }
}
