package uk.trialmonster.observer.redundant;
// TODO Check ridingNumber for consistency in all modes
// TODO Upload / email of time data

import static java.lang.String.valueOf;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.opencsv.CSVWriter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import uk.trialmonster.observer.MainViewModel;
import uk.trialmonster.observer.NumberPadFragment;
import uk.trialmonster.observer.R;
import uk.trialmonster.observer.TouchFragment;
import uk.trialmonster.observer.activities.HelpActivity;
import uk.trialmonster.observer.activities.ScoreListActivity;
import uk.trialmonster.observer.activities.SettingsActivity;
import uk.trialmonster.observer.data.ScoreContract;
import uk.trialmonster.observer.data.ScoreDbHelper;
import uk.trialmonster.observer.data.TimeDbHelper;
import uk.trialmonster.observer.data.TrialDbHelper;

/*
    Method  getTrialList(URL)

    * called - onCreate()
    * returns

    @ post
    theNames - CSV String of trial names
    theIds - - CSV String of trial ids
    saved in prefs


    Method getTrialDetails(int trialid)

    Scoring mode for TrialMonster
    *   0 - traditional observer scoring / manual detail entry
    *   1 - observer scoring to web
    *   2 - rider scoring to web
    *   3 - group scoring to web
    *   4 - time and observation

 */

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    NumberPadFragment numberPadFragment;
    LinearLayout content;
    TextView riderNumberLabel, statusLine;
    SharedPreferences localPrefs;
    Button finishButton, startClockButton;
    long clockStartTime, startInterval, penaltyTariff;
    MediaPlayer mediaPlayer;
    // Layout variables
    TextView numberLabel, scoreLabel, sectionNumber, decrementTextView, incrementTextView, sectionDetail;
    LinearLayout sectionPicker, sectionLabelLayout;
    ConstraintLayout top;
    String theURL;
    private int ridingNumber, trialid, mode;

    public static final int TEXT_REQUEST = 1;
    public static final int NOT_SYNCED = -1;
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";

    MainViewModel model;
    // Databases
    private ScoreDbHelper mDbHelper;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialData;
    private TimeDbHelper timeDbHelper;
    private TrialDbHelper trialDbHelper;
    private String message;
    private String status, filename, observer, theTrialName, detail, email, club;
    private boolean isSingleUser, trialHasChanged, isOnline, timeMode;
    TouchFragment touchFragment;
    Button saveButton;
    ProgressDialog dialog = null;
    private int score, scoreCount, serverResponseCode = 0, usermode, section, numsections, numlaps, numberInGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Info", "MainActivity: onCreate called");
        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(this).get(MainViewModel.class);

        // Load existing settings and check for connectivity
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        timeMode = localPrefs.getBoolean("timeMode", false);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline());
        editor.apply();

        model.setRefreshed(false);
        setContentView(R.layout.activity_main);

        // Create database connection
        mDbHelper = new ScoreDbHelper(this);
        trialDbHelper = new TrialDbHelper(this);

        UISetup();
        getPrefs();
        setMode();
        // Check for connectivity
        isOnline = isOnline();

        if (!isOnline && trialid == -999) {
            Toast.makeText(MainActivity.this, "Offline only", Toast.LENGTH_LONG).show();
            goSetup();
        }

        // ArrayList trials = trialDbHelper.getTrials();
        // Set up button to save scores
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                save(this);
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

    private void setMode() {
        mode = localPrefs.getInt("mode", 0);
        switch (mode) {
            case 0: // Manual entry
                sectionPicker.setVisibility(View.GONE);
                sectionLabelLayout.setVisibility(View.GONE);
                sectionDetail.setText("Section: " + section);
                break;
            case 1: // Observer with online trial details
                sectionPicker.setVisibility(View.GONE);
                sectionLabelLayout.setVisibility(View.GONE);
                sectionDetail.setText("Section: " + section);
                break;
            case 2: // Single rider - online details
                sectionPicker.setVisibility(View.VISIBLE);
                sectionLabelLayout.setVisibility(View.GONE);
                //  getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();
                getSupportFragmentManager().beginTransaction().remove(numberPadFragment).commit();
                sectionDetail.setText("Section: " + section);
                break;
            case 3: // Riding group online details
                sectionPicker.setVisibility(View.VISIBLE);
                sectionLabelLayout.setVisibility(View.GONE);
                sectionDetail.setText("Section: " + section);
                break;
            case 4: // T&O
                sectionPicker.setVisibility(View.GONE);
                sectionLabelLayout.setVisibility(View.GONE);
                sectionDetail.setText("Section: " + section);
                break;
//            case 5: // Time mode
//                sectionPicker.setVisibility(View.GONE);
//                sectionLabelLayout.setVisibility(View.GONE);
//                scoreLabel.setVisibility(View.GONE);
//                getSupportFragmentManager().beginTransaction().remove(touchFragment).commit();
//                break;
            default:
                sectionPicker.setVisibility(View.GONE);
                sectionLabelLayout.setVisibility(View.GONE);
                sectionDetail.setText("Section: " + section);
                break;
        }
/*        if (usermode == 3) {
            // goTimer();
        }
        if (mode == 0) {
            sectionPicker.setVisibility(View.INVISIBLE);
            sectionLabelLayout.setVisibility(View.VISIBLE);
            sectionDetail.setText("Section: " + section);
            // decrementTextView.setVisibility(View.INVISIBLE);
        } else {
            sectionPicker.setVisibility(View.VISIBLE);
            sectionLabelLayout.setVisibility(View.INVISIBLE);
            // decrementTextView.setVisibility(View.VISIBLE);
        }*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("Info", "MainActivity::onStart called");
        // Check network connectivity and set Prefs
        getPrefs();
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline());
        editor.apply();

        if (!isSingleUser) {
            numberLabel.setText("");
            top.setVisibility(View.VISIBLE);
        } else {
            top.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Info", "MainActivity: onResume called");
        // Restore values from model
        getPrefs();
        if (timeMode) {
            goTimer();
        } else {
            reloadFromModel();
            setMode();
            if (ridingNumber != 0) {
                numberLabel.setText(valueOf(ridingNumber));
            } else {
                numberLabel.setText("");
            }
            scoreLabel.setText(valueOf(score));
            sectionNumber.setText(valueOf(section));
        }
    }

    @Override
    protected void onPause() {
        Log.i("Note", "onPause called");
        super.onPause();
        saveCurrentState();
    }

    @Override
    protected void onStop() {
        Log.i("Note", "onStop called");
        super.onStop();
        saveCurrentState();
    }

    private void saveCurrentState() {
        Log.i("Note", "saveCurrentState called");
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        int score = Integer.parseInt(scoreLabel.getText().toString());
        String currentRiderText = numberLabel.getText().toString();
        int rider = 0;
        if (!currentRiderText.equals("")) {
            rider = Integer.parseInt(numberLabel.getText().toString());
        }
        editor.putInt("ridingNumber", rider);
        editor.putInt("section", section);
        editor.putInt("score", score);
        editor.putInt("scoreCount", scoreCount);
        editor.apply();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i("Info", "onSaveInstanceState called");
        model.setRidingNumber(ridingNumber);
        model.setScore(score);
        model.setSection(section);
        model.setTrialid(trialid);
        model.setNumLaps(numlaps);
        model.setNumSections(numsections);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i("Info", "onRestoreInstanceState called");
        reloadFromModel();
    }

    private void reloadFromModel() {
        ridingNumber = model.getRidingNumber();
        score = model.getScore();
        section = model.getSection();
        trialid = model.getTrialid();
        numlaps = model.getNumlaps();
        numsections = model.getNumsections();
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
                goSync();
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

    private void reset() {
        mDbHelper.clearResults();
        trialDbHelper.clearTrials();
    }

    private void UISetup() {
        // Add custom ActionBar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getMenu();

        // Add score and numberPad fragemnts
        numberPadFragment = new NumberPadFragment();
        touchFragment = new TouchFragment();
        numberLabel = findViewById(R.id.numberLabel);
        scoreLabel = findViewById(R.id.scoreLabel);
        statusLine = findViewById(R.id.statusLine);
        sectionNumber = findViewById(R.id.sectionNumber);
        top = findViewById(R.id.top);
        incrementTextView = findViewById(R.id.incrementTextView);
        decrementTextView = findViewById(R.id.decrementTextView);
        sectionPicker = findViewById(R.id.sectionPicker);
        sectionDetail = findViewById(R.id.sectionDetail);
        sectionLabelLayout = findViewById(R.id.sectionLabelLayout);

        getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();
        getSupportFragmentManager().beginTransaction().replace(R.id.bottom, touchFragment).commit();
    }
    /*private void UISetupForTimer() {
        statusLine = findViewById(R.id.statusLine);
        saveButton.setText(R.string.startTimerButtonText);
       // top = findViewById(R.id.content);
       // riderNumberLabel = findViewById(R.id.riderNumberLabel);
        saveButton = findViewById(R.id.finishButton);

        // Set listeners
        saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                save(this);
                return false;
            }
        });

        saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startClock(this);
                return false;
            }

            // called when button pressed
            private void startClock(View.OnLongClickListener onLongClickListener) {
                Calendar startTime = Calendar.getInstance();
                //clockStartTime = startTime.getTimeInMillis();

                // Save start time in prefs
                SharedPreferences.Editor editor = localPrefs.edit();
             //   editor.putLong("clockStartTime", clockStartTime);
                editor.apply();
                SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
              //  String dateString = dateFormat.format(clockStartTime);
                statusLine.setText("Clock started at " + "dateString");
                top.setVisibility(View.VISIBLE);
               // startClockButton.setVisibility(View.GONE);
                statusLine.setVisibility(View.VISIBLE);
               // finishButton.setVisibility(View.VISIBLE);
                numberLabel.setText("");
            }
        });
    }*/


    public void countDabs(View view) {
        int intID = view.getId();
        Button button = view.findViewById(intID);
        String digit = button.getText().toString();

        switch (digit) {
            case "Clean":
                score = 0;
                break;
            case "10":
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
    }

    // Menu options
    private void goSync() {
        Intent intent = new Intent(this, ScoreListActivity.class);
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

    private void goTimer() {
//        Intent intent = new Intent(this, TimerActivity.class);
//        intent.putExtra(EXTRA_MESSAGE, message);
//        startActivity(intent);

        Log.i("Info", "goTimer() called");
        //UISetupForTimer();
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        observer = localPrefs.getString("observer", "");
        section = localPrefs.getInt("section", 1);
        trialid = localPrefs.getInt("trialid", 0);
        numlaps = localPrefs.getInt("numlaps", 1);
        numsections = localPrefs.getInt("numsections", 1);
        email = localPrefs.getString("email", "");
        isSingleUser = localPrefs.getBoolean("isSingleUser", false);
        ridingNumber = localPrefs.getInt("ridingNumber", 0);
        score = localPrefs.getInt("score", 0);
        numberInGroup = localPrefs.getInt("numberInGroup", 6);
        scoreCount = localPrefs.getInt("scoreCount", 0);
        theTrialName = localPrefs.getString("name", "None selected");
        club = localPrefs.getString("club", "None selected");
        trialHasChanged = localPrefs.getBoolean("", true);
        mode = localPrefs.getInt("mode", 0);
        usermode = Integer.valueOf(localPrefs.getString("usermode", "0"));
        timeMode = localPrefs.getBoolean("timeMode", false);
        // isOnline = localPrefs.getBoolean("isOnline", false);

        // Set up status line
        status = theTrialName + " - Observer: " + observer;
        sectionNumber.setText(valueOf(section));

        if (isSingleUser) {
            numberLabel.setText(valueOf(ridingNumber));
        }
        statusLine.setText(status);
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        model.setOnline(netInfo != null && netInfo.isConnectedOrConnecting());
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void save(View.OnLongClickListener view) {

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

            // Update prefs for single rider
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putInt("ridingNumber", riderNumber);
            editor.putString("riderText", rider);
            editor.apply();

            insertScore(riderNumber, scoreValue);
            scoreCount++;
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
            //   toneGen1.startTone(ToneGenerator.TONE_CDMA_CONFIRM, ToneGenerator.MAX_VOLUME);

            Log.i("Note", "trialid: " + trialid);
            // Confirm committed with sound
            playSoundFile(R.raw.ting);
            Toast.makeText(this, "Score saved", Toast.LENGTH_SHORT).show();
        }
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

    // Reset the  rider/score values
    // Patched for singleUserMode
    private void clearScore() {
        // Clear score label
        score = 0;
        scoreLabel.setText("0");

        // Clear rider number if not a single rider
        if (mode != 2) {
            numberLabel.setText("");
        }
        // If a single rider, then increment section
        else {
            SharedPreferences.Editor editor = localPrefs.edit();
            if (section < numsections) {
                section++;
            } else if (section == numsections) {
                section = 1;
            }
            sectionNumber.setText(valueOf(section));
            editor.putInt("section", section);
            editor.apply();
        }
    }

    public void increment(View v) {
        SharedPreferences.Editor editor = localPrefs.edit();
        if (section < numsections) {
            section++;
        } else if (section == numsections) {
            section = 1;
        }
        sectionNumber.setText(valueOf(section));
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
        sectionNumber.setText(valueOf(section));
        editor.putInt("section", section);
        editor.putString("sectionText", String.valueOf(section));
        editor.apply();
    }

    private void saveToCSV() {
        String id, observer, section, rider, lap, created, updated, edited, sync, score, thetrialid;

        try {
            File exportDir = new File(getFilesDir(), filename);
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

            String[] header = {"id", "rider", "section",
                    "lap", "score", "observer", "created", "updated", "edited", "trialid", "sync", email};

            csvWrite.writeNext(header, false);

            // Get current data

            Cursor curChild = mDbHelper.getAll(trialid);
            while (curChild.moveToNext()) {
                id = curChild.getString(0);
                observer = curChild.getString(1);
                section = curChild.getString(2);
                rider = curChild.getString(3);
                lap = curChild.getString(4);
                created = curChild.getString(5);
                updated = curChild.getString(6);
                edited = curChild.getString(7);
                thetrialid = curChild.getString(8);
                sync = curChild.getString(9);
                score = curChild.getString(10);

                String[] arrStr = {id, rider, section, lap, score, observer, created, updated, edited, thetrialid, sync
                };

                csvWrite.writeNext(arrStr, false);
            }
            Log.i("Info", "MainActivity: saveToCSV: 834");
            curChild.close();
            csvWrite.close();

        } catch (IOException e) {
            //  Log.e("Child", e.getMessage(), e);
        }
    }

    private void sendEmail() {
        boolean isOnline = isOnline();
        if (!isOnline) {
            Toast.makeText(MainActivity.this, "Email cannot be sent at this time - no Internet connection.",
                    Toast.LENGTH_LONG).show();
        } else {
            // Get timestamp and add to filename
            Date date = new Date();
            // getTime() returns current time in milliseconds
            long time = date.getTime();
            String ts = valueOf(time);
            filename = "scores_" + ts + ".csv";
            String sendMailURL = "https://www.trialmonster.uk/android/sendMailWithFile.php?id=" + ts + "&trialid=" + trialid + "&email=" + email;

            Log.i("Monitor", sendMailURL);
            processCSV(sendMailURL);
        }
    }

    private void processCSV(final String sendMailURL) {
        /*
         * Processing the CSV done online
         * so we need an AsyncTask
         * The constrains defined here are
         * Void -> We are not passing anything
         * Void -> Nothing at progress update as well
         * String -> After completion it should return a string and it will be the json string
         * */
        class ProcessCSV extends AsyncTask<Void, Void, String> {

            //this method will be called before execution
            //you can display a progress bar or something
            //so that user can understand that he should wait
            //as network operation may take some time
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = ProgressDialog.show(MainActivity.this, "Scoremonster",
                        "Processing scores… this make take some time!", true);
                // Prepare CSV file
                saveToCSV();
            }

            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();

                if (s.contentEquals("OK")) {
                    String toastMessage = "The email has been sent successfully to " + email;
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, toastMessage,
                            Toast.LENGTH_LONG).show());
                }
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {

                uploadFile(uploadFilePath + filename);
                try {
                    //creating a URL
                    URL url = new URL(sendMailURL);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    message = con.getResponseMessage();
                    return message;

                } catch (Exception e) {
                    return null;
                }
            }
        }
        ProcessCSV processCSV = new ProcessCSV();
        processCSV.execute();
    }

    public void uploadFile(String sourceFileUri) {
        File directory = getFilesDir();
        File sourceFile = new File(directory, filename);
        HttpURLConnection conn;
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;

        if (!sourceFile.isFile()) {
            dialog.dismiss();
            runOnUiThread(() -> {
            });

        } else {
            try {
                final String upLoadServerUri = "http://android.trialmonster.uk/UploadToServer.php";
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + sourceFileUri + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necessary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                if (serverResponseCode != 200) {

                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error processing data",
                            Toast.LENGTH_LONG).show());
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(() -> {
                    // messageText.setText("MalformedURLException Exception : check script url.");
                    Toast.makeText(MainActivity.this, "MalformedURLException",
                            Toast.LENGTH_SHORT).show();
                });

                //   Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(() -> {
                    // messageText.setText("Got Exception : see logcat ");
                    Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                            Toast.LENGTH_SHORT).show();
                });
                //   Log.e("Upload file Exception", "Exception : " + e.getMessage(), e);
            }
            dialog.dismiss();
        }
    }

    //play a soundfile
    public void playSoundFile(Integer fileName) {
        mediaPlayer = MediaPlayer.create(this, fileName);
        mediaPlayer.start();
    }
}