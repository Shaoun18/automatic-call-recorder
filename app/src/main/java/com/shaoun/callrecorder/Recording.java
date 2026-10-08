package com.shaoun.callrecorder;

public class Recording {
    public long id;
    public String caller;
    public String source;
    public String filePath;
    public long timestamp;
    public long duration; // ms
    public long fileSize; // bytes

    public Recording(String caller, String source, String filePath, long timestamp, long duration) {
        this.caller = caller;
        this.source = source;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public String getFormattedDuration() {
        long secs = duration / 1000;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    public String getFormattedSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return (fileSize / 1024) + " KB";
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
