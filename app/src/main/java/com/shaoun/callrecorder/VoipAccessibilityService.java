package com.shaoun.callrecorder;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * VoipAccessibilityService
 * Uses Android Accessibility API to detect when VoIP call screens appear.
 * This is the standard method used by call recorders to detect WhatsApp/Viber/Imo calls.
 * User must enable this in Settings > Accessibility — that IS the notification mechanism.
 */
public class VoipAccessibilityService extends AccessibilityService {

    private static final String TAG = "VoipAccessibility";

    // Package identifiers
    private static final String PKG_WHATSAPP = "com.whatsapp";
    private static final String PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b";
    private static final String PKG_VIBER = "com.viber.voip";
    private static final String PKG_IMO = "com.imo.android.imoim";
    private static final String PKG_IMO_HD = "com.imo.android.imoimhd";

    // UI keywords that indicate a call is in progress
    private static final String[] WHATSAPP_CALL_INDICATORS = {
        "Ongoing call", "Voice call", "Video call", "Call connected",
        "Swipe up to return to call"
    };
    private static final String[] VIBER_CALL_INDICATORS = {
        "Viber call in progress", "Voice call", "Connected"
    };
    private static final String[] IMO_CALL_INDICATORS = {
        "Voice call", "Call in progress"
    };

    private boolean isVoipCallActive = false;
    private String activePackage = null;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", true)) return;

        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        int eventType = event.getEventType();

        // Only process relevant packages
        if (!isVoipPackage(pkg)) return;

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            boolean callDetected = detectCallInProgress(pkg);

            if (callDetected && !isVoipCallActive) {
                isVoipCallActive = true;
                activePackage = pkg;
                String source = packageToSource(pkg);
                Log.d(TAG, "VoIP call detected: " + source);

                if (prefs.getBoolean("record_" + source, true)) {
                    startRecording("VoIP Call", source);
                }

            } else if (!callDetected && isVoipCallActive && pkg.equals(activePackage)) {
                // Check if the call ended — window changed away from call screen
                if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    isVoipCallActive = false;
                    activePackage = null;
                    Log.d(TAG, "VoIP call ended");
                    stopRecording();
                }
            }
        }
    }

    private boolean detectCallInProgress(String pkg) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        String[] indicators = getIndicatorsForPackage(pkg);
        for (String indicator : indicators) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(indicator);
            if (nodes != null && !nodes.isEmpty()) {
                root.recycle();
                return true;
            }
        }

        // Also check content description of visible nodes
        boolean found = checkNodeTree(root, pkg);
        root.recycle();
        return found;
    }

    private boolean checkNodeTree(AccessibilityNodeInfo node, String pkg) {
        if (node == null) return false;
        CharSequence desc = node.getContentDescription();
        CharSequence text = node.getText();
        String[] indicators = getIndicatorsForPackage(pkg);

        for (String indicator : indicators) {
            if ((desc != null && desc.toString().contains(indicator)) ||
                (text != null && text.toString().contains(indicator))) {
                return true;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (checkNodeTree(child, pkg)) {
                if (child != null) child.recycle();
                return true;
            }
            if (child != null) child.recycle();
        }
        return false;
    }

    private String[] getIndicatorsForPackage(String pkg) {
        switch (pkg) {
            case PKG_WHATSAPP:
            case PKG_WHATSAPP_BUSINESS:
                return WHATSAPP_CALL_INDICATORS;
            case PKG_VIBER:
                return VIBER_CALL_INDICATORS;
            case PKG_IMO:
            case PKG_IMO_HD:
                return IMO_CALL_INDICATORS;
            default:
                return new String[]{};
        }
    }

    private boolean isVoipPackage(String pkg) {
        return pkg.equals(PKG_WHATSAPP) || pkg.equals(PKG_WHATSAPP_BUSINESS) ||
               pkg.equals(PKG_VIBER) || pkg.equals(PKG_IMO) || pkg.equals(PKG_IMO_HD);
    }

    private String packageToSource(String pkg) {
        if (pkg.contains("whatsapp")) return "whatsapp";
        if (pkg.contains("viber")) return "viber";
        if (pkg.contains("imo")) return "imo";
        return "voip";
    }

    private void startRecording(String caller, String source) {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_START);
        intent.putExtra(RecordingService.EXTRA_CALLER, caller);
        intent.putExtra(RecordingService.EXTRA_SOURCE, source);
        startForegroundService(intent);
    }

    private void stopRecording() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_STOP);
        startService(intent);
    }

    @Override
    public void onInterrupt() {
        if (isVoipCallActive) {
            stopRecording();
            isVoipCallActive = false;
        }
    }
}
