package uk.trialmonster.observer.activities;

import static java.lang.String.valueOf;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import uk.trialmonster.observer.MainViewModel;
import uk.trialmonster.observer.NumberPadFragment;
import uk.trialmonster.observer.R;
import uk.trialmonster.observer.TouchFragment;
import uk.trialmonster.observer.data.ScoreContract;
import uk.trialmonster.observer.data.ScoreDbHelper;
import uk.trialmonster.observer.data.TimeContract;
import uk.trialmonster.observer.data.TimeDbHelper;
import uk.trialmonster.observer.data.TrialDbHelper;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    public static final int TEXT_REQUEST = 1;
    public static final int NOT_SYNCED = -1;
    SharedPreferences localPrefs;
    MainViewModel model;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialData;

    // UI Components
    TouchFragment touchFragment;
    NumberPadFragment numberPadFragment;
    Button saveButton;

    // Layout variables
    TextView numberLabel, scoreLabel, sectionNumberTextView, decrementTextView,
            incrementTextView, statusLine;
    LinearLayout sectionPicker, sectionLabelLayout;
    ConstraintLayout top, bottom;
    // Utility
    ProgressDialog dialog = null;
    MediaPlayer mediaPlayer;
    // Variables
    long clockStartTime, startInterval, penaltyTariff;
    // Databases
    private ScoreDbHelper scoreDbHelper;
    private TimeDbHelper timeDbHelper;
    private TrialDbHelper trialDbHelper;
    private String status, mobile, observer, theTrialName, detail, email, club, message, username;
    private final int serverResponseCode = 0;
    private int score;
    private int scoreCount;
    private int usermode;
    private int section;
    private int numsections;
    private int numlaps;
    private int loggedInUserID;
    private int numberInGroup;
    private boolean isSingleUser, trialHasChanged, canConnect, timeMode, isManualTrial,
            isLoggedInUser;
    private int ridingNumber, trialid, mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.i("Info", "MainACtivityNew onCreate: called");
        setContentView(R.layout.activity_main_new);

        // Add custom ActionBar
        Toolbar myToolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getMenu();
        model = new MainViewModel();

        // Create database connection
        scoreDbHelper = new ScoreDbHelper(this);
        trialDbHelper = new TrialDbHelper(this);
        timeDbHelper = new TimeDbHelper(this);

        // TODO - add routine to check for timeMode
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (timeMode) {
                    saveTime(this);
                } else {
                    saveScore(this);
                }
                return false;
            }
        });
        model.saveCurrentValuesToModel(ridingNumber,
                score,
                section,
                trialid,
                numlaps,
                numsections);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Show scores on remote server
            case R.id.help:
                goHelp();
                return true;

            // Enter andinitialise section details
            case R.id.setup:
                goSetup();
                return true;

            // Show scores on remote server
//            case R.id.email:
//                // goShowScoresFromServer();
//                // goShowSummaryScores();
//                sendEmail();
//                return true;

            // Sync scores with remote db
            // Shows scores stored on device
            case R.id.scoresheet:
                if (timeMode) {
                    goTimeList();
                } else {
                    goScoreList();
                }
                return true;

/*            case R.id.timer:
                goTimer();
                return true;*/

