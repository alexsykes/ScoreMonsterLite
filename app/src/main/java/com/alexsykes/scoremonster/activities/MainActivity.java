package com.alexsykes.scoremonster.activities;

import static java.lang.String.join;
import static java.lang.String.valueOf;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.MainViewModel;
import com.alexsykes.scoremonster.NumberPadFragment;
import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.TouchFragment;
import com.alexsykes.scoremonster.data.ScoreContract;
import com.alexsykes.scoremonster.data.ScoreDbHelper;
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

// TODO Important - move database setup method from ScoreDbHelper
// TODO Important - add message to setup for no connection

public class MainActivity extends AppCompatActivity {

    public static final int TEXT_REQUEST = 1;
    public static final String EXTRA_MESSAGE = "com.alexsykes.scoremonster.activities.MESSAGE";
    public static final int NOT_SYNCED = -1;
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";
    MediaPlayer mediaPlayer;

    MainViewModel model;

    private String message, status, filename, observer, theTrialName, detail, email;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialData;
    private int score, scoreCount, serverResponseCode = 0, trialid, section, numsections, numlaps, ridingNumber, numberInGroup;
    private boolean isSingleUser, trialHasChanged;

    // Layout variables
    TextView numberLabel, scoreLabel, statusLine, sectionNumber;
    ConstraintLayout top;
    NumberPadFragment numberPadFragment;
    TouchFragment touchFragment;
    Button saveButton;

    SharedPreferences localPrefs;
    ProgressDialog dialog = null;

    // Databases
    private ScoreDbHelper mDbHelper;

