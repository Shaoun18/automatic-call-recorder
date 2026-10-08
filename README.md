# Call Recorder — by Programmershaoun

A transparent, lightweight Android call recorder supporting Phone, WhatsApp, Viber, and Imo.

---

## Features
- Auto-records standard phone calls via telephony state listener
- Detects WhatsApp / Viber / Imo calls via Android Accessibility API
- Foreground service — always shows notification during recording (required by Android)
- Dormant when idle — zero battery drain between calls
- Built-in recordings list with playback and delete
- Per-source toggles (enable/disable each app individually)
- Saves as .m4a to Music/CallRecorder/ on device storage

---

## How to Build

### Requirements
- Android Studio Hedgehog or newer
- Android SDK 34
- Java 8+

### Steps
1. Open Android Studio → Open Project → select this folder
2. Wait for Gradle sync to complete
3. Add drawable resources (see below)
4. Connect an Android device (API 26+) or create an emulator
5. Click Run ▶

### Drawable Resources Needed
Create these in `/app/src/main/res/drawable/`:

**ic_launcher.xml** — app icon (use Android Asset Studio)
**ic_recording.xml** — microphone icon for notification
**ic_play.xml** — play button
**ic_stop.xml** — stop button
**ic_delete.xml** — trash icon
**indicator_active.xml** — green circle shape
**indicator_inactive.xml** — red circle shape

Quick shape drawables:
```xml
<!-- indicator_active.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#4CAF50"/>
</shape>

<!-- indicator_inactive.xml -->
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="#F44336"/>
</shape>
```

For icons, use Android Studio's built-in Vector Asset tool (right-click res → New → Vector Asset).

---

## First-Time Setup (on device)

1. Install the app
2. Grant permissions: Microphone, Phone, Call Log, Notifications
3. For WhatsApp / Viber / Imo:
   - Tap "Enable Now" on the yellow banner
   - Find "Call Recorder" in Accessibility Settings and enable it
4. Done — recording starts automatically when any call begins

---

## How It Works

| Call Type       | Detection Method                  |
|----------------|-----------------------------------|
| Phone call      | BroadcastReceiver (PHONE_STATE)   |
| WhatsApp        | Accessibility Service (UI events) |
| Viber           | Accessibility Service (UI events) |
| Imo             | Accessibility Service (UI events) |

The app is **dormant** between calls — no polling, no background processes, zero CPU until a call event fires.

---

## Transparency Notice
This app always shows a persistent notification while recording is active. This is:
- Required by Android OS for foreground services
- The mechanism by which the other party is aware a recording is in progress
- Cannot be suppressed on Android 8+

---

## Legal
Complying with local laws regarding call recording is the user's responsibility.
In many regions, you must inform the other party before recording.

---

## File Output
Recordings saved to: `[Device Storage]/Music/CallRecorder/`
Format: `[source]_[caller]_[timestamp].m4a`
Example: `whatsapp_+8801XXXXXXXX_20241015_143022.m4a`
