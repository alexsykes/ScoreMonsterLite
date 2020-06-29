package com.alexsykes.scoremonster.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alexsykes.scoremonster.NumberPadFragment;
import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.ScorePadFragment;
import com.alexsykes.scoremonster.TouchFragment;
import com.alexsykes.scoremonster.data.FinishTimeDbHelper;
import com.alexsykes.scoremonster.data.ScoreContract;
import com.alexsykes.scoremonster.data.ScoreDbHelper;

// TODO Important - move database setup method from ScoreDbHelper

public class MainActivity extends AppCompatActivity {

    public static final int TEXT_REQUEST = 1;
    public static final int NOT_SYNCED = -1;

    TextView numberLabel, scoreLabel, statusLine;
    String riderNumber, status, theTrialName;
    ScorePadFragment scorePadFragment;
    NumberPadFragment numberPadFragment;
    TouchFragment touchFragment;
    SharedPreferences localPrefs;

    // Databases
    private ScoreDbHelper mDbHelper;
    private FinishTimeDbHelper timeDbHelper;

    private String observer;
    private int section;
    private int trialid;
    private int numlaps;
    private int score;
    private boolean showDabPad;
    private boolean showNumberPad;
    int modeIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create database connection
        mDbHelper = new ScoreDbHelper(this);
        mDbHelper.getWritableDatabase();
        timeDbHelper = new FinishTimeDbHelper(this);
        timeDbHelper.getWritableDatabase();

        // Add custom ActionBar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(myToolbar);
        myToolbar.getMenu();

        // Add score and numberPad fragemnts
        scorePadFragment = new ScorePadFragment();
        numberPadFragment = new NumberPadFragment();
        touchFragment = new TouchFragment();
        numberLabel = findViewById(R.id.numberLabel);
        scoreLabel = findViewById(R.id.scoreLabel);
        statusLine = findViewById(R.id.statusLine);


        getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();

