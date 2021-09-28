package com.alexsykes.scoremonster.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.NumberPadFragment;
import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.data.TimeContract;
import com.alexsykes.scoremonster.data.TimeDbHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimerActivity extends AppCompatActivity {
    NumberPadFragment numberPadFragment;
    LinearLayout content;
    TextView numberLabel, statusLine;
    SharedPreferences localPrefs;
    Button finishButton, startClockButton;
    long clockStartTime, startInterval;
    private int ridingNumber, trialid, mode;
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    private TimeDbHelper timeDbHelper;
    MediaPlayer mediaPlayer;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        getPrefs();
        UISetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Alex", "onResume called");
        getPrefs();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy H:mm:ss");
        String dateString = dateFormat.format(clockStartTime);
        statusLine.setText("Start time: " + dateString);
    }

    @Override
    protected void onPause() {
        Log.i("Alex", "onPause called");
        super.onPause();
        saveCurrentState();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.timer_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Show scores on remote server
            case R.id.timesheet:
                goTimesheet();
                return true;

            // Enter andinitialise section details
            case R.id.setup:
                goSetup();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void goTimesheet() {
        Intent intent = new Intent(this, TimeListActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void goSetup() {
        Intent intent = new Intent(this, TimerResetActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void saveCurrentState() {
        Log.i("Note", "saveCurrentState called");
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        if (clockStartTime > 0) {
            String currentRiderText = numberLabel.getText().toString();
            int rider = 0;
            if (!currentRiderText.equals("")) {
                rider = Integer.parseInt(numberLabel.getText().toString());
            }
            editor.putInt("ridingNumber", rider);
            editor.apply();
        }
    }

    private void UISetup() {
        // Add custom ActionBar
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getMenu();

        statusLine = findViewById(R.id.statusLine);
        startClockButton = findViewById(R.id.startClockButton);
        startClockButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startClock(this);
                return false;
            }

            private void startClock(View.OnLongClickListener onLongClickListener) {
                Calendar startTime = Calendar.getInstance();
                clockStartTime = startTime.getTimeInMillis();

                // Save start time in prefs
                SharedPreferences.Editor editor = localPrefs.edit();
                editor.putLong("clockStartTime", clockStartTime);
                editor.apply();
            }
        });
        numberPadFragment = new NumberPadFragment();
        content = findViewById(R.id.content);
        numberLabel = findViewById(R.id.riderNumberLabel);
        finishButton = findViewById(R.id.finishButton);
        finishButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                save(this);
                return false;
            }
        });

        // Button setup
        if (ridingNumber > 0) {
            numberLabel.setText(String.valueOf(ridingNumber));
        } else {
            numberLabel.setText("");
        }
        if (clockStartTime > 0) {
            startClockButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.VISIBLE);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy H:mm:ss");
            String dateString = dateFormat.format(clockStartTime);
            statusLine.setText("Start time: " + dateString);
            getSupportFragmentManager().beginTransaction().replace(R.id.content, numberPadFragment).commit();
        } else {
            startClockButton.setVisibility(View.VISIBLE);
            finishButton.setVisibility(View.GONE);
            numberLabel.setText("Clock not started");
        }
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        trialid = localPrefs.getInt("trialid", 0);
        ridingNumber = localPrefs.getInt("ridingNumber", 0);
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        mode = localPrefs.getInt("mode", 0);
    }

    public void addDigit(View view) {
        // Get length of rider riderNumber
        String riderNumber = numberLabel.getText().toString();
        int len = riderNumber.length();

        // Get id from clicked button to get clicked digit
        int intID = view.getId();
        Button button = view.findViewById(intID);
        String digit = button.getText().toString();

        // Compare with backspace
        if (digit.equals("⌫")) {
            if (len > 0) {
                riderNumber = riderNumber.substring(0, len - 1);
            }
        } else if (digit.equals("C")) {
            riderNumber = "";
        } else {
            riderNumber = riderNumber + digit;
            if (len > 2)
                riderNumber = riderNumber.substring(1, 4);
        }

        if (riderNumber.equals("0")) {
            riderNumber = "";
        }
        numberLabel.setText(riderNumber);
    }

    private void save(View.OnLongClickListener view) {
        Log.i("Note", "Saving finish time");

        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Get String values for rider and scoreLabel
        String rider = numberLabel.getText().toString();
        // Get value for time
        Calendar finishTime = Calendar.getInstance();
        long finishTimeInMillis = finishTime.getTimeInMillis();

        // NOTE Do NOT use null
        if (rider.equals("")) {
            toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else vibrator.vibrate(500);
            new AlertDialog.Builder(this).setTitle("Warning").setMessage(R.string.missing_rider_number).setNeutralButton("Close", null).show();
        } else {
            // Otherwise enter scores
            int riderNumber = Integer.parseInt(rider);
//            int scoreValue = Integer.parseInt(score);

            // Update prefs for single rider
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putInt("ridingNumber", riderNumber);
            editor.putString("riderText", rider);
            editor.apply();

            insertTime(riderNumber, finishTimeInMillis);
            clearScore();
        }
    }

    private void insertTime(int riderNumber, long finishTimeInMillis) {
        long elapsedTime;
        timeDbHelper = new TimeDbHelper(this);
        SQLiteDatabase db = timeDbHelper.getWritableDatabase();
        long deltaTime = finishTimeInMillis - clockStartTime;

        // Calculate rider's elapsed time using startInterval

        // Create a ContentValues object where column names are the keys,
        ContentValues values = new ContentValues();
        values.put(TimeContract.TimeEntry.COLUMN_TIME_NUMBER, riderNumber);
        values.put(TimeContract.TimeEntry.COLUMN_TIME_TRIALID, trialid);
        values.put(TimeContract.TimeEntry.COLUMN_TIME_FINISHTIME, finishTimeInMillis);
        values.put(TimeContract.TimeEntry.COLUMN_TIME_ELAPSEDTIME, elapsedTime);
        db.insert(TimeContract.TimeEntry.TABLE_NAME, null, values);


        // Confirm committed with sound
        playSoundFile(R.raw.ting);
        Toast.makeText(this, "Finish time recorded", Toast.LENGTH_SHORT).show();
    }

    private void clearScore() {
        numberLabel.setText("");
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("ridingNumber", 0);
        editor.apply();

    }

    //play a soundfile
    public void playSoundFile(Integer fileName) {
        mediaPlayer = MediaPlayer.create(this, fileName);
        mediaPlayer.start();
    }
}