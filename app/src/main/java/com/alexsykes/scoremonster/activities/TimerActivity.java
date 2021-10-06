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
import com.alexsykes.scoremonster.data.TrialDbHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimerActivity extends AppCompatActivity {
    NumberPadFragment numberPadFragment;
    LinearLayout content;
    TextView riderNumberLabel, statusLine;
    SharedPreferences localPrefs;
    Button finishButton, startClockButton;
    long clockStartTime, startInterval, penaltyTariff;
    private int ridingNumber, trialid, mode;
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    private TimeDbHelper timeDbHelper;
    private TrialDbHelper trialDbHelper;
    MediaPlayer mediaPlayer;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("Info", "TimeActivity: onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        // Add custom ActionBar
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getMenu();

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(false);
        numberPadFragment = new NumberPadFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.content, numberPadFragment).commit();

        // Add widgets
        UISetup();

        // Load up preferences for trialid, riderNumber, clockStartTime
        getPrefs();
    }

    private void UISetup() {
        statusLine = findViewById(R.id.statusLine);
        startClockButton = findViewById(R.id.startClockButton);
        content = findViewById(R.id.content);
        riderNumberLabel = findViewById(R.id.riderNumberLabel);
        finishButton = findViewById(R.id.finishButton);

        // Set listeners
        finishButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveTime(this);
                return false;
            }
        });

        startClockButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startClock(this);
                return false;
            }

            // called when button pressed
            private void startClock(View.OnLongClickListener onLongClickListener) {
                Calendar startTime = Calendar.getInstance();
                clockStartTime = startTime.getTimeInMillis();

                // Save start time in prefs
                SharedPreferences.Editor editor = localPrefs.edit();
                editor.putLong("clockStartTime", clockStartTime);
                editor.apply();
                SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
                String dateString = dateFormat.format(clockStartTime);
                statusLine.setText("Clock started at " + dateString);
                content.setVisibility(View.VISIBLE);
                startClockButton.setVisibility(View.GONE);
                statusLine.setVisibility(View.VISIBLE);
                finishButton.setVisibility(View.VISIBLE);
                riderNumberLabel.setText("");
            }
        });
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        trialid = localPrefs.getInt("trialid", 0);
        ridingNumber = localPrefs.getInt("ridingNumber", 0);
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        startInterval = localPrefs.getLong("startInterval", 60);
        penaltyTariff = localPrefs.getLong("penaltyTariff", 60);

        if (clockStartTime == 0) {
            statusLine.setVisibility(View.INVISIBLE);
            startClockButton.setVisibility(View.VISIBLE);
            finishButton.setVisibility(View.GONE);
            // riderNumberLabel.setVisibility(View.VISIBLE);
            riderNumberLabel.setText("Timer not started");
            content.setVisibility(View.GONE);
        } else {
            statusLine.setVisibility(View.VISIBLE);
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
            String dateString = dateFormat.format(clockStartTime);
            statusLine.setText("Start time: " + dateString);
            startClockButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.VISIBLE);
            content.setVisibility(View.VISIBLE);
            // Button setup
            if (ridingNumber > 0) {
                riderNumberLabel.setText(String.valueOf(ridingNumber));
            } else {
                riderNumberLabel.setText("");
            }
        }
    }

    public void addDigit(View view) {
        // Get length of rider riderNumber
        String riderNumber = riderNumberLabel.getText().toString();
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
        riderNumberLabel.setText(riderNumber);
    }

    private void saveTime(View.OnLongClickListener view) {
        Log.i("Note", "Saving finish time");

        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Get String values for rider and scoreLabel
        String rider = riderNumberLabel.getText().toString();
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
        long elapsedTime, timeInterval, deltaTime, riderStartTime;
        trialDbHelper = new TrialDbHelper(this);
        // startInterval = trialDbHelper.getStartInterval(trialid);

        /*  finishTimeInMillis - real finishtime
            timeInterval - time delay for each rider
            Zero for #1
            deltaTime - real time difference between startTime and riderStartTime
            riderStartTime - time rider actually started
            elapsedTime - time on course for rider
         */
        timeInterval = (riderNumber - 1) * 1000 * startInterval;
        riderStartTime = clockStartTime + timeInterval;

        timeDbHelper = new TimeDbHelper(this);
        SQLiteDatabase db = timeDbHelper.getWritableDatabase();
        deltaTime = finishTimeInMillis - riderStartTime;

        // Calculate rider's elapsed time using startInterval
        // startInterval measured in seconds

        // Create a ContentValues object where column names are the keys,
        ContentValues values = new ContentValues();
        values.put(TimeContract.TimeEntry.COLUMN_TIME_NUMBER, riderNumber);
        values.put(TimeContract.TimeEntry.COLUMN_TIME_TRIALID, trialid);
        values.put(TimeContract.TimeEntry.COLUMN_TIME_FINISHTIME, finishTimeInMillis);
        values.put(TimeContract.TimeEntry.COLUMN_TIME_ELAPSEDTIME, deltaTime);
        db.insert(TimeContract.TimeEntry.TABLE_NAME, null, values);


        // timeDbHelper.updateTrial(trialid, startInterval, penaltyTariff);
        // Confirm committed with sound
        playSoundFile(R.raw.ting);
        Toast.makeText(this, "Finish time recorded", Toast.LENGTH_SHORT).show();
    }

    private void clearScore() {
        riderNumberLabel.setText("");
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("ridingNumber", 0);
        editor.apply();

    }

    //play a soundfile
    public void playSoundFile(Integer fileName) {
        mediaPlayer = MediaPlayer.create(this, fileName);
        mediaPlayer.start();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Show scores on remote server
            case R.id.help:
                goHelp();
                return true;

            // Show scores on remote server
            case R.id.scoresheet:
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

    private void goHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void goSetup() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPrefs();

        Log.i("Info", "TimeActivity: onResume called");
    }

    @Override
    protected void onPause() {
        Log.i("Info", "onPause called");
        super.onPause();
        saveCurrentState();
    }

    private void saveCurrentState() {
        Log.i("Info", "saveCurrentState called");
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        if (clockStartTime > 0) {
            String currentRiderText = riderNumberLabel.getText().toString();
            int rider = 0;
            if (!currentRiderText.equals("")) {
                rider = Integer.parseInt(riderNumberLabel.getText().toString());
            }
            editor.putInt("ridingNumber", rider);
            editor.apply();
        }
    }
}