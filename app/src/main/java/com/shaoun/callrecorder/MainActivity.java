package com.shaoun.callrecorder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;

    private Switch masterSwitch;
    private TextView statusText;
    private TextView recordingCountText;
    private View statusIndicator;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        masterSwitch       = findViewById(R.id.masterSwitch);
        statusText         = findViewById(R.id.statusText);
        statusIndicator    = findViewById(R.id.statusIndicator);
        recordingCountText = findViewById(R.id.recordingCountText);

        masterSwitch.setChecked(prefs.getBoolean("enabled", true));
        masterSwitch.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("enabled", checked).apply();
            updateStatus();
        });

        findViewById(R.id.btnRecordings).setOnClickListener(v ->
            startActivity(new Intent(this, RecordingsActivity.class)));

        findViewById(R.id.btnSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.btnAccessibility).setOnClickListener(v ->
            openAccessibilitySettings());

        checkAndRequestPermissions();
        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updateRecordingCount();
    }

    private void updateStatus() {
        boolean enabled = prefs.getBoolean("enabled", true);
        boolean accessibilityOn = isAccessibilityServiceEnabled();

        if (enabled) {
            statusText.setText("Active — monitoring calls");
            statusIndicator.setBackgroundResource(R.drawable.indicator_active);
        } else {
            statusText.setText("Paused — recording disabled");
            statusIndicator.setBackgroundResource(R.drawable.indicator_inactive);
        }

        // Show accessibility hint if not enabled
        View accessibilityCard = findViewById(R.id.accessibilityCard);
        if (accessibilityCard != null) {
            accessibilityCard.setVisibility(accessibilityOn ? View.GONE : View.VISIBLE);
        }
    }

    private void updateRecordingCount() {
        int count = RecordingDatabase.getInstance(this).getAllRecordings().size();
        recordingCountText.setText(count + " recording" + (count == 1 ? "" : "s"));
    }

    private void checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();
        String[] required = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
        };

        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                needed.toArray(new String[0]), PERM_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                    "Permission required: " + permissions[i].replace("android.permission.", ""),
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openAccessibilitySettings() {
        new AlertDialog.Builder(this)
            .setTitle("Enable VoIP Detection")
            .setMessage("To record WhatsApp, Viber, and Imo calls, enable " +
                "'Call Recorder' in Accessibility Settings.\n\nThis allows the app to " +
                "detect when a VoIP call screen is active.")
            .setPositiveButton("Open Settings", (d, w) ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
            .setNegativeButton("Not now", null)
            .show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + VoipAccessibilityService.class.getName();
        try {
            int enabled = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String services = Settings.Secure.getString(
                    getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                return services != null && services.contains(service);
            }
        } catch (Settings.SettingNotFoundException ignored) {}
        return false;
    }
}
