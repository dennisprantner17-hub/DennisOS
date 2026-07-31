package de.dennis.dennisos;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarEvent {

    private final String title;
    private final String startDate;
    private final String endDateExclusive;
    private final String description;
    private final String location;
    private final String attendees;

    public CalendarEvent(
            String title,
            String startDate
    ) {
        this(
                title,
                startDate,
                nextDay(startDate),
                "",
                "",
                ""
        );
    }

    public CalendarEvent(
            String title,
            String startDate,
            String endDateExclusive,
            String description,
            String location,
            String attendees
    ) {
        this.title =
                title == null
                        ? ""
                        : title;

        this.startDate =
                startDate == null
                        ? ""
                        : startDate;

        this.endDateExclusive =
                endDateExclusive == null
                        || endDateExclusive.length() == 0
                        ? nextDay(this.startDate)
                        : endDateExclusive;

        this.description =
                description == null
                        ? ""
                        : description;

        this.location =
                location == null
                        ? ""
                        : location;

        this.attendees =
                attendees == null
                        ? ""
                        : attendees;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return startDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDateExclusive() {
        return endDateExclusive;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getAttendees() {
        return attendees;
    }

    public boolean occursOn(
            String dateKey
    ) {
        if (dateKey == null
                || dateKey.length() == 0
                || startDate.length() == 0) {
            return false;
        }

        return dateKey.compareTo(startDate) >= 0
                && dateKey.compareTo(endDateExclusive) < 0;
    }

    public String getDisplayDateRange() {
        try {
            SimpleDateFormat source =
                    new SimpleDateFormat(
                            "yyyyMMdd",
                            Locale.US
                    );

            SimpleDateFormat target =
                    new SimpleDateFormat(
                            "dd.MM.yyyy",
                            Locale.GERMAN
                    );

            Date start =
                    source.parse(startDate);

            Calendar endCalendar =
                    Calendar.getInstance();

            Date exclusiveEnd =
                    source.parse(
                            endDateExclusive
                    );

            endCalendar.setTime(
                    exclusiveEnd
            );

            endCalendar.add(
                    Calendar.DAY_OF_MONTH,
                    -1
            );

            String startText =
                    target.format(start);

            String endText =
                    target.format(
                            endCalendar.getTime()
                    );

            if (startText.equals(endText)) {
                return startText;
            }

            return startText
                    + " – "
                    + endText;

        } catch (Exception ignored) {
            return startDate;
        }
    }

    private static String nextDay(
            String dateKey
    ) {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyyMMdd",
                            Locale.US
                    );

            format.setLenient(false);

            Date date =
                    format.parse(dateKey);

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(date);

            calendar.add(
                    Calendar.DAY_OF_MONTH,
                    1
            );

            return format.format(
                    calendar.getTime()
            );

        } catch (Exception ignored) {
            return dateKey;
        }
    }
}