//            case R.id.reset:
//                reset();
//                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        Log.i("Info", "MainActivityNew:onStart called");
        getPrefs();
        if (!checkPrefs()) {
            goSetup();
        }
        initialUISetup();
    }

    private boolean checkPrefs() {
        if (observer.equals("")) {
            return false;
        }
        if (mobile.equals("")) {
            return false;
        }
        if (email.equals("")) {
            return false;
        }

        if (!timeMode) {
            if (section == 0) {
                return false;
            }
            if (numlaps == 0) {
                return false;
            }
            return numsections != 0;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPrefs();
        if (ridingNumber > 0) {
            numberLabel.setText(String.valueOf(ridingNumber));
        }
        scoreLabel.setText(String.valueOf(score));
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        startInterval = localPrefs.getLong("startInterval", 60);
        penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
        observer = localPrefs.getString("observer", "");
        mobile = localPrefs.getString("mobile", "");
        section = localPrefs.getInt("section", 0);
        trialid = localPrefs.getInt("trialid", 0);
        numlaps = localPrefs.getInt("numlaps", 0);
        numsections = localPrefs.getInt("numsections", 0);
        email = localPrefs.getString("email", "");
        isSingleUser = localPrefs.getBoolean("isSingleUser", false);
        isManualTrial = localPrefs.getBoolean("manualTrial", true);
        ridingNumber = localPrefs.getInt("ridingNumber", 0);
        score = localPrefs.getInt("score", 0);
        numberInGroup = localPrefs.getInt("numberInGroup", 6);
        scoreCount = localPrefs.getInt("scoreCount", 0);
        theTrialName = localPrefs.getString("trialName", "");
        club = localPrefs.getString("club", "None selected");
//        trialHasChanged = localPrefs.getBoolean("", true);
        mode = localPrefs.getInt("mode", 0);
        usermode = Integer.valueOf(localPrefs.getString("usermode", "0"));
        timeMode = localPrefs.getBoolean("timeMode", false);
        isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
        loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
        username = localPrefs.getString("username", "");
        if (loggedInUserID == 0) {
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putInt("loggedInUserID", 0);
            editor.putBoolean("isLoggedInUser", false);
            editor.apply();
        }
    }

    void initialUISetup() {
        // Initialise UI fields
        numberLabel = findViewById(R.id.numberLabel);
        scoreLabel = findViewById(R.id.scoreLabel);
        statusLine = findViewById(R.id.statusLine);
//        statusLine.setVisibility(View.INVISIBLE);
        sectionLabelLayout = findViewById(R.id.sectionLabelLayout);
        sectionNumberTextView = findViewById(R.id.sectionNumber);
        sectionNumberTextView.setText(valueOf(section));
        top = findViewById(R.id.top);
        incrementTextView = findViewById(R.id.incrementTextView);
        decrementTextView = findViewById(R.id.decrementTextView);
//        sectionDetail = findViewById(R.id.sectionDetail);
        saveButton = findViewById(R.id.saveButton);

        // Set initial values
        if (timeMode) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
            String dateString = dateFormat.format(clockStartTime);
            statusLine.setText(R.string.clock_started_at + dateString);
            sectionLabelLayout.setVisibility(View.GONE);
            scoreLabel.setVisibility(View.INVISIBLE);
            if (clockStartTime > 0) {
                saveButton.setText(R.string.enter);
                saveButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        saveTime(this);
                        return false;
                    }
                });
            } else {
                saveButton.setText(R.string.start_clock);
                saveButton.setOnLongClickListener(new View.OnLongClickListener() {
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
                        statusLine.setText(getString(R.string.clock_started_at) + dateString);
                        saveButton.setText(R.string.enter);
                        numberLabel.setText("");
                    }
                });
            }

        } else {
            sectionLabelLayout.setVisibility(View.GONE);
            status = theTrialName + " - Observer: " + observer + " - Section: " + section;
            statusLine.setText(status);
            saveButton.setText(R.string.save);
            scoreLabel.setVisibility(View.VISIBLE);

            if (touchFragment == null && !timeMode) {
                touchFragment = new TouchFragment();
                getSupportFragmentManager().beginTransaction().add(R.id.bottom, touchFragment).commit();
            }
        }

        if (numberPadFragment == null) {
            numberPadFragment = new NumberPadFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();
        }

        if (touchFragment != null && timeMode) {
            getSupportFragmentManager().beginTransaction().remove(touchFragment).commit();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Section button methods
    public void increment(View v) {
        SharedPreferences.Editor editor = localPrefs.edit();
        if (section < numsections) {
            section++;
        } else if (section == numsections) {
            section = 1;
        }
        sectionNumberTextView.setText(valueOf(section));
        editor.putInt("section", section);
        editor.putString("sectionText", String.valueOf(section));
        editor.apply();
    }
    public void decrement(View v) {
        SharedPreferences.Editor editor = localPrefs.edit();
        if (section > 1) {
            section--;
        } else if (section == 1) {
            section = numsections;
        }
        sectionNumberTextView.setText(valueOf(section));
        editor.putInt("section", section);
        editor.putString("sectionText", String.valueOf(section));
        editor.apply();
    }

    // Score utility methods
    private void saveScore(View.OnLongClickListener view) {

        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Get String values for rider and scoreLabel
        String rider = numberLabel.getText().toString();
        String score = scoreLabel.getText().toString();

        // NOTE Do NOT use null

        if (score.equals("") || rider.equals("")) {
            toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2, 150);
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else vibrator.vibrate(500);
            new AlertDialog.Builder(this).setTitle("Warning").setMessage("Missing rider number or score").setNeutralButton("Close", null).show();
        } else {
            // Otherwise enter scores
            int riderNumber = Integer.parseInt(rider);
            int scoreValue = Integer.parseInt(score);

            if (score.equals("10")) {
                score = "x";
            }
            // Update prefs for single rider
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putInt("ridingNumber", riderNumber);
            editor.putString("riderText", rider);
            editor.apply();

            insertScore(riderNumber, score);
            scoreCount++;
            clearScore();
        }
    }

    private void insertScore(int rider, String score) {
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Check for numberof completed laps
        // Gets the database in write mode
        SQLiteDatabase db = scoreDbHelper.getWritableDatabase();
        int lap = 1 + scoreDbHelper.getRiderLap(rider, section, trialid);
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
            //   toneGen1.startTone(ToneGenerator.TONE_CDMA_CONFIRM, ToneGenerator.MAX_VOLUME);

            Log.i("Note", "trialid: " + trialid);
            // Confirm committed with sound
            playSoundFile(R.raw.ting);
            Toast.makeText(this, "Score saved", Toast.LENGTH_SHORT).show();
        }

        scoreDbHelper.close();
    }

    public void addDigit(View view) {
        // Get length of rider riderNumber
        numberLabel = findViewById(R.id.numberLabel);
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
            } else {
                riderNumber = "0";
                ridingNumber = 0;
            }
        } else if (digit.equals("C")) {
            riderNumber = "";
            ridingNumber = 0;
        } else {
            riderNumber = riderNumber + digit;
            ridingNumber = Integer.parseInt(riderNumber);
            if (len > 2)
                riderNumber = riderNumber.substring(1, 4);
            ridingNumber = Integer.parseInt(riderNumber);
        }

        if (riderNumber.equals("0")) {
            riderNumber = "";
        }
        numberLabel.setText(riderNumber);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("ridingNumber", ridingNumber);
        editor.apply();
    }

    public void countDabs(View view) {
        int intID = view.getId();
        Button button = view.findViewById(intID);
        String digit = button.getText().toString();

        switch (digit) {
            case "Clean":
                score = 0;
                break;
            case "x":
                score = 10;
                break;
            case "5":
                score = 5;
                break;
            case "Dab":
                if (score < 3)
                    score++;
                break;
        }
        scoreLabel.setText(valueOf(score));
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
    }

    private void clearScore() {
        // Clear score label
        score = 0;
        scoreLabel.setText("0");

        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("ridingNumber", 0);
        editor.putInt("score", 0);
        editor.apply();

        // Clear rider number if not a single rider
        if (mode != 1) {
            numberLabel.setText("");
        }
        // If a single rider, then increment section
        else {
            if (section < numsections) {
                section++;
            } else if (section == numsections) {
                section = 1;
            }
            sectionNumberTextView.setText(valueOf(section));
            editor.putInt("section", section);
            editor.apply();
        }
    }

    // Time utility methods
    private void saveTime(View.OnLongClickListener view) {
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
        long timeInterval, deltaTime, riderStartTime;

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

        // Confirm committed with sound
        playSoundFile(R.raw.ting);
        Toast.makeText(this, "Finish time recorded", Toast.LENGTH_SHORT).show();
    }

    // Menu options
    private void goScoreList() {
        Intent intent = new Intent(this, ScoreListActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void goTimeList() {
        Intent intent = new Intent(this, TimeListActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void goSetup() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        // getPrefs();
    }

    private void goHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    //play a soundfile
    public void playSoundFile(Integer fileName) {
        mediaPlayer = MediaPlayer.create(this, fileName);
        mediaPlayer.start();
    }
}