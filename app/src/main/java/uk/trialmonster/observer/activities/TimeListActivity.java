package uk.trialmonster.observer.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.Date;
import java.util.HashMap;

import uk.trialmonster.observer.R;
import uk.trialmonster.observer.data.TimeDbHelper;

public class TimeListActivity extends AppCompatActivity {
    /**********  File Path *************/
    final String uploadFilePath = "mnt/sdcard/Documents/Scoremonster/";
    private final String baseURL = "https://android.trialmonster.uk/processTimeUpload.php?trialid=";
    File exportDir = new File(Environment.getExternalStoragePublicDirectory("Documents/Scoremonster"), "");
    RecyclerView timeView;
    ArrayList<HashMap<String, String>> theTimeList;
    SharedPreferences localPrefs;
    HashMap<String, String> theTime;
    int serverResponseCode = 0;
    private int trialid;
    ProgressDialog dialog = null;
    TextView statusLine;
    private long clockStartTime, penaltyTariff, startInterval, fastestTime;
    private long baseTime;
    private TimeDbHelper timeDbHelper;
    private boolean canConnect;
    private String filename, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        // Get shared preferences for trialid, section
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
        startInterval = localPrefs.getLong("startInterval", 60);

        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss");
        String dateString = dateFormat.format(clockStartTime);

        statusLine = findViewById(R.id.statusLine);

        // Get data
        trialid = localPrefs.getInt("trialid", -999);
        timeDbHelper = new TimeDbHelper(this);
        timeDbHelper.updateTimes(trialid, startInterval, penaltyTariff);
        fastestTime = timeDbHelper.getFastestTime(trialid);
        baseTime = (fastestTime / 1000) + 1;

        long elapsedTime = fastestTime / 1000;
        long minutes = elapsedTime / 60;
        long seconds = elapsedTime % 60;

        String secondsString = "00" + seconds;
        secondsString = secondsString.substring(secondsString.length() - 2);
        String elapsedTimeString = minutes + ":" + secondsString;
        statusLine.setText("Standard time: " + elapsedTimeString);
        populateTimeList(baseTime);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.email:
                emailTimes();
                return true;

            case R.id.upload:
                uploadTimes();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void uploadTimes() {
        canConnect = canConnect();
        if (!canConnect) {
            Toast.makeText(TimeListActivity.this, "No Internet connection. Please try again later",
                    Toast.LENGTH_LONG).show();
        } else {
            // Get timestamp and add to filename
            Date date = new Date();
            // getTime() returns current time in milliseconds
            long time = date.getTime();
            String ts = String.valueOf(time);
            filename = "data_" + ts + ".csv";
            String processURL = baseURL + trialid + "&id=" + ts;
             Log.i("URL",processURL);
            processCSV(processURL);
        }
    }

    private void emailTimes() {
        canConnect = localPrefs.getBoolean("canConnect", false);
        if (!canConnect) {
            Toast.makeText(TimeListActivity.this, "No Internet connection. Please try again later",
                    Toast.LENGTH_LONG).show();
        } else {
            email = localPrefs.getString("email", "blackhole@alexsykes.net");
            Date date = new Date();
            // getTime() returns current time in milliseconds
            long time = date.getTime();
            String ts = String.valueOf(time);
            filename = "data_" + ts + ".csv";
            String sendMailURL = "https://android.trialmonster.uk/sendMailWithFile.php?id=" + ts + "&trialid=" + trialid + "&email=" + email;

            Log.i("Monitor", sendMailURL);
            processCSV(sendMailURL);
        }
    }

    protected boolean canConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void populateTimeList(long baseTime) {
        theTimeList = timeDbHelper.getTimeList(trialid);
        timeView = findViewById(R.id.timeView);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        timeView.setLayoutManager(llm);
        timeView.setHasFixedSize(true);
        initializeAdapter();
    }

