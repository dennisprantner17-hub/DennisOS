package de.dennis.dennisos;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WeatherForecastSync {

    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=47.0707"
                    + "&longitude=15.4395"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,"
                    + "apparent_temperature_max,apparent_temperature_min,"
                    + "precipitation_sum,precipitation_probability_max,"
                    + "sunrise,sunset,sunshine_duration,uv_index_max,"
                    + "wind_speed_10m_max,wind_gusts_10m_max,"
                    + "wind_direction_10m_dominant"
                    + "&hourly=temperature_2m,apparent_temperature,"
                    + "precipitation_probability,relative_humidity_2m,"
                    + "wind_speed_10m,wind_gusts_10m,wind_direction_10m,"
                    + "surface_pressure,visibility,dew_point_2m,weather_code"
                    + "&timezone=Europe%2FVienna"
                    + "&forecast_days=7";

    public interface Callback {
        void onFinished(
                ArrayList<ForecastDay> days
        );

        void onError(
                Exception exception
        );
    }

    public static void sync(
            final Context context,
            final Callback callback
    ) {
        new AsyncTask<Void, Void, Result>() {

            @Override
            protected Result doInBackground(
                    Void... params
            ) {
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(FORECAST_URL);

                    connection =
                            (HttpURLConnection)
                                    url.openConnection();

                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty(
                            "User-Agent",
                            "DennisOS/2.0"
                    );
                    connection.setRequestProperty(
                            "Accept",
                            "application/json"
                    );

                    int responseCode =
                            connection.getResponseCode();

                    if (responseCode
                            < HttpURLConnection.HTTP_OK
                            || responseCode
                            >= HttpURLConnection.HTTP_MULT_CHOICE) {

                        throw new Exception(
                                "Wetterserver antwortet mit "
                                        + responseCode
                        );
                    }

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream(),
                                            "UTF-8"
                                    )
                            );

                    StringBuilder json =
                            new StringBuilder();

                    String line;

                    while ((line = reader.readLine())
                            != null) {
                        json.append(line);
                    }

                    reader.close();

                    String rawJson =
                            json.toString();

                    StorageHelper.saveWeatherForecast(
                            context,
                            rawJson
                    );

                    return new Result(
                            parseForecast(rawJson),
                            null
                    );

                } catch (Exception exception) {
                    try {
                        String cached =
                                StorageHelper.loadWeatherForecast(
                                        context
                                );

                        if (cached != null
                                && cached.length() > 0) {

                            return new Result(
                                    parseForecast(cached),
                                    null
                            );
                        }

                    } catch (Exception ignored) {
                    }

                    return new Result(
                            null,
                            exception
                    );

                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void onPostExecute(
                    Result result
            ) {
                if (result.error != null) {
                    callback.onError(result.error);
                } else {
                    callback.onFinished(result.days);
                }
            }
        }.execute();
    }

    private static ArrayList<ForecastDay> parseForecast(
            String json
    ) throws Exception {

        JSONObject root =
                new JSONObject(json);

        JSONObject daily =
                root.getJSONObject("daily");

        JSONObject hourly =
                root.getJSONObject("hourly");

        JSONArray dailyTimes =
                daily.getJSONArray("time");

        JSONArray weatherCodes =
                daily.getJSONArray("weather_code");

        JSONArray maxTemperatures =
                daily.getJSONArray(
                        "temperature_2m_max"
                );

        JSONArray minTemperatures =
                daily.getJSONArray(
                        "temperature_2m_min"
                );

        JSONArray apparentMax =
                daily.getJSONArray(
                        "apparent_temperature_max"
                );

        JSONArray apparentMin =
                daily.getJSONArray(
                        "apparent_temperature_min"
                );

        JSONArray precipitationSum =
                daily.getJSONArray(
                        "precipitation_sum"
                );

        JSONArray rainProbabilities =
                daily.getJSONArray(
                        "precipitation_probability_max"
                );

        JSONArray sunrises =
                daily.getJSONArray("sunrise");

        JSONArray sunsets =
                daily.getJSONArray("sunset");

        JSONArray sunshineDuration =
                daily.getJSONArray(
                        "sunshine_duration"
                );

        JSONArray uvIndex =
                daily.getJSONArray(
                        "uv_index_max"
                );

        JSONArray maxWindSpeeds =
                daily.getJSONArray(
                        "wind_speed_10m_max"
                );

        JSONArray maxWindGusts =
                daily.getJSONArray(
                        "wind_gusts_10m_max"
                );

        JSONArray dominantWindDirection =
                daily.getJSONArray(
                        "wind_direction_10m_dominant"
                );

        JSONArray hourlyTimes =
                hourly.getJSONArray("time");

        JSONArray hourlyTemperatures =
                hourly.getJSONArray(
                        "temperature_2m"
                );

        JSONArray hourlyApparent =
                hourly.getJSONArray(
                        "apparent_temperature"
                );

        JSONArray hourlyRain =
                hourly.getJSONArray(
                        "precipitation_probability"
                );

        JSONArray hourlyHumidity =
                hourly.getJSONArray(
                        "relative_humidity_2m"
                );

        JSONArray hourlyWind =
                hourly.getJSONArray(
                        "wind_speed_10m"
                );

        JSONArray hourlyGusts =
                hourly.getJSONArray(
                        "wind_gusts_10m"
                );

        JSONArray hourlyWindDirection =
                hourly.getJSONArray(
                        "wind_direction_10m"
                );

        JSONArray hourlyPressure =
                hourly.getJSONArray(
                        "surface_pressure"
                );

        JSONArray hourlyVisibility =
                hourly.getJSONArray(
                        "visibility"
                );

        JSONArray hourlyDewPoint =
                hourly.getJSONArray(
                        "dew_point_2m"
                );

        JSONArray hourlyCodes =
                hourly.getJSONArray(
                        "weather_code"
                );

        ArrayList<ForecastDay> result =
                new ArrayList<ForecastDay>();

        int dayCount =
                Math.min(
                        7,
                        dailyTimes.length()
                );

        for (int dayIndex = 0;
             dayIndex < dayCount;
             dayIndex++) {

            String isoDate =
                    dailyTimes.getString(
                            dayIndex
                    );

            int weatherCode =
                    weatherCodes.optInt(
                            dayIndex,
                            3
                    );

            ArrayList<ForecastHour> hours =
                    new ArrayList<ForecastHour>();

            int representativeHumidity = 0;
            int representativePressure = 0;
            int representativeVisibility = 0;
            int representativeDewPoint = 0;

            for (int hourIndex = 0;
                 hourIndex < hourlyTimes.length();
                 hourIndex++) {

                String timestamp =
                        hourlyTimes.getString(
                                hourIndex
                        );

                if (!timestamp.startsWith(
                        isoDate + "T"
                )) {
                    continue;
                }

                ForecastHour hour =
                        new ForecastHour(
                                formatIsoTime(timestamp),
                                round(hourlyTemperatures.optDouble(
                                        hourIndex,
                                        0
                                )),
                                round(hourlyApparent.optDouble(
                                        hourIndex,
                                        0
                                )),
                                hourlyRain.optInt(
                                        hourIndex,
                                        0
                                ),
                                hourlyHumidity.optInt(
                                        hourIndex,
                                        0
                                ),
                                round(hourlyWind.optDouble(
                                        hourIndex,
                                        0
                                )),
                                round(hourlyGusts.optDouble(
                                        hourIndex,
                                        0
                                )),
                                hourlyWindDirection.optInt(
                                        hourIndex,
                                        0
                                ),
                                round(hourlyPressure.optDouble(
                                        hourIndex,
                                        0
                                )),
                                round(hourlyVisibility.optDouble(
                                        hourIndex,
                                        0
                                ) / 1000.0),
                                round(hourlyDewPoint.optDouble(
                                        hourIndex,
                                        0
                                )),
                                hourlyCodes.optInt(
                                        hourIndex,
                                        3
                                )
                        );

                if (timestamp.endsWith("T12:00")) {
                    representativeHumidity =
                            hour.getHumidity();

                    representativePressure =
                            hour.getPressureHpa();

                    representativeVisibility =
                            hour.getVisibilityKm();

                    representativeDewPoint =
                            hour.getDewPointC();
                }

                hours.add(hour);
            }

            if (hours.size() > 0
                    && representativeHumidity == 0) {

                ForecastHour middle =
                        hours.get(
                                hours.size() / 2
                        );

                representativeHumidity =
                        middle.getHumidity();

                representativePressure =
                        middle.getPressureHpa();

                representativeVisibility =
                        middle.getVisibilityKm();

                representativeDewPoint =
                        middle.getDewPointC();
            }

            Date parsedDate =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    ).parse(isoDate);

            result.add(
                    new ForecastDay(
                            new SimpleDateFormat(
                                    "EEE",
                                    Locale.GERMAN
                            ).format(parsedDate),
                            new SimpleDateFormat(
                                    "dd.MM.yyyy",
                                    Locale.GERMAN
                            ).format(parsedDate),
                            new SimpleDateFormat(
                                    "dd.MM.",
                                    Locale.GERMAN
                            ).format(parsedDate),
                            round(minTemperatures.optDouble(
                                    dayIndex,
                                    0
                            )),
                            round(maxTemperatures.optDouble(
                                    dayIndex,
                                    0
                            )),
                            round(apparentMin.optDouble(
                                    dayIndex,
                                    0
                            )),
                            round(apparentMax.optDouble(
                                    dayIndex,
                                    0
                            )),
                            rainProbabilities.optInt(
                                    dayIndex,
                                    0
                            ),
                            round(precipitationSum.optDouble(
                                    dayIndex,
                                    0
                            ) * 10.0) / 10.0,
                            representativeHumidity,
                            round(maxWindSpeeds.optDouble(
                                    dayIndex,
                                    0
                            )),
                            round(maxWindGusts.optDouble(
                                    dayIndex,
                                    0
                            )),
                            dominantWindDirection.optInt(
                                    dayIndex,
                                    0
                            ),
                            representativePressure,
                            representativeVisibility,
                            representativeDewPoint,
                            round(uvIndex.optDouble(
                                    dayIndex,
                                    0
                            ) * 10.0) / 10.0,
                            round(sunshineDuration.optDouble(
                                    dayIndex,
                                    0
                            ) / 3600.0 * 10.0) / 10.0,
                            formatIsoTime(
                                    sunrises.optString(
                                            dayIndex,
                                            ""
                                    )
                            ),
                            formatIsoTime(
                                    sunsets.optString(
                                            dayIndex,
                                            ""
                                    )
                            ),
                            weatherCode,
                            descriptionForCode(
                                    weatherCode
                            ),
                            hours
                    )
            );
        }

        return result;
    }

    private static int round(
            double value
    ) {
        return (int) Math.round(value);
    }

    private static String formatIsoTime(
            String iso
    ) {
        if (iso == null) {
            return "";
        }

        int separator =
                iso.indexOf('T');

        if (separator >= 0
                && iso.length()
                >= separator + 6) {

            return iso.substring(
                    separator + 1,
                    separator + 6
            );
        }

        return iso;
    }

    public static String descriptionForCode(
            int code
    ) {
        if (code == 0) {
            return "Klar";
        }

        if (code == 1) {
            return "Überwiegend klar";
        }

        if (code == 2) {
            return "Teilweise bewölkt";
        }

        if (code == 3) {
            return "Bewölkt";
        }

        if (code == 45
                || code == 48) {
            return "Nebel";
        }

        if (code >= 51 && code <= 57) {
            return "Nieselregen";
        }

        if (code >= 61 && code <= 67) {
            return "Regen";
        }

        if (code >= 71 && code <= 77) {
            return "Schnee";
        }

        if (code >= 80 && code <= 82) {
            return "Regenschauer";
        }

        if (code == 85
                || code == 86) {
            return "Schneeschauer";
        }

        if (code >= 95) {
            return "Gewitter";
        }

        return "Bewölkt";
    }

    public static String windDirectionText(
            int degrees
    ) {
        String[] directions = {
                "N", "NNO", "NO", "ONO",
                "O", "OSO", "SO", "SSO",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
        };

        int normalized =
                ((degrees % 360) + 360) % 360;

        int index =
                (int) Math.round(
                        normalized / 22.5
                ) % 16;

        return directions[index];
    }

    private static class Result {

        private final ArrayList<ForecastDay> days;
        private final Exception error;

        private Result(
                ArrayList<ForecastDay> days,
                Exception error
        ) {
            this.days = days;
            this.error = error;
        }
    }

    public static class ForecastDay {

        private final String weekday;
        private final String dateLabel;
        private final String dateShort;

        private final int minTempC;
        private final int maxTempC;
        private final int apparentMinC;
        private final int apparentMaxC;
        private final int rainChance;
        private final double precipitationMm;
        private final int humidity;
        private final int windKmph;
        private final int windGustKmph;
        private final int windDirectionDegrees;
        private final int pressureHpa;
        private final int visibilityKm;
        private final int dewPointC;
        private final double uvIndex;
        private final double sunshineHours;

        private final String sunrise;
        private final String sunset;
        private final int weatherCode;
        private final String description;

        private final ArrayList<ForecastHour> hours;

        public ForecastDay(
                String weekday,
                String dateLabel,
                String dateShort,
                int minTempC,
                int maxTempC,
                int apparentMinC,
                int apparentMaxC,
                int rainChance,
                double precipitationMm,
                int humidity,
                int windKmph,
                int windGustKmph,
                int windDirectionDegrees,
                int pressureHpa,
                int visibilityKm,
                int dewPointC,
                double uvIndex,
                double sunshineHours,
                String sunrise,
                String sunset,
                int weatherCode,
                String description,
                ArrayList<ForecastHour> hours
        ) {
            this.weekday = weekday;
            this.dateLabel = dateLabel;
            this.dateShort = dateShort;
            this.minTempC = minTempC;
            this.maxTempC = maxTempC;
            this.apparentMinC = apparentMinC;
            this.apparentMaxC = apparentMaxC;
            this.rainChance = rainChance;
            this.precipitationMm = precipitationMm;
            this.humidity = humidity;
            this.windKmph = windKmph;
            this.windGustKmph = windGustKmph;
            this.windDirectionDegrees = windDirectionDegrees;
            this.pressureHpa = pressureHpa;
            this.visibilityKm = visibilityKm;
            this.dewPointC = dewPointC;
            this.uvIndex = uvIndex;
            this.sunshineHours = sunshineHours;
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.weatherCode = weatherCode;
            this.description = description;
            this.hours = hours;
        }

        public String getWeekday() {
            return weekday;
        }

        public String getDateLabel() {
            return dateLabel;
        }

        public String getDateShort() {
            return dateShort;
        }

        public int getMinTempC() {
            return minTempC;
        }

        public int getMaxTempC() {
            return maxTempC;
        }

        public int getApparentMinC() {
            return apparentMinC;
        }

        public int getApparentMaxC() {
            return apparentMaxC;
        }

        public int getRainChance() {
            return rainChance;
        }

        public double getPrecipitationMm() {
            return precipitationMm;
        }

        public int getHumidity() {
            return humidity;
        }

        public int getWindKmph() {
            return windKmph;
        }

        public int getWindGustKmph() {
            return windGustKmph;
        }

        public int getWindDirectionDegrees() {
            return windDirectionDegrees;
        }

        public String getWindDirectionText() {
            return windDirectionText(
                    windDirectionDegrees
            );
        }

        public int getPressureHpa() {
            return pressureHpa;
        }

        public int getVisibilityKm() {
            return visibilityKm;
        }

        public int getDewPointC() {
            return dewPointC;
        }

        public double getUvIndex() {
            return uvIndex;
        }

        public double getSunshineHours() {
            return sunshineHours;
        }

        public String getSunrise() {
            return sunrise;
        }

        public String getSunset() {
            return sunset;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public String getDescription() {
            return description;
        }

        public String getSymbol() {
            return "";
        }

        public ArrayList<ForecastHour> getHours() {
            return hours;
        }
    }

    public static class ForecastHour {

        private final String time;
        private final int tempC;
        private final int apparentTempC;
        private final int rainChance;
        private final int humidity;
        private final int windKmph;
        private final int windGustKmph;
        private final int windDirectionDegrees;
        private final int pressureHpa;
        private final int visibilityKm;
        private final int dewPointC;
        private final int weatherCode;

        public ForecastHour(
                String time,
                int tempC,
                int apparentTempC,
                int rainChance,
                int humidity,
                int windKmph,
                int windGustKmph,
                int windDirectionDegrees,
                int pressureHpa,
                int visibilityKm,
                int dewPointC,
                int weatherCode
        ) {
            this.time = time;
            this.tempC = tempC;
            this.apparentTempC = apparentTempC;
            this.rainChance = rainChance;
            this.humidity = humidity;
            this.windKmph = windKmph;
            this.windGustKmph = windGustKmph;
            this.windDirectionDegrees = windDirectionDegrees;
            this.pressureHpa = pressureHpa;
            this.visibilityKm = visibilityKm;
            this.dewPointC = dewPointC;
            this.weatherCode = weatherCode;
        }

        public String getTime() {
            return time;
        }

        public int getTempC() {
            return tempC;
        }

        public int getApparentTempC() {
            return apparentTempC;
        }

        public int getRainChance() {
            return rainChance;
        }

        public int getHumidity() {
            return humidity;
        }

        public int getWindKmph() {
            return windKmph;
        }

        public int getWindGustKmph() {
            return windGustKmph;
        }

        public int getWindDirectionDegrees() {
            return windDirectionDegrees;
        }

        public String getWindDirectionText() {
            return windDirectionText(
                    windDirectionDegrees
            );
        }

        public int getPressureHpa() {
            return pressureHpa;
        }

        public int getVisibilityKm() {
            return visibilityKm;
        }

        public int getDewPointC() {
            return dewPointC;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public String getDescription() {
            return descriptionForCode(
                    weatherCode
            );
        }

        public String getSymbol() {
            return "";
        }
    }
}
