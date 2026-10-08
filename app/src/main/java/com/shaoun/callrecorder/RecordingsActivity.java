package com.shaoun.callrecorder;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private List<Recording> recordings;
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView    = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadRecordings();
    }

    private void loadRecordings() {
        recordings = RecordingDatabase.getInstance(this).getAllRecordings();

        if (recordings.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            recyclerView.setAdapter(new RecordingsAdapter());
        }
    }

    private void playRecording(int position) {
        Recording r = recordings.get(position);
        File f = new File(r.filePath);
        if (!f.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(r.filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            playingPosition = position;
            mediaPlayer.setOnCompletionListener(mp -> {
                playingPosition = -1;
                recyclerView.getAdapter().notifyItemChanged(position);
            });
            recyclerView.getAdapter().notifyItemChanged(position);
        } catch (IOException e) {
            Toast.makeText(this, "Cannot play file", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecording(int position) {
        Recording r = recordings.get(position);
        new AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Delete this recording? This cannot be undone.")
            .setPositiveButton("Delete", (d, w) -> {
                new File(r.filePath).delete();
                RecordingDatabase.getInstance(this).deleteRecording(r.id);
                recordings.remove(position);
                recyclerView.getAdapter().notifyItemRemoved(position);
                if (recordings.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    // ——— Adapter ———

    class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView caller, source, timestamp, duration, size;
            ImageButton btnPlay, btnDelete;

            VH(View v) {
                super(v);
                caller    = v.findViewById(R.id.tvCaller);
                source    = v.findViewById(R.id.tvSource);
                timestamp = v.findViewById(R.id.tvTimestamp);
                duration  = v.findViewById(R.id.tvDuration);
                size      = v.findViewById(R.id.tvSize);
                btnPlay   = v.findViewById(R.id.btnPlay);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Recording r = recordings.get(pos);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

            h.caller.setText(r.caller != null && !r.caller.isEmpty() ? r.caller : "Unknown");
            h.source.setText(r.source != null ? r.source.toUpperCase() : "CALL");
            h.timestamp.setText(sdf.format(new Date(r.timestamp)));
            h.duration.setText(r.getFormattedDuration());
            h.size.setText(r.getFormattedSize());

            boolean playing = (playingPosition == pos);
            h.btnPlay.setImageResource(playing ? R.drawable.ic_stop : R.drawable.ic_play);

            h.btnPlay.setOnClickListener(v -> {
                if (playingPosition == pos) {
                    if (mediaPlayer != null) mediaPlayer.stop();
                    playingPosition = -1;
                    notifyItemChanged(pos);
                } else {
                    playRecording(pos);
                }
            });

            h.btnDelete.setOnClickListener(v -> deleteRecording(pos));
        }

        @Override
        public int getItemCount() { return recordings.size(); }
    }
}