    private void initializeAdapter() {
        TimeListAdapter adapter = new TimeListAdapter(theTimeList);
        timeView.setAdapter(adapter);

        // Start
        // on below line we are creating a method to create item touch helper
        // method for adding swipe to delete functionality.
        // in this we are specifying drag direction and position to right
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // this method is called
                // when the item is moved.

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // this method is called when we swipe our item to right direction.
                // on below line we are getting the item at a particular position.
                // RecyclerData deletedCourse = theTimeList.get(viewHolder.getAdapterPosition());

                // below line is to get the position
                // of the item at that position.
                int position = viewHolder.getAdapterPosition();
                String timeID = theTimeList.get(position).get("id");
                // this method is called when item is swiped.
                // below line is to remove item from our array list.

                theTimeList.remove(viewHolder.getAdapterPosition());

                // Remove method flips trialid -> -trialid
                timeDbHelper.remove(timeID, trialid);
                adapter.notifyItemRangeRemoved(position, 1);
                adapter.notifyDataSetChanged();
            }
            // at last we are adding this
            // to our recycler view.
        }).attachToRecyclerView(timeView);


        // End

    }

    private boolean saveToCSV() {
        String id, number, finishTime, elapsedTime, created, trialID, penalty;

        try {
            // Get fastest time
            long fastestTime = timeDbHelper.getFastestTime(trialid);
            String fastestTimeString = String.valueOf(fastestTime);


            exportDir = new File(getFilesDir(), filename);

            exportDir.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(exportDir));

            String[] header = {"id", "number", "finishTime",
                    "elapsedTime", "penalty", "created", "trialid", fastestTimeString};

            csvWrite.writeNext(header, false);

            // Get current data

            Cursor curChild = timeDbHelper.getAll(trialid);
            while (curChild.moveToNext()) {
                id = curChild.getString(0);
                number = curChild.getString(1);
                finishTime = curChild.getString(2);
                elapsedTime = curChild.getString(3);
                penalty = curChild.getString(4);
                created = curChild.getString(5);
                trialID = curChild.getString(6);

                String[] arrStr = {id, number, finishTime, elapsedTime, penalty, created, trialID
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
                dialog = ProgressDialog.show(TimeListActivity.this, "Scoremonster",
                        "Processing scores… this make take some time!", true);
                // Prepare CSV file
                saveToCSV();
                // CSV file is now saved on local storage
            }

            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();

                if (s.contentEquals("OK")) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(TimeListActivity.this, "Times uploaded successfully",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            protected String doInBackground(Void... voids) {

                // First upload the file
                int response = uploadFile(uploadFilePath + filename);
                Log.i("Info", "filename: " + filename);
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);
                    Log.i("Info", "urlWebService: " + urlWebService);

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

            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + fileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    // messageText.setText("Source File not exist :" + uploadFilePath + "" + fileName);
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
                            Toast.makeText(TimeListActivity.this, "Error processing data",
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
                        //  messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(TimeListActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        // messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(TimeListActivity.this, "Got Exception : see logcat ",
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

    public static class TimeHolder extends RecyclerView.ViewHolder {
        TextView riderTextView, finishTimeTextView, elapsedTimeTextView, timePenaltyTextView;
        public TimeHolder(@NonNull View itemView) {
            super(itemView);
            riderTextView = itemView.findViewById(R.id.rider);
            finishTimeTextView = itemView.findViewById(R.id.finishTime);
            elapsedTimeTextView = itemView.findViewById(R.id.elapsedTime);
            timePenaltyTextView = itemView.findViewById(R.id.marksLost);
        }
    }

    private class TimeListAdapter extends RecyclerView.Adapter<TimeHolder> {
        ArrayList<HashMap<String, String>> theTimeList;
        HashMap<String, String> theTime;

        public TimeListAdapter(ArrayList<HashMap<String, String>> theTimeList) {
            this.theTimeList = theTimeList;
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        @NonNull
        @Override
        public TimeHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            // Point to data holder layout
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.time_row, viewGroup, false);
            TimeHolder timeHolder = new TimeHolder(v);
            return timeHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull TimeHolder holder, int position) {
            long deltaTime;
            double fastestTimeInSeconds = fastestTime / 1000;
            theTime = theTimeList.get(position);
            String timeID = theTime.get("id");

            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss");
            String finishTimeString = timeFormat.format(Long.valueOf(theTime.get("finishTime")));
            // Get elapsedTime, then convert to seconds and minutes
            long elapsedTime = Long.valueOf(theTime.get("elapsedTime")) / 1000;
            long minutes = elapsedTime / 60;
            long seconds = elapsedTime % 60;

            String secondsString = "00" + seconds;
            secondsString = secondsString.substring(secondsString.length() - 2);
            String elapsedTimeString = minutes + ":" + secondsString;

            deltaTime = (long) Math.ceil((elapsedTime - fastestTimeInSeconds) / 60);
            // Calculate lost marks
            // deltaTime = elapsedTime - baseTime;
            // penalties = deltaTime/penaltyTariff;
            holder.riderTextView.setText(theTime.get("rider"));
            holder.finishTimeTextView.setText(finishTimeString);
            holder.elapsedTimeTextView.setText(elapsedTimeString);
            holder.timePenaltyTextView.setText(String.valueOf(deltaTime));
        }

        @Override
        public int getItemCount() {
            return theTimeList.size();
        }
    }
}