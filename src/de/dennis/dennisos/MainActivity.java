package de.dennis.dennisos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final long SYNC_INTERVAL_MS =
            15L * 60L * 1000L;

    private static final long RESET_DELAY_MS =
            60L * 1000L;

    private static final long NIGHT_WAKE_TIME_MS =
            60L * 1000L;

    private static final String SETTINGS_PREFS =
            "DennisOS_Settings";


    private static final int DISPLAY_WEEKS = 5;
    private static final int NAVIGATION_STEP_WEEKS = 4;
    private static final int PREVIEW_EVENT_LIMIT = 3;

    private static final int VIEW_CALENDAR = 0;
    private static final int VIEW_AGENDA = 1;
    private static final int VIEW_YEAR = 2;

    private LinearLayout rootLayout;
    private LinearLayout weatherDaysRow;
    private LinearLayout contentContainer;
    private GridLayout calendarGrid;

    private TextView clockText;
    private TextView dateText;
    private TextView currentWeatherText;
    private TextView warningCard;
    private TextView syncText;
    private TextView countdownText;
    private TextView batteryText;
    private TextView currentViewButton;
    private TextView viewTitleText;
    private BulbButton lightButton;
    private TextView syncNowButton;
    private SettingsButton settingsButton;

    private AlertDialog dayDialog;
    private Dialog fullscreenDayDialog;
    private Dialog weatherDialog;
    private Dialog warningDialog;
    private Dialog settingsDialog;
    private Dialog screensaverDialog;

    private final Handler clockHandler = new Handler();
    private final Handler countdownHandler = new Handler();
    private final Handler syncHandler = new Handler();
    private final Handler batteryHandler = new Handler();
    private final Handler resetHandler = new Handler();
    private final Handler nightHandler = new Handler();
    private final Handler buttonAnimationHandler = new Handler();
    private final Handler screensaverHandler = new Handler();
    private final Handler lightIdleHandler = new Handler();

    private final String[] syncAnimationFrames = {
            "↻",
            "↺",
            "↻",
            "↺"
    };

    private int syncAnimationFrame = 0;

    private ArrayList<CalendarEvent> currentEvents =
            new ArrayList<CalendarEvent>();

    private ArrayList<WeatherForecastSync.ForecastDay> forecastDays =
            new ArrayList<WeatherForecastSync.ForecastDay>();

    private ArrayList<WarningSync.WeatherWarning> currentWarnings =
            new ArrayList<WarningSync.WeatherWarning>();

    private int navigationOffsetWeeks = 0;
    private int activeView = VIEW_CALENDAR;

    private boolean temporaryNightWakeActive = false;
    private boolean consumeWakeGesture = false;
    private Boolean manualLightOverride = null;
    private boolean lastNightState = false;
    private boolean lightCurrentlyOn = true;
    private final int[] nightStartHours = {
            22, 22, 22, 22, 22, 23, 23
    };
    private final int[] nightEndHours = {
            6, 6, 6, 6, 6, 8, 8
    };
    private int brightnessPercent = 100;
    private boolean screensaverEnabled = true;
    private int screensaverIdleMinutes = 10;
    private int screensaverChangeMinutes = 15;
    private boolean screensaverLightOn = false;
    private int screensaverLightStartHour = 7;
    private int screensaverLightEndHour = 22;
    private boolean autoLightOffEnabled = false;
    private int autoLightOffMinutes = 5;
    private boolean autoLightSleeping = false;
    private boolean syncRunning = false;
    private boolean updateCheckRequestedByUser = false;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        gestureDetector = new GestureDetector(
                this,
                new SwipeListener()
        );

        loadDisplaySettings();

        lastNightState = isNightTime();

        buildLayout();
        installTouchListener();

        startClock();
        startCountdown();
        startBatteryUpdates();
        startNightModeController();
        scheduleScreensaver();
        scheduleAutomaticLightOff();

        showActiveView();
        runSync();

        syncHandler.postDelayed(
                automaticSync,
                SYNC_INTERVAL_MS
        );
    }

    private void buildLayout() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(
                LinearLayout.VERTICAL
        );
        rootLayout.setBackgroundColor(
                Color.WHITE
        );
        rootLayout.setPadding(
                10,
                8,
                10,
                8
        );

        buildHeaderArea();
        buildNavigationArea();
        buildWeatherArea();
        buildContentArea();
        buildFooter();

        setContentView(rootLayout);
    }

    private void buildHeaderArea() {
        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout left =
                new LinearLayout(this);

        left.setOrientation(
                LinearLayout.VERTICAL
        );

        left.setGravity(
                Gravity.LEFT
        );

        left.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        batteryText = new TextView(this);
        batteryText.setText(
                "Akku wird geladen …"
        );
        batteryText.setTextSize(13);
        batteryText.setTextColor(
                Color.DKGRAY
        );
        batteryText.setGravity(
                Gravity.LEFT
        );
        batteryText.setPadding(
                8,
                0,
                0,
                4
        );

        lightButton = new BulbButton(this);
        lightButton.setOnState(true);

        lightButton.setLayoutParams(
                new LinearLayout.LayoutParams(
                        92,
                        66
                )
        );

        lightButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        toggleLight();
                    }
                }
        );

        left.addView(batteryText);

        LinearLayout leftButtons =
                new LinearLayout(this);

        leftButtons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        leftButtons.setGravity(
                Gravity.LEFT
                        | Gravity.CENTER_VERTICAL
        );

        leftButtons.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        leftButtons.addView(lightButton);

        settingsButton =
                new SettingsButton(this);

        settingsButton.setLayoutParams(
                new LinearLayout.LayoutParams(
                        70,
                        66
                )
        );

        settingsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        showSettingsFullscreen();
                    }
                }
        );

        leftButtons.addView(settingsButton);

        warningCard = new TextView(this);
        warningCard.setTextSize(11);
        warningCard.setTextColor(Color.BLACK);
        warningCard.setGravity(Gravity.CENTER);
        warningCard.setPadding(6, 3, 6, 3);
        warningCard.setVisibility(View.GONE);
        warningCard.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        66,
                        1
                )
        );
        warningCard.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showWarningFullscreen();
                        scheduleAutomaticReset();
                    }
                }
        );

        leftButtons.addView(warningCard);
        left.addView(leftButtons);

        LinearLayout center =
                new LinearLayout(this);

        center.setOrientation(
                LinearLayout.VERTICAL
        );

        center.setGravity(
                Gravity.CENTER
        );

        center.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        clockText = new TextView(this);
        clockText.setTextSize(38);
        clockText.setTextColor(
                Color.BLACK
        );
        clockText.setGravity(
                Gravity.CENTER
        );

        dateText = new TextView(this);
        dateText.setTextSize(15);
        dateText.setTextColor(
                Color.BLACK
        );
        dateText.setGravity(
                Gravity.CENTER
        );

        center.addView(clockText);
        center.addView(dateText);

        LinearLayout right =
                new LinearLayout(this);

        right.setOrientation(
                LinearLayout.VERTICAL
        );

        right.setGravity(
                Gravity.RIGHT
        );

        right.setPadding(
                0,
                0,
                8,
                0
        );

        right.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        currentWeatherText =
                createRightStatusText(
                        "Graz · Wetter folgt",
                        13
                );

        syncText =
                createRightStatusText(
                        "Kalender wird synchronisiert …",
                        12
                );

        countdownText =
                createRightStatusText(
                        "Nächste Synchronisierung in 15 Min.",
                        11
                );

        syncNowButton = createIconButton(
                "↻"
        );

        syncNowButton.setGravity(
                Gravity.CENTER
        );

        syncNowButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        syncNowButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        updateCheckRequestedByUser = false;
                        runSync();
                    }
                }
        );

        right.addView(currentWeatherText);
        right.addView(syncText);
        right.addView(countdownText);
        right.addView(syncNowButton);

        header.addView(left);
        header.addView(center);
        header.addView(right);

        rootLayout.addView(header);
    }

    private TextView createRightStatusText(
            String text,
            int size
    ) {
        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(
                Color.DKGRAY
        );
        view.setGravity(
                Gravity.RIGHT
        );

        return view;
    }

    private TextView createIconButton(
            String symbol
    ) {
        TextView button =
                new TextView(this);

        button.setText(symbol);
        button.setTextSize(30);
        button.setTextColor(
                Color.BLACK
        );
        button.setGravity(
                Gravity.CENTER
        );
        button.setPadding(
                18,
                8,
                18,
                8
        );
        button.setBackgroundColor(
                Color.TRANSPARENT
        );

        return button;
    }

    private void buildNavigationArea() {
        LinearLayout navigation =
                new LinearLayout(this);

        navigation.setOrientation(
                LinearLayout.HORIZONTAL
        );

        navigation.setGravity(
                Gravity.CENTER
        );

        navigation.setPadding(
                0,
                3,
                0,
                3
        );

        TextView previous =
                createNavigationButton("‹");

        TextView next =
                createNavigationButton("›");

        viewTitleText =
                new TextView(this);

        viewTitleText.setTextSize(14);
        viewTitleText.setTextColor(
                Color.BLACK
        );
        viewTitleText.setGravity(
                Gravity.CENTER
        );
        viewTitleText.setPadding(
                10,
                6,
                10,
                6
        );

        viewTitleText.setLayoutParams(
                new LinearLayout.LayoutParams(
                        170,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        currentViewButton =
                new TextView(this);

        currentViewButton.setText(
                "Aktuelle Ansicht"
        );
        currentViewButton.setTextSize(13);
        currentViewButton.setTextColor(
                Color.BLACK
        );
        currentViewButton.setGravity(
                Gravity.CENTER
        );
        currentViewButton.setPadding(
                12,
                6,
                12,
                6
        );
        currentViewButton.setVisibility(
                View.GONE
        );

        currentViewButton.setBackground(
                createBorderDrawable(
                        Color.WHITE,
                        Color.rgb(
                                145,
                                145,
                                145
                        ),
                        1
                )
        );

        previous.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        if (activeView
                                == VIEW_CALENDAR) {

                            navigationOffsetWeeks -=
                                    NAVIGATION_STEP_WEEKS;

                            currentViewButton.setVisibility(
                                    View.VISIBLE
                            );

                            showActiveView();
                            scheduleAutomaticReset();

                        } else {
                            showPreviousView();
                        }
                    }
                }
        );

        next.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        if (activeView
                                == VIEW_CALENDAR) {

                            navigationOffsetWeeks +=
                                    NAVIGATION_STEP_WEEKS;

                            currentViewButton.setVisibility(
                                    View.VISIBLE
                            );

                            showActiveView();
                            scheduleAutomaticReset();

                        } else {
                            showNextView();
                        }
                    }
                }
        );

        currentViewButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        activeView =
                                VIEW_CALENDAR;

                        navigationOffsetWeeks = 0;

                        currentViewButton.setVisibility(
                                View.GONE
                        );

                        resetHandler.removeCallbacks(
                                automaticReset
                        );

                        showActiveView();
                    }
                }
        );

        navigation.addView(previous);
        navigation.addView(viewTitleText);
        navigation.addView(currentViewButton);
        navigation.addView(next);

        rootLayout.addView(navigation);
    }

    private TextView createNavigationButton(
            String text
    ) {
        TextView button =
                new TextView(this);

        button.setText(text);
        button.setTextSize(34);
        button.setTextColor(
                Color.BLACK
        );
        button.setGravity(
                Gravity.CENTER
        );
        button.setPadding(
                26,
                2,
                26,
                2
        );

        button.setBackground(
                createBorderDrawable(
                        Color.WHITE,
                        Color.rgb(
                                145,
                                145,
                                145
                        ),
                        1
                )
        );

        return button;
    }

    private void buildWeatherArea() {
        weatherDaysRow =
                new LinearLayout(this);

        weatherDaysRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        weatherDaysRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        weatherDaysRow.setPadding(
                0,
                4,
                0,
                5
        );

        TextView loading =
                new TextView(this);

        loading.setText(
                "7-Tage-Vorhersage wird geladen …"
        );
        loading.setTextSize(12);
        loading.setTextColor(
                Color.DKGRAY
        );
        loading.setGravity(
                Gravity.CENTER
        );

        loading.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        weatherDaysRow.addView(loading);
        rootLayout.addView(weatherDaysRow);
    }

    private void buildContentArea() {
        contentContainer =
                new LinearLayout(this);

        contentContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        contentContainer.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        rootLayout.addView(contentContainer);
    }

    private void buildFooter() {
        TextView footer =
                new TextView(this);

        footer.setText(
                "Wischen: Kalender · Agenda · Jahresübersicht"
                        + "   |   Tippen: Details"
        );

        footer.setTextSize(11);
        footer.setTextColor(
                Color.DKGRAY
        );
        footer.setGravity(
                Gravity.CENTER
        );
        footer.setPadding(
                0,
                4,
                0,
                0
        );

        rootLayout.addView(footer);
    }

    @Override
    public boolean dispatchTouchEvent(
            MotionEvent event
    ) {
        int action =
                event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN
                && screensaverDialog != null
                && screensaverDialog.isShowing()) {
            consumeWakeGesture = false;
            autoLightSleeping = false;
            screensaverDialog.dismiss();
            scheduleScreensaver();
            scheduleAutomaticLightOff();
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            scheduleScreensaver();
            if (autoLightOffEnabled) {
                boolean wasSleeping = autoLightSleeping || !lightCurrentlyOn;
                autoLightSleeping = false;
                setLightState(true);
                scheduleAutomaticLightOff();
                if (wasSleeping) {
                    consumeWakeGesture = true;
                    return true;
                }
            }
        }

        if (consumeWakeGesture) {
            if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                consumeWakeGesture = false;
            }

            return true;
        }

        if (action == MotionEvent.ACTION_DOWN
                && isNightTime()
                && !lightCurrentlyOn) {

            wakeNightLightTemporarily();
            consumeWakeGesture = true;
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN
                && isNightTime()
                && temporaryNightWakeActive
                && lightCurrentlyOn) {

            nightHandler.removeCallbacks(
                    turnNightLightOff
            );

            nightHandler.postDelayed(
                    turnNightLightOff,
                    NIGHT_WAKE_TIME_MS
            );
        }

        return super.dispatchTouchEvent(
                event
        );
    }

    private void installTouchListener() {
        rootLayout.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {
                        gestureDetector.onTouchEvent(
                                event
                        );

                        return false;
                    }
                }
        );
    }

    private class SwipeListener
            extends GestureDetector.SimpleOnGestureListener {

        private static final int MIN_DISTANCE = 120;
        private static final int MIN_VELOCITY = 100;

        @Override
        public boolean onDown(
                MotionEvent event
        ) {
            return true;
        }

        @Override
        public boolean onFling(
                MotionEvent first,
                MotionEvent second,
                float velocityX,
                float velocityY
        ) {
            if (first == null
                    || second == null) {
                return false;
            }

            float distanceX =
                    second.getX()
                            - first.getX();

            if (Math.abs(distanceX)
                    < MIN_DISTANCE
                    || Math.abs(velocityX)
                    < MIN_VELOCITY) {

                return false;
            }

            if (distanceX < 0) {
                showNextView();
            } else {
                showPreviousView();
            }

            scheduleAutomaticReset();
            return true;
        }
    }

    private void showNextView() {
        activeView++;

        if (activeView > VIEW_YEAR) {
            activeView =
                    VIEW_CALENDAR;
        }

        showActiveView();
    }

    private void showPreviousView() {
        activeView--;

        if (activeView < VIEW_CALENDAR) {
            activeView =
                    VIEW_YEAR;
        }

        showActiveView();
    }

    private void showActiveView() {
        contentContainer.removeAllViews();

        if (activeView == VIEW_AGENDA) {
            viewTitleText.setText(
                    "Agenda"
            );

            currentViewButton.setVisibility(
                    View.VISIBLE
            );

            buildAgendaView();

        } else if (activeView == VIEW_YEAR) {
            viewTitleText.setText(
                    "Jahresübersicht"
            );

            currentViewButton.setVisibility(
                    View.VISIBLE
            );

            buildYearView();

        } else {
            viewTitleText.setText(
                    "Kalender"
            );

            drawCalendar(
                    currentEvents
            );
        }
    }

    private void buildAgendaView() {
        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        ArrayList<CalendarEvent> sorted =
                new ArrayList<CalendarEvent>(
                        currentEvents
                );

        Collections.sort(
                sorted,
                new Comparator<CalendarEvent>() {
                    @Override
                    public int compare(
                            CalendarEvent first,
                            CalendarEvent second
                    ) {
                        return first.getStartDate()
                                .compareTo(
                                        second.getStartDate()
                                );
                    }
                }
        );

        String todayKey =
                new SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.US
                ).format(
                        new Date()
                );

        int shown = 0;

        for (final CalendarEvent event
                : sorted) {

            if (event.getEndDateExclusive()
                    .compareTo(todayKey)
                    <= 0) {

                continue;
            }

            TextView row =
                    new TextView(this);

            row.setText(
                    event.getDisplayDateRange()
                            + "\n"
                            + event.getTitle()
            );

            row.setTextSize(14);
            row.setTextColor(
                    Color.BLACK
            );
            row.setPadding(
                    12,
                    8,
                    12,
                    8
            );

            row.setBackground(
                    createBorderDrawable(
                            Color.WHITE,
                            Color.rgb(
                                    175,
                                    175,
                                    175
                            ),
                            1
                    )
            );

            row.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(
                                View view
                        ) {
                            showEventDetails(
                                    event
                            );
                        }
                    }
            );

            list.addView(row);
            shown++;

            if (shown >= 30) {
                break;
            }
        }

        if (shown == 0) {
            TextView empty =
                    new TextView(this);

            empty.setText(
                    "Keine zukünftigen Termine gefunden."
            );
            empty.setTextSize(14);
            empty.setTextColor(
                    Color.DKGRAY
            );
            empty.setGravity(
                    Gravity.CENTER
            );
            empty.setPadding(
                    10,
                    30,
                    10,
                    30
            );

            list.addView(empty);
        }

        scrollView.addView(list);
        contentContainer.addView(
                scrollView
        );
    }

    private void buildYearView() {
        Calendar now =
                Calendar.getInstance();

        int year =
                now.get(
                        Calendar.YEAR
                );

        GridLayout yearGrid =
                new GridLayout(this);

        yearGrid.setColumnCount(4);

        int width =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels / 4 - 8;

        for (int monthIndex = 0;
             monthIndex < 12;
             monthIndex++) {

            final int selectedMonth =
                    monthIndex;

            TextView month =
                    new TextView(this);

            month.setText(
                    buildMiniMonth(
                            year,
                            monthIndex
                    )
            );

            month.setTextSize(10);
            month.setTextColor(
                    Color.BLACK
            );
            month.setGravity(
                    Gravity.TOP
                            | Gravity.CENTER_HORIZONTAL
            );
            month.setPadding(
                    4,
                    4,
                    4,
                    4
            );

            month.setBackground(
                    createBorderDrawable(
                            Color.WHITE,
                            Color.rgb(
                                    170,
                                    170,
                                    170
                            ),
                            1
                    )
            );

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();

            params.width = width;
            params.height = 170;
            params.setMargins(
                    2,
                    2,
                    2,
                    2
            );

            month.setLayoutParams(
                    params
            );

            month.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(
                                View view
                        ) {
                            jumpToMonth(
                                    selectedMonth
                            );
                        }
                    }
            );

            yearGrid.addView(month);
        }

        contentContainer.addView(
                yearGrid
        );
    }

    private String buildMiniMonth(
            int year,
            int monthIndex
    ) {
        Calendar month =
                Calendar.getInstance();

        month.set(
                year,
                monthIndex,
                1
        );

        StringBuilder text =
                new StringBuilder();

        text.append(
                new SimpleDateFormat(
                        "MMMM",
                        Locale.GERMAN
                ).format(
                        month.getTime()
                )
        );

        text.append(
                "\nMo Di Mi Do Fr Sa So\n"
        );

        int firstDay =
                month.get(
                        Calendar.DAY_OF_WEEK
                );

        int empty =
                firstDay
                        == Calendar.SUNDAY
                        ? 6
                        : firstDay - 2;

        for (int i = 0;
             i < empty;
             i++) {

            text.append("   ");
        }

        int days =
                month.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

        for (int day = 1;
             day <= days;
             day++) {

            text.append(
                    String.format(
                            Locale.GERMAN,
                            "%2d ",
                            day
                    )
            );

            if ((empty + day) % 7
                    == 0) {

                text.append("\n");
            }
        }

        return text.toString();
    }

    private void jumpToMonth(
            int monthIndex
    ) {
        Calendar now =
                Calendar.getInstance();

        Calendar target =
                Calendar.getInstance();

        target.set(
                now.get(
                        Calendar.YEAR
                ),
                monthIndex,
                1
        );

        int weeks =
                weeksBetween(
                        getCurrentWeekMonday(),
                        target
                );

        navigationOffsetWeeks =
                weeks;

        activeView =
                VIEW_CALENDAR;

        currentViewButton.setVisibility(
                View.VISIBLE
        );

        showActiveView();
        scheduleAutomaticReset();
    }

    private Calendar getCurrentWeekMonday() {
        Calendar monday =
                Calendar.getInstance();

        int dayOfWeek =
                monday.get(
                        Calendar.DAY_OF_WEEK
                );

        int daysSinceMonday =
                dayOfWeek
                        == Calendar.SUNDAY
                        ? 6
                        : dayOfWeek - 2;

        monday.add(
                Calendar.DAY_OF_MONTH,
                -daysSinceMonday
        );

        clearTime(monday);

        return monday;
    }

    private int weeksBetween(
            Calendar start,
            Calendar end
    ) {
        long diff =
                end.getTimeInMillis()
                        - start.getTimeInMillis();

        return (int) (
                diff
                        / (7L
                        * 24L
                        * 60L
                        * 60L
                        * 1000L)
        );
    }

    private void startNightModeController() {
        nightModeUpdater.run();
    }

    private final Runnable nightModeUpdater =
            new Runnable() {
                @Override
                public void run() {
                    boolean nightNow =
                            isNightTime();

                    if (nightNow
                            != lastNightState) {

                        manualLightOverride =
                                null;

                        temporaryNightWakeActive =
                                false;

                        lastNightState =
                                nightNow;
                    }

                    applyNightBrightness();

                    nightHandler.postDelayed(
                            this,
                            60L * 1000L
                    );
                }
            };

    private boolean isNightTime() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int today = scheduleIndexForCalendarDay(
                now.get(Calendar.DAY_OF_WEEK)
        );
        int previous = (today + 6) % 7;

        int todayStart = nightStartHours[today];
        int todayEnd = nightEndHours[today];

        if (todayStart < todayEnd
                && hour >= todayStart
                && hour < todayEnd) {
            return true;
        }

        if (todayStart > todayEnd
                && hour >= todayStart) {
            return true;
        }

        int previousStart = nightStartHours[previous];
        int previousEnd = nightEndHours[previous];

        return previousStart > previousEnd
                && hour < previousEnd;
    }

    private int scheduleIndexForCalendarDay(
            int calendarDay
    ) {
        return (calendarDay + 5) % 7;
    }

    private void applyNightBrightness() {
        if (autoLightSleeping) {
            setLightState(false);
            return;
        }

        if (manualLightOverride
                != null) {

            setLightState(
                    manualLightOverride
            );

            return;
        }

        if (isNightTime()) {
            setLightState(
                    temporaryNightWakeActive
            );
        } else {
            temporaryNightWakeActive =
                    false;

            nightHandler.removeCallbacks(
                    turnNightLightOff
            );

            setLightState(true);
        }
    }

    private void toggleLight() {
        final boolean newState =
                !lightCurrentlyOn;

        manualLightOverride =
                newState;

        temporaryNightWakeActive =
                false;

        nightHandler.removeCallbacks(
                turnNightLightOff
        );

        setLightState(newState);
    }

    private void wakeNightLightTemporarily() {
        if (manualLightOverride
                != null
                && !manualLightOverride) {

            return;
        }

        temporaryNightWakeActive =
                true;

        setLightState(true);

        nightHandler.removeCallbacks(
                turnNightLightOff
        );

        nightHandler.postDelayed(
                turnNightLightOff,
                NIGHT_WAKE_TIME_MS
        );
    }

    private final Runnable turnNightLightOff =
            new Runnable() {
                @Override
                public void run() {
                    temporaryNightWakeActive =
                            false;

                    applyNightBrightness();
                }
            };

    private void setLightState(
            boolean on
    ) {
        lightCurrentlyOn = on;

        WindowManager.LayoutParams params =
                getWindow().getAttributes();

        params.screenBrightness =
                on
                        ? Math.max(
                                0.01f,
                                brightnessPercent / 100.0f
                        )
                        : 0.0f;

        getWindow().setAttributes(
                params
        );

        if (lightButton != null) {
            lightButton.setOnState(on);
        }
    }

    private static class WeatherIconView
            extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final int weatherCode;
        private final boolean night;

        public WeatherIconView(
                android.content.Context context,
                int weatherCode
        ) {
            this(
                    context,
                    weatherCode,
                    false
            );
        }

        public WeatherIconView(
                android.content.Context context,
                int weatherCode,
                boolean night
        ) {
            super(context);

            this.weatherCode =
                    weatherCode;

            this.night =
                    night;

            setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        @Override
        protected void onDraw(
                Canvas canvas
        ) {
            super.onDraw(canvas);

            float width =
                    getWidth();

            float height =
                    getHeight();

            float stroke =
                    Math.max(
                            2.4f,
                            Math.min(
                                    width,
                                    height
                            ) / 22f
                    );

            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(
                    Paint.Cap.ROUND
            );
            paint.setStrokeJoin(
                    Paint.Join.ROUND
            );
            paint.setColor(
                    Color.BLACK
            );

            if (night
                    && weatherCode <= 2) {

                drawMoon(canvas);

                if (weatherCode == 2) {
                    drawCloud(canvas);
                }

                return;
            }

            if (weatherCode == 0) {
                drawSun(
                        canvas,
                        0.50f,
                        0.50f,
                        0.21f
                );

            } else if (weatherCode == 1
                    || weatherCode == 2) {

                drawSun(
                        canvas,
                        0.67f,
                        0.34f,
                        0.13f
                );

                drawCloud(canvas);

            } else if (weatherCode == 3) {
                drawCloud(canvas);

            } else if (weatherCode == 45
                    || weatherCode == 48) {

                drawCloud(canvas);
                drawFog(canvas);

            } else if ((weatherCode >= 51
                    && weatherCode <= 67)
                    || (weatherCode >= 80
                    && weatherCode <= 82)) {

                drawCloud(canvas);
                drawRain(canvas);

            } else if ((weatherCode >= 71
                    && weatherCode <= 77)
                    || weatherCode == 85
                    || weatherCode == 86) {

                drawCloud(canvas);
                drawSnow(canvas);

            } else if (weatherCode >= 95) {
                drawCloud(canvas);
                drawThunder(canvas);

            } else {
                drawCloud(canvas);
            }
        }

        private void drawSun(
                Canvas canvas,
                float centerXFraction,
                float centerYFraction,
                float radiusFraction
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            float centerX =
                    width
                            * centerXFraction;

            float centerY =
                    height
                            * centerYFraction;

            float radius =
                    Math.min(
                            width,
                            height
                    ) * radiusFraction;

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setColor(
                    Color.BLACK
            );

            canvas.drawCircle(
                    centerX,
                    centerY,
                    radius,
                    paint
            );

            for (int index = 0;
                 index < 8;
                 index++) {

                double angle =
                        Math.PI * 2.0
                                * index / 8.0;

                float startRadius =
                        radius * 1.42f;

                float endRadius =
                        radius * 1.88f;

                canvas.drawLine(
                        centerX
                                + (float) Math.cos(angle)
                                * startRadius,
                        centerY
                                + (float) Math.sin(angle)
                                * startRadius,
                        centerX
                                + (float) Math.cos(angle)
                                * endRadius,
                        centerY
                                + (float) Math.sin(angle)
                                * endRadius,
                        paint
                );
            }
        }

        private void drawMoon(
                Canvas canvas
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            float centerX =
                    width * 0.50f;

            float centerY =
                    height * 0.48f;

            float radius =
                    Math.min(
                            width,
                            height
                    ) * 0.26f;

            Path moon =
                    new Path();

            moon.moveTo(
                    centerX
                            + radius * 0.35f,
                    centerY
                            - radius
            );

            moon.cubicTo(
                    centerX
                            - radius * 0.70f,
                    centerY
                            - radius * 0.80f,
                    centerX
                            - radius * 0.85f,
                    centerY
                            + radius * 0.55f,
                    centerX
                            + radius * 0.10f,
                    centerY
                            + radius
            );

            moon.cubicTo(
                    centerX
                            - radius * 0.10f,
                    centerY
                            + radius * 0.42f,
                    centerX
                            + radius * 0.12f,
                    centerY
                            - radius * 0.28f,
                    centerX
                            + radius * 0.35f,
                    centerY
                            - radius
            );

            moon.close();

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.WHITE
            );

            canvas.drawPath(
                    moon,
                    paint
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setColor(
                    Color.BLACK
            );

            canvas.drawPath(
                    moon,
                    paint
            );

            float starRadius =
                    radius * 0.08f;

            canvas.drawCircle(
                    centerX
                            + radius * 0.95f,
                    centerY
                            - radius * 0.40f,
                    starRadius,
                    paint
            );

            canvas.drawCircle(
                    centerX
                            + radius * 0.72f,
                    centerY
                            - radius * 0.85f,
                    starRadius * 0.72f,
                    paint
            );
        }

        private void drawCloud(
                Canvas canvas
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            Path cloud =
                    new Path();

            cloud.moveTo(
                    width * 0.20f,
                    height * 0.68f
            );

            cloud.cubicTo(
                    width * 0.10f,
                    height * 0.65f,
                    width * 0.12f,
                    height * 0.48f,
                    width * 0.27f,
                    height * 0.46f
            );

            cloud.cubicTo(
                    width * 0.29f,
                    height * 0.30f,
                    width * 0.44f,
                    height * 0.24f,
                    width * 0.56f,
                    height * 0.36f
            );

            cloud.cubicTo(
                    width * 0.66f,
                    height * 0.31f,
                    width * 0.78f,
                    height * 0.38f,
                    width * 0.79f,
                    height * 0.50f
            );

            cloud.cubicTo(
                    width * 0.94f,
                    height * 0.51f,
                    width * 0.95f,
                    height * 0.69f,
                    width * 0.81f,
                    height * 0.73f
            );

            cloud.lineTo(
                    width * 0.27f,
                    height * 0.73f
            );

            cloud.cubicTo(
                    width * 0.23f,
                    height * 0.73f,
                    width * 0.21f,
                    height * 0.71f,
                    width * 0.20f,
                    height * 0.68f
            );

            cloud.close();

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.WHITE
            );

            canvas.drawPath(
                    cloud,
                    paint
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setColor(
                    Color.BLACK
            );

            canvas.drawPath(
                    cloud,
                    paint
            );
        }

        private void drawRain(
                Canvas canvas
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            for (int index = 0;
                 index < 3;
                 index++) {

                float x =
                        width
                                * (0.34f
                                + index * 0.16f);

                canvas.drawLine(
                        x,
                        height * 0.79f,
                        x - width * 0.035f,
                        height * 0.94f,
                        paint
                );
            }
        }

        private void drawSnow(
                Canvas canvas
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            for (int index = 0;
                 index < 3;
                 index++) {

                float centerX =
                        width
                                * (0.33f
                                + index * 0.17f);

                float centerY =
                        height * 0.86f;

                float radius =
                        Math.min(
                                width,
                                height
                        ) * 0.045f;

                canvas.drawLine(
                        centerX - radius,
                        centerY,
                        centerX + radius,
                        centerY,
                        paint
                );

                canvas.drawLine(
                        centerX,
                        centerY - radius,
                        centerX,
                        centerY + radius,
                        paint
                );

                canvas.drawLine(
                        centerX - radius * 0.70f,
                        centerY - radius * 0.70f,
                        centerX + radius * 0.70f,
                        centerY + radius * 0.70f,
                        paint
                );

                canvas.drawLine(
                        centerX + radius * 0.70f,
                        centerY - radius * 0.70f,
                        centerX - radius * 0.70f,
                        centerY + radius * 0.70f,
                        paint
                );
            }
        }

        private void drawThunder(
                Canvas canvas
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            Path bolt =
                    new Path();

            bolt.moveTo(
                    width * 0.55f,
                    height * 0.71f
            );

            bolt.lineTo(
                    width * 0.43f,
                    height * 0.84f
            );

            bolt.lineTo(
                    width * 0.54f,
                    height * 0.83f
            );

            bolt.lineTo(
                    width * 0.45f,
                    height * 0.98f
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setColor(
                    Color.BLACK
            );

            canvas.drawPath(
                    bolt,
                    paint
            );
        }

        private void drawFog(
                Canvas canvas
        ) {
            float width =
                    getWidth();

            float height =
                    getHeight();

            canvas.drawLine(
                    width * 0.20f,
                    height * 0.82f,
                    width * 0.80f,
                    height * 0.82f,
                    paint
            );

            canvas.drawLine(
                    width * 0.29f,
                    height * 0.91f,
                    width * 0.71f,
                    height * 0.91f,
                    paint
            );
        }
    }

    private static class SettingsButton
            extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        public SettingsButton(
                android.content.Context context
        ) {
            super(context);
            setClickable(true);
            setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        @Override
        protected void onDraw(
                Canvas canvas
        ) {
            super.onDraw(canvas);

            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(3f);
            paint.setStrokeCap(
                    Paint.Cap.ROUND
            );

            float width = getWidth();
            float height = getHeight();

            float[] rows = {
                    height * 0.30f,
                    height * 0.50f,
                    height * 0.70f
            };

            float[] knobs = {
                    width * 0.38f,
                    width * 0.67f,
                    width * 0.48f
            };

            for (int index = 0;
                 index < rows.length;
                 index++) {

                canvas.drawLine(
                        width * 0.18f,
                        rows[index],
                        width * 0.82f,
                        rows[index],
                        paint
                );

                paint.setStyle(
                        Paint.Style.FILL
                );

                canvas.drawCircle(
                        knobs[index],
                        rows[index],
                        5.5f,
                        paint
                );

                paint.setStyle(
                        Paint.Style.STROKE
                );
            }
        }
    }

    private static class BulbButton extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private boolean onState = true;

        public BulbButton(
                android.content.Context context
        ) {
            super(context);
            setClickable(true);
            setFocusable(true);
            setBackgroundColor(
                    Color.TRANSPARENT
            );
        }

        public void setOnState(
                boolean on
        ) {
            onState = on;
            invalidate();
        }

        @Override
        protected void onDraw(
                Canvas canvas
        ) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            float centerX = width / 2f;
            float bulbCenterY = height * 0.40f;
            float radius = Math.min(
                    width,
                    height
            ) * 0.22f;

            paint.setStrokeWidth(3.2f);
            paint.setStrokeCap(
                    Paint.Cap.ROUND
            );
            paint.setStrokeJoin(
                    Paint.Join.ROUND
            );
            paint.setColor(
                    Color.BLACK
            );

            if (onState) {
                paint.setStyle(
                        Paint.Style.FILL
                );
                paint.setColor(
                        Color.rgb(
                                225,
                                225,
                                225
                        )
                );

                canvas.drawCircle(
                        centerX,
                        bulbCenterY,
                        radius,
                        paint
                );

                paint.setStyle(
                        Paint.Style.STROKE
                );
                paint.setColor(
                        Color.BLACK
                );

                canvas.drawCircle(
                        centerX,
                        bulbCenterY,
                        radius,
                        paint
                );
            } else {
                paint.setStyle(
                        Paint.Style.STROKE
                );

                canvas.drawCircle(
                        centerX,
                        bulbCenterY,
                        radius,
                        paint
                );
            }

            float neckTop =
                    bulbCenterY + radius * 0.72f;

            float neckBottom =
                    bulbCenterY + radius * 1.32f;

            RectF neck =
                    new RectF(
                            centerX - radius * 0.48f,
                            neckTop,
                            centerX + radius * 0.48f,
                            neckBottom
                    );

            paint.setStyle(
                    Paint.Style.STROKE
            );
            paint.setColor(
                    Color.BLACK
            );

            canvas.drawRoundRect(
                    neck,
                    4f,
                    4f,
                    paint
            );

            float baseY =
                    neckBottom + 4f;

            canvas.drawLine(
                    centerX - radius * 0.38f,
                    baseY,
                    centerX + radius * 0.38f,
                    baseY,
                    paint
            );

            canvas.drawLine(
                    centerX - radius * 0.28f,
                    baseY + 6f,
                    centerX + radius * 0.28f,
                    baseY + 6f,
                    paint
            );

            if (onState) {
                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        -90f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        -45f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        0f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        45f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        180f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        225f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        270f
                );

                drawRay(
                        canvas,
                        centerX,
                        bulbCenterY,
                        radius,
                        315f
                );
            } else {
                paint.setStrokeWidth(2.4f);

                canvas.drawLine(
                        centerX - radius * 0.54f,
                        bulbCenterY - radius * 0.54f,
                        centerX + radius * 0.54f,
                        bulbCenterY + radius * 0.54f,
                        paint
                );
            }
        }

        private void drawRay(
                Canvas canvas,
                float centerX,
                float centerY,
                float radius,
                float degrees
        ) {
            double radians =
                    Math.toRadians(
                            degrees
                    );

            float startRadius =
                    radius * 1.42f;

            float endRadius =
                    radius * 1.82f;

            float startX =
                    centerX
                            + (float) Math.cos(
                            radians
                    ) * startRadius;

            float startY =
                    centerY
                            + (float) Math.sin(
                            radians
                    ) * startRadius;

            float endX =
                    centerX
                            + (float) Math.cos(
                            radians
                    ) * endRadius;

            float endY =
                    centerY
                            + (float) Math.sin(
                            radians
                    ) * endRadius;

            canvas.drawLine(
                    startX,
                    startY,
                    endX,
                    endY,
                    paint
            );
        }
    }

    private void loadDisplaySettings() {
        SharedPreferences preferences =
                getSharedPreferences(
                        SETTINGS_PREFS,
                        MODE_PRIVATE
                );

        int oldStart = preferences.getInt(
                "night_start_hour",
                22
        );
        int oldEnd = preferences.getInt(
                "night_end_hour",
                6
        );

        for (int index = 0;
             index < 7;
             index++) {
            nightStartHours[index] = preferences.getInt(
                    "night_start_" + index,
                    index < 5 ? oldStart : 23
            );
            nightEndHours[index] = preferences.getInt(
                    "night_end_" + index,
                    index < 5 ? oldEnd : 8
            );
        }

        brightnessPercent =
                preferences.getInt(
                        "brightness_percent",
                        100
                );

        screensaverEnabled = preferences.getBoolean(
                "screensaver_enabled", true
        );
        screensaverIdleMinutes = preferences.getInt(
                "screensaver_idle_minutes", 10
        );
        screensaverChangeMinutes = preferences.getInt(
                "screensaver_change_minutes", 15
        );
        screensaverLightOn = preferences.getBoolean(
                "screensaver_light_on", false
        );
        screensaverLightStartHour = preferences.getInt(
                "screensaver_light_start", 7
        );
        screensaverLightEndHour = preferences.getInt(
                "screensaver_light_end", 22
        );
        autoLightOffEnabled = preferences.getBoolean(
                "auto_light_off_enabled", false
        );
        autoLightOffMinutes = preferences.getInt(
                "auto_light_off_minutes", 5
        );
    }

    private void saveDisplaySettings() {
        SharedPreferences.Editor editor = getSharedPreferences(
                SETTINGS_PREFS,
                MODE_PRIVATE
        ).edit();

        editor.putInt(
                "brightness_percent",
                brightnessPercent
        );

        editor.putBoolean("screensaver_enabled", screensaverEnabled);
        editor.putInt("screensaver_idle_minutes", screensaverIdleMinutes);
        editor.putInt("screensaver_change_minutes", screensaverChangeMinutes);
        editor.putBoolean("screensaver_light_on", screensaverLightOn);
        editor.putInt("screensaver_light_start", screensaverLightStartHour);
        editor.putInt("screensaver_light_end", screensaverLightEndHour);
        editor.putBoolean("auto_light_off_enabled", autoLightOffEnabled);
        editor.putInt("auto_light_off_minutes", autoLightOffMinutes);

        for (int index = 0;
             index < 7;
             index++) {
            editor.putInt(
                    "night_start_" + index,
                    nightStartHours[index]
            );
            editor.putInt(
                    "night_end_" + index,
                    nightEndHours[index]
            );
        }

        editor.apply();
    }

    private void scheduleScreensaver() {
        screensaverHandler.removeCallbacks(startScreensaver);
        screensaverHandler.removeCallbacks(changeScreensaverImage);

        if (!screensaverEnabled) {
            return;
        }

        screensaverHandler.postDelayed(
                startScreensaver,
                Math.max(1, screensaverIdleMinutes) * 60L * 1000L
        );
    }

    private void scheduleAutomaticLightOff() {
        lightIdleHandler.removeCallbacks(turnLightOffAfterInactivity);
        if (!autoLightOffEnabled) {
            autoLightSleeping = false;
            return;
        }
        lightIdleHandler.postDelayed(
                turnLightOffAfterInactivity,
                Math.max(1, autoLightOffMinutes) * 60L * 1000L
        );
    }

    private final Runnable turnLightOffAfterInactivity = new Runnable() {
        @Override
        public void run() {
            if (!autoLightOffEnabled) {
                return;
            }
            autoLightSleeping = true;
            setLightState(false);
        }
    };

    private final Runnable startScreensaver = new Runnable() {
        @Override
        public void run() {
            screensaverEnabled = getSharedPreferences(
                    SETTINGS_PREFS,
                    MODE_PRIVATE
            ).getBoolean("screensaver_enabled", screensaverEnabled);
            if (!screensaverEnabled) {
                screensaverHandler.removeCallbacks(changeScreensaverImage);
                return;
            }
            showScreensaver();
        }
    };

    private final Runnable changeScreensaverImage = new Runnable() {
        @Override
        public void run() {
            if (screensaverDialog != null
                    && screensaverDialog.isShowing()) {
                loadScreensaverImage();
                screensaverHandler.postDelayed(
                        this,
                        Math.max(1, screensaverChangeMinutes) * 60L * 1000L
                );
            }
        }
    };

    private ImageView screensaverImage;
    private TextView screensaverStatus;

    private void showScreensaver() {
        if (!screensaverEnabled
                || isFinishing()
                || (settingsDialog != null && settingsDialog.isShowing())
                || (weatherDialog != null && weatherDialog.isShowing())
                || (warningDialog != null && warningDialog.isShowing())) {
            scheduleScreensaver();
            return;
        }

        screensaverDialog = new Dialog(
                this,
                android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
        );

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setBackgroundColor(Color.BLACK);

        screensaverImage = new ImageView(this);
        screensaverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        screensaverImage.setBackgroundColor(Color.BLACK);
        page.addView(
                screensaverImage,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        screensaverStatus = new TextView(this);
        screensaverStatus.setText("Bild wird geladen …");
        screensaverStatus.setTextSize(13);
        screensaverStatus.setTextColor(Color.WHITE);
        screensaverStatus.setGravity(Gravity.CENTER);
        screensaverStatus.setPadding(0, 4, 0, 6);
        page.addView(screensaverStatus);

        page.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getActionMasked() != MotionEvent.ACTION_UP) {
                    return true;
                }
                float third = Math.max(1, view.getWidth()) / 3.0f;
                if (event.getX() < third) {
                    loadScreensaverRelative(-1);
                } else if (event.getX() >= third * 2.0f) {
                    loadScreensaverRelative(1);
                } else {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (screensaverDialog != null
                                    && screensaverDialog.isShowing()) {
                                screensaverDialog.dismiss();
                            }
                        }
                    }, 150L);
                }
                return true;
            }
        });

        screensaverDialog.setContentView(page);
        screensaverDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        screensaverHandler.removeCallbacks(changeScreensaverImage);
                        consumeWakeGesture = false;
                        autoLightSleeping = false;
                        screensaverDialog = null;
                        applyNightBrightness();
                        scheduleScreensaver();
                        scheduleAutomaticLightOff();
                        rootLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                rootLayout.setEnabled(true);
                                rootLayout.setClickable(true);
                                rootLayout.setFocusableInTouchMode(true);
                                rootLayout.requestFocus();
                                getWindow().getDecorView().setSystemUiVisibility(
                                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                );
                            }
                        }, 100L);
                    }
                }
        );
        screensaverDialog.show();
        applyScreensaverLight();
        loadScreensaverImage();
        screensaverHandler.postDelayed(
                changeScreensaverImage,
                Math.max(1, screensaverChangeMinutes) * 60L * 1000L
        );
    }

    private void loadScreensaverImage() {
        ScreensaverSync.loadRandom(new ScreensaverSync.Callback() {
            @Override
            public void onFinished(android.graphics.Bitmap bitmap) {
                if (screensaverDialog != null
                        && screensaverDialog.isShowing()
                        && screensaverImage != null) {
                    screensaverImage.setImageBitmap(bitmap);
                    screensaverStatus.setText(
                            "Links: zurück · Mitte: Startseite · Rechts: weiter"
                    );
                }
            }

            @Override
            public void onError(Exception exception) {
                if (screensaverStatus != null) {
                    screensaverStatus.setText("Fotoalbum momentan nicht verfügbar");
                }
            }
        });
    }

    private void loadScreensaverRelative(int direction) {
        if (screensaverStatus != null) {
            screensaverStatus.setText("Bild wird geladen …");
        }
        ScreensaverSync.loadRelative(direction, new ScreensaverSync.Callback() {
            @Override
            public void onFinished(android.graphics.Bitmap bitmap) {
                if (screensaverDialog != null
                        && screensaverDialog.isShowing()
                        && screensaverImage != null) {
                    screensaverImage.setImageBitmap(bitmap);
                    screensaverStatus.setText(
                            "Links: zurück · Mitte: Startseite · Rechts: weiter"
                    );
                }
            }

            @Override
            public void onError(Exception exception) {
                if (screensaverStatus != null) {
                    screensaverStatus.setText("Fotoalbum momentan nicht verfügbar");
                }
            }
        });
    }

    private void applyScreensaverLight() {
        if (!screensaverLightOn) {
            setScreensaverLightState(false);
            return;
        }

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        boolean inside;
        if (screensaverLightStartHour == screensaverLightEndHour) {
            inside = true;
        } else if (screensaverLightStartHour < screensaverLightEndHour) {
            inside = hour >= screensaverLightStartHour
                    && hour < screensaverLightEndHour;
        } else {
            inside = hour >= screensaverLightStartHour
                    || hour < screensaverLightEndHour;
        }

        if (inside) {
            setScreensaverLightState(true);
        } else {
            applyNightBrightness();
            setScreensaverDialogBrightness(lightCurrentlyOn);
        }
    }

    private void setScreensaverLightState(boolean on) {
        setLightState(on);
        setScreensaverDialogBrightness(on);
    }

    private void setScreensaverDialogBrightness(boolean on) {
        if (screensaverDialog == null
                || screensaverDialog.getWindow() == null) {
            return;
        }
        WindowManager.LayoutParams params =
                screensaverDialog.getWindow().getAttributes();
        params.screenBrightness = on
                ? Math.max(0.01f, brightnessPercent / 100.0f)
                : 0.0f;
        screensaverDialog.getWindow().setAttributes(params);
    }

    private void showSettingsFullscreen() {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }

        settingsDialog = new Dialog(
                this,
                android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
        );

        LinearLayout page = createSettingsPage("Einstellungen");
        page.addView(createSettingsMenuButton(
                "Licht und Nachtmodus",
                "Helligkeit und Zeiten pro Wochentag",
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        showLightSettings();
                    }
                }
        ));
        page.addView(createSettingsMenuButton(
                "Bildschirmschoner",
                "Start, Bildwechsel und Licht festlegen",
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        showScreensaverSettings();
                    }
                }
        ));
        page.addView(createSettingsMenuButton(
                "Synchronisierung",
                "Kalender, Wetter und Warnungen jetzt aktualisieren",
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        settingsDialog.dismiss();
                        runSync();
                    }
                }
        ));
        page.addView(createSettingsMenuButton(
                "Updates",
                "Nach einer neuen DennisOS-Version suchen",
                new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        UpdateManager.checkForUpdate(MainActivity.this, true);
                    }
                }
        ));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        settingsDialog.setContentView(scroll);
        settingsDialog.show();
        applyDialogBrightness(settingsDialog);
        scheduleAutomaticReset();
    }

    private LinearLayout createSettingsPage(String titleText) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);
        page.setPadding(26, 14, 26, 14);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextSize(38);
        close.setTextColor(Color.BLACK);
        close.setGravity(Gravity.CENTER);
        close.setLayoutParams(new LinearLayout.LayoutParams(80, 72));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                settingsDialog.dismiss();
            }
        });

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, 72, 1));

        TextView spacer = new TextView(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(80, 72));
        header.addView(close);
        header.addView(title);
        header.addView(spacer);
        page.addView(header);
        page.addView(createHorizontalLine());
        return page;
    }

    private View createSettingsMenuButton(
            String title,
            String description,
            View.OnClickListener listener
    ) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setPadding(22, 16, 22, 16);
        button.setBackground(createBorderDrawable(
                Color.WHITE, Color.rgb(140, 140, 140), 2
        ));
        button.setOnClickListener(listener);

        TextView heading = createWeatherText(title + "  ›", 21, Color.BLACK);
        TextView detail = createWeatherText(description, 14, Color.DKGRAY);
        detail.setPadding(0, 4, 0, 0);
        button.addView(heading);
        button.addView(detail);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(80, 14, 80, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void showScreensaverSettings() {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }
        settingsDialog = new Dialog(
                this,
                android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
        );
        final LinearLayout page = createSettingsPage("Bildschirmschoner");

        final TextView enabled = createSettingsMenuButtonText(
                "Bildschirmschoner: " + (screensaverEnabled ? "EIN" : "AUS")
        );
        enabled.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                screensaverEnabled = !screensaverEnabled;
                enabled.setText("Bildschirmschoner: "
                        + (screensaverEnabled ? "EIN" : "AUS"));
                saveDisplaySettings();
                if (!screensaverEnabled
                        && screensaverDialog != null
                        && screensaverDialog.isShowing()) {
                    screensaverDialog.dismiss();
                }
                scheduleScreensaver();
            }
        });
        page.addView(enabled);

        page.addView(createSettingTitle("Start nach Inaktivität"));
        final TextView idleValue = createSettingValue(
                screensaverIdleMinutes + " Minuten"
        );
        page.addView(idleValue);
        SeekBar idle = new SeekBar(this);
        idle.setMax(59);
        idle.setProgress(Math.max(0, screensaverIdleMinutes - 1));
        idle.setPadding(50, 2, 50, 10);
        idle.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void changed(int progress) {
                screensaverIdleMinutes = progress + 1;
                idleValue.setText(screensaverIdleMinutes + " Minuten");
                saveDisplaySettings();
                scheduleScreensaver();
            }
        });
        page.addView(idle);

        page.addView(createSettingTitle("Neues Bild nach"));
        final TextView changeValue = createSettingValue(
                screensaverChangeMinutes + " Minuten"
        );
        page.addView(changeValue);
        SeekBar change = new SeekBar(this);
        change.setMax(59);
        change.setProgress(Math.max(0, screensaverChangeMinutes - 1));
        change.setPadding(50, 2, 50, 10);
        change.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void changed(int progress) {
                screensaverChangeMinutes = progress + 1;
                changeValue.setText(screensaverChangeMinutes + " Minuten");
                saveDisplaySettings();
            }
        });
        page.addView(change);

        final TextView light = createSettingsMenuButtonText(
                "Licht im Bildschirmschoner: "
                        + (screensaverLightOn ? "EIN" : "AUS")
        );
        light.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                screensaverLightOn = !screensaverLightOn;
                light.setText("Licht im Bildschirmschoner: "
                        + (screensaverLightOn ? "EIN" : "AUS"));
                saveDisplaySettings();
            }
        });
        page.addView(light);

        final TextView lightTimes = createSettingValue(
                formatHour(screensaverLightStartHour) + " bis "
                        + formatHour(screensaverLightEndHour)
        );
        page.addView(createSettingTitle("Licht-Einstellung gilt von"));
        page.addView(lightTimes);
        page.addView(createSettingLabel("Beginn"));
        SeekBar lightStart = new SeekBar(this);
        lightStart.setMax(23);
        lightStart.setProgress(screensaverLightStartHour);
        lightStart.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void changed(int progress) {
                screensaverLightStartHour = progress;
                lightTimes.setText(formatHour(screensaverLightStartHour)
                        + " bis " + formatHour(screensaverLightEndHour));
                saveDisplaySettings();
            }
        });
        page.addView(lightStart);
        page.addView(createSettingLabel("Ende"));
        SeekBar lightEnd = new SeekBar(this);
        lightEnd.setMax(23);
        lightEnd.setProgress(screensaverLightEndHour);
        lightEnd.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void changed(int progress) {
                screensaverLightEndHour = progress;
                lightTimes.setText(formatHour(screensaverLightStartHour)
                        + " bis " + formatHour(screensaverLightEndHour));
                saveDisplaySettings();
            }
        });
        page.addView(lightEnd);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        settingsDialog.setContentView(scroll);
        settingsDialog.show();
        applyDialogBrightness(settingsDialog);
        scheduleAutomaticReset();
    }

    private TextView createSettingsMenuButtonText(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(18);
        button.setTextColor(Color.BLACK);
        button.setGravity(Gravity.CENTER);
        button.setPadding(18, 14, 18, 14);
        button.setBackground(createBorderDrawable(
                Color.WHITE, Color.rgb(130, 130, 130), 2
        ));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(80, 14, 80, 4);
        button.setLayoutParams(params);
        return button;
    }

    private void showLightSettings() {
        if (settingsDialog != null
                && settingsDialog.isShowing()) {

            settingsDialog.dismiss();
        }

        settingsDialog =
                new Dialog(
                        this,
                        android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
                );

        LinearLayout page =
                new LinearLayout(this);

        page.setOrientation(
                LinearLayout.VERTICAL
        );

        page.setBackgroundColor(
                Color.WHITE
        );

        page.setPadding(
                26,
                14,
                26,
                14
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView close =
                new TextView(this);

        close.setText("×");
        close.setTextSize(38);
        close.setTextColor(Color.BLACK);
        close.setGravity(Gravity.CENTER);

        close.setLayoutParams(
                new LinearLayout.LayoutParams(
                        80,
                        72
                )
        );

        close.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        settingsDialog.dismiss();
                    }
                }
        );

        TextView title =
                new TextView(this);

        title.setText("Licht und Nachtmodus");
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        title.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        72,
                        1
                )
        );

        TextView spacer =
                new TextView(this);

        spacer.setLayoutParams(
                new LinearLayout.LayoutParams(
                        80,
                        72
                )
        );

        header.addView(close);
        header.addView(title);
        header.addView(spacer);

        page.addView(header);
        page.addView(createHorizontalLine());

        final TextView brightnessValue =
                createSettingValue(
                        brightnessPercent
                                + " %"
                );

        page.addView(
                createSettingTitle(
                        "Bildschirmhelligkeit"
                )
        );

        page.addView(brightnessValue);

        SeekBar brightness =
                new SeekBar(this);

        brightness.setMax(100);
        brightness.setProgress(
                brightnessPercent
        );

        brightness.setPadding(
                50,
                4,
                50,
                14
        );

        brightness.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        brightnessPercent =
                                Math.max(
                                        1,
                                        progress
                                );

                        brightnessValue.setText(
                                brightnessPercent
                                        + " %"
                        );

                        setLightState(true);

                        if (settingsDialog != null
                                && settingsDialog.isShowing()) {
                            applyDialogBrightness(
                                    settingsDialog
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                        saveDisplaySettings();
                    }
                }
        );

        page.addView(brightness);
        page.addView(createHorizontalLine());

        final TextView autoLightToggle = createSettingsMenuButtonText(
                "Licht automatisch aus: " + (autoLightOffEnabled ? "EIN" : "AUS")
        );
        autoLightToggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                autoLightOffEnabled = !autoLightOffEnabled;
                autoLightToggle.setText("Licht automatisch aus: "
                        + (autoLightOffEnabled ? "EIN" : "AUS"));
                if (!autoLightOffEnabled) {
                    autoLightSleeping = false;
                    applyNightBrightness();
                }
                saveDisplaySettings();
                scheduleAutomaticLightOff();
            }
        });
        page.addView(autoLightToggle);

        page.addView(createSettingTitle("Licht aus nach Inaktivität"));
        final TextView autoLightValue = createSettingValue(
                autoLightOffMinutes + " Minuten"
        );
        page.addView(autoLightValue);
        SeekBar autoLightDelay = new SeekBar(this);
        autoLightDelay.setMax(59);
        autoLightDelay.setProgress(Math.max(0, autoLightOffMinutes - 1));
        autoLightDelay.setPadding(50, 2, 50, 10);
        autoLightDelay.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void changed(int progress) {
                autoLightOffMinutes = progress + 1;
                autoLightValue.setText(autoLightOffMinutes + " Minuten");
                saveDisplaySettings();
                scheduleAutomaticLightOff();
            }
        });
        page.addView(autoLightDelay);
        page.addView(createHorizontalLine());

        page.addView(
                createSettingTitle(
                        "Automatische Nachtabschaltung"
                )
        );

        String[] scheduleDays = {
                "Montag", "Dienstag", "Mittwoch",
                "Donnerstag", "Freitag", "Samstag",
                "Sonntag"
        };

        for (int dayIndex = 0;
             dayIndex < scheduleDays.length;
             dayIndex++) {
            final int selectedDay = dayIndex;

            page.addView(
                    createSettingTitle(
                            scheduleDays[dayIndex]
                    )
            );

            final TextView dayTimes = createSettingValue(
                    formatHour(nightStartHours[dayIndex])
                            + " bis "
                            + formatHour(nightEndHours[dayIndex])
            );
            page.addView(dayTimes);

            page.addView(createSettingLabel(
                    "Licht aus ab"
            ));

            SeekBar dayStartSlider = new SeekBar(this);
            dayStartSlider.setMax(23);
            dayStartSlider.setProgress(
                    nightStartHours[dayIndex]
            );
            dayStartSlider.setPadding(50, 0, 50, 6);
            dayStartSlider.setOnSeekBarChangeListener(
                    new SimpleSeekListener() {
                        @Override
                        public void changed(int progress) {
                            nightStartHours[selectedDay] = progress;
                            dayTimes.setText(
                                    formatHour(progress)
                                            + " bis "
                                            + formatHour(
                                            nightEndHours[selectedDay]
                                    )
                            );
                            saveDisplaySettings();
                        }
                    }
            );
            page.addView(dayStartSlider);

            page.addView(createSettingLabel(
                    "Licht an ab"
            ));

            SeekBar dayEndSlider = new SeekBar(this);
            dayEndSlider.setMax(23);
            dayEndSlider.setProgress(
                    nightEndHours[dayIndex]
            );
            dayEndSlider.setPadding(50, 0, 50, 10);
            dayEndSlider.setOnSeekBarChangeListener(
                    new SimpleSeekListener() {
                        @Override
                        public void changed(int progress) {
                            nightEndHours[selectedDay] = progress;
                            dayTimes.setText(
                                    formatHour(
                                            nightStartHours[selectedDay]
                                    )
                                            + " bis "
                                            + formatHour(progress)
                            );
                            saveDisplaySettings();
                        }
                    }
            );
            page.addView(dayEndSlider);
            page.addView(createHorizontalLine());
        }
        page.addView(createHorizontalLine());

        TextView updateButton =
                new TextView(this);

        updateButton.setText(
                "Nach DennisOS-Update suchen"
        );

        updateButton.setTextSize(18);
        updateButton.setTextColor(Color.BLACK);
        updateButton.setGravity(Gravity.CENTER);
        updateButton.setPadding(
                18,
                16,
                18,
                16
        );

        updateButton.setBackground(
                createBorderDrawable(
                        Color.WHITE,
                        Color.rgb(
                                120,
                                120,
                                120
                        ),
                        2
                )
        );

        updateButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        UpdateManager.checkForUpdate(
                                MainActivity.this,
                                true
                        );
                    }
                }
        );

        LinearLayout.LayoutParams updateParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        updateParams.setMargins(
                100,
                18,
                100,
                0
        );

        updateButton.setLayoutParams(
                updateParams
        );

        page.addView(updateButton);

        TextView note =
                createSettingLabel(
                        "Änderungen werden sofort gespeichert."
                );

        note.setGravity(Gravity.CENTER);
        note.setPadding(
                0,
                18,
                0,
                0
        );

        page.addView(note);

        ScrollView settingsScroll = new ScrollView(this);
        settingsScroll.setFillViewport(true);
        settingsScroll.addView(page);
        settingsDialog.setContentView(settingsScroll);

        settingsDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(
                            DialogInterface dialog
                    ) {
                        saveDisplaySettings();
                        applyNightBrightness();
                    }
                }
        );

        settingsDialog.show();
        applyDialogBrightness(
                settingsDialog
        );
        scheduleAutomaticReset();
    }

    private TextView createSettingTitle(
            String text
    ) {
        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(18);
        view.setTextColor(Color.BLACK);
        view.setPadding(
                10,
                12,
                10,
                4
        );

        return view;
    }

    private TextView createSettingLabel(
            String text
    ) {
        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(Color.DKGRAY);
        view.setPadding(
                50,
                5,
                50,
                0
        );

        return view;
    }

    private TextView createSettingValue(
            String text
    ) {
        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(18);
        view.setTextColor(Color.BLACK);
        view.setGravity(Gravity.CENTER);
        view.setPadding(
                0,
                0,
                0,
                2
        );

        return view;
    }

    private String formatHour(
            int hour
    ) {
        return String.format(
                Locale.GERMAN,
                "%02d:00 Uhr",
                hour
        );
    }

    private abstract static class SimpleSeekListener
            implements SeekBar.OnSeekBarChangeListener {

        public abstract void changed(
                int progress
        );

        @Override
        public void onProgressChanged(
                SeekBar seekBar,
                int progress,
                boolean fromUser
        ) {
            changed(progress);
        }

        @Override
        public void onStartTrackingTouch(
                SeekBar seekBar
        ) {
        }

        @Override
        public void onStopTrackingTouch(
                SeekBar seekBar
        ) {
        }
    }

    private void startClock() {
        clockUpdater.run();
    }

    private final Runnable clockUpdater =
            new Runnable() {
                @Override
                public void run() {
                    Date now =
                            new Date();

                    clockText.setText(
                            new SimpleDateFormat(
                                    "HH:mm",
                                    Locale.GERMAN
                            ).format(now)
                    );

                    dateText.setText(
                            new SimpleDateFormat(
                                    "EEEE, dd. MMMM yyyy",
                                    Locale.GERMAN
                            ).format(now)
                    );

                    clockHandler.postDelayed(
                            this,
                            30000L
                    );
                }
            };

    private void startCountdown() {
        countdownUpdater.run();
    }

    private final Runnable countdownUpdater =
            new Runnable() {
                @Override
                public void run() {
                    long lastSync =
                            StorageHelper.loadSyncTime(
                                    MainActivity.this
                            );

                    long nextSync =
                            lastSync
                                    + SYNC_INTERVAL_MS;

                    long remaining =
                            nextSync
                                    - System.currentTimeMillis();

                    if (lastSync == 0
                            || remaining <= 0) {

                        countdownText.setText(
                                "Synchronisierung läuft …"
                        );

                    } else {
                        long minutes =
                                (remaining + 59999L)
                                        / 60000L;

                        countdownText.setText(
                                String.format(
                                        Locale.GERMAN,
                                        "Nächste Synchronisierung in %d Min.",
                                        minutes
                                )
                        );
                    }

                    countdownHandler.postDelayed(
                            this,
                            1000L
                    );
                }
            };

    private void startBatteryUpdates() {
        batteryUpdater.run();
    }

    private final Runnable batteryUpdater =
            new Runnable() {
                @Override
                public void run() {
                    updateBatteryText();

                    batteryHandler.postDelayed(
                            this,
                            60000L
                    );
                }
            };

    private void updateBatteryText() {
        Intent batteryIntent =
                registerReceiver(
                        null,
                        new IntentFilter(
                                Intent.ACTION_BATTERY_CHANGED
                        )
                );

        if (batteryIntent == null) {
            batteryText.setText(
                    "Akku nicht verfügbar"
            );

            return;
        }

        int level =
                batteryIntent.getIntExtra(
                        BatteryManager.EXTRA_LEVEL,
                        -1
                );

        int scale =
                batteryIntent.getIntExtra(
                        BatteryManager.EXTRA_SCALE,
                        -1
                );

        int status =
                batteryIntent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                );

        int percent = 0;

        if (level >= 0
                && scale > 0) {

            percent =
                    Math.round(
                            level
                                    * 100f
                                    / scale
                    );
        }

        boolean charging =
                status
                        == BatteryManager.BATTERY_STATUS_CHARGING
                        || status
                        == BatteryManager.BATTERY_STATUS_FULL;

        batteryText.setText(
                charging
                        ? "⚡ Akku "
                        + percent
                        + " %"
                        : "Akku "
                        + percent
                        + " %"
        );
    }

    private final Runnable automaticSync =
            new Runnable() {
                @Override
                public void run() {
                    runSync();

                    syncHandler.postDelayed(
                            this,
                            SYNC_INTERVAL_MS
                    );
                }
            };

    private void runSync() {
        if (syncRunning) {
            return;
        }

        syncRunning = true;

        syncText.setText(
                "Synchronisierung läuft …"
        );

        startSyncButtonAnimation();

        updateBatteryText();

        final boolean[] weatherFinished = {
                false
        };

        final boolean[] forecastFinished = {
                false
        };

        final boolean[] calendarFinished = {
                false
        };

        WeatherSync.sync(
                this,
                new WeatherSync.Callback() {
                    @Override
                    public void onFinished(
                            String weather
                    ) {
                        currentWeatherText.setText(
                                "Graz · "
                                        + weather
                        );

                        weatherFinished[0] =
                                true;

                        finishManualSyncIfReady(
                                weatherFinished[0],
                                forecastFinished[0],
                                calendarFinished[0]
                        );
                    }

                    @Override
                    public void onError(
                            Exception exception
                    ) {
                        currentWeatherText.setText(
                                "Graz · Wetter nicht verfügbar"
                        );

                        weatherFinished[0] =
                                true;

                        finishManualSyncIfReady(
                                weatherFinished[0],
                                forecastFinished[0],
                                calendarFinished[0]
                        );
                    }
                }
        );

        WeatherForecastSync.sync(
                this,
                new WeatherForecastSync.Callback() {
                    @Override
                    public void onFinished(
                            ArrayList<WeatherForecastSync.ForecastDay> days
                    ) {
                        forecastDays = days;
                        drawWeatherForecast();

                        forecastFinished[0] =
                                true;

                        finishManualSyncIfReady(
                                weatherFinished[0],
                                forecastFinished[0],
                                calendarFinished[0]
                        );
                    }

                    @Override
                    public void onError(
                            Exception exception
                    ) {
                        if (forecastDays.size()
                                == 0) {

                            weatherDaysRow.removeAllViews();

                            TextView error =
                                    new TextView(
                                            MainActivity.this
                                    );

                            error.setText(
                                    "7-Tage-Vorhersage momentan nicht verfügbar"
                            );

                            error.setTextSize(12);
                            error.setTextColor(
                                    Color.DKGRAY
                            );
                            error.setGravity(
                                    Gravity.CENTER
                            );

                            error.setLayoutParams(
                                    new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                            );

                            weatherDaysRow.addView(
                                    error
                            );
                        }

                        forecastFinished[0] =
                                true;

                        finishManualSyncIfReady(
                                weatherFinished[0],
                                forecastFinished[0],
                                calendarFinished[0]
                        );
                    }
                }
        );

        WarningSync.sync(
                new WarningSync.Callback() {
                    @Override
                    public void onFinished(
                            ArrayList<WarningSync.WeatherWarning> warnings
                    ) {
                        currentWarnings = warnings;
                        updateWarningCard();
                    }

                    @Override
                    public void onError(
                            Exception exception
                    ) {
                        currentWarnings.clear();
                        updateWarningCard();
                    }
                }
        );

        CalendarSync.sync(
                this,
                new CalendarSync.Callback() {
                    @Override
                    public void onFinished(
                            ArrayList<CalendarEvent> events
                    ) {
                        currentEvents = events;

                        long syncTime =
                                StorageHelper.loadSyncTime(
                                        MainActivity.this
                                );

                        String formattedTime =
                                new SimpleDateFormat(
                                        "HH:mm",
                                        Locale.GERMAN
                                ).format(
                                        new Date(
                                                syncTime
                                        )
                                );

                        syncText.setText(
                                "Synchronisiert: "
                                        + formattedTime
                        );

                        showActiveView();

                        calendarFinished[0] =
                                true;

                        finishManualSyncIfReady(
                                weatherFinished[0],
                                forecastFinished[0],
                                calendarFinished[0]
                        );
                    }

                    @Override
                    public void onError(
                            Exception exception
                    ) {
                        syncText.setText(
                                "Kalender nicht aktualisiert"
                        );

                        showActiveView();

                        calendarFinished[0] =
                                true;

                        finishManualSyncIfReady(
                                weatherFinished[0],
                                forecastFinished[0],
                                calendarFinished[0]
                        );
                    }
                }
        );
    }

    private void finishManualSyncIfReady(
            boolean weatherFinished,
            boolean forecastFinished,
            boolean calendarFinished
    ) {
        if (!weatherFinished
                || !forecastFinished
                || !calendarFinished) {

            return;
        }

        syncRunning = false;
        stopSyncButtonAnimation(true);

        UpdateManager.checkForUpdate(
                MainActivity.this,
                updateCheckRequestedByUser
        );

        updateCheckRequestedByUser = false;
    }

    private void startSyncButtonAnimation() {
        buttonAnimationHandler.removeCallbacks(
                syncButtonAnimation
        );

        syncAnimationFrame = 0;

        if (syncNowButton != null) {
            syncNowButton.setText(
                    syncAnimationFrames[0]
            );
        }

        buttonAnimationHandler.postDelayed(
                syncButtonAnimation,
                350L
        );
    }

    private final Runnable syncButtonAnimation =
            new Runnable() {
                @Override
                public void run() {
                    if (!syncRunning) {
                        return;
                    }

                    syncAnimationFrame =
                            (syncAnimationFrame + 1)
                                    % syncAnimationFrames.length;

                    if (syncNowButton != null) {
                        syncNowButton.setText(
                                syncAnimationFrames[
                                        syncAnimationFrame
                                ]
                        );
                    }

                    buttonAnimationHandler.postDelayed(
                            this,
                            350L
                    );
                }
            };

    private void stopSyncButtonAnimation(
            boolean successful
    ) {
        buttonAnimationHandler.removeCallbacks(
                syncButtonAnimation
        );

        if (syncNowButton == null) {
            return;
        }

        syncNowButton.setText(
                successful
                        ? "✓"
                        : "!"
        );

        buttonAnimationHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!syncRunning
                                && syncNowButton != null) {

                            syncNowButton.setText(
                                    "↻"
                            );
                        }
                    }
                },
                2200L
        );
    }

    private void updateWarningCard() {
        if (warningCard == null) {
            return;
        }

        if (currentWarnings == null
                || currentWarnings.size() == 0) {
            warningCard.setText("");
            warningCard.setVisibility(View.GONE);
            return;
        }

        WarningSync.WeatherWarning warning =
                highestWarning();

        String period = formatWarningTime(
                warning.getStartSeconds()
        ) + " – " + formatWarningTime(
                warning.getEndSeconds()
        );

        String more = currentWarnings.size() > 1
                ? "  (+" + (currentWarnings.size() - 1) + ")"
                : "";

        warningCard.setText(
                WarningSync.levelName(warning.getLevel())
                        + " · "
                        + WarningSync.typeName(warning.getType())
                        + more
                        + "\n"
                        + period
        );

        warningCard.setBackground(
                createBorderDrawable(
                        warningBackgroundColor(warning.getLevel()),
                        Color.DKGRAY,
                        2
                )
        );
        warningCard.setVisibility(View.VISIBLE);
    }

    private WarningSync.WeatherWarning highestWarning() {
        WarningSync.WeatherWarning selected =
                currentWarnings.get(0);

        for (WarningSync.WeatherWarning warning : currentWarnings) {
            if (warning.getLevel() > selected.getLevel()
                    || (warning.getLevel() == selected.getLevel()
                    && warning.getStartSeconds()
                    < selected.getStartSeconds())) {
                selected = warning;
            }
        }

        return selected;
    }

    private int warningBackgroundColor(int level) {
        if (level >= 3) {
            return Color.rgb(235, 125, 125);
        }
        if (level == 2) {
            return Color.rgb(245, 185, 105);
        }
        return Color.rgb(250, 230, 120);
    }

    private String formatWarningTime(long seconds) {
        if (seconds <= 0L) {
            return "?";
        }

        return new SimpleDateFormat(
                "dd.MM. HH:mm",
                Locale.GERMAN
        ).format(new Date(seconds * 1000L));
    }

    private void showWarningFullscreen() {
        if (currentWarnings == null
                || currentWarnings.size() == 0) {
            return;
        }

        if (warningDialog != null
                && warningDialog.isShowing()) {
            warningDialog.dismiss();
        }

        warningDialog = new Dialog(
                this,
                android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
        );

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.WHITE);
        page.setPadding(18, 10, 18, 12);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextSize(36);
        close.setTextColor(Color.BLACK);
        close.setGravity(Gravity.CENTER);
        close.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        close.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        warningDialog.dismiss();
                    }
                }
        );

        TextView title = new TextView(this);
        title.setText("Wetterwarnungen · Am Katzelbach 21");
        title.setTextSize(25);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(12, 0, 0, 0);
        title.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        64,
                        1
                )
        );

        header.addView(close);
        header.addView(title);
        page.addView(header);
        page.addView(createHorizontalLine());

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 10, 0, 20);

        for (WarningSync.WeatherWarning warning : currentWarnings) {
            list.addView(createWarningSummary(warning));
        }

        scroll.addView(list);
        page.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        warningDialog.setContentView(page);
        warningDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        applyNightBrightness();
                        scheduleAutomaticReset();
                    }
                }
        );
        warningDialog.show();
        applyDialogBrightness(warningDialog);
    }

    private View createWarningSummary(
            final WarningSync.WeatherWarning warning
    ) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createBorderDrawable(
                warningBackgroundColor(warning.getLevel()),
                Color.DKGRAY,
                2
        ));

        TextView summary = new TextView(this);
        summary.setText(
                formatWarningDay(warning.getStartSeconds())
                        + "  ·  "
                        + WarningSync.levelName(warning.getLevel())
                        + "  ·  "
                        + WarningSync.typeName(warning.getType())
                        + "\n"
                        + formatWarningTime(warning.getStartSeconds())
                        + " – "
                        + formatWarningTime(warning.getEndSeconds())
                        + "     Details ›"
        );
        summary.setTextSize(17);
        summary.setTextColor(Color.BLACK);
        summary.setPadding(18, 12, 18, 12);

        final View details = createWarningDetail(warning);
        details.setVisibility(View.GONE);
        card.addView(summary);
        card.addView(details);

        summary.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                boolean open = details.getVisibility() == View.VISIBLE;
                details.setVisibility(open ? View.GONE : View.VISIBLE);
                ((TextView) view).setText(
                        formatWarningDay(warning.getStartSeconds())
                                + "  ·  "
                                + WarningSync.levelName(warning.getLevel())
                                + "  ·  "
                                + WarningSync.typeName(warning.getType())
                                + "\n"
                                + formatWarningTime(warning.getStartSeconds())
                                + " – "
                                + formatWarningTime(warning.getEndSeconds())
                                + (open ? "     Details ›" : "     Details ⌄")
                );
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);
        return card;
    }

    private String formatWarningDay(long seconds) {
        if (seconds <= 0L) {
            return "Warnung";
        }
        return new SimpleDateFormat(
                "EEE, dd.MM.",
                Locale.GERMAN
        ).format(new Date(seconds * 1000L));
    }

    private View createWarningDetail(
            WarningSync.WeatherWarning warning
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 12, 18, 16);
        card.setBackground(
                createBorderDrawable(
                        warningBackgroundColor(warning.getLevel()),
                        Color.DKGRAY,
                        2
                )
        );

        TextView heading = createWeatherText(
                WarningSync.levelName(warning.getLevel())
                        + " · "
                        + WarningSync.typeName(warning.getType()),
                23,
                Color.BLACK
        );
        heading.setPadding(0, 0, 0, 5);
        card.addView(heading);

        TextView period = createWeatherText(
                "Von " + formatWarningTime(warning.getStartSeconds())
                        + " bis " + formatWarningTime(warning.getEndSeconds()),
                16,
                Color.BLACK
        );
        period.setPadding(0, 0, 0, 8);
        card.addView(period);

        addWarningSection(card, "Meldung", warning.getText());
        addWarningSection(card, "Meteorologische Details", warning.getMeteorologicalText());
        addWarningSection(card, "Mögliche Auswirkungen", warning.getEffects());
        addWarningSection(card, "Empfehlungen", warning.getRecommendations());

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);
        return card;
    }

    private void addWarningSection(
            LinearLayout card,
            String heading,
            String value
    ) {
        if (value == null || value.length() == 0) {
            return;
        }

        TextView view = createWeatherText(
                heading + "\n" + value,
                15,
                Color.BLACK
        );
        view.setPadding(0, 4, 0, 7);
        card.addView(view);
    }

    private void drawWeatherForecast() {
        weatherDaysRow.removeAllViews();

        int dayCount =
                Math.min(
                        7,
                        forecastDays.size()
                );

        for (int index = 0;
             index < dayCount;
             index++) {

            final WeatherForecastSync.ForecastDay day =
                    forecastDays.get(index);

            LinearLayout dayCell =
                    new LinearLayout(this);

            dayCell.setOrientation(
                    LinearLayout.VERTICAL
            );

            dayCell.setGravity(
                    Gravity.CENTER
            );

            dayCell.setPadding(
                    2,
                    2,
                    2,
                    2
            );

            dayCell.setBackgroundColor(
                    Color.TRANSPARENT
            );

            dayCell.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            0,
                            118,
                            1
                    )
            );

            TextView heading =
                    new TextView(this);

            heading.setText(
                    day.getWeekday()
                            + " "
                            + day.getDateShort()
            );

            heading.setTextSize(11);
            heading.setTextColor(Color.BLACK);
            heading.setGravity(Gravity.CENTER);

            WeatherIconView icon =
                    new WeatherIconView(
                            this,
                            day.getWeatherCode()
                    );

            icon.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            58,
                            46
                    )
            );

            TextView temperature =
                    new TextView(this);

            temperature.setText(
                    day.getMaxTempC()
                            + "° / "
                            + day.getMinTempC()
                            + "°"
            );

            temperature.setTextSize(11);
            temperature.setTextColor(Color.BLACK);
            temperature.setGravity(Gravity.CENTER);

            TextView rain =
                    new TextView(this);

            rain.setText(
                    "Regen "
                            + day.getRainChance()
                            + " %"
            );

            rain.setTextSize(10);
            rain.setTextColor(Color.DKGRAY);
            rain.setGravity(Gravity.CENTER);

            dayCell.addView(heading);
            dayCell.addView(icon);
            dayCell.addView(temperature);
            dayCell.addView(rain);

            dayCell.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(
                                View view
                        ) {
                            showHourlyWeather(day);
                            scheduleAutomaticReset();
                        }
                    }
            );

            weatherDaysRow.addView(dayCell);

            if (index < dayCount - 1) {
                View divider =
                        new View(this);

                divider.setBackgroundColor(
                        Color.rgb(
                                215,
                                215,
                                215
                        )
                );

                divider.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                1,
                                96
                        )
                );

                weatherDaysRow.addView(divider);
            }
        }
    }

    private void showHourlyWeather(
            final WeatherForecastSync.ForecastDay day
    ) {
        if (weatherDialog != null
                && weatherDialog.isShowing()) {

            weatherDialog.dismiss();
        }

        weatherDialog =
                new Dialog(
                        this,
                        android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
                );

        LinearLayout page =
                new LinearLayout(this);

        page.setOrientation(
                LinearLayout.VERTICAL
        );

        page.setBackgroundColor(
                Color.WHITE
        );

        page.setPadding(
                14,
                8,
                14,
                8
        );

        page.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        LinearLayout top =
                new LinearLayout(this);

        top.setOrientation(
                LinearLayout.HORIZONTAL
        );

        top.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView close =
                new TextView(this);

        close.setText("×");
        close.setTextSize(36);
        close.setTextColor(Color.BLACK);
        close.setGravity(Gravity.CENTER);

        close.setLayoutParams(
                new LinearLayout.LayoutParams(
                        58,
                        220
                )
        );

        close.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        weatherDialog.dismiss();
                    }
                }
        );

        LinearLayout hero =
                new LinearLayout(this);

        hero.setOrientation(
                LinearLayout.HORIZONTAL
        );

        hero.setGravity(
                Gravity.CENTER_VERTICAL
        );

        hero.setPadding(
                4,
                0,
                12,
                0
        );

        hero.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        220,
                        1.20f
                )
        );

        WeatherIconView largeIcon =
                new WeatherIconView(
                        this,
                        day.getWeatherCode()
                );

        largeIcon.setLayoutParams(
                new LinearLayout.LayoutParams(
                        132,
                        112
                )
        );

        LinearLayout heroText =
                new LinearLayout(this);

        heroText.setOrientation(
                LinearLayout.VERTICAL
        );

        heroText.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView heroDate =
                createWeatherText(
                        day.getWeekday()
                                + ", "
                                + day.getDateLabel(),
                        13,
                        Color.DKGRAY
                );

        TextView heroTemperature =
                createWeatherText(
                        day.getMaxTempC()
                                + "° / "
                                + day.getMinTempC()
                                + "°",
                        32,
                        Color.BLACK
                );

        TextView heroDescription =
                createWeatherText(
                        day.getDescription()
                                + "\nGefühlt: "
                                + day.getApparentMaxC()
                                + "° / "
                                + day.getApparentMinC()
                                + "°",
                        14,
                        Color.DKGRAY
                );

        heroText.addView(heroDate);
        heroText.addView(heroTemperature);
        heroText.addView(heroDescription);

        hero.addView(largeIcon);
        hero.addView(heroText);

        GridLayout metrics =
                new GridLayout(this);

        metrics.setColumnCount(2);
        metrics.setRowCount(3);
        metrics.setPadding(
                12,
                4,
                4,
                4
        );

        metrics.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        220,
                        1
                )
        );

        addTopWeatherMetric(
                metrics,
                "Regen",
                day.getRainChance() + " %"
        );

        addTopWeatherMetric(
                metrics,
                "Niederschlag",
                formatDecimal(
                        day.getPrecipitationMm()
                ) + " mm"
        );

        addTopWeatherMetric(
                metrics,
                "Wind",
                day.getWindKmph()
                        + " km/h "
                        + day.getWindDirectionText()
        );

        addTopWeatherMetric(
                metrics,
                "Böen",
                day.getWindGustKmph()
                        + " km/h"
        );

        addTopWeatherMetric(
                metrics,
                "Luftfeuchtigkeit",
                day.getHumidity() + " %"
        );

        addTopWeatherMetric(
                metrics,
                "Luftdruck",
                day.getPressureHpa()
                        + " hPa"
        );

        top.addView(close);
        top.addView(hero);
        top.addView(
                createVerticalLine(210)
        );
        top.addView(metrics);

        page.addView(top);

        LinearLayout featureRow =
                new LinearLayout(this);

        featureRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        featureRow.addView(
                createFeatureCard(
                        "Sonnenaufgang",
                        day.getSunrise()
                )
        );

        featureRow.addView(
                createFeatureCard(
                        "Sonnenuntergang",
                        day.getSunset()
                )
        );

        featureRow.addView(
                createFeatureCard(
                        "Sonnenstunden",
                        formatDecimal(
                                day.getSunshineHours()
                        ) + " h"
                )
        );

        featureRow.addView(
                createFeatureCard(
                        "Sichtweite",
                        day.getVisibilityKm() + " km"
                )
        );

        featureRow.addView(
                createFeatureCard(
                        "Höchsttemperatur",
                        day.getMaxTempC() + "°"
                )
        );

        featureRow.addView(
                createFeatureCard(
                        "Tiefsttemperatur",
                        day.getMinTempC() + "°"
                )
        );

        featureRow.addView(
                createFeatureCard(
                        "Taupunkt",
                        day.getDewPointC() + "°"
                )
        );

        page.addView(
                featureRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        80
                )
        );

        page.addView(
                createSectionTitle(
                        "Stündliche Vorhersage"
                )
        );

        LinearLayout hourlyRow =
                new LinearLayout(this);

        hourlyRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        ArrayList<WeatherForecastSync.ForecastHour> visibleHours =
                selectVisibleHours(day);

        for (WeatherForecastSync.ForecastHour hour
                : visibleHours) {

            hourlyRow.addView(
                    createHourlyCell(hour)
            );
        }

        page.addView(
                hourlyRow,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        180
                )
        );

        page.addView(
                createHorizontalLine()
        );

        LinearLayout lower =
                new LinearLayout(this);

        lower.setOrientation(
                LinearLayout.HORIZONTAL
        );

        lower.setPadding(
                0,
                6,
                0,
                0
        );

        lower.addView(
                createDetailColumn(
                        "Details",
                        new String[]{
                                "Höchsttemperatur",
                                "Tiefsttemperatur",
                                "Gefühlt (max.)",
                                "Gefühlt (min.)"
                        },
                        new String[]{
                                day.getMaxTempC() + "°",
                                day.getMinTempC() + "°",
                                day.getApparentMaxC() + "°",
                                day.getApparentMinC() + "°"
                        }
                )
        );

        lower.addView(
                createVerticalLine(124)
        );

        lower.addView(
                createDetailColumn(
                        "Weitere Informationen",
                        new String[]{
                                "Sonnenstunden",
                                "UV-Index",
                                "Luftfeuchtigkeit",
                                "Niederschlag"
                        },
                        new String[]{
                                formatDecimal(
                                        day.getSunshineHours()
                                ) + " h",
                                formatDecimal(
                                        day.getUvIndex()
                                ),
                                day.getHumidity() + " %",
                                formatDecimal(
                                        day.getPrecipitationMm()
                                ) + " mm"
                        }
                )
        );

        lower.addView(
                createVerticalLine(124)
        );

        lower.addView(
                createDetailColumn(
                        "Wind",
                        new String[]{
                                "Wind",
                                "Böen",
                                "Windrichtung",
                                "Luftdruck"
                        },
                        new String[]{
                                day.getWindKmph()
                                        + " km/h",
                                day.getWindGustKmph()
                                        + " km/h",
                                day.getWindDirectionText(),
                                day.getPressureHpa()
                                        + " hPa"
                        }
                )
        );

        page.addView(
                lower,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        360
                )
        );

        weatherDialog.setContentView(page);

        weatherDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(
                            DialogInterface dialog
                    ) {
                        applyNightBrightness();
                        scheduleAutomaticReset();
                    }
                }
        );

        weatherDialog.show();
        applyDialogBrightness(
                weatherDialog
        );
    }

    private TextView createWeatherText(
            String text,
            int size,
            int color
    ) {
        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        return view;
    }

    private void addTopWeatherMetric(
            GridLayout grid,
            String label,
            String value
    ) {
        LinearLayout cell =
                new LinearLayout(this);

        cell.setOrientation(
                LinearLayout.VERTICAL
        );

        cell.setPadding(
                10,
                5,
                10,
                5
        );

        TextView labelView =
                createWeatherText(
                        label,
                        11,
                        Color.DKGRAY
                );

        TextView valueView =
                createWeatherText(
                        value,
                        14,
                        Color.BLACK
                );

        cell.addView(labelView);
        cell.addView(valueView);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 198;
        params.height = 68;

        cell.setLayoutParams(params);
        grid.addView(cell);
    }

    private int parseHourNumber(
            String time
    ) {
        if (time == null
                || time.length() < 2) {
            return -1;
        }

        try {
            return Integer.parseInt(
                    time.substring(
                            0,
                            2
                    )
            );
        } catch (Exception ignored) {
            return -1;
        }
    }

    private ArrayList<WeatherForecastSync.ForecastHour>
            selectVisibleHours(
                    WeatherForecastSync.ForecastDay day
            ) {

        ArrayList<WeatherForecastSync.ForecastHour> candidates =
                new ArrayList<WeatherForecastSync.ForecastHour>();

        boolean today =
                forecastDays.size() > 0
                        && forecastDays.get(0) == day;

        int currentHour =
                Calendar.getInstance().get(
                        Calendar.HOUR_OF_DAY
                );

        for (WeatherForecastSync.ForecastHour hour
                : day.getHours()) {

            int hourNumber =
                    parseHourNumber(
                            hour.getTime()
                    );

            if (today
                    && hourNumber <= currentHour) {

                continue;
            }

            candidates.add(hour);
        }

        ArrayList<WeatherForecastSync.ForecastHour> result =
                new ArrayList<WeatherForecastSync.ForecastHour>();

        if (candidates.size() <= 8) {
            result.addAll(candidates);
            return result;
        }

        double step =
                (candidates.size() - 1)
                        / 7.0;

        int lastIndex = -1;

        for (int index = 0;
             index < 8;
             index++) {

            int candidateIndex =
                    (int) Math.round(
                            index * step
                    );

            if (candidateIndex == lastIndex) {
                continue;
            }

            result.add(
                    candidates.get(
                            candidateIndex
                    )
            );

            lastIndex = candidateIndex;
        }

        return result;
    }

    private void applyDialogBrightness(
            Dialog dialog
    ) {
        Window window =
                dialog.getWindow();

        if (window == null) {
            return;
        }

        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );

        window.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                        Color.WHITE
                )
        );

        WindowManager.LayoutParams attributes =
                window.getAttributes();

        attributes.screenBrightness =
                lightCurrentlyOn
                        ? Math.max(
                                0.01f,
                                brightnessPercent / 100.0f
                        )
                        : 0.0f;

        window.setAttributes(
                attributes
        );
    }

    private void addCompactMetric(
            GridLayout grid,
            String label,
            String value
    ) {
        LinearLayout cell =
                new LinearLayout(this);

        cell.setOrientation(
                LinearLayout.VERTICAL
        );

        cell.setPadding(
                12,
                5,
                12,
                5
        );

        TextView labelView =
                new TextView(this);

        labelView.setText(label);
        labelView.setTextSize(11);
        labelView.setTextColor(Color.DKGRAY);

        TextView valueView =
                new TextView(this);

        valueView.setText(value);
        valueView.setTextSize(15);
        valueView.setTextColor(Color.BLACK);

        cell.addView(labelView);
        cell.addView(valueView);

        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width = 188;
        params.height = 43;

        cell.setLayoutParams(params);
        grid.addView(cell);
    }

    private LinearLayout createFeatureCard(
            String label,
            String value
    ) {
        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setGravity(
                Gravity.CENTER
        );

        card.setPadding(
                5,
                6,
                5,
                6
        );

        card.setBackground(
                createBorderDrawable(
                        Color.rgb(
                                250,
                                250,
                                250
                        ),
                        Color.rgb(
                                215,
                                215,
                                215
                        ),
                        1
                )
        );

        card.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        76,
                        1
                )
        );

        TextView labelView =
                new TextView(this);

        labelView.setText(label);
        labelView.setTextSize(10);
        labelView.setTextColor(Color.DKGRAY);
        labelView.setGravity(Gravity.CENTER);

        TextView valueView =
                new TextView(this);

        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setTextColor(Color.BLACK);
        valueView.setGravity(Gravity.CENTER);

        card.addView(labelView);
        card.addView(valueView);

        return card;
    }

    private LinearLayout createHourlyCell(
            WeatherForecastSync.ForecastHour hour
    ) {
        LinearLayout cell =
                new LinearLayout(this);

        cell.setOrientation(
                LinearLayout.VERTICAL
        );

        cell.setGravity(
                Gravity.CENTER
        );

        cell.setPadding(
                6,
                2,
                6,
                2
        );

        cell.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        170,
                        1
                )
        );

        TextView time =
                new TextView(this);

        time.setText(
                hour.getTime()
        );

        time.setTextSize(12);
        time.setTextColor(Color.DKGRAY);
        time.setGravity(Gravity.CENTER);

        WeatherIconView icon =
                new WeatherIconView(
                        this,
                        hour.getWeatherCode(),
                        isNightHour(
                                hour.getTime()
                        )
                );

        icon.setLayoutParams(
                new LinearLayout.LayoutParams(
                        64,
                        54
                )
        );

        TextView temperature =
                new TextView(this);

        temperature.setText(
                hour.getTempC() + "°"
        );

        temperature.setTextSize(17);
        temperature.setTextColor(Color.BLACK);
        temperature.setGravity(Gravity.CENTER);

        TextView rain =
                new TextView(this);

        rain.setText(
                hour.getRainChance() + " %"
        );

        rain.setTextSize(10);
        rain.setTextColor(Color.DKGRAY);
        rain.setGravity(Gravity.CENTER);

        cell.addView(time);
        cell.addView(icon);
        cell.addView(temperature);
        cell.addView(rain);

        return cell;
    }

    private boolean isNightHour(
            String time
    ) {
        try {
            int hour =
                    Integer.parseInt(
                            time.substring(
                                    0,
                                    2
                            )
                    );

            return hour < 6
                    || hour >= 21;

        } catch (Exception ignored) {
            return false;
        }
    }

    private LinearLayout createDetailColumn(
            String title,
            String[] labels,
            String[] values
    ) {
        LinearLayout column =
                new LinearLayout(this);

        column.setOrientation(
                LinearLayout.VERTICAL
        );

        column.setPadding(
                10,
                0,
                10,
                0
        );

        column.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                )
        );

        TextView titleView =
                new TextView(this);

        titleView.setText(title);
        titleView.setTextSize(12);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(
                0,
                0,
                0,
                7
        );

        column.addView(titleView);

        for (int index = 0;
             index < labels.length;
             index++) {

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setPadding(
                    0,
                    1,
                    0,
                    1
            );

            TextView labelView =
                    new TextView(this);

            labelView.setText(
                    labels[index]
            );

            labelView.setTextSize(10);
            labelView.setTextColor(Color.DKGRAY);

            labelView.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    )
            );

            TextView valueView =
                    new TextView(this);

            valueView.setText(
                    values[index]
            );

            valueView.setTextSize(11);
            valueView.setTextColor(Color.BLACK);

            row.addView(labelView);
            row.addView(valueView);

            column.addView(row);
        }

        return column;
    }

    private View createVerticalLine(
            int height
    ) {
        View line =
                new View(this);

        line.setBackgroundColor(
                Color.rgb(
                        215,
                        215,
                        215
                )
        );

        line.setLayoutParams(
                new LinearLayout.LayoutParams(
                        1,
                        height
                )
        );

        return line;
    }

    private TextView createSectionTitle(
            String text
    ) {
        TextView title =
                new TextView(this);

        title.setText(text);
        title.setTextSize(14);
        title.setTextColor(Color.BLACK);
        title.setPadding(
                4,
                4,
                4,
                4
        );

        return title;
    }

    private View createHorizontalLine() {
        View line =
                new View(this);

        line.setBackgroundColor(
                Color.rgb(
                        185,
                        185,
                        185
                )
        );

        line.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                )
        );

        return line;
    }

    private String formatDecimal(
            double value
    ) {
        return String.format(
                Locale.GERMAN,
                "%.1f",
                value
        );
    }

    private void drawCalendar(
            ArrayList<CalendarEvent> events
    ) {
        contentContainer.removeAllViews();

        calendarGrid =
                new GridLayout(this);

        calendarGrid.setColumnCount(8);

        addWeekdayHeadings();

        Calendar today =
                Calendar.getInstance();

        Calendar weekStart =
                getVisibleRangeStart();

        int dayCellHeight =
                calculateDayCellHeight();

        int dayCellWidth =
                calculateCellWidth();

        int weekNumberWidth =
                calculateWeekNumberWidth();

        for (int week = 0;
             week < DISPLAY_WEEKS;
             week++) {

            calendarGrid.addView(
                    createWeekNumberCell(
                            weekStart,
                            weekNumberWidth,
                            dayCellHeight
                    )
            );

            Calendar day =
                    (Calendar)
                            weekStart.clone();

            for (int dayIndex = 0;
                 dayIndex < 7;
                 dayIndex++) {

                final Calendar cellDate =
                        (Calendar)
                                day.clone();

                LinearLayout cell =
                        createDayCell(
                                cellDate,
                                today,
                                events
                        );

                cell.setLayoutParams(
                        createGridParams(
                                dayCellWidth,
                                dayCellHeight,
                                0
                        )
                );

                cell.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(
                                    View view
                            ) {
                                showDayDetails(
                                        cellDate,
                                        currentEvents
                                );

                                scheduleAutomaticReset();
                            }
                        }
                );

                cell.setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(
                                    View view
                            ) {
                                showDayExtendedDetails(
                                        cellDate,
                                        currentEvents
                                );

                                return true;
                            }
                        }
                );

                calendarGrid.addView(
                        cell
                );

                day.add(
                        Calendar.DAY_OF_MONTH,
                        1
                );
            }

            weekStart.add(
                    Calendar.WEEK_OF_YEAR,
                    1
            );
        }

        contentContainer.addView(
                calendarGrid
        );
    }

    private void addWeekdayHeadings() {
        String[] weekdays = {
                "Mo",
                "Di",
                "Mi",
                "Do",
                "Fr",
                "Sa",
                "So"
        };

        int dayWidth =
                calculateCellWidth();

        int weekNumberWidth =
                calculateWeekNumberWidth();

        calendarGrid.addView(
                createHeadingCell(
                        "KW",
                        weekNumberWidth
                )
        );

        for (String weekday
                : weekdays) {

            calendarGrid.addView(
                    createHeadingCell(
                            weekday,
                            dayWidth
                    )
            );
        }
    }

    private TextView createHeadingCell(
            String text,
            int width
    ) {
        TextView heading =
                new TextView(this);

        heading.setText(text);
        heading.setTextSize(14);
        heading.setTextColor(
                Color.BLACK
        );
        heading.setGravity(
                Gravity.CENTER
        );

        heading.setBackground(
                createBorderDrawable(
                        Color.rgb(
                                220,
                                220,
                                220
                        ),
                        Color.rgb(
                                105,
                                105,
                                105
                        ),
                        2
                )
        );

        heading.setLayoutParams(
                createGridParams(
                        width,
                        40,
                        0
                )
        );

        return heading;
    }

    private TextView createWeekNumberCell(
            Calendar weekStart,
            int width,
            int height
    ) {
        TextView weekNumber =
                new TextView(this);

        weekNumber.setText(
                String.valueOf(
                        weekStart.get(
                                Calendar.WEEK_OF_YEAR
                        )
                )
        );

        weekNumber.setTextSize(14);
        weekNumber.setTextColor(
                Color.BLACK
        );
        weekNumber.setGravity(
                Gravity.CENTER
        );

        weekNumber.setBackground(
                createBorderDrawable(
                        Color.rgb(
                                225,
                                225,
                                225
                        ),
                        Color.rgb(
                                105,
                                105,
                                105
                        ),
                        2
                )
        );

        weekNumber.setLayoutParams(
                createGridParams(
                        width,
                        height,
                        0
                )
        );

        return weekNumber;
    }

    private Calendar getVisibleRangeStart() {
        Calendar start =
                getCurrentWeekMonday();

        start.add(
                Calendar.WEEK_OF_YEAR,
                navigationOffsetWeeks - 1
        );

        return start;
    }

    private void clearTime(
            Calendar calendar
    ) {
        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );
    }

    private LinearLayout createDayCell(
            Calendar day,
            Calendar today,
            ArrayList<CalendarEvent> events
    ) {
        LinearLayout cell =
                new LinearLayout(this);

        cell.setOrientation(
                LinearLayout.VERTICAL
        );

        boolean currentDay =
                isSameDay(
                        day,
                        today
                );

        boolean weekend =
                day.get(
                        Calendar.DAY_OF_WEEK
                ) == Calendar.SATURDAY
                        || day.get(
                        Calendar.DAY_OF_WEEK
                ) == Calendar.SUNDAY;

        int bodyColor;

        if (currentDay) {
            bodyColor =
                    Color.rgb(
                            205,
                            205,
                            205
                    );
        } else if (weekend) {
            bodyColor =
                    Color.rgb(
                            242,
                            242,
                            242
                    );
        } else {
            bodyColor =
                    Color.WHITE;
        }

        cell.setBackground(
                createBorderDrawable(
                        bodyColor,
                        Color.rgb(
                                105,
                                105,
                                105
                        ),
                        2
                )
        );

        TextView dateStrip =
                new TextView(this);

        dateStrip.setText(
                new SimpleDateFormat(
                        "d. MMM",
                        Locale.GERMAN
                ).format(
                        day.getTime()
                )
        );

        dateStrip.setTextSize(13);
        dateStrip.setTextColor(
                Color.BLACK
        );
        dateStrip.setGravity(
                Gravity.CENTER
        );
        dateStrip.setPadding(
                3,
                2,
                3,
                2
        );

        boolean monthStart =
                day.get(
                        Calendar.DAY_OF_MONTH
                ) == 1;

        if (currentDay) {
            dateStrip.setBackgroundColor(
                    Color.rgb(
                            130,
                            130,
                            130
                    )
            );

        } else if (monthStart) {
            dateStrip.setBackgroundColor(
                    Color.rgb(
                            175,
                            175,
                            175
                    )
            );

        } else {
            dateStrip.setBackgroundColor(
                    Color.rgb(
                            228,
                            228,
                            228
                    )
            );
        }

        EventPreview preview =
                buildEventPreview(
                        day,
                        events
                );

        TextView eventsText =
                new TextView(this);

        eventsText.setText(
                preview.text
        );
        eventsText.setTextSize(9);
        eventsText.setTextColor(
                Color.BLACK
        );
        eventsText.setMaxLines(5);
        eventsText.setEllipsize(
                TextUtils.TruncateAt.END
        );
        eventsText.setLineSpacing(
                0f,
                0.92f
        );
        eventsText.setGravity(
                Gravity.TOP
                        | Gravity.CENTER_HORIZONTAL
        );
        eventsText.setPadding(
                4,
                4,
                4,
                2
        );
        eventsText.setBackgroundColor(
                bodyColor
        );

        eventsText.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        cell.addView(dateStrip);
        cell.addView(eventsText);

        if (preview.moreCount > 0) {
            TextView more =
                    new TextView(this);

            more.setText(
                    "+ "
                            + preview.moreCount
                            + " weitere"
            );

            more.setTextSize(9);
            more.setTextColor(
                    Color.DKGRAY
            );
            more.setGravity(
                    Gravity.CENTER
            );
            more.setPadding(
                    2,
                    1,
                    2,
                    2
            );
            more.setBackgroundColor(
                    bodyColor
            );

            cell.addView(more);
        }

        return cell;
    }

    private EventPreview buildEventPreview(
            Calendar day,
            ArrayList<CalendarEvent> events
    ) {
        String dateKey =
                new SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.US
                ).format(
                        day.getTime()
                );

        StringBuilder preview =
                new StringBuilder();

        int totalEvents = 0;
        int shownEvents = 0;

        for (CalendarEvent event
                : events) {

            if (event.occursOn(
                    dateKey
            )) {
                totalEvents++;

                if (shownEvents
                        < PREVIEW_EVENT_LIMIT) {

                    if (preview.length()
                            > 0) {

                        preview.append(
                                "\n"
                        );
                    }

                    preview.append(
                            shortenTitle(
                                    event.getTitle(),
                                    24
                            )
                    );

                    shownEvents++;
                }
            }
        }

        return new EventPreview(
                preview.toString(),
                Math.max(
                        0,
                        totalEvents
                                - shownEvents
                )
        );
    }

    private void showDayDetails(
            Calendar day,
            ArrayList<CalendarEvent> events
    ) {
        showDayDetailsFullscreen(
                (Calendar) day.clone()
        );
    }

    private void showDayDetailsFullscreen(
            final Calendar selectedDay
    ) {
        if (dayDialog != null
                && dayDialog.isShowing()) {

            dayDialog.dismiss();
        }

        final Dialog dialog =
                new Dialog(
                        this,
                        android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen
                );

        fullscreenDayDialog =
                dialog;

        LinearLayout page =
                new LinearLayout(this);

        page.setOrientation(
                LinearLayout.VERTICAL
        );

        page.setBackgroundColor(
                Color.WHITE
        );

        page.setPadding(
                18,
                10,
                18,
                10
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView close =
                new TextView(this);

        close.setText("×");
        close.setTextSize(38);
        close.setTextColor(Color.BLACK);
        close.setGravity(Gravity.CENTER);
        close.setPadding(
                8,
                0,
                20,
                0
        );

        close.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        dialog.dismiss();
                    }
                }
        );

        LinearLayout titleArea =
                new LinearLayout(this);

        titleArea.setOrientation(
                LinearLayout.VERTICAL
        );

        titleArea.setGravity(
                Gravity.CENTER
        );

        titleArea.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView dateTitle =
                new TextView(this);

        dateTitle.setText(
                new SimpleDateFormat(
                        "EEEE, dd. MMMM yyyy",
                        Locale.GERMAN
                ).format(
                        selectedDay.getTime()
                )
        );

        dateTitle.setTextSize(28);
        dateTitle.setTextColor(Color.BLACK);
        dateTitle.setGravity(Gravity.CENTER);

        TextView weekTitle =
                new TextView(this);

        weekTitle.setText(
                "Kalenderwoche "
                        + selectedDay.get(
                        Calendar.WEEK_OF_YEAR
                )
        );

        weekTitle.setTextSize(15);
        weekTitle.setTextColor(Color.DKGRAY);
        weekTitle.setGravity(Gravity.CENTER);

        titleArea.addView(dateTitle);
        titleArea.addView(weekTitle);

        TextView rightSpacer =
                new TextView(this);

        rightSpacer.setText("");
        rightSpacer.setLayoutParams(
                new LinearLayout.LayoutParams(
                        70,
                        1
                )
        );

        header.addView(close);
        header.addView(titleArea);
        header.addView(rightSpacer);

        page.addView(header);

        ArrayList<CalendarEvent> dayEvents =
                eventsForDay(
                        selectedDay
                );

        LinearLayout summaryRow =
                new LinearLayout(this);

        summaryRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        summaryRow.setPadding(
                100,
                8,
                100,
                10
        );

        summaryRow.addView(
                createDaySummaryCard(
                        "Termine",
                        String.valueOf(
                                dayEvents.size()
                        )
                )
        );

        summaryRow.addView(
                createDaySummaryCard(
                        "Datum",
                        new SimpleDateFormat(
                                "dd.MM.yyyy",
                                Locale.GERMAN
                        ).format(
                                selectedDay.getTime()
                        )
                )
        );

        summaryRow.addView(
                createDaySummaryCard(
                        "Kalenderwoche",
                        String.valueOf(
                                selectedDay.get(
                                        Calendar.WEEK_OF_YEAR
                                )
                        )
                )
        );

        page.addView(summaryRow);

        ScrollView scroll =
                new ScrollView(this);

        LinearLayout eventList =
                new LinearLayout(this);

        eventList.setOrientation(
                LinearLayout.VERTICAL
        );

        eventList.setPadding(
                90,
                0,
                90,
                0
        );

        if (dayEvents.size() == 0) {
            TextView empty =
                    new TextView(this);

            empty.setText(
                    "Keine Termine an diesem Tag."
            );

            empty.setTextSize(18);
            empty.setTextColor(Color.DKGRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(
                    20,
                    50,
                    20,
                    50
            );

            eventList.addView(empty);

        } else {
            for (CalendarEvent event
                    : dayEvents) {

                eventList.addView(
                        createDayEventCard(
                                event
                        )
                );
            }
        }

        scroll.addView(eventList);

        page.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout navigation =
                new LinearLayout(this);

        navigation.setOrientation(
                LinearLayout.HORIZONTAL
        );

        navigation.setGravity(
                Gravity.CENTER
        );

        navigation.setPadding(
                0,
                8,
                0,
                0
        );

        TextView previous =
                createNavigationButton("‹");

        TextView previousLabel =
                new TextView(this);

        Calendar previousDay =
                (Calendar)
                        selectedDay.clone();

        previousDay.add(
                Calendar.DAY_OF_MONTH,
                -1
        );

        previousLabel.setText(
                new SimpleDateFormat(
                        "EEEE, dd. MMMM",
                        Locale.GERMAN
                ).format(
                        previousDay.getTime()
                )
        );

        previousLabel.setTextSize(14);
        previousLabel.setTextColor(Color.DKGRAY);
        previousLabel.setGravity(Gravity.CENTER);

        previousLabel.setLayoutParams(
                new LinearLayout.LayoutParams(
                        300,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView nextLabel =
                new TextView(this);

        Calendar nextDay =
                (Calendar)
                        selectedDay.clone();

        nextDay.add(
                Calendar.DAY_OF_MONTH,
                1
        );

        nextLabel.setText(
                new SimpleDateFormat(
                        "EEEE, dd. MMMM",
                        Locale.GERMAN
                ).format(
                        nextDay.getTime()
                )
        );

        nextLabel.setTextSize(14);
        nextLabel.setTextColor(Color.DKGRAY);
        nextLabel.setGravity(Gravity.CENTER);

        nextLabel.setLayoutParams(
                new LinearLayout.LayoutParams(
                        300,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView next =
                createNavigationButton("›");

        previous.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        dialog.dismiss();

                        selectedDay.add(
                                Calendar.DAY_OF_MONTH,
                                -1
                        );

                        showDayDetailsFullscreen(
                                selectedDay
                        );
                    }
                }
        );

        next.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(
                            View view
                    ) {
                        dialog.dismiss();

                        selectedDay.add(
                                Calendar.DAY_OF_MONTH,
                                1
                        );

                        showDayDetailsFullscreen(
                                selectedDay
                        );
                    }
                }
        );

        navigation.addView(previous);
        navigation.addView(previousLabel);
        navigation.addView(nextLabel);
        navigation.addView(next);

        page.addView(navigation);

        dialog.setContentView(page);

        dialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(
                            DialogInterface ignored
                    ) {
                        scheduleAutomaticReset();
                    }
                }
        );

        dialog.show();

        Window window =
                dialog.getWindow();

        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );

            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            Color.WHITE
                    )
            );

            WindowManager.LayoutParams attributes =
                    window.getAttributes();

            attributes.screenBrightness =
                    lightCurrentlyOn
                            ? Math.max(
                                    0.01f,
                                    brightnessPercent / 100.0f
                            )
                            : 0.0f;

            window.setAttributes(
                    attributes
            );
        }

        dayDialog = null;
    }

    private ArrayList<CalendarEvent> eventsForDay(
            Calendar day
    ) {
        ArrayList<CalendarEvent> result =
                new ArrayList<CalendarEvent>();

        String dateKey =
                new SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.US
                ).format(
                        day.getTime()
                );

        for (CalendarEvent event
                : currentEvents) {

            if (event.occursOn(
                    dateKey
            )) {
                result.add(event);
            }
        }

        return result;
    }

    private LinearLayout createDaySummaryCard(
            String label,
            String value
    ) {
        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setGravity(
                Gravity.CENTER
        );

        card.setPadding(
                8,
                7,
                8,
                7
        );

        card.setBackground(
                createBorderDrawable(
                        Color.rgb(
                                250,
                                250,
                                250
                        ),
                        Color.rgb(
                                205,
                                205,
                                205
                        ),
                        1
                )
        );

        card.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        62,
                        1
                )
        );

        TextView labelView =
                new TextView(this);

        labelView.setText(label);
        labelView.setTextSize(10);
        labelView.setTextColor(Color.DKGRAY);
        labelView.setGravity(Gravity.CENTER);

        TextView valueView =
                new TextView(this);

        valueView.setText(value);
        valueView.setTextSize(15);
        valueView.setTextColor(Color.BLACK);
        valueView.setGravity(Gravity.CENTER);

        card.addView(labelView);
        card.addView(valueView);

        return card;
    }

    private LinearLayout createDayEventCard(
            CalendarEvent event
    ) {
        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setPadding(
                14,
                10,
                14,
                10
        );

        card.setBackground(
                createBorderDrawable(
                        Color.WHITE,
                        Color.rgb(
                                190,
                                190,
                                190
                        ),
                        1
                )
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                8
        );

        card.setLayoutParams(cardParams);

        TextView dateRange =
                new TextView(this);

        dateRange.setText(
                event.getDisplayDateRange()
        );

        dateRange.setTextSize(12);
        dateRange.setTextColor(Color.DKGRAY);
        dateRange.setGravity(
                Gravity.CENTER
        );

        dateRange.setLayoutParams(
                new LinearLayout.LayoutParams(
                        180,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
        );

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setPadding(
                18,
                0,
                8,
                0
        );

        content.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView title =
                new TextView(this);

        title.setText(
                event.getTitle()
        );

        title.setTextSize(17);
        title.setTextColor(Color.BLACK);
        title.setSingleLine(false);

        content.addView(title);

        if (event.getLocation()
                .length() > 0) {

            TextView location =
                    new TextView(this);

            location.setText(
                    "Ort: "
                            + event.getLocation()
            );

            location.setTextSize(12);
            location.setTextColor(Color.DKGRAY);
            location.setPadding(
                    0,
                    3,
                    0,
                    0
            );

            content.addView(location);
        }

        if (event.getDescription()
                .length() > 0) {

            TextView description =
                    new TextView(this);

            description.setText(
                    event.getDescription()
            );

            description.setTextSize(12);
            description.setTextColor(Color.DKGRAY);
            description.setPadding(
                    0,
                    4,
                    0,
                    0
            );

            content.addView(description);
        }

        card.addView(dateRange);
        card.addView(
                createVerticalLine(58)
        );
        card.addView(content);

        return card;
    }

    private void showDayExtendedDetails(
            Calendar day,
            ArrayList<CalendarEvent> events
    ) {
        String dateKey =
                new SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.US
                ).format(
                        day.getTime()
                );

        StringBuilder details =
                new StringBuilder();

        for (CalendarEvent event
                : events) {

            if (!event.occursOn(
                    dateKey
            )) {
                continue;
            }

            if (details.length()
                    > 0) {

                details.append(
                        "\n\n────────────\n\n"
                );
            }

            details.append(
                    event.getTitle()
            );
            details.append("\n");
            details.append(
                    event.getDisplayDateRange()
            );

            if (event.getLocation()
                    .length() > 0) {

                details.append(
                        "\nOrt: "
                );
                details.append(
                        event.getLocation()
                );
            }

            if (event.getDescription()
                    .length() > 0) {

                details.append("\n\n");
                details.append(
                        event.getDescription()
                );
            }

            if (event.getAttendees()
                    .length() > 0) {

                details.append(
                        "\n\nTeilnehmer: "
                );
                details.append(
                        event.getAttendees()
                );
            }
        }

        if (details.length()
                == 0) {

            details.append(
                    "Keine erweiterten Termindaten vorhanden."
            );
        }

        showTextDialog(
                "Termindetails",
                details.toString()
        );
    }

    private void showEventDetails(
            CalendarEvent event
    ) {
        StringBuilder details =
                new StringBuilder();

        details.append(
                event.getDisplayDateRange()
        );

        if (event.getLocation()
                .length() > 0) {

            details.append(
                    "\nOrt: "
            );

            details.append(
                    event.getLocation()
            );
        }

        if (event.getDescription()
                .length() > 0) {

            details.append("\n\n");
            details.append(
                    event.getDescription()
            );
        }

        if (event.getAttendees()
                .length() > 0) {

            details.append(
                    "\n\nTeilnehmer: "
            );

            details.append(
                    event.getAttendees()
            );
        }

        showTextDialog(
                event.getTitle(),
                details.toString()
        );
    }

    private void showTextDialog(
            String title,
            String message
    ) {
        if (dayDialog != null
                && dayDialog.isShowing()) {

            dayDialog.dismiss();
        }

        dayDialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(
                                "Schließen",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which
                                    ) {
                                        dialog.dismiss();

                                        scheduleAutomaticReset();
                                    }
                                }
                        )
                        .create();

        dayDialog.show();
    }

    private void scheduleAutomaticReset() {
        resetHandler.removeCallbacks(
                automaticReset
        );

        resetHandler.postDelayed(
                automaticReset,
                RESET_DELAY_MS
        );
    }

    private final Runnable automaticReset =
            new Runnable() {
                @Override
                public void run() {
                    navigationOffsetWeeks = 0;
                    activeView =
                            VIEW_CALENDAR;

                    if (currentViewButton
                            != null) {

                        currentViewButton.setVisibility(
                                View.GONE
                        );
                    }

                    if (dayDialog != null
                            && dayDialog.isShowing()) {

                        dayDialog.dismiss();
                    }

                    if (weatherDialog != null
                            && weatherDialog.isShowing()) {

                        weatherDialog.dismiss();
                    }

                    if (warningDialog != null
                            && warningDialog.isShowing()) {

                        warningDialog.dismiss();
                    }

                    if (settingsDialog != null
                            && settingsDialog.isShowing()) {

                        settingsDialog.dismiss();
                    }

                    if (fullscreenDayDialog != null
                            && fullscreenDayDialog.isShowing()) {

                        fullscreenDayDialog.dismiss();
                    }

                    showActiveView();
                }
            };

    private boolean isSameDay(
            Calendar first,
            Calendar second
    ) {
        return first.get(
                Calendar.YEAR
        ) == second.get(
                Calendar.YEAR
        ) && first.get(
                Calendar.DAY_OF_YEAR
        ) == second.get(
                Calendar.DAY_OF_YEAR
        );
    }

    private int calculateWeekNumberWidth() {
        int screenWidth =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        return Math.max(
                42,
                screenWidth / 18
        );
    }

    private int calculateCellWidth() {
        int screenWidth =
                getResources()
                        .getDisplayMetrics()
                        .widthPixels;

        int availableWidth =
                screenWidth
                        - 20
                        - calculateWeekNumberWidth();

        return availableWidth / 7;
    }

    private int calculateDayCellHeight() {
        int screenHeight =
                getResources()
                        .getDisplayMetrics()
                        .heightPixels;

        int reservedHeight = 320;

        int availableHeight =
                screenHeight
                        - reservedHeight;

        int calculated =
                availableHeight
                        / DISPLAY_WEEKS;

        return Math.max(
                72,
                calculated
        );
    }

    private GridLayout.LayoutParams createGridParams(
            int width,
            int height,
            int margin
    ) {
        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();

        params.width =
                Math.max(
                        1,
                        width
                                - margin * 2
                );

        params.height =
                height;

        params.setMargins(
                margin,
                margin,
                margin,
                margin
        );

        return params;
    }

    private GradientDrawable createBorderDrawable(
            int fillColor,
            int strokeColor,
            int strokeWidth
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                fillColor
        );

        drawable.setStroke(
                strokeWidth,
                strokeColor
        );

        return drawable;
    }

    private String formatCalendarPreviewTitle(
            String title
    ) {
        if (title == null) {
            return "";
        }

        String clean =
                title.trim()
                        .replace(
                                "\n",
                                " "
                        );

        if (clean.length() <= 15) {
            return clean;
        }

        int breakPosition =
                clean.lastIndexOf(
                        ' ',
                        15
                );

        if (breakPosition < 6) {
            breakPosition = 15;
        }

        String firstLine =
                clean.substring(
                        0,
                        breakPosition
                ).trim();

        String secondLine =
                clean.substring(
                        breakPosition
                ).trim();

        if (secondLine.length() > 16) {
            secondLine =
                    secondLine.substring(
                            0,
                            15
                    ).trim()
                            + "…";
        }

        return firstLine
                + "\n"
                + secondLine;
    }

    private String shortenTitle(
            String title,
            int maxLength
    ) {
        if (title == null) {
            return "";
        }

        if (title.length()
                > maxLength) {

            return title.substring(
                    0,
                    maxLength
            ) + "…";
        }

        return title;
    }

    private static class EventPreview {

        private final String text;
        private final int moreCount;

        private EventPreview(
                String text,
                int moreCount
        ) {
            this.text = text;
            this.moreCount =
                    moreCount;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        clockHandler.removeCallbacksAndMessages(
                null
        );

        countdownHandler.removeCallbacksAndMessages(
                null
        );

        syncHandler.removeCallbacksAndMessages(
                null
        );

        batteryHandler.removeCallbacksAndMessages(
                null
        );

        resetHandler.removeCallbacksAndMessages(
                null
        );

        nightHandler.removeCallbacksAndMessages(
                null
        );

        buttonAnimationHandler.removeCallbacksAndMessages(
                null
        );

        lightIdleHandler.removeCallbacksAndMessages(
                null
        );

        if (dayDialog != null
                && dayDialog.isShowing()) {

            dayDialog.dismiss();
        }

        if (weatherDialog != null
                && weatherDialog.isShowing()) {

            weatherDialog.dismiss();
        }

        if (fullscreenDayDialog != null
                && fullscreenDayDialog.isShowing()) {

            fullscreenDayDialog.dismiss();
        }

        if (settingsDialog != null
                && settingsDialog.isShowing()) {

            settingsDialog.dismiss();
        }
    }
}
