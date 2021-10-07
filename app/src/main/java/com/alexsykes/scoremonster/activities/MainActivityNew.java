package com.alexsykes.scoremonster.activities;

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
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.MainViewModel;
import com.alexsykes.scoremonster.NumberPadFragment;
import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.TouchFragment;
import com.alexsykes.scoremonster.data.ScoreContract;
import com.alexsykes.scoremonster.data.ScoreDbHelper;
import com.alexsykes.scoremonster.data.TimeContract;
import com.alexsykes.scoremonster.data.TimeDbHelper;
import com.alexsykes.scoremonster.data.TrialDbHelper;
import com.opencsv.CSVWriter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class MainActivityNew extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    public static final int TEXT_REQUEST = 1;
    public static final int NOT_SYNCED = -1;
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";
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
            incrementTextView, sectionDetail, statusLine;
    LinearLayout sectionPicker, sectionLabelLayout;
    ConstraintLayout top, bottom;
    // Utility
    ProgressDialog dialog = null;
    MediaPlayer mediaPlayer;
    // Databases
    private ScoreDbHelper scoreDbHelper;
    private TimeDbHelper timeDbHelper;
    private TrialDbHelper trialDbHelper;

    // Variables
    long clockStartTime, startInterval, penaltyTariff;
    private String status, filename, observer, theTrialName, detail, email, club, message;
    private int score, scoreCount, serverResponseCode = 0, usermode, section, numsections, numlaps, numberInGroup;
    private boolean isSingleUser, trialHasChanged, isOnline, timeMode;
    private int ridingNumber, trialid, mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Info", "MainACtivityNew onCreate: called");
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
            case R.id.email:
                // goShowScoresFromServer();
                // goShowSummaryScores();
                sendEmail();
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

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("Info", "MainActivityNew:onStart called");
        getPrefs();
        initialUISetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        startInterval = localPrefs.getLong("startInterval", 60);
        penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
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
    }

    void initialUISetup() {
        // Initialise UI fields
        numberLabel = findViewById(R.id.numberLabel);
        scoreLabel = findViewById(R.id.scoreLabel);
        statusLine = findViewById(R.id.statusLine);
        sectionLabelLayout = findViewById(R.id.sectionLabelLayout);
        sectionNumberTextView = findViewById(R.id.sectionNumber);
        sectionNumberTextView.setText(valueOf(section));
        top = findViewById(R.id.top);
        incrementTextView = findViewById(R.id.incrementTextView);
        decrementTextView = findViewById(R.id.decrementTextView);
        sectionDetail = findViewById(R.id.sectionDetail);
        saveButton = findViewById(R.id.saveButton);

        // Set initial values
        if (timeMode) {
            statusLine.setText("Time mode");
            sectionLabelLayout.setVisibility(View.GONE);
            scoreLabel.setVisibility(View.INVISIBLE);
            if (clockStartTime > 0) {
                saveButton.setText("Enter");
                saveButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        saveTime(this);
                        return false;
                    }
                });
            } else {
                saveButton.setText("Start clock");
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
                        statusLine.setText("Clock started at " + dateString);
                        saveButton.setText("Enter");
                        numberLabel.setText("");
                    }
                });
            }

        } else {
            sectionLabelLayout.setVisibility(View.GONE);
            statusLine.setText("Scoring mode");
            saveButton.setText("Save");
            scoreLabel.setVisibility(View.VISIBLE);

            if (touchFragment == null && !timeMode) {
                touchFragment = new TouchFragment();
            }
            getSupportFragmentManager().beginTransaction().add(R.id.bottom, touchFragment).commit();
        }

        if (numberPadFragment == null) {
            numberPadFragment = new NumberPadFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();
        }
//        if (touchFragment == null && !timeMode) {
//            touchFragment = new TouchFragment();
//            getSupportFragmentManager().beginTransaction().add(R.id.bottom, touchFragment).commit();
//        }
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

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        model.setOnline(netInfo != null && netInfo.isConnectedOrConnecting());
        return netInfo != null && netInfo.isConnectedOrConnecting();
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

    private void sendEmail() {
        isOnline = isOnline();
        if (!isOnline) {
            Toast.makeText(MainActivityNew.this, "Email cannot be sent at this time - no Internet connection.",
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
            // processCSV(sendMailURL);
        }
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

            Cursor curChild = scoreDbHelper.getAll(trialid);
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
                dialog = ProgressDialog.show(MainActivityNew.this, "Scoremonster",
                        "Processing scores… this make take some time!", true);
                // Prepare CSV file
                saveToCSV();
            }

            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();

                if (s.contentEquals("OK")) {
                    String toastMessage = "The email has been sent successfully to " + email;
                    runOnUiThread(() -> Toast.makeText(MainActivityNew.this, toastMessage,
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

                    runOnUiThread(() -> Toast.makeText(MainActivityNew.this, "Error processing data",
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
                    Toast.makeText(MainActivityNew.this, "MalformedURLException",
                            Toast.LENGTH_SHORT).show();
                });

                //   Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(() -> {
                    // messageText.setText("Got Exception : see logcat ");
                    Toast.makeText(MainActivityNew.this, "Got Exception : see logcat ",
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