package com.shaoun.callrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * PhoneCallReceiver
 * Listens for standard cellular call state changes.
 * Triggers RecordingService when a call goes OFFHOOK (answered).
 * Stops when call goes IDLE (ended).
 */
public class PhoneCallReceiver extends BroadcastReceiver {

    private static final String TAG = "PhoneCallReceiver";
    private static String lastState = TelephonyManager.EXTRA_STATE_IDLE;
    private static String incomingNumber = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", true)) return;
        if (!prefs.getBoolean("record_phone_calls", true)) return;

        String action = intent.getAction();

        // Capture outgoing number
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            incomingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (state == null) return;

        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (number != null && !number.isEmpty()) {
            incomingNumber = number;
        }

        Log.d(TAG, "Call state: " + state + " | Number: " + incomingNumber);

        if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            if (!lastState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                startRecording(context, incomingNumber, "phone");
            }
        } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            if (!lastState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                stopRecording(context);
            }
            incomingNumber = "";
        } else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            incomingNumber = number != null ? number : "";
        }

        lastState = state;
    }

    private void startRecording(Context context, String caller, String source) {
        Intent serviceIntent = new Intent(context, RecordingService.class);
        serviceIntent.setAction(RecordingService.ACTION_START);
        serviceIntent.putExtra(RecordingService.EXTRA_CALLER, caller);
        serviceIntent.putExtra(RecordingService.EXTRA_SOURCE, source);
        context.startForegroundService(serviceIntent);
    }

    private void stopRecording(Context context) {
        Intent serviceIntent = new Intent(context, RecordingService.class);
        serviceIntent.setAction(RecordingService.ACTION_STOP);
        context.startService(serviceIntent);
    }
}
