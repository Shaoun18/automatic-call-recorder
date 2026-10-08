package com.shaoun.callrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Restarts accessibility monitoring after device reboot.
 * The Accessibility Service restarts automatically by Android OS.
 * This receiver is a safety net.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootReceiver", "Device booted — call monitoring ready");
        // Accessibility service auto-restarts; no action needed here
        // Add any init logic if needed in future
    }
}
