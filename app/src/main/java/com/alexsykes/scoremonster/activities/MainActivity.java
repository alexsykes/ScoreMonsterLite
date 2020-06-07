package com.alexsykes.scoremonster.activities;

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
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
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
import com.opencsv.CSVWriter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static androidx.core.content.FileProvider.getUriForFile;

// TODO Important - move database setup method from ScoreDbHelper

public class MainActivity extends AppCompatActivity {

    public static final int TEXT_REQUEST = 1;
    public static final int NOT_SYNCED = -1;

    String email;
    String subject;
    String message;
    String timestamp;
    Uri URI = null;

    TextView numberLabel, scoreLabel, statusLine, sectionNumber;
    String riderNumber, status, theTrialName;
    ScorePadFragment scorePadFragment;
    NumberPadFragment numberPadFragment;
    TouchFragment touchFragment;
    SharedPreferences localPrefs;
    ProgressDialog dialog = null;
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";
    final String uploadFileName = "scores.csv";

    // Databases
    private ScoreDbHelper mDbHelper;
    private FinishTimeDbHelper timeDbHelper;

    private String observer;
    private int section;
    private int trialid;
    private int numlaps;
    private int numsections;
    private int score;
    private boolean showDabPad;
    private boolean showNumberPad;
    int modeIndex;;
    int serverResponseCode = 0;
    private String filename;
    String upLoadServerUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*  Php script path  */
         upLoadServerUri = "http://android.trialmonster.uk/sendMailWithFile.php";

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
        sectionNumber = findViewById(R.id.sectionNumber);


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
            goSetup();
        }
    }

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline());
        editor.apply();


        clearScore();
        super.onStart();
        getPrefs();
        getSupportFragmentManager().beginTransaction().replace(R.id.bottom, touchFragment).commit();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the state of item position
        outState.putString("rider", numberLabel.getText().toString());
        outState.putString("score", scoreLabel.getText().toString());
        //   outState.putString("section", sectionNumber.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Read the state of item position
        numberLabel.setText(savedInstanceState.getString("rider"));
        scoreLabel.setText(savedInstanceState.getString("score"));
        // sectionNumber.setText(savedInstanceState.getString("section"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

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

            case R.id.timeMode:
                goTimingMode();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void sendEmail() {
        // Process data to produce CSV file
        Date date = new Date();
        // getTime() returns current time in milliseconds
        long time = date.getTime();
        timestamp = String.valueOf(time);
        filename = "scores_" + timestamp + ".csv";
        String processURL = "http://android.trialmonster.uk/sendMailWithFile.php?ts=";
        // Upload file
        processCSV(processURL);

        // Prepare and send email





 /*       try {

        File path = new File(Environment.getExternalStoragePublicDirectory(""),"data");
        File newFile = new File(path, "Scores.csv");
        Uri URI = FileProvider.getUriForFile(this, "com.alexsykes.scoremonster.fileprovider", newFile);


        subject = "Scores from " + theTrialName;
        message ="Attached";
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        String filename = "Scores.csv";
        File file  = new File(getFilesDir(), filename);

        // URI = Uri.fromFile(newFile);
        if (URI != null) {
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.putExtra(Intent.EXTRA_STREAM, URI);

        }
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
        //this.startActivity(emailIntent);
            this.startActivity(Intent.createChooser(emailIntent, "Sending email..."));

    } catch (Throwable t) {
        Toast.makeText(this, "Request failed try again: "+ t.toString(), Toast.LENGTH_LONG).show();
    } */
    }
    private void processCSV(final String urlWebService) {
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
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Score Update Complete",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {

                int response = uploadFile(uploadFilePath + filename);
                try {
                    //creating a URL
                   // URL url = new URL(urlWebService + ts);
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    String message = con.getResponseMessage();
                    return message;

                } catch (Exception e) {
                    return null;
                }
            }
        }
        ProcessCSV processCSV = new ProcessCSV();
        processCSV.execute();
    }
    public int uploadFile(String sourceFileUri) {
        File directory = getFilesDir();
        File sourceFile = new File(directory, filename);

        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        //File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            dialog.dismiss();
            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + "" + uploadFileName);

           /* runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"
                            + uploadFilePath + "" + uploadFileName);
                }
            }); */

            return 0;

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
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

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
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode != 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error processing data",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                     //   messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                      //  messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file Exception", "Exception : "
                        + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;
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
        //
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
        section = localPrefs.getInt("section", 1);
        trialid = localPrefs.getInt("trialid", 0);
        numlaps = localPrefs.getInt("numlaps", 0);
        numsections = localPrefs.getInt("numsections", 0);
        section = localPrefs.getInt("section", 1);
        email = localPrefs.getString("email", "");
        theTrialName = localPrefs.getString("theTrialName", "None selected");
        status = theTrialName + " - Observer: " + observer;
        sectionNumber.setText(String.valueOf(section));

        statusLine.setText(status);
        return trialid != 0;
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
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
        editor.apply();
    }

    public void increment(View view) {
        SharedPreferences.Editor editor = localPrefs.edit();
        if (section < numsections) {
            section++ ; }
        else if (section == numsections) {
            section = 1 ;
        }
        sectionNumber.setText(String.valueOf(section));
        editor.putInt("section", section);
        editor.apply();
    }

    public void decrement(View view) {
        SharedPreferences.Editor editor = localPrefs.edit();
        if (section > 1) {
            section--;
        } else if (section == 1) {
            section = numsections;
        }
        sectionNumber.setText(String.valueOf(section));
        editor.putInt("section", section);
        editor.apply();
    }

    private boolean saveToCSV() {
        // Get timestamp and add to filename

        Date date = new Date();
        // getTime() returns current time in milliseconds
        long time = date.getTime();
        String ts = String.valueOf(time);
        filename = "scores_" + ts + ".csv";
        String id, observer, section, rider, lap, created, updated, edited, sync, score, thetrialid;

        try {
            File exportDir = new File(getFilesDir(), filename);

            exportDir.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

            String[] header = {"id", "rider", "section",
                    "lap", "score", "observer", "created", "updated", "edited", "trialid", "sync"};

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
            return true;

        } catch (IOException e) {
            Log.e("Child", e.getMessage(), e);
            return false;
        }
    }
}