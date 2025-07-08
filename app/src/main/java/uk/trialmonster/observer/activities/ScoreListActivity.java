package uk.trialmonster.observer.activities;
// see - https://stuff.mit.edu/afs/sipb/project/android/docs/training/basics/data-storage/files.html

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.json.JSONArray;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import uk.trialmonster.observer.R;
import uk.trialmonster.observer.ScoreListAdapter;
import uk.trialmonster.observer.data.ScoreDbHelper;

public class ScoreListActivity extends AppCompatActivity {
    public static final int PICKFILE_RESULT_CODE = 1;
    private static final String TAG = "Info";
    private static final String DEBUG_TAG = "Info";
//    String TAG = "Info";
    /**********  File Path *************/
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";
    File exportDir = new File(Environment.getExternalStoragePublicDirectory("Documents/Scoremonster"), "");

    MenuItem emailMenuItem;
    MenuItem uploadMenuItem;
    private static final String[] END_OF_MARKERS = new String[]{"End of markers"};

    // https://androidexample.com/Upload_File_To_Server_-_Android_Example/index.php?view=article_discription&aid=83
    RecyclerView scoreView;
    ArrayList<HashMap<String, String>> theScoreList;
    ArrayList<HashMap<String, String>> theManualScoreList;
    TextView messageText;
    boolean canConnect;
    SharedPreferences localPrefs;
    private static final String[] END_OF_FILE = new String[]{"End of file"};
    private ScoreDbHelper scoreDbHelper;
    private String filename, email, observer, mobile;
    private boolean isLoggedInUser;
    MenuItem saveToDownloadsItem;
    int serverResponseCode = 0, section, trialid, day, numlaps, numsections;
    CSVWriter csvWriter;
    CSVReader csvReader;
    private Uri fileUri;
    private String filePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_score_list);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_score_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Add this:
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        canConnect = canConnect();
        // Get shared preferences for trialid, section
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        numlaps = localPrefs.getInt("numlaps", 1);
        numsections = localPrefs.getInt("numsections", 1);
        section = localPrefs.getInt("section", 1);
        day = localPrefs.getInt("dayNum", 1);
        trialid = localPrefs.getInt("trialid", -999);
        observer = localPrefs.getString("observer", "");
        mobile = localPrefs.getString("mobile", "");
        isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
        populateScoreList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        theScoreList.clear();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);

        emailMenuItem = menu.findItem(R.id.email);
        uploadMenuItem = menu.findItem(R.id.upload);
        saveToDownloadsItem = menu.findItem(R.id.saveAsFileButton);

        if (trialid == -999 || !isLoggedInUser) {
            uploadMenuItem.setEnabled(false);
            uploadMenuItem.setVisible(false);
        } else {
            uploadMenuItem.setEnabled(true);
            uploadMenuItem.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.email:
                if (canConnect()) {
                    emailScores();
                } else {
                    Toast.makeText(ScoreListActivity.this, "No Internet connection. Please try again later",
                            Toast.LENGTH_LONG).show();
                }

                return true;

            case R.id.upload:
                if (canConnect()) {
                    uploadScores();
                } else {
                    Toast.makeText(ScoreListActivity.this, "No Internet connection. Please try again later",
                            Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.saveAsFileButton:
                String filename = "Section " + section + " scores.txt";
//                Log.i(TAG, "onOptionsItemSelected: saveAsFileButtonClicked");
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("text/plain");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                startActivityForResult(intent, PICKFILE_RESULT_CODE);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private File writeToInternal(File exportDir, String filename) {
        Cursor exportData = scoreDbHelper.getScoresForEmail(trialid);
        try {
            exportDir = new File(exportDir, filename);
            exportDir.createNewFile();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(exportDir));

            String[] arrStr1 = {"Hello"};
            csvWriter.writeNext(arrStr1);
            // Get current data
            while (exportData.moveToNext()) {
                String[] arrStr = new String[exportData.getColumnCount()];
                for (int i = 0; i < exportData.getColumnCount(); i++)
                    arrStr[i] = exportData.getString(i);
                csvWriter.writeNext(arrStr);
            }

            csvWriter.close();
        } catch (IOException e) {
            Log.e("Child", e.getMessage(), e);
        }
        return exportDir;
    }

    private void saveToDownloads() {
        Log.i(TAG, "saveToDownloads");
        Log.i(TAG, "numlaps: " + numlaps);
        Log.i(TAG, "numsections: " + numsections);
        Log.i(TAG, "section: " + section);
        Log.i(TAG, "trialid: " + trialid);
        boolean success;
        File exportDir = getFilesDir();
        String filename = "Section " + section + " scores.dat";
        File exportFile = writeToInternal(exportDir, filename);

//        Log.i(TAG, "scores: " + theScoreList.size());
//        Log.i(TAG, "numlaps" + numlaps);
//        Log.i(TAG, "numlaps" + numlaps);
    }

    public void onClickCalled(String scoreid, int score) {
        amendScore(scoreid, score);
    }

    protected boolean canConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // Method to update scores for display
    // Responds to click on score line
    public void amendScore(final String scoreid, final int score) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ScoreListActivity.this);

        // Set the dialog title
        builder.setTitle("Change score to:")
                // specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive call backs when items are selected
                // again, R.array.choices were set in the resources res/values/strings.xml
                .setSingleChoiceItems(R.array.scores, score, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //  Toast.makeText(SyncActivity.this, "Some actions maybe? Selected index ", Toast.LENGTH_LONG).show();
                    }
                })

                // Set the action buttons
                .setPositiveButton("OK", (dialog, id) -> {
                    // user clicked OK, so save the mSelectedItems results somewhere
                    // or return them to the component that opened the dialog

                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    String score1 = null;

                    switch (selectedPosition) {
                        case 0:
                            score1 = "0";

                            break;
                        case 1:
                            score1 = "1";
                            break;
                        case 2:
                            score1 = "2";
                            break;
                        case 3:
                            score1 = "3";
                            break;
                        case 4:
                            score1 = "5";
                            break;
                        case 5:
                            score1 = "x";
                            break;
                        case 6:
                            deleteScore(scoreid);
                            return;
                    }
                    scoreDbHelper = new ScoreDbHelper(ScoreListActivity.this);
                    scoreDbHelper.update(scoreid, score1, observer);
                    scoreDbHelper.close();
                    populateScoreList();
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    // removes the dialog from the screen
                })
                .show();
    }

    private void deleteScore(String id) {
        scoreDbHelper.deleteScore(id, numlaps, observer);
        populateScoreList();
    }

    private void populateScoreList() {
        scoreDbHelper = new ScoreDbHelper(this);
        Log.i("Info", "IsManual");
        boolean isManualTrial = localPrefs.getBoolean("isManualTrial", true);
//        if(isManualTrial) {
//            theScoreList = scoreDbHelper.getManualLapScores(trialid, day, section);
//
//        } else {
            theScoreList = scoreDbHelper.getScoreList(trialid, day, section);
//        } Log.i("trialid", "" + trialid);
        scoreView = findViewById(R.id.scoreView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        scoreView.setLayoutManager(llm);
        scoreView.setHasFixedSize(true);
        initializeAdapter();
        scoreDbHelper.close();
    }

    private void initializeAdapter() {
        ScoreListAdapter adapter = new ScoreListAdapter(theScoreList);
        scoreView.setAdapter(adapter);
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
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
////                dialog = ProgressDialog.show(ScoreListActivity.this, "Scoremonster",
////                        "Processing scores… this make take some time!", true);
//                // Prepare CSV file
//                // saveToCSV();
//                // CSV file is now saved on local storage
//            }

            protected void onPostExecute(String s) {
                super.onPostExecute(s);
//                dialog.dismiss();

                if (s.contentEquals("OK")) {
//                    mDbHelper.markAsDone(trialid);
                    //   markAsDone(trialid);
                    populateScoreList();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(ScoreListActivity.this, "Email sent to " + email, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            protected String doInBackground(Void... voids) {

                // First upload the file
                int response = uploadFile(uploadFilePath + filename);
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

    private void markAsDone(int trialid) {
        scoreDbHelper = new ScoreDbHelper(this);
        scoreDbHelper.markAsDone(trialid);
        scoreDbHelper.close();
        populateScoreList();
    }

    // Save current scores to CSV
    private boolean saveToCSV() {
        String id, observer, section, rider, lap, created, updated, edited, sync, score, thetrialid;

        try {
            exportDir = new File(getFilesDir(), filename);

            // Create new CSV file in storage
            exportDir.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

//            Prepare and write filednames as header
            String[] header = {"id", "Rider", "Section",
                    "Lap", "Score", "Observer", "trialID", "Timestamp - GMT", "Updated - GMT"};
            csvWrite.writeNext(header, false);

            // Get score data for current trial
            Cursor curChild = scoreDbHelper.getAll(trialid);
            while (curChild.moveToNext()) {
                id = curChild.getString(0);
                observer = curChild.getString(1);
                section = curChild.getString(2);
                rider = curChild.getString(3);
                lap = curChild.getString(4);
                created = curChild.getString(5);
                updated = curChild.getString(6);
//                edited = curChild.getString(7);
                thetrialid = curChild.getString(8);
//                sync = curChild.getString(9);
                score = curChild.getString(10);

                String[] arrStr = {id, rider, section, lap, score, observer, thetrialid, created,
                        updated
                };

                csvWrite.writeNext(arrStr, false);
            }
            // Close filewriter
            curChild.close();
            csvWrite.close();
            scoreDbHelper.close();
            return true;

        } catch (IOException e) {
            Log.e("Child", e.getMessage(), e);
            return false;
        }
    }    // Save current scores to CSV

    private boolean saveSectionScoresToCSV(String section) {
        String rider, scores, day;
        filename = "Section " + section + " scores.csv";
        try {
            exportDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "TMData.csv");
            File file = new File(filename);

//            if (!exportDir.mkdirs()) {
//                Log.e(TAG, "Directory not created");
//            }

            // Create new CSV file in storage
            exportDir.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

//            Prepare and write filednames as header
            String[] details = {"Observer: ", observer, mobile};
            String[] header = {"Day", "Section", "Rider", "Scores"};
            csvWrite.writeNext(details, false);
            csvWrite.writeNext(header, false);

            // Get score data for current trial
            Cursor curChild = scoreDbHelper.getScoresForEmail(trialid);
            while (curChild.moveToNext()) {
//                section = curChild.getString(1);
                rider = curChild.getString(0);
                scores = curChild.getString(2);
                day = curChild.getString(3);
//                observer = curChild.getString(3);
                String[] arrStr = {day, section, rider, scores
                };

                csvWrite.writeNext(arrStr, false);
            }
            // Close filewriter
            curChild.close();
            csvWrite.close();
            scoreDbHelper.close();
            return true;

        } catch (IOException e) {
            Log.e("Child", e.getMessage(), e);
            return false;
        }
    }

    private boolean newSaveToCSV() {
        String rider, section, scores, day;
        filename = "scores.csv";
        try {
            exportDir = new File(getFilesDir(), filename);

            // Create new CSV file in storage
            exportDir.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

//            Prepare and write filednames as header
            String[] details = {"Observer: ", observer, mobile};
            String[] header = {"Day", "Section", "Rider", "Scores"};
            csvWrite.writeNext(details, false);
            csvWrite.writeNext(header, false);

            // Get score data for current trial
            Cursor curChild = scoreDbHelper.getScoresForEmail(trialid);
            while (curChild.moveToNext()) {
                section = curChild.getString(1);
                rider = curChild.getString(0);
                scores = curChild.getString(2);
                day = curChild.getString(3);
//                observer = curChild.getString(3);
                String[] arrStr = {day, section, rider, scores
                };

                csvWrite.writeNext(arrStr, false);
            }
            // Close filewriter
            curChild.close();
            csvWrite.close();
            scoreDbHelper.close();
            return true;

        } catch (IOException e) {
            Log.e("Child", e.getMessage(), e);
            return false;
        }
    }

    // From https://stackoverflow.com/questions/32262829/how-to-upload-file-using-volley-library-in-android
    public int uploadFile(String sourceFileUri) {
        File directory = getFilesDir();
        File sourceFile = new File(directory, filename);
        final String fileName = sourceFileUri;

        HttpURLConnection conn;
        DataOutputStream dos;
        dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        //File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

//            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + fileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"
                            + uploadFilePath + fileName);
                }
            });

            return 0;

        } else {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                String upLoadServerUri = "https://android.trialmonster.uk/UploadToServer.php";
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
                            Toast.makeText(ScoreListActivity.this, "Error processing data",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

//                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(ScoreListActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

//                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(ScoreListActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file Exception", "Exception : "
                        + e.getMessage(), e);
            }
            return serverResponseCode;
        }
    }

    private void uploadScores() {
        canConnect = canConnect();
        if (!canConnect) {
            Toast.makeText(ScoreListActivity.this, "No Internet connection. Please try again later",
                    Toast.LENGTH_LONG).show();
        } else {
            volleyScoreUpload();
//            markAsDone(trialid);
        }
    }

    private void emailScores() {
        // Check for connection
        canConnect = canConnect();
        if (!canConnect) {
            Toast.makeText(ScoreListActivity.this, "No Internet connection. Please try again later",
                    Toast.LENGTH_LONG).show();
        } else {
            // Get email from prefs - if no saved value, then send to blackhole
            email = localPrefs.getString("email", "blackhole@alexsykes.net");
//            email = "alex@alexsykes.net";
            Date date = new Date();
            // getTime() returns current time in milliseconds -
            // gives
            long time = date.getTime();
            String ts = String.valueOf(time);
            filename = "data_" + ts + ".csv";
            String sendMailURL =
                    "https://android.trialmonster.uk/sendMailWithFileLive.php?id=" + ts +
                            "&trialid=" + trialid + "&email=" + email;

            newSaveToCSV();
            processCSV(sendMailURL);
        }
    }

    private JSONArray getDataForUpload() {
        ArrayList<HashMap<String, String>> dataToUpload =
                scoreDbHelper.getNewScoreListForUpload(trialid, section, day);
        JSONArray scoresJSONArray = new JSONArray();

        String scoreStr;
        for (int i = 0; i < dataToUpload.size(); i++) {
            JSONArray score = new JSONArray();
            HashMap<String, String> scoreItem = dataToUpload.get(i);
            score.put(scoreItem.get("id"));

            scoreStr = scoreItem.get("score");
//            scoreStr = scoreStr.toUpperCase();
            score.put(scoreStr);
            score.put(scoreItem.get("updated"));
            score.put(scoreItem.get("created"));
            scoresJSONArray.put(score);
        }
        return scoresJSONArray;
    }

    private void volleyScoreUpload() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String URL = "https://android.trialmonster.uk/androidNewScoreUploadLive.php";

        JSONArray data = getDataForUpload();
        String requestBody = data.toString();
