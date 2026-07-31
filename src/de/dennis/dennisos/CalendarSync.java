package de.dennis.dennisos;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class CalendarSync {

    private static final String CALENDAR_URL =
            "https://p164-caldav.icloud.com/published/2/NTUyOTU3NjIyNTUyOTU3NiEdXMOBO0dAlCU2HEZa3mprtDPTYqFnWybDF4MAEcmyCyrhxT-r0Omj_JbBwZaKssXqT6NWJvH-Q2BxUKkGJ-s";

    public interface Callback {
        void onFinished(
                ArrayList<CalendarEvent> events
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
                    URL url =
                            new URL(
                                    CALENDAR_URL
                            );

                    connection =
                            (HttpURLConnection)
                                    url.openConnection();

                    connection.setConnectTimeout(
                            15000
                    );

                    connection.setReadTimeout(
                            15000
                    );

                    connection.setRequestMethod(
                            "GET"
                    );

                    connection.setRequestProperty(
                            "User-Agent",
                            "DennisOS/2.0"
                    );

                    InputStream input =
                            connection.getInputStream();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            input,
                                            "UTF-8"
                                    )
                            );

                    StringBuilder raw =
                            new StringBuilder();

                    String line;

                    while ((line = reader.readLine())
                            != null) {
                        raw.append(line);
                        raw.append("\n");
                    }

                    reader.close();

                    ArrayList<CalendarEvent> events =
                            parseCalendar(
                                    raw.toString()
                            );

                    StorageHelper.saveCalendar(
                            context,
                            raw.toString()
                    );

                    StorageHelper.saveSyncTime(
                            context,
                            System.currentTimeMillis()
                    );

                    return new Result(
                            events,
                            null
                    );

                } catch (Exception exception) {
                    try {
                        String cached =
                                StorageHelper.loadCalendar(
                                        context
                                );

                        if (cached != null
                                && cached.length() > 0) {

                            return new Result(
                                    parseCalendar(cached),
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
                    callback.onError(
                            result.error
                    );
                } else {
                    callback.onFinished(
                            result.events
                    );
                }
            }
        }.execute();
    }

    private static ArrayList<CalendarEvent> parseCalendar(
            String rawCalendar
    ) {
        ArrayList<CalendarEvent> events =
                new ArrayList<CalendarEvent>();

        String normalized =
                rawCalendar
                        .replace(
                                "\r\n",
                                "\n"
                        )
                        .replace(
                                "\r",
                                "\n"
                        );

        String[] originalLines =
                normalized.split("\n");

        ArrayList<String> lines =
                new ArrayList<String>();

        for (String originalLine
                : originalLines) {

            if ((originalLine.startsWith(" ")
                    || originalLine.startsWith("\t"))
                    && lines.size() > 0) {

                int lastIndex =
                        lines.size() - 1;

                lines.set(
                        lastIndex,
                        lines.get(lastIndex)
                                + originalLine.substring(1)
                );

            } else {
                lines.add(
                        originalLine
                );
            }
        }

        boolean insideEvent = false;

        String title = "";
        String startDate = "";
        String endDateExclusive = "";
        boolean startAllDay = false;
        boolean endAllDay = false;
        String description = "";
        String location = "";
        String attendees = "";
        String recurrenceRule = "";
        HashSet<String> excludedDates =
                new HashSet<String>();

        for (String line : lines) {
            if ("BEGIN:VEVENT".equals(line)) {
                insideEvent = true;
                title = "";
                startDate = "";
                endDateExclusive = "";
                startAllDay = false;
                endAllDay = false;
                description = "";
                location = "";
                attendees = "";
                recurrenceRule = "";
                excludedDates.clear();
                continue;
            }

            if ("END:VEVENT".equals(line)) {
                if (insideEvent
                        && startDate.length() > 0) {

                    String normalizedEnd =
                            normalizeEndDate(
                                    startDate,
                                    endDateExclusive,
                                    startAllDay,
                                    endAllDay
                            );

                    addEventOccurrences(
                            events,
                            unescapeText(title),
                            startDate,
                            normalizedEnd,
                            unescapeText(description),
                            unescapeText(location),
                            unescapeText(attendees),
                            recurrenceRule,
                            excludedDates
                    );
                }

                insideEvent = false;
                continue;
            }

            if (!insideEvent) {
                continue;
            }

            if (line.startsWith("SUMMARY")) {
                title =
                        valueAfterColon(line);

            } else if (line.startsWith("DTSTART")) {
                String rawStart =
                        valueAfterColon(line);

                startAllDay =
                        isAllDayValue(
                                line,
                                rawStart
                        );

                startDate =
                        parseDateKey(
                                rawStart
                        );

            } else if (line.startsWith("DTEND")) {
                String rawEnd =
                        valueAfterColon(line);

                endAllDay =
                        isAllDayValue(
                                line,
                                rawEnd
                        );

                endDateExclusive =
                        parseDateKey(
                                rawEnd
                        );

            } else if (line.startsWith("DESCRIPTION")) {
                description =
                        valueAfterColon(line);

            } else if (line.startsWith("LOCATION")) {
                location =
                        valueAfterColon(line);

            } else if (line.startsWith("ATTENDEE")) {
                String attendee =
                        valueAfterColon(line);

                if (attendees.length() > 0) {
                    attendees += ", ";
                }

                attendees += attendee
                        .replace(
                                "mailto:",
                                ""
                        );
            } else if (line.startsWith("RRULE")) {
                recurrenceRule = valueAfterColon(line);

            } else if (line.startsWith("EXDATE")) {
                String[] values = valueAfterColon(line).split(",");
                for (String value : values) {
                    String excluded = parseDateKey(value);
                    if (excluded.length() > 0) {
                        excludedDates.add(excluded);
                    }
                }
            }
        }

        return events;
    }

    private static void addEventOccurrences(
            ArrayList<CalendarEvent> events,
            String title,
            String startDate,
            String endDateExclusive,
            String description,
            String location,
            String attendees,
            String recurrenceRule,
            HashSet<String> excludedDates
    ) {
        if (recurrenceRule == null
                || recurrenceRule.length() == 0
                || !recurrenceRule.contains("FREQ=WEEKLY")) {
            if (!excludedDates.contains(startDate)) {
                events.add(new CalendarEvent(
                        title,
                        startDate,
                        endDateExclusive,
                        description,
                        location,
                        attendees
                ));
            }
            return;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyyMMdd",
                    Locale.US
            );
            format.setLenient(false);

            Date parsedStart = format.parse(startDate);
            Date parsedEnd = format.parse(endDateExclusive);
            long durationDays = Math.max(
                    1L,
                    (parsedEnd.getTime() - parsedStart.getTime())
                            / (24L * 60L * 60L * 1000L)
            );

            int interval = ruleInt(
                    recurrenceRule,
                    "INTERVAL",
                    1
            );
            int maximumCount = ruleInt(
                    recurrenceRule,
                    "COUNT",
                    Integer.MAX_VALUE
            );
            String until = ruleValue(
                    recurrenceRule,
                    "UNTIL"
            );
            String untilDate = parseDateKey(until);

            Calendar earliest = Calendar.getInstance();
            earliest.add(Calendar.YEAR, -1);
            String earliestKey = format.format(earliest.getTime());

            Calendar latest = Calendar.getInstance();
            latest.add(Calendar.YEAR, 2);
            String latestKey = format.format(latest.getTime());

            Calendar occurrence = Calendar.getInstance();
            occurrence.setTime(parsedStart);

            int generatedCount = 0;
            while (generatedCount < maximumCount) {
                String occurrenceStart = format.format(
                        occurrence.getTime()
                );

                if (occurrenceStart.compareTo(latestKey) > 0
                        || (untilDate.length() > 0
                        && occurrenceStart.compareTo(untilDate) > 0)) {
                    break;
                }

                if (occurrenceStart.compareTo(earliestKey) >= 0
                        && !excludedDates.contains(occurrenceStart)) {
                    Calendar occurrenceEnd = (Calendar) occurrence.clone();
                    occurrenceEnd.add(
                            Calendar.DAY_OF_MONTH,
                            (int) durationDays
                    );

                    events.add(new CalendarEvent(
                            title,
                            occurrenceStart,
                            format.format(occurrenceEnd.getTime()),
                            description,
                            location,
                            attendees
                    ));
                }

                generatedCount++;
                occurrence.add(
                        Calendar.DAY_OF_MONTH,
                        7 * Math.max(1, interval)
                );
            }
        } catch (Exception ignored) {
            if (!excludedDates.contains(startDate)) {
                events.add(new CalendarEvent(
                        title,
                        startDate,
                        endDateExclusive,
                        description,
                        location,
                        attendees
                ));
            }
        }
    }

    private static int ruleInt(
            String rule,
            String name,
            int fallback
    ) {
        try {
            String value = ruleValue(rule, name);
            return value.length() == 0
                    ? fallback
                    : Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String ruleValue(
            String rule,
            String name
    ) {
        if (rule == null) {
            return "";
        }

        String prefix = name + "=";
        String[] parts = rule.split(";");
        for (String part : parts) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }
        return "";
    }

    private static String valueAfterColon(
            String line
    ) {
        int colon =
                line.indexOf(':');

        if (colon < 0
                || colon + 1 >= line.length()) {
            return "";
        }

        return line.substring(
                colon + 1
        );
    }

    private static String parseDateKey(
            String rawValue
    ) {
        if (rawValue == null) {
            return "";
        }

        String value =
                rawValue.trim();

        if (value.length() >= 8) {
            return value.substring(
                    0,
                    8
            );
        }

        return "";
    }

    private static boolean isAllDayValue(
            String propertyLine,
            String rawValue
    ) {
        if (propertyLine != null
                && propertyLine.toUpperCase().contains(
                        "VALUE=DATE"
                )) {
            return true;
        }

        return rawValue != null
                && rawValue.trim().length() == 8;
    }

    private static String normalizeEndDate(
            String startDate,
            String parsedEndDate,
            boolean startAllDay,
            boolean endAllDay
    ) {
        if (parsedEndDate == null
                || parsedEndDate.length() == 0) {
            return nextDayKey(
                    startDate
            );
        }

        if (startAllDay || endAllDay) {
            return parsedEndDate;
        }

        /*
         * Bei Terminen mit Uhrzeit liegt DTSTART und DTEND
         * oft am selben Kalendertag. Der bisherige Code hat
         * dann Start und exklusives Ende identisch gespeichert,
         * wodurch der Termin an keinem Tag sichtbar war.
         */
        return nextDayKey(
                parsedEndDate
        );
    }

    private static String nextDayKey(
            String dateKey
    ) {
        try {
            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat(
                            "yyyyMMdd",
                            java.util.Locale.US
                    );

            format.setLenient(false);

            java.util.Date date =
                    format.parse(dateKey);

            java.util.Calendar calendar =
                    java.util.Calendar.getInstance();

            calendar.setTime(date);
            calendar.add(
                    java.util.Calendar.DAY_OF_MONTH,
                    1
            );

            return format.format(
                    calendar.getTime()
            );

        } catch (Exception ignored) {
            return dateKey;
        }
    }

    private static String unescapeText(
            String text
    ) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\n", "\n")
                .replace("\\N", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private static class Result {
        private final ArrayList<CalendarEvent> events;
        private final Exception error;

        private Result(
                ArrayList<CalendarEvent> events,
                Exception error
        ) {
            this.events = events;
            this.error = error;
        }
    }
}
