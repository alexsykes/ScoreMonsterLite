package com.alexsykes.scoremonster.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.ScoreListSyncAdapter;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class SyncActivity extends AppCompatActivity {
    public static final int TEXT_REQUEST = 1;

    /**********  File Path *************/
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";
    final String uploadFileName = "scores.csv";
    // https://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83
    RecyclerView scoreView;
    ArrayList<HashMap<String, String>> theScoreList;
    TextView messageText;
    Button uploadButton, processButton;
    int serverResponseCode = 0, section, trialid;
    ProgressDialog dialog = null;
    String upLoadServerUri = null;
    String processURL = null;
    File datafile;
    File exportDir = new File(Environment.getExternalStoragePublicDirectory("Documents/Scoremonster"), "");
    private ScoreDbHelper mDbHelper;
    private String filename;

    SharedPreferences localPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        // Get shared preferences for trialid, section
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);

        section = localPrefs.getInt("section", 0);
        trialid = localPrefs.getInt("trialid", 0);

        // Create database connection
        mDbHelper = new ScoreDbHelper(this);
        populateScoreList();

        // uploadButton = findViewById(R.id.uploadButton);
        processButton = findViewById(R.id.processButton);

        /*  Php script path  */
        upLoadServerUri = "http://www.trialmonster.uk/android/UploadToServer.php";
        processURL = "http://www.trialmonster.uk/android/addCSVtodb.php";

       /* uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog = ProgressDialog.show(SyncActivity.this, "Scoremonster", "Uploading scores...", true);

                new Thread(new Runnable() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                            }
                        });

                        uploadFile(uploadFilePath + "" + uploadFileName);

                    }
                }).start();
            }
        }); */

        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCSV(processURL);
            }
        });
    }

    public void onClickCalled(String scoreid, int score) {
        alertSingleChoiceItems(scoreid, score);
    }

    public void alertSingleChoiceItems(final String scoreid, final int score) {


        AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this);

        // Set the dialog title
        builder.setTitle("Change score to:")

                // specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive call backs when items are selected
                // again, R.array.choices were set in the resources res/values/strings.xml
                .setSingleChoiceItems(R.array.scores, score, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //  showToast("Some actions maybe? Selected index: " + arg1);
                        //  Toast.makeText(SyncActivity.this, "Some actions maybe? Selected index ", Toast.LENGTH_LONG).show();
                    }

                })

                // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // user clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog

                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        String score = null;

                        switch (selectedPosition) {
                            case 0:
                                score = "0";

                                break;
                            case 1:
                                score = "1";

                                break;
                            case 2:
                                score = "2";

                                break;
                            case 3:
                                score = "3";

                                break;
                            case 4:
                                score = "5";

                                break;
                            case 5:
                                score = "10";

                                break;
                        }
                        mDbHelper.update(scoreid, score);
                        populateScoreList();

                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // removes the dialog from the screen

                    }
                })

                .show();

    }

    private void populateScoreList() {
        theScoreList = mDbHelper.getScoreList(trialid, section);
        scoreView = findViewById(R.id.scoreView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        scoreView.setLayoutManager(llm);
        scoreView.setHasFixedSize(true);

        initializeAdapter();
    }

    private void initializeAdapter() {
        ScoreListSyncAdapter adapter = new ScoreListSyncAdapter(theScoreList);
        scoreView.setAdapter(adapter);
    }

    private boolean saveToCSV() {
        String id, observer, section, rider, lap, created, updated, edited, sync, score, thetrialid;

        // Get timestamp and add to filename

        Date date = new Date();
        //getTime() returns current time in milliseconds
        long time = date.getTime();
        String ts = String.valueOf(time);
        filename = "scores_" + ts + ".csv";
        filename = "scores.csv";

        try {
            exportDir = new File(getFilesDir(), filename);

            exportDir.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

            String[] header = {"id", "section", "rider",
                    "lap", "score", "observer", "created", "updated", "edited", "trialid",
                    "sync"};

            csvWrite.writeNext(header, false);

            // Get current data

            Cursor curChild = mDbHelper.getUnSynced(trialid);
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

                String[] arrStr = {id, section, rider, lap, score, observer, created, updated, edited, thetrialid, sync
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
                dialog = ProgressDialog.show(SyncActivity.this, "Scoremonster",
                        "Processing scores… this make take some time!", true);
                // Prepare CSV file
                saveToCSV();
            }

            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();
                mDbHelper.markAsDone(trialid);
                populateScoreList();
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {

                int response = uploadFile(uploadFilePath + uploadFileName);
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    return con.getResponseMessage();

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

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"
                            + uploadFilePath + "" + uploadFileName);
                }
            });

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

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(SyncActivity.this, "Score Upload Complete",
                                    Toast.LENGTH_SHORT).show();
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
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(SyncActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(SyncActivity.this, "Got Exception : see logcat ",
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
}