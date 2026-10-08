package com.shaoun.callrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RecordingService
 * Foreground service that handles actual audio capture.
 * Uses MediaRecorder for reliability and low battery impact.
 * Notification is always shown (required for foreground + transparency).
 */
public class RecordingService extends Service {

    private static final String TAG = "RecordingService";
    public static final String CHANNEL_ID = "call_recorder_channel";
    public static final String ACTION_START = "com.shaoun.callrecorder.START";
    public static final String ACTION_STOP  = "com.shaoun.callrecorder.STOP";
    public static final String EXTRA_CALLER = "caller_name";
    public static final String EXTRA_SOURCE = "call_source"; // "phone", "whatsapp", "viber", "imo"

    private MediaRecorder mediaRecorder;
    private PowerManager.WakeLock wakeLock;
    private String currentFilePath;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            String caller = intent.getStringExtra(EXTRA_CALLER);
            String source = intent.getStringExtra(EXTRA_SOURCE);
            if (!isRecording) {
                startForeground(1, buildNotification(caller, source));
                startRecording(caller, source);
            }
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void startRecording(String caller, String source) {
        try {
            acquireWakeLock();
            currentFilePath = buildFilePath(caller, source);
            ensureDirectoryExists(currentFilePath);

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setOutputFile(currentFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            Log.d(TAG, "Recording started: " + currentFilePath);

            // Save to DB
            RecordingDatabase.getInstance(this).insertRecording(
                new Recording(caller, source, currentFilePath, System.currentTimeMillis(), 0)
            );

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            // Fallback to VOICE_CALL source
            startRecordingFallback(caller, source);
        }
    }

    private void startRecordingFallback(String caller, String source) {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setOutputFile(currentFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "Fallback recording started");
        } catch (IOException ex) {
            Log.e(TAG, "Fallback also failed", ex);
            stopSelf();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;

                // Update duration in DB
                if (currentFilePath != null) {
                    File f = new File(currentFilePath);
                    long duration = getDurationMs(currentFilePath);
                    RecordingDatabase.getInstance(this)
                        .updateDuration(currentFilePath, duration, f.length());
                }

                Log.d(TAG, "Recording stopped: " + currentFilePath);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
            }
        }
        releaseWakeLock();
    }

    private String buildFilePath(String caller, String source) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String customPath = prefs.getString("save_path", null);

        File dir;
        if (customPath != null) {
            dir = new File(customPath);
        } else {
            dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "CallRecorder");
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(new Date());
        String safeCaller = (caller != null ? caller.replaceAll("[^a-zA-Z0-9_]", "_") : "Unknown");
        String filename = source + "_" + safeCaller + "_" + timestamp + ".m4a";
        return new File(dir, filename).getAbsolutePath();
    }

    private void ensureDirectoryExists(String filePath) {
        File dir = new File(filePath).getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    private long getDurationMs(String path) {
        try {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            mmr.setDataSource(path);
            String d = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            mmr.release();
            return d != null ? Long.parseLong(d) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Notification buildNotification(String caller, String source) {
        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, openApp,
            PendingIntent.FLAG_IMMUTABLE);

        String title = "Recording " + (source != null ? source : "call");
        String text  = caller != null ? "With: " + caller : "Call in progress...";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_recording)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Call Recorder", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows when a call is being recorded");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + ":WakeLock");
            wakeLock.acquire(10 * 60 * 1000L); // max 10 min safety
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }
}
