package com.alexsykes.scoremonster;

import static java.lang.String.join;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.data.ScoreContract;
import com.alexsykes.scoremonster.data.ScoreDbHelper;
import com.alexsykes.scoremonster.data.TimeContract;
import com.alexsykes.scoremonster.data.TrialContract;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class ScoreMonsterLite extends Application {
    private static final String BASE_URL = "https://android.trialmonster.uk/";
    private final int trialid = -999;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialData;

    // Databases
    private ScoreDbHelper mDbHelper;
    private String theTrialName;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Note", "OnAppStart");

        // Create database connection
        dbInit();

        // Check for connectivity
        boolean isOnline = isOnline();

        // if online, loads list of trials
        if (isOnline) {
            String URL = BASE_URL + "getTrialList.php";
            try {
                getTrialList(URL);
                Log.i(null, "Trials data loaded");
            } catch (NullPointerException e) {
                Log.e(null, "Error loading trials data");
            }
        }
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

        // Create a String that contains the SQL statement to create the scores table
        String SQL_CREATE_TRIALS_TABLE = "CREATE TABLE IF NOT EXISTS " + TrialContract.TrialEntry.TABLE_NAME + " ("
                + TrialContract.TrialEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_MODE + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_NAME + " TEXT , "
                + TrialContract.TrialEntry.COLUMN_TRIAL_DATE + " TEXT , "
                + TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL + " TEXT , "
                + TrialContract.TrialEntry.COLUMN_TRIAL_CLUB + " TEXT , "
                + TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID + " INTEGER NOT NULL DEFAULT 0 );";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_TRIALS_TABLE);

        // Create a String that contains the SQL statement to create the scores table
        String SQL_CREATE_TIMES_TABLE = "CREATE TABLE IF NOT EXISTS " + TimeContract.TimeEntry.TABLE_NAME + " ("
                + TimeContract.TimeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TimeContract.TimeEntry.COLUMN_TIME_NUMBER + " INTEGER NOT NULL DEFAULT 0, "
                + TimeContract.TimeEntry.COLUMN_TIME_SEQUENCE + " INTEGER NOT NULL DEFAULT 0, "
                + TimeContract.TimeEntry.COLUMN_TIME_CREATED + " TEXT , "
                + TimeContract.TimeEntry.COLUMN_TIME_TRIALID + " INTEGER NOT NULL DEFAULT 0 );";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_TIMES_TABLE);
    }

    // Get trial list from server
    private void getTrialList(final String urlWebService) {
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
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                // Populate ArrayList with JSON data
                theTrialData = populateResultArrayList(s);

                int size = theTrialData.size();
                theTrials = new String[size];
                theIDs = new String[size];
                String id;

                for (int index = 0; index < theTrialData.size(); index++) {
                    theTrialName = theTrialData.get(index).get("name");
                    id = theTrialData.get(index).get("id");
                    theTrials[index] = theTrialName;
                    theIDs[index] = id;
                }

                // Put values of trial name and trialid into prefs
                setTrialsList(theTrialData);

                // Then save into database
                saveTrialData(theTrialData);
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
                String date, name, id, club, numsections, numlaps, starttime, email, mode;

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
                        mode = jsonArray.getJSONObject(index).getString("scoringmode");

                        // trial = club + " - " + name;
                        theTrial.put("id", id);
                        theTrial.put("date", date);
                        theTrial.put("club", club);
                        theTrial.put("name", name);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("starttime", starttime);
                        theTrial.put("email", email);
                        theTrial.put("mode", mode);
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
                    // TODO handle timeout
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
            theTrialIds[index] = theTrialList.get(index).get("id");
        }
        String theTrialListNames = join(",", theTrialNames);
        String theTrialListIds = join(",", theTrialIds);

        SharedPreferences.Editor editor = localPrefs.edit();

        editor.putString("theNames", theTrialListNames);
        editor.putString("theIds", theTrialListIds);
        editor.apply();
    }

    private void saveTrialData(ArrayList<HashMap<String, String>> theTrialData) {
        // Database operations - https://www.tutorialspoint.com/android/android_sqlite_database.htm
        // First, get your database

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (int i = 0; i < theTrialData.size(); i++) {
            HashMap<String, String> theTrial = theTrialData.get(i);
            String theDate = theTrial.get("date");
            String theName = theTrial.get("name");
            String theNumSections = theTrial.get("numsections");
            String theNumLaps = theTrial.get("numlaps");
            String theID = theTrial.get("id");
            String theEmail = theTrial.get("email");
            String club = theTrial.get("club");
            String mode = theTrial.get("mode");

            // Create a ContentValues object where column names are the keys,
            ContentValues values = new ContentValues();
            // String dateString = currentTimeStamp;
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NAME, theName);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_DATE, theDate);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL, theEmail);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS, theNumLaps);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS, theNumSections);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID, theID);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_CLUB, club);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_MODE, mode);
            values.put(TrialContract.TrialEntry._ID, theID);

            db.insertWithOnConflict("trials", null, values, SQLiteDatabase.CONFLICT_REPLACE);

            Log.i("Note", "Result: ");
        }

    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