    // URL constants
    private final String upLoadServerUri = "http://android.trialmonster.uk/sendMailWithFile.php";
    private final String sendMailURL = "http://android.trialmonster.uk/sendMailWithFile.php";
    private static final String BASE_URL = "https://android.trialmonster.uk/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Note", "MainActivity::onCreate called");
        super.onCreate(savedInstanceState);

        model = new ViewModelProvider(this).get(MainViewModel.class);

        setContentView(R.layout.activity_main);

        // Create database connection
        dbInit();

        UISetup();

        // if online, loads list of trials
        if (isOnline()) {
            // Get trialList from server
            String URL = BASE_URL + "getTrialList.php";
            try {
                getJSONDataset(URL);
            } catch (NullPointerException e) {
                Toast.makeText(MainActivity.this, "Empty data", Toast.LENGTH_LONG).show();
            }
        }

        // Set up button to save scores
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                save(this);
                return false;
            }
        });

        getPrefs();
    }

    private void UISetup() {
        // Add custom ActionBar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(Color.WHITE);
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

        getSupportFragmentManager().beginTransaction().add(R.id.top, numberPadFragment).commit();
        getSupportFragmentManager().beginTransaction().replace(R.id.bottom, touchFragment).commit();
    }

    // Databaise initialisation
    private void dbInit() {
        // Database operations - https://www.tutorialspoint.com/android/android_sqlite_database.htm
        // First, get your database
        final String DATABASE_NAME = "monster.db";
        SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);

        // Create a String that contains the SQL statement to create the scores table
        String SQL_CREATE_SCORES_TABLE = "CREATE TABLE IF NOT EXISTS " + ScoreContract.ScoreEntry.TABLE_NAME + " ("
                + ScoreContract.ScoreEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_OBSERVER + " TEXT NOT NULL, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION + " INTEGER NOT NULL, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER + " INTEGER NOT NULL, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_LAP + " INTEGER NOT NULL DEFAULT 0, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_UPDATED + " TEXT , "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_EDITED + " INTEGER NOT NULL DEFAULT 0, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_TRIALID + " INTEGER NOT NULL DEFAULT 0, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC + " INTEGER NOT NULL DEFAULT 1, "
                + ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE + " INTEGER NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_SCORES_TABLE);

        mDbHelper = new ScoreDbHelper(this);
        mDbHelper.getWritableDatabase();
    }

    @Override
    protected void onStart() {
        Log.i("Note", "onStart called");
        // Check network connectivity and set Prefs
        // localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline());
        editor.apply();

        super.onStart();

        if (!isSingleUser) {
            numberLabel.setText("");
            top.setVisibility(View.VISIBLE);
        } else {
            top.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        Log.i("Note", "onPause called");
        super.onPause();
        saveCurrentState();
    }

    private void saveCurrentState() {

        //  Log.i("Note", "saveCurrentState called");
        // localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        int score = Integer.parseInt(scoreLabel.getText().toString());
        String currentRiderText = numberLabel.getText().toString();
        if (!currentRiderText.equals("")) {
            int rider = Integer.parseInt(numberLabel.getText().toString());
            editor.putInt("ridingNumber", rider);
        } else {
            editor.putInt("ridingNumber", 0);
        }
        editor.putInt("section", section);
        editor.putInt("score", score);
        editor.putInt("scoreCount", scoreCount);
        editor.apply();

        // Log.i("Note", "Current rider: " + currentRiderText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Note", "onResume called");
        getPrefs();

        Log.i("Note","trialHasChanged is: " + trialHasChanged);
        scoreLabel.setText(valueOf(score));
        sectionNumber.setText(valueOf(section));
        if (ridingNumber != 0) {
            numberLabel.setText(valueOf(ridingNumber));
        } else {
            numberLabel.setText("");
        }

        if(trialHasChanged) {
            // Get trialList from server
            String URL = BASE_URL + "getTrialDetailsScoreMonster.php?id=" + trialid;
            try {
                getTrialDetails(URL);
            } catch (NullPointerException e) {
                Toast.makeText(MainActivity.this, "Empty data", Toast.LENGTH_LONG).show();
            }

            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putBoolean("trialHasChanged", false);
            editor.apply();
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.i("Note", "onSaveInstanceState called");
        // Save the state of item position
//        outState.putString("rider", numberLabel.getText().toString());
//        outState.putString("score", scoreLabel.getText().toString());
//        outState.putInt("section", section);
//        outState.putInt("numberInGroup", numberInGroup);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {

        Log.i("Note", "onRestoreInstanceState called");
        // Read the state of item position
//        numberLabel.setText(savedInstanceState.getString("rider"));
//        scoreLabel.setText(savedInstanceState.getString("score"));
//        section = savedInstanceState.getInt("section");
//        sectionNumber.setText(valueOf(section));
//        numberInGroup = savedInstanceState.getInt("numberInGroup");
//        super.onRestoreInstanceState(savedInstanceState);
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
            case R.id.upload:
                goSync();
                return true;


            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void goHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivityForResult(intent, TEXT_REQUEST);
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
            String sendMailURL = "http://www.trialmonster.uk/android/sendMailWithFile.php?id=" + ts + "&trialid=" + trialid + "&email=" + email;

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

                if (s.contentEquals("OK")){
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "The email has been sent successfully.",
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
        scoreLabel.setText(valueOf(score));
    }

    private void goSync() {
        Intent intent = new Intent(this, SyncActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void goSetup() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        // getPrefs();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
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
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else vibrator.vibrate(500);
            new AlertDialog.Builder(this).setTitle("Warning").setMessage("Missing rider number or score").setNeutralButton("Close", null).show();
        } else {
            // Otherwise enter scores
            int riderNumber = Integer.parseInt(rider);
            int scoreValue = Integer.parseInt(score);

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

            playSoundFile(R.raw.ting);
            Toast.makeText(this, "Score saved", Toast.LENGTH_SHORT).show();
        }
    }

    // Reset the  rider/score values
    // Patched for singleUserMode
    private void clearScore() {
        // Clear score label
        score = 0;
        scoreLabel.setText("0");

        // Clear rider number if not a single rider
        if (!isSingleUser) {
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

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        observer = localPrefs.getString("observer", "");
        // String sectionText  = localPrefs.getString("sectionText", "1");
        section = Integer.valueOf(localPrefs.getString("sectionText", "1"));
        trialid = Integer.valueOf(localPrefs.getString("thetrialid", "1"));
        numlaps = Integer.valueOf(localPrefs.getString("numlapsText", "1"));
        numsections = Integer.valueOf(localPrefs.getString("numsectionsText", "1"));
        email = localPrefs.getString("email", "");
        isSingleUser = localPrefs.getBoolean("isSingleUser", false);
        ridingNumber = localPrefs.getInt("ridingNumber", 0);
        score = localPrefs.getInt("score", 0);
        numberInGroup = localPrefs.getInt("numberInGroup", 6);
        scoreCount = localPrefs.getInt("scoreCount", 0);
        theTrialName = localPrefs.getString("theTrialName", "None selected");
        trialHasChanged = localPrefs.getBoolean("trialHasChanged", true);


        status = theTrialName + " - Observer: " + observer;
        sectionNumber.setText(valueOf(section));

//        model.setNumLaps(numlaps);
//        model.setNumSections(numsections);
//        model.setTrialid(trialid);

        if (isSingleUser) {
            numberLabel.setText(valueOf(ridingNumber));
        }
        statusLine.setText(status);
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
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
            csvWrite.close();

        } catch (IOException e) {
            //  Log.e("Child", e.getMessage(), e);
        }
    }

    //play a soundfile
    public void playSoundFile(Integer fileName) {
        mediaPlayer = MediaPlayer.create(this, fileName);
        mediaPlayer.start();
    }


    private void getJSONDataset(final String urlWebService) {
        /*
         * As fetching the json string is a network operation
         * And we cannot perform a network operation in main thread
         * so we need an AsyncTask
         * The constrains defined here are
         * Void -> We are not passing anything
         * Void -> Nothing at progress update as well
         * String -> After completion it should return a string and it will be the json string
         * */
        class GetData extends AsyncTask<Void, Void, String> {

            //this method will be called before execution

            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                // Show dialog during server transaction
                // dialog = ProgressDialog.show(SetupActivity.this, "Scoremonster", "Getting trial list", true);
//                dialog = new ProgressDialog(MainActivity.this);
//                dialog.setMessage("Loading…");
//                dialog.setCancelable(false);
//                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> dialog.dismiss());
//                dialog.show();
            }


            /* this method will be called after execution

                s contains trial details in JSON string
             */

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //  dialog.dismiss();

                // Populate ArrayList with JSON data
                theTrialData = populateResultArrayList(s);

                int size = theTrialData.size();
                theTrials = new String[size];
                theIDs = new String[size];
                String id;

                for (int index = 0; index < theTrialData.size(); index++) {
                    theTrialName= theTrialData.get(index).get("name");
                    id = theTrialData.get(index).get("id");
                    theTrials[index] = theTrialName;
                    theIDs[index] = id;
                }

                setTrialsList(theTrialData);

                if (trialid == 0) {
                    theTrialName = "Manual Entry";
                }
            }

            /*
            @param String json JSON string returned from MySQL
            @return ArrayList of trials data
             */
            private ArrayList<HashMap<String, String>> populateResultArrayList(String json) {
                ArrayList<HashMap<String, String>> theTrialList = new ArrayList<>();
                String date, name, id, club, numsections, numlaps, starttime, email, scoringmode;

                try {
                    // Parse string data into JSON
                    JSONArray jsonArray = new JSONArray(json);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        HashMap<String, String> theTrial = new HashMap<>();
                        id = jsonArray.getJSONObject(index).getString("id");
                        date = jsonArray.getJSONObject(index).getString("date");
                        club = jsonArray.getJSONObject(index).getString("club");
                        name = jsonArray.getJSONObject(index).getString("name");
                        numsections = jsonArray.getJSONObject(index).getString("numsections");
                        numlaps = jsonArray.getJSONObject(index).getString("numlaps");
                        starttime = jsonArray.getJSONObject(index).getString("starttime");
                        email = jsonArray.getJSONObject(index).getString("email");
                        scoringmode = jsonArray.getJSONObject(index).getString("scoringmode");

                        // trial = club + " - " + name;
                        theTrial.put("id", id);
                        theTrial.put("date", date);
                        theTrial.put("club", club);
                        theTrial.put("name", name);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("starttime", starttime);
                        theTrial.put("email", email);
                        theTrial.put("scoringmode", scoringmode);
                        theTrialList.add(theTrial);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return theTrialList;
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {
                int TIMEOUT_VALUE = 1000;
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(TIMEOUT_VALUE);
                    con.setReadTimeout(TIMEOUT_VALUE);
                    //StringBuilder object to read the string from the service
                    StringBuilder sb = new StringBuilder();

                    //We will use a buffered reader to read the string from service
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    //A simple string to read values from each line
                    String json;

                    //reading until we don't find null
                    while ((json = bufferedReader.readLine()) != null) {
                        json = json + "\n";
                        //appending it to string builder
                        sb.append(json);
                    }

                    //finally returning the read string
                    return sb.toString().trim();
                } catch (SocketTimeoutException e) {
                    Toast.makeText(MainActivity.this, "SocketTimeoutException", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        //creating asynctask object and executing it
        GetData getJSON = new GetData();
        getJSON.execute();
    }

    private void getTrialDetails(final String urlWebService) {
        /*
         * As fetching the json string is a network operation
         * And we cannot perform a network operation in main thread
         * so we need an AsyncTask
         * The constrains defined here are
         * Void -> We are not passing anything
         * Void -> Nothing at progress update as well
         * String -> After completion it should return a string and it will be the json string
         * */
        class GetData extends AsyncTask<Void, Void, String> {

            //this method will be called before execution

            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                // Show dialog during server transaction
                // dialog = ProgressDialog.show(SetupActivity.this, "Scoremonster", "Getting trial list", true);
//                dialog = new ProgressDialog(MainActivity.this);
//                dialog.setMessage("Loading…");
//                dialog.setCancelable(false);
//                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> dialog.dismiss());
//                dialog.show();
            }


            /* this method will be called after execution

                s contains trial details in JSON string
             */

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //  dialog.dismiss();

                // Populate ArrayList with JSON data
                theTrialData = readTrialData(s);


                theTrialName = theTrialData.get(0).get("name");
                trialid = Integer.parseInt(theTrialData.get(0).get("id"));
                numlaps = Integer.valueOf(theTrialData.get(0).get("numlaps"));
                numsections  = Integer.valueOf(theTrialData.get(0).get("numsections"));


                //setTrialsList(theTrialData);

                if (trialid == 0) {
                    theTrialName = "Manual Entry";
                }
            }

            private ArrayList<HashMap<String, String>> readTrialData(String json) {

                ArrayList<HashMap<String, String>> theTrialData = new ArrayList<>();
                String id, numsections, numlaps, name;

                try {
                    // Parse string data into JSON
                    JSONArray jsonArray = new JSONArray(json);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        HashMap<String, String> theTrial = new HashMap<>();
                        id = jsonArray.getJSONObject(index).getString("id");
                        numsections = jsonArray.getJSONObject(index).getString("numsections");
                        numlaps = jsonArray.getJSONObject(index).getString("numlaps");
                        name = jsonArray.getJSONObject(index).getString("name");

                        theTrial.put("id", id);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("name", name);
                        theTrialData.add(theTrial);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return theTrialData;
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {
                int TIMEOUT_VALUE = 1000;
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(TIMEOUT_VALUE);
                    con.setReadTimeout(TIMEOUT_VALUE);
                    //StringBuilder object to read the string from the service
                    StringBuilder sb = new StringBuilder();

                    //We will use a buffered reader to read the string from service
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    //A simple string to read values from each line
                    String json;

                    //reading until we don't find null
                    while ((json = bufferedReader.readLine()) != null) {
                        json = json + "\n";
                        //appending it to string builder
                        sb.append(json);
                    }

                    //finally returning the read string
                    return sb.toString().trim();
                } catch (SocketTimeoutException e) {
                    Toast.makeText(MainActivity.this, "SocketTimeoutException", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        //creating asynctask object and executing it
        GetData getJSON = new GetData();
        getJSON.execute();
    }

    private void setTrialsList(ArrayList<HashMap<String, String>> theTrialList) {
        SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Need name and id from theTrialList
        int size = theTrialList.size();
        String[] theTrialNames = new String[size];
        String[] theTrialIds = new String[size];
        for (int index = 0; index < size; index++) {
            theTrialNames[index] = theTrialList.get(index).get("name");
            theTrialIds[index] =  theTrialList.get(index).get("id");
        }
        String theTrialListNames = join(",", theTrialNames);
        String theTrialListIds = join(",", theTrialIds);

        SharedPreferences.Editor editor = localPrefs.edit();

        editor.putString("theNames", theTrialListNames);
        editor.putString("theIds", theTrialListIds);
        editor.apply();
    }
}