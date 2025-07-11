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
import android.os.StrictMode;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import uk.trialmonster.observer.MainViewModel;
import uk.trialmonster.observer.NumberPadFragment;
import uk.trialmonster.observer.PadFragment;
import uk.trialmonster.observer.R;
import uk.trialmonster.observer.TouchFragment;
import uk.trialmonster.observer.data.ScoreContract;
import uk.trialmonster.observer.data.ScoreDbHelper;
import uk.trialmonster.observer.data.TimeContract;
import uk.trialmonster.observer.data.TimeDbHelper;
import uk.trialmonster.observer.data.TrialDbHelper;

public class MainActivity extends AppCompatActivity {
    //    Starts here
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    public static final String TAG = "Info";
    public static final int NOT_SYNCED = -1;
    SharedPreferences localPrefs;
    SharedPreferences.Editor editor;
    MainViewModel model;

    // UI Components
    TouchFragment touchFragment;
    NumberPadFragment numberPadFragment;
    Fragment padFragment;
    Button saveButton;

    // Layout variables
    TextView numberLabel, scoreLabel, newScoreLabel, sectionNumberTextView, decrementTextView,
            incrementTextView, statusLine;
    LinearLayout  sectionLabelLayout;
    LinearLayout top, bottom, bottom2;
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
    private int day;
    private int scoreCount;
    private int usermode;
    private int section;
    private int numsections;
    private int numlaps;
    private int loggedInUserID;
    private int numberInGroup;
    private boolean isSingleUser, trialHasChanged, canConnect, timeMode, isManualTrial,
            isLoggedInUser, isAdminUser;
    private int ridingNumber, trialid, mode;
    private String scorePadType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.new_main_layout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Add this:
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());
        // Add custom ActionBar
        Toolbar toolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);

        toolbar.getMenu();
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
                goInfo();
                return true;

            // Enter andinitialise section details
            case R.id.setup:
                goSetup();
                return true;

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

    private void goInfo() {
        Intent intent = new Intent(this, SummaryActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
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
        String scoreString = String.valueOf(score);
        if (score == 10) {
            scoreString = "X";
        }
        scoreLabel.setText(scoreString);
        switch (scoreString) {
            case "0":
                scoreLabel.setTextColor(getColor(R.color.colorButtonGreen));
                break;
            case "5":
                scoreLabel.setTextColor(getColor(R.color.colorButtonBrightRed));
                break;
            case "X":
                scoreLabel.setTextColor(getColor(R.color.colorButtonBrightRed));
                break;
            default:
                scoreLabel.setTextColor(getColor(R.color.colorButtonAmber));
        }
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = localPrefs.edit();
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        startInterval = localPrefs.getLong("startInterval", 60);
        penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
        observer = localPrefs.getString("observer", "");
        mobile = localPrefs.getString("mobile", "");
        section = localPrefs.getInt("section", 1);
        trialid = localPrefs.getInt("trialid", -999);
        numlaps = localPrefs.getInt("numlaps", 1);
        numsections = localPrefs.getInt("numsections", 1);
        email = localPrefs.getString("email", "");
        isSingleUser = localPrefs.getBoolean("isSingleUser", false);
        isManualTrial = localPrefs.getBoolean("isManualTrial", true);
//        isAdminUser = false;
        ridingNumber = localPrefs.getInt("ridingNumber", 0);
        day = localPrefs.getInt("dayNum", 1);
        score = localPrefs.getInt("score", 0);
        numberInGroup = localPrefs.getInt("numberInGroup", 6);
        scoreCount = localPrefs.getInt("scoreCount", 0);
        theTrialName = localPrefs.getString("trialName", "My Trial");
        club = localPrefs.getString("club", "None selected");
        mode = localPrefs.getInt("mode", 0);
        usermode = Integer.valueOf(localPrefs.getString("usermode", "0"));
        timeMode = localPrefs.getBoolean("timeMode", false);
        isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
        loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
        username = localPrefs.getString("username", "");
        scorePadType = localPrefs.getString("scorePadType", "trad");


        if (loggedInUserID == 0) {
            editor.putInt("loggedInUserID", 0);
            editor.putBoolean("isLoggedInUser", false);
        }
        editor.apply();
    }

    void initialUISetup() {
        // Initialise UI fields
        numberLabel = findViewById(R.id.numberLabel);
        scoreLabel = findViewById(R.id.scoreLabel);
        statusLine = findViewById(R.id.statusLine);
//        statusLine.setVisibility(View.VISIBLE);
//        sectionLabelLayout = findViewById(R.id.sectionLabelLayout);
//        sectionNumberTextView = findViewById(R.id.sectionNumber);
//        sectionNumberTextView.setText(valueOf(section));
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
//            sectionLabelLayout.setVisibility(View.GONE);
            status = theTrialName + " - Observer: " + observer + " - Day: " + day + " - " +
                    "Section: " + section;
            statusLine.setText(status);
            saveButton.setText(R.string.save);
            scoreLabel.setVisibility(View.VISIBLE);

//            if (touchFragment == null && !timeMode) {
//                touchFragment = new TouchFragment();
//                getSupportFragmentManager().beginTransaction().add(R.id.bottom, touchFragment).commit();
//            }
// Set up number pad

            numberPadFragment = new NumberPadFragment();
            padFragment = new PadFragment();
            touchFragment = new TouchFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.bottom, padFragment).commit();
            getSupportFragmentManager().beginTransaction().add(R.id.bottom, touchFragment).commit();

            if (scorePadType.equals("trad")) {
                getSupportFragmentManager().beginTransaction().replace(R.id.bottom, padFragment).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.bottom, touchFragment).commit();
            }


