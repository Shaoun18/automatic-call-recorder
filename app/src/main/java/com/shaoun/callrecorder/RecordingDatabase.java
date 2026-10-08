package com.shaoun.callrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class RecordingDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "recordings.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "recordings";

    private static RecordingDatabase instance;

    public static synchronized RecordingDatabase getInstance(Context ctx) {
        if (instance == null) {
            instance = new RecordingDatabase(ctx.getApplicationContext());
        }
        return instance;
    }

    private RecordingDatabase(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "caller TEXT," +
            "source TEXT," +
            "file_path TEXT UNIQUE," +
            "timestamp INTEGER," +
            "duration INTEGER," +
            "file_size INTEGER" +
            ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public void insertRecording(Recording r) {
        ContentValues cv = new ContentValues();
        cv.put("caller", r.caller);
        cv.put("source", r.source);
        cv.put("file_path", r.filePath);
        cv.put("timestamp", r.timestamp);
        cv.put("duration", r.duration);
        cv.put("file_size", 0);
        getWritableDatabase().insertOrThrow(TABLE, null, cv);
    }

    public void updateDuration(String filePath, long duration, long size) {
        ContentValues cv = new ContentValues();
        cv.put("duration", duration);
        cv.put("file_size", size);
        getWritableDatabase().update(TABLE, cv, "file_path=?", new String[]{filePath});
    }

    public List<Recording> getAllRecordings() {
        List<Recording> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query(TABLE, null, null, null,
            null, null, "timestamp DESC");
        while (c.moveToNext()) {
            Recording r = new Recording(
                c.getString(c.getColumnIndexOrThrow("caller")),
                c.getString(c.getColumnIndexOrThrow("source")),
                c.getString(c.getColumnIndexOrThrow("file_path")),
                c.getLong(c.getColumnIndexOrThrow("timestamp")),
                c.getLong(c.getColumnIndexOrThrow("duration"))
            );
            r.id = c.getLong(c.getColumnIndexOrThrow("id"));
            r.fileSize = c.getLong(c.getColumnIndexOrThrow("file_size"));
            list.add(r);
        }
        c.close();
        return list;
    }

    public void deleteRecording(long id) {
        getWritableDatabase().delete(TABLE, "id=?", new String[]{String.valueOf(id)});
    }
}