//        Log.i("Info", "volleyScoreUpload: " + requestBody);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d("Volley", "Response: " + response);
                if (response.equals("1")) {
                    markAsDone(trialid, day, section);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Volley", error.toString());
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                // request body goes here
//                String requestBody = data.toString();
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };

        Log.d("string", stringRequest.toString());
        int MY_SOCKET_TIMEOUT_MS = 5000;

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

    private void markAsDone(int trialid, int day, int section) {
        scoreDbHelper = new ScoreDbHelper(this);
        scoreDbHelper.markAsDone(trialid, day, section);
        scoreDbHelper.close();
        populateScoreList();
    }


    private void writeCSVFile(Intent data) {
        fileUri = data.getData();
        filePath = fileUri.getPath();
        Cursor scoresForExport =
                scoreDbHelper.getScoresForSaving(trialid, section);
        try {
            OutputStream os = getContentResolver().openOutputStream(data.getData());
            Writer writer = new OutputStreamWriter(os);
            csvWriter = new CSVWriter(writer);

            String[] markerHeaderRecord = {"Rider", "Scores"};
            csvWriter.writeNext(markerHeaderRecord);

            while (scoresForExport.moveToNext()) {
                String[] arrStr = new String[scoresForExport.getColumnCount()];
                for (int i = 0; i < scoresForExport.getColumnCount(); i++)
                    arrStr[i] = scoresForExport.getString(i);
                csvWriter.writeNext(arrStr);
            }
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeTXTFile(Intent data) {
        fileUri = data.getData();
        filePath = fileUri.getPath();

        Cursor scoreData = scoreDbHelper.getScoreDataForSaving(trialid, section);

        Cursor scoresForExport =
                scoreDbHelper.getScoresForSaving(trialid, section);
        try {
            OutputStream os = getContentResolver().openOutputStream(data.getData());
            OutputStreamWriter writer = new OutputStreamWriter(os);

//            String[] markerHeaderRecord = {"Rider", "Scores"};
            writer.write("Scores for section " + section + "\n");
            writer.write("Rider,Scores\n");

            while (scoreData.moveToNext()) {
                String rider = scoreData.getString(0);
                String score = scoreData.getString(1);
                writer.write(rider + "," + score + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case (MotionEvent.ACTION_DOWN):
                Log.d(DEBUG_TAG, "Action was DOWN");
                return true;
            case (MotionEvent.ACTION_MOVE):
                Log.d(DEBUG_TAG, "Action was MOVE");
                return true;
            case (MotionEvent.ACTION_UP):
                Log.d(DEBUG_TAG, "Action was UP");
                return true;
            case (MotionEvent.ACTION_CANCEL):
                Log.d(DEBUG_TAG, "Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE):
                Log.d(DEBUG_TAG, "Movement occurred outside bounds of current screen element");
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == -1) {
//                    writeCSVFile(data);
                    writeTXTFile(data);
                }
                break;


            default:
                break;
        }
    }
}