//            if (numberPadFragment == null) {
            numberPadFragment = new NumberPadFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();
//            }

//            if (padFragment == null && !timeMode) {
//                if (scorePadType.equals("pad")) {
////                    padFragment = new PadFragment();
//                } else {
////                    padFragment = new TouchFragment();
//                }
//                getSupportFragmentManager().beginTransaction().add(R.id.bottom, padFragment).commit();
//            }

//            bottom.setVisibility(View.GONE);
//            R.id.bottom2.setVisibility(View.GONE);
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
//            int scoreValue = Integer.parseInt(score);

            if (score.equals("X")) {
                score = "x";
            }
            // Update prefs for single rider
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putInt("ridingNumber", riderNumber);
            editor.putString("riderText", rider);
            editor.apply();
            if (!isManualTrial) {
                updateScore(riderNumber, score, day);
                scoreCount++;
                clearScore();
            } else {
                saveManualScore(riderNumber, score, day);
                clearScore();
            }
        }
    }

    private void saveManualScore(int rider, String score, int day) {
//        Log.i(TAG, "saveManualScore: ");
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);

        // Check number of laps completed
        SQLiteDatabase db = scoreDbHelper.getWritableDatabase();
        int lap = 1 + scoreDbHelper.getRiderLap(rider, section, trialid, day);

        if (lap > numlaps) {
            toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150);
            Toast.makeText(this, "Already completed " + numlaps + " laps", Toast.LENGTH_LONG).show();
        } else {
            String query =
                    "INSERT INTO scores ('lap', 'rider', 'score', 'created', 'updated', 'trialid', " +
                            "'section', " +
                            "'sync') " +
                            "VALUES(" + lap + " , " + rider + "," + score + "," +
                            " DATETIME('now'), " +
                            " DATETIME('now'), " + trialid + ", " + section + ", -1) ";
//            Log.i(TAG, "Query: " + query);
            db.execSQL(query);

            playSoundFile(R.raw.ting);
            Toast.makeText(this, "Score saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateScore(int rider, String score, int day) {

        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);

        // Check number of laps completed
        SQLiteDatabase db = scoreDbHelper.getWritableDatabase();
        int lap = 1 + scoreDbHelper.getRiderLap(rider, section, trialid, day);

        if (lap > numlaps) {
            toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150);
            Toast.makeText(this, "Already completed " + numlaps + " laps", Toast.LENGTH_LONG).show();
        } else {
            String query =
                    "UPDATE scores SET score = '" + score + "', sync = -1, updated = DATETIME" +
                            " ('now'), " +
                            " observer = '" + observer +
                            "' WHERE trialid = " + trialid +
                            " AND section =  " + section +
                            " AND day = " + day +
                            " AND lap = " + lap +
                            " AND rider = " + rider;
            db.execSQL(query);

            playSoundFile(R.raw.ting);
            Toast.makeText(this, "Score saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void insertScore(int rider, String score) {
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME);
        // Check for numberof completed laps
        // Gets the database in write mode
        SQLiteDatabase db = scoreDbHelper.getWritableDatabase();
        int lap = 1 + scoreDbHelper.getRiderLap(rider, section, trialid, day);
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

//            Log.i("Note", "trialid: " + trialid);
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
        String scoreString = valueOf(score);
        if (score == 10) {
            scoreString = "X";
        }
        scoreLabel.setText(scoreString);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
    }

    private void clearScore() {
        // Clear score label
        score = 0;
        scoreLabel.setText("0");
        scoreLabel.setTextColor(getColor(R.color.colorButtonGreen));

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

    public void scoreClean(View view) {
        score = 0;
        scoreLabel.setText("0");
        scoreLabel.setTextColor(getColor(R.color.colorButtonGreen));
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
        Log.i(TAG, "scoreClean: ");
    }


    public void scoreOne(View view) {
        score = 1;
        scoreLabel.setText("1");
        scoreLabel.setTextColor(getColor(R.color.colorButtonAmber));
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
        Log.i(TAG, "scoreOne: ");
    }


    public void scoreTwo(View view) {
        score = 2;
        scoreLabel.setText("2");
        scoreLabel.setTextColor(getColor(R.color.colorButtonAmber));
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
        Log.i(TAG, "scoreTwo: ");
    }

    public void scoreThree(View view) {
        score = 3;
        scoreLabel.setText("3");
        scoreLabel.setTextColor(getColor(R.color.colorButtonAmber));
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
        Log.i(TAG, "scoreThree: ");
    }

    public void scoreFive(View view) {
        score = 5;
        scoreLabel.setText("5");
        scoreLabel.setTextColor(getColor(R.color.colorButtonBrightRed));
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putInt("score", score);
        editor.apply();
        Log.i(TAG, "scoreFive: ");
    }

    // Time utility methods
    private void saveTime(View.OnLongClickListener view) {
//        Log.i("Note", "Saving finish time");

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