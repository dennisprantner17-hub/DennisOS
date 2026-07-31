package de.dennis.dennisos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public abstract class AppBaseActivity extends Activity {

    protected LinearLayout content;
    private final Handler resetHandler = new Handler();

    protected abstract String appTitle();
    protected abstract void buildApp();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(14, 10, 14, 10);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView home = actionButton("⌂ Startseite");
        home.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openHome(); }
        });
        header.addView(home, new LinearLayout.LayoutParams(150, 62));

        TextView apps = actionButton("Apps");
        apps.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openApps(); }
        });
        header.addView(apps, new LinearLayout.LayoutParams(100, 62));

        TextView title = label(appTitle(), 27, Color.BLACK);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, 62, 1));

        TextView refresh = actionButton("↻");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { refreshApp(); }
        });
        header.addView(refresh, new LinearLayout.LayoutParams(72, 62));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(12, 12, 12, 18);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        buildApp();
        scheduleReset();
    }

    protected void refreshApp() {
        content.removeAllViews();
        buildApp();
    }

    protected TextView label(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(10, 8, 10, 8);
        return view;
    }

    protected TextView actionButton(String value) {
        TextView view = label(value, 18, Color.BLACK);
        view.setGravity(Gravity.CENTER);
        view.setBackground(border(Color.WHITE, Color.DKGRAY, 2));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    protected TextView sectionTitle(String value) {
        TextView view = label(value, 23, Color.BLACK);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackgroundColor(Color.rgb(235, 235, 235));
        view.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 6);
        view.setLayoutParams(params);
        return view;
    }

    protected GradientDrawable border(int fill, int stroke, int width) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(width, stroke);
        drawable.setCornerRadius(5);
        return drawable;
    }

    protected void showLoading(String value) {
        content.addView(label(value, 18, Color.DKGRAY));
    }

    protected interface TextCallback {
        void finished(String text);
        void failed(Exception error);
    }

    protected void loadText(final String address, final TextCallback callback) {
        new AsyncTask<Void, Void, String>() {
            private Exception error;

            @Override protected String doInBackground(Void... ignored) {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    connection = (HttpURLConnection) new URL(address).openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setUseCaches(false);
                    connection.setRequestProperty("User-Agent", "DennisOS/4.0");

                    if (connection instanceof HttpsURLConnection
                            && android.os.Build.VERSION.SDK_INT < 21) {
                        SSLContext context = SSLContext.getInstance("TLSv1.2");
                        context.init(null, null, null);
                        ((HttpsURLConnection) connection).setSSLSocketFactory(
                                new Tls12SocketFactory(context.getSocketFactory()));
                    }

                    reader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), "UTF-8"));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line).append('\n');
                    }
                    return result.toString();
                } catch (Exception exception) {
                    error = exception;
                    return null;
                } finally {
                    try { if (reader != null) reader.close(); } catch (Exception closeError) { }
                    if (connection != null) connection.disconnect();
                }
            }

            @Override protected void onPostExecute(String result) {
                if (isFinishing()) return;
                if (result != null) callback.finished(result);
                else callback.failed(error);
            }
        }.execute();
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openApps() {
        Intent intent = new Intent(this, AppsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        if (!(this instanceof AppsActivity)) finish();
    }

    private void scheduleReset() {
        resetHandler.removeCallbacksAndMessages(null);
        int minutes = getSharedPreferences("DennisOS_Settings", MODE_PRIVATE)
                .getInt("automatic_reset_minutes", 1);
        resetHandler.postDelayed(new Runnable() {
            @Override public void run() { openHome(); }
        }, Math.max(1, minutes) * 60L * 1000L);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) scheduleReset();
        return super.dispatchTouchEvent(event);
    }

    @Override protected void onDestroy() {
        resetHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
