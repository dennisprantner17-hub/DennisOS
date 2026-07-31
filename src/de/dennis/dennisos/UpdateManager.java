package de.dennis.dennisos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public final class UpdateManager {

    private static boolean checkRunning = false;
    private static boolean downloadRunning = false;

    private UpdateManager() {
    }

    public static void checkForUpdate(
            final Activity activity,
            final boolean showUpToDateMessage
    ) {
        if (checkRunning || activity == null) {
            return;
        }

        if (UpdateConfig.VERSION_JSON_URL.contains(
                "DEIN_GITHUB_NAME"
        ) || UpdateConfig.VERSION_JSON_URL.contains(
                "DEIN_REPOSITORY"
        )) {
            if (showUpToDateMessage) {
                showMessage(
                        activity,
                        "OTA noch nicht eingerichtet",
                        "Bitte zuerst den GitHub-Link in UpdateConfig.java eintragen."
                );
            }
            return;
        }

        checkRunning = true;

        new AsyncTask<Void, Void, CheckResult>() {

            @Override
            protected CheckResult doInBackground(
                    Void... ignored
            ) {
                try {
                    String json = downloadText(
                            UpdateConfig.VERSION_JSON_URL
                    );

                    JSONObject object =
                            new JSONObject(json);

                    UpdateInfo info =
                            new UpdateInfo(
                                    object.getInt(
                                            "versionCode"
                                    ),
                                    object.optString(
                                            "versionName",
                                            "Neue Version"
                                    ),
                                    object.getString(
                                            "apkUrl"
                                    ),
                                    object.optString(
                                            "sha256",
                                            ""
                                    ).trim().toLowerCase(
                                            Locale.US
                                    ),
                                    object.optString(
                                            "notes",
                                            ""
                                    )
                            );

                    return new CheckResult(
                            info,
                            null
                    );

                } catch (Exception exception) {
                    return new CheckResult(
                            null,
                            exception
                    );
                }
            }

            @Override
            protected void onPostExecute(
                    CheckResult result
            ) {
                checkRunning = false;

                if (activity.isFinishing()) {
                    return;
                }

                if (result.error != null) {
                    if (showUpToDateMessage) {
                        showMessage(
                                activity,
                                "Updateprüfung fehlgeschlagen",
                                friendlyError(result.error)
                        );
                    }
                    return;
                }

                int installedVersion =
                        installedVersionCode(
                                activity
                        );

                if (result.info.versionCode
                        <= installedVersion) {

                    if (showUpToDateMessage) {
                        showMessage(
                                activity,
                                "DennisOS ist aktuell",
                                "Installierte Version: "
                                        + installedVersion
                        );
                    }
                    return;
                }

                showUpdateDialog(
                        activity,
                        result.info
                );
            }
        }.execute();
    }

    private static void showUpdateDialog(
            final Activity activity,
            final UpdateInfo info
    ) {
        String message =
                "Version "
                        + info.versionName
                        + " ist verfügbar.";

        if (info.notes.length() > 0) {
            message += "\n\nNeu:\n"
                    + info.notes;
        }

        new AlertDialog.Builder(activity)
                .setTitle("DennisOS-Update")
                .setMessage(message)
                .setNegativeButton(
                        "Später",
                        null
                )
                .setPositiveButton(
                        "Herunterladen",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which
                            ) {
                                downloadUpdate(
                                        activity,
                                        info
                                );
                            }
                        }
                )
                .show();
    }

    private static void downloadUpdate(
            final Activity activity,
            final UpdateInfo info
    ) {
        if (downloadRunning) {
            return;
        }

        downloadRunning = true;

        final DownloadDialog progress =
                new DownloadDialog(activity);

        progress.show();

        new AsyncTask<Void, Integer, DownloadResult>() {

            @Override
            protected DownloadResult doInBackground(
                    Void... ignored
            ) {
                HttpURLConnection connection = null;
                FileOutputStream output = null;
                InputStream input = null;

                try {
                    connection = openConnection(
                            info.apkUrl
                    );

                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty(
                            "User-Agent",
                            "DennisOS-Updater/1.0"
                    );
                    connection.connect();

                    int responseCode =
                            connection.getResponseCode();

                    if (responseCode < 200
                            || responseCode >= 300) {
                        throw new Exception(
                                "Download-Fehler "
                                        + responseCode
                        );
                    }

                    int totalBytes =
                            connection.getContentLength();

                    File updateDirectory =
                            findWritableUpdateDirectory(
                                    activity
                            );

                    File apkFile =
                            new File(
                                    updateDirectory,
                                    "DennisOS-update.apk"
                            );

                    if (apkFile.exists()
                            && !apkFile.delete()) {
                        throw new Exception(
                                "Alte Update-Datei konnte nicht entfernt werden."
                        );
                    }

                    input = new BufferedInputStream(
                            connection.getInputStream()
                    );

                    output = new FileOutputStream(
                            apkFile,
                            false
                    );

                    byte[] buffer =
                            new byte[8192];

                    long received = 0;
                    int read;

                    while ((read = input.read(buffer))
                            != -1) {
                        output.write(
                                buffer,
                                0,
                                read
                        );

                        received += read;

                        if (totalBytes > 0) {
                            publishProgress(
                                    (int) (
                                            received * 100L
                                                    / totalBytes
                                    )
                            );
                        }
                    }

                    output.flush();

                    if (info.sha256.length() > 0) {
                        String actualHash =
                                sha256(apkFile);

                        if (!info.sha256.equals(
                                actualHash
                        )) {
                            apkFile.delete();
                            throw new Exception(
                                    "Die Prüfsumme der APK stimmt nicht."
                            );
                        }
                    }

                    return new DownloadResult(
                            apkFile,
                            null
                    );

                } catch (Exception exception) {
                    return new DownloadResult(
                            null,
                            exception
                    );

                } finally {
                    closeQuietly(input);
                    closeQuietly(output);

                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            @Override
            protected void onProgressUpdate(
                    Integer... values
            ) {
                if (values.length > 0) {
                    progress.setProgress(
                            values[0]
                    );
                }
            }

            @Override
            protected void onPostExecute(
                    DownloadResult result
            ) {
                downloadRunning = false;
                progress.dismiss();

                if (activity.isFinishing()) {
                    return;
                }

                if (result.error != null) {
                    showMessage(
                            activity,
                            "Update konnte nicht geladen werden",
                            friendlyError(result.error)
                    );
                    return;
                }

                openInstaller(
                        activity,
                        result.apkFile
                );
            }
        }.execute();
    }

    private static File findWritableUpdateDirectory(
            Activity activity
    ) throws Exception {

        File[] candidates =
                new File[]{
                        activity.getCacheDir(),
                        activity.getExternalCacheDir(),
                        activity.getExternalFilesDir(
                                Environment.DIRECTORY_DOWNLOADS
                        ),
                        activity.getExternalFilesDir(null),
                        new File(
                                Environment.getExternalStorageDirectory(),
                                "DennisOS"
                        )
                };

        for (File candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            try {
                if (!candidate.exists()
                        && !candidate.mkdirs()) {
                    continue;
                }

                // Alte Tolino-Firmware meldet externe App-Ordner teils als
                // vorhanden, verweigert aber bereits mkdirs(). Der interne
                // Cache ist zuverlässig beschreibbar. Für den separaten
                // Paket-Installer muss der Ordner außerdem durchsuchbar sein.
                candidate.setReadable(true, false);
                candidate.setExecutable(true, false);
            } catch (Exception ignored) {
                continue;
            }

            File testFile =
                    new File(
                            candidate,
                            ".write-test"
                    );

            FileOutputStream testOutput = null;

            try {
                testOutput =
                        new FileOutputStream(
                                testFile,
                                false
                        );

                testOutput.write(1);
                testOutput.flush();

                testFile.delete();

                return candidate;

            } catch (Exception ignored) {
                testFile.delete();

            } finally {
                closeQuietly(testOutput);
            }
        }

        throw new Exception(
                "Kein beschreibbarer Speicherort für das Update gefunden."
        );
    }

    private static void openInstaller(
            Activity activity,
            File apkFile
    ) {
        try {
            apkFile.setReadable(
                    true,
                    false
            );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            intent.setDataAndType(
                    Uri.fromFile(apkFile),
                    "application/vnd.android.package-archive"
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            activity.startActivity(intent);

        } catch (Exception exception) {
            showMessage(
                    activity,
                    "Installer konnte nicht geöffnet werden",
                    "Die APK liegt hier:\n"
                            + apkFile.getAbsolutePath()
                            + "\n\n"
                            + friendlyError(exception)
            );
        }
    }

    private static String downloadText(
            String urlText
    ) throws Exception {
        HttpURLConnection connection =
                openConnection(urlText);

        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty(
                "User-Agent",
                "DennisOS-Updater/1.0"
        );

        BufferedReader reader = null;

        try {
            int responseCode =
                    connection.getResponseCode();

            if (responseCode < 200
                    || responseCode >= 300) {
                throw new Exception(
                        "Server antwortet mit "
                                + responseCode
                );
            }

            reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            "UTF-8"
                    )
            );

            StringBuilder text =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine())
                    != null) {
                text.append(line);
            }

            return text.toString();

        } finally {
            closeQuietly(reader);
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(
            String urlText
    ) throws Exception {
        URL url = new URL(urlText);

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        connection.setInstanceFollowRedirects(true);

        if (connection instanceof HttpsURLConnection
                && android.os.Build.VERSION.SDK_INT < 21) {

            SSLContext context =
                    SSLContext.getInstance(
                            "TLSv1.2"
                    );

            context.init(
                    null,
                    null,
                    null
            );

            ((HttpsURLConnection) connection)
                    .setSSLSocketFactory(
                            new Tls12SocketFactory(
                                    context.getSocketFactory()
                            )
                    );
        }

        return connection;
    }

    private static int installedVersionCode(
            Activity activity
    ) {
        try {
            PackageInfo info =
                    activity.getPackageManager()
                            .getPackageInfo(
                                    activity.getPackageName(),
                                    0
                            );

            return info.versionCode;

        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String sha256(
            File file
    ) throws Exception {
        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        InputStream input =
                new BufferedInputStream(
                        new java.io.FileInputStream(
                                file
                        )
                );

        try {
            byte[] buffer =
                    new byte[8192];

            int read;

            while ((read = input.read(buffer))
                    != -1) {
                digest.update(
                        buffer,
                        0,
                        read
                );
            }

        } finally {
            input.close();
        }

        byte[] bytes = digest.digest();
        StringBuilder hex = new StringBuilder();

        for (byte value : bytes) {
            hex.append(
                    String.format(
                            Locale.US,
                            "%02x",
                            value & 0xff
                    )
            );
        }

        return hex.toString();
    }

    private static void showMessage(
            Activity activity,
            String title,
            String message
    ) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private static String friendlyError(
            Exception exception
    ) {
        String message = exception.getMessage();

        if (message == null
                || message.length() == 0) {
            return exception.getClass()
                    .getSimpleName();
        }

        return message;
    }

    private static void closeQuietly(
            java.io.Closeable closeable
    ) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static class UpdateInfo {
        private final int versionCode;
        private final String versionName;
        private final String apkUrl;
        private final String sha256;
        private final String notes;

        private UpdateInfo(
                int versionCode,
                String versionName,
                String apkUrl,
                String sha256,
                String notes
        ) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.notes = notes;
        }
    }

    private static class CheckResult {
        private final UpdateInfo info;
        private final Exception error;

        private CheckResult(
                UpdateInfo info,
                Exception error
        ) {
            this.info = info;
            this.error = error;
        }
    }

    private static class DownloadResult {
        private final File apkFile;
        private final Exception error;

        private DownloadResult(
                File apkFile,
                Exception error
        ) {
            this.apkFile = apkFile;
            this.error = error;
        }
    }

    private static class DownloadDialog {
        private final Dialog dialog;
        private final TextView status;

        private DownloadDialog(
                Activity activity
        ) {
            dialog = new Dialog(
                    activity,
                    android.R.style.Theme_Holo_Light_Dialog_NoActionBar
            );

            LinearLayout content =
                    new LinearLayout(activity);

            content.setOrientation(
                    LinearLayout.VERTICAL
            );
            content.setPadding(
                    30,
                    24,
                    30,
                    24
            );
            content.setGravity(Gravity.CENTER);
            content.setBackgroundColor(Color.WHITE);

            TextView title =
                    new TextView(activity);

            title.setText("Update wird geladen");
            title.setTextSize(20);
            title.setTextColor(Color.BLACK);
            title.setGravity(Gravity.CENTER);

            status = new TextView(activity);
            status.setText("0 %");
            status.setTextSize(18);
            status.setTextColor(Color.DKGRAY);
            status.setGravity(Gravity.CENTER);
            status.setPadding(
                    0,
                    14,
                    0,
                    0
            );

            content.addView(title);
            content.addView(status);
            dialog.setContentView(content);
            dialog.setCancelable(false);
        }

        private void show() {
            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        460,
                        WindowManager.LayoutParams.WRAP_CONTENT
                );
            }
        }

        private void setProgress(
                int percent
        ) {
            status.setText(
                    percent + " %"
            );
        }

        private void dismiss() {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }
}