        // Set up button to save scores
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                save(this);
                return false;
            }
        });

        if (!getPrefs()) {
            //  goSetup();
        }
    }

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline());
        editor.commit();


        clearScore();
        super.onStart();
        getPrefs();
        switch (modeIndex) {
            case 1:
                getSupportFragmentManager().beginTransaction().replace(R.id.bottom, touchFragment).commit();
                break;
            case 0:
                getSupportFragmentManager().beginTransaction().replace(R.id.bottom, scorePadFragment).commit();
                break;
            /*case 2:
                Intent intent = new Intent(this, TimerActivity.class);
                intent.putExtra("trialid", trialid);
                startActivityForResult(intent, TEXT_REQUEST);
                break;*/
        }
       /* if (showDabPad) {
            getSupportFragmentManager().beginTransaction().replace(R.id.bottom, touchFragment).commit();
        } else if (showNumberPad) {
            getSupportFragmentManager().beginTransaction().replace(R.id.bottom, scorePadFragment).commit();
        }
       else if (timingModeSelect) {
            Intent intent = new Intent(this, TimerActivity.class);
            intent.putExtra("trialid", trialid);
            startActivityForResult(intent, TEXT_REQUEST);
        }*/
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the state of item position
        outState.putString("rider", numberLabel.getText().toString());
        outState.putString("score", scoreLabel.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Read the state of item position
        numberLabel.setText(savedInstanceState.getString("rider"));
        scoreLabel.setText(savedInstanceState.getString("score"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Enter andinitialise section details
            case R.id.setup:
                goSetup();
                return true;

            // Show scores on remote server
            case R.id.list:
                // goShowScoresFromServer();
                goShowSummaryScores();
                return true;

            // Sync scores with remote db
            // Shows scores stored on device
            case R.id.upload:
                goSync();
                return true;

            case R.id.timeMode:
                goTimingMode();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

/*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("isStartTimeSet", false);
        editor.putLong("startTime", 0);
        //editor.commit();
    }
*/

    private void goTimingMode() {
        Intent intent = new Intent(this, TimerActivity.class);
        intent.putExtra("trialid", trialid);
        startActivityForResult(intent, TEXT_REQUEST);
    }

    private void goShowSummaryScores() {
        Intent intent = new Intent(this, SummaryScoreActivity.class);
        intent.putExtra("trialid", trialid);
        intent.putExtra("section", section);
        startActivityForResult(intent, TEXT_REQUEST);

    }

    public void countDabs(View view) {
        int intID = view.getId();
        Button button = view.findViewById(intID);
        String digit = button.getText().toString();

        switch (digit) {
            case "Clean":
                score = 0;
                break;
            case "Ten":
                score = 10;
                break;
            case "Five":
                score = 5;
                break;
            case "Dab":
                if (score < 3)
                    score++;
                break;
        }
        scoreLabel.setText(String.valueOf(score));
    }

    private void goSync() {
        Intent intent = new Intent(this, SyncActivity.class);
        startActivityForResult(intent, TEXT_REQUEST);
    }

    private void goSetup() {
        Intent intent = new Intent(this, SetupActivity.class);
        startActivityForResult(intent, TEXT_REQUEST);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void addDigit(View view) {
        // Get length of rider riderNumber
        numberLabel = findViewById(R.id.numberLabel);
        riderNumber = numberLabel.getText().toString();
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

    public void enterScore(View view) {
        scoreLabel = findViewById(R.id.scoreLabel);

        // Get id from clicked button to get clicked digit
        int intID = view.getId();
        Button button = view.findViewById(intID);
        String digit = button.getText().toString();

        scoreLabel.setText(digit);
    }

    private void save(View.OnLongClickListener view) {

        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Get String values for rider and scoreLabel
        String rider = numberLabel.getText().toString();
        String score = scoreLabel.getText().toString();

        // NOTE Do NOT use null

        if (score.equals("") || rider.equals("")) {
            toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
            new AlertDialog.Builder(this).setTitle("Warning").setMessage("Missing rider number or score").setNeutralButton("Close", null).show();
        } else {
            // Otherwise enter scores
            int riderNumber = Integer.parseInt(rider);
            int scoreValue = Integer.parseInt(score);

            insertScore(riderNumber, scoreValue);

            clearScore();
        }
    }

    private void insertScore(int rider, int score) {
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Check for numberof completed laps
        // Gets the database in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int lap = 1 + mDbHelper.getRiderLap(rider, section, trialid);

        if (lap > numlaps) {
            toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150);
            Toast.makeText(this, "Already completed " + numlaps + " laps", Toast.LENGTH_LONG).show();
        } else {

            // Create a ContentValues object where column names are the keys,
            ContentValues values = new ContentValues();
            // String dateString = currentTimeStamp;
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_OBSERVER, observer);
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER, rider);
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE, score);
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION, section);
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_LAP, lap);
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_TRIALID, trialid);
            values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC, NOT_SYNCED);

            db.insert(ScoreContract.ScoreEntry.TABLE_NAME, null, values);
            toneGen1.startTone(ToneGenerator.TONE_CDMA_CONFIRM, ToneGenerator.MAX_VOLUME);
            Toast.makeText(this, "Score saved", Toast.LENGTH_SHORT).show();
        }
    }

    // Reset the  rider/score values
    private void clearScore() {
        score = 0;
        numberLabel.setText("");
        scoreLabel.setText("0");
    }

    private boolean getPrefs() {
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);

        observer = localPrefs.getString("observer", "");
        section = localPrefs.getInt("section", 0);
        trialid = localPrefs.getInt("trialid", 0);
        numlaps = localPrefs.getInt("numlaps", 0);
        theTrialName = localPrefs.getString("theTrialName", "None selected");
        showDabPad = localPrefs.getBoolean("showDabPad", true);
        showNumberPad = localPrefs.getBoolean("showNumberPad", true);
        showDabPad = localPrefs.getBoolean("showDabPad", true);
        showNumberPad = localPrefs.getBoolean("showNumberPad", true);
        modeIndex = localPrefs.getInt("modeIndex", 0);

        status = theTrialName + " - Section: " + section + " - Observer: " + observer;

        statusLine.setText(status);

        return trialid != 0;
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public void checkConnection() {

        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        SharedPreferences.Editor editor = localPrefs.edit();

        if (isOnline()) {
            editor.putBoolean("canConnect", true);
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        } else {
            editor.putBoolean("canConnect", false);
            Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_LONG).show();
        }
        editor.commit();
    }
}
