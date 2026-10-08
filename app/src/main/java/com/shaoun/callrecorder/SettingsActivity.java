package com.shaoun.callrecorder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        bindSwitch(R.id.switchPhoneCalls,  "record_phone_calls",  true);
        bindSwitch(R.id.switchWhatsApp,    "record_whatsapp",     true);
        bindSwitch(R.id.switchViber,       "record_viber",        true);
        bindSwitch(R.id.switchImo,         "record_imo",          true);
    }

    private void bindSwitch(int viewId, String prefKey, boolean defaultVal) {
        Switch sw = findViewById(viewId);
        if (sw == null) return;
        sw.setChecked(prefs.getBoolean(prefKey, defaultVal));
        sw.setOnCheckedChangeListener((btn, checked) ->
            prefs.edit().putBoolean(prefKey, checked).apply());
    }
}
