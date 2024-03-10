package uk.trialmonster.observer;
// TODO - update elapsedTime to take account of startTime and startInterval
// TODO - SettingsActivity - update trial data on chamge of trial - done

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

import uk.trialmonster.observer.data.ScoreContract;
import uk.trialmonster.observer.data.ScoreDbHelper;
import uk.trialmonster.observer.data.TimeContract;
import uk.trialmonster.observer.data.TrialContract;

public class Observer extends Application {
    ArrayList<HashMap<String, String>> theTrialList, theScoreList;
    boolean canConnect;

    // Databases
    private ScoreDbHelper mDbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Note", "OnAppStart");

        // Create database connection
        dbInit();

        // Check for connectivity
        canConnect = canConnect();

        // if online, loads list of trials
        if (canConnect) {
            try {
                getTrialListFromServer();
                getScoreListFromServer();
                Log.i("Info", "Trials data loaded");
            } catch (NullPointerException e) {
                Log.e("Info", "Error loading trials data");
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
                + ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_SCORES_TABLE);

        mDbHelper = new ScoreDbHelper(this);
        mDbHelper.getWritableDatabase();

        // Create a String that contains the SQL statement to create the scores table
        String SQL_CREATE_TRIALS_TABLE = "CREATE TABLE IF NOT EXISTS " + TrialContract.TrialEntry.TABLE_NAME + " ("
                + TrialContract.TrialEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_CREATED_BY + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_MODE + " INTEGER NOT NULL DEFAULT 0, "
                + TrialContract.TrialEntry.COLUMN_TRIAL_INTERVAL + " INTEGER NOT NULL DEFAULT 0, "
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
                + TimeContract.TimeEntry.COLUMN_TIME_FINISHTIME + " INTEGER NOT NULL DEFAULT 0, "
                + TimeContract.TimeEntry.COLUMN_TIME_ELAPSEDTIME + " INTEGER NOT NULL DEFAULT 0, "
                + TimeContract.TimeEntry.COLUMN_TIME_PENALTY + " INTEGER NOT NULL DEFAULT 0, "
                + TimeContract.TimeEntry.COLUMN_TIME_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + TimeContract.TimeEntry.COLUMN_TIME_TRIALID + " INTEGER NOT NULL DEFAULT 0 );";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_TIMES_TABLE);
        db.close();
    }

    private void getTrialListFromServer() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://android.trialmonster.uk/getTrialListScoreMonsterLive.php";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        updateTrialsDB(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Log.i("Info", "That didn't work!");

            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void getScoreListFromServer() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://android.trialmonster.uk/getScoreListScoreMonsterLive.php";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        updateScoresDB(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Log.i("Info", "That didn't work!");

            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void updateScoresDB(String response) {
    }

    private void updateTrialsDB(String response) {
        // Convert response to arraylist
        try {
            theTrialList = getTrialListFromServer(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        for (int i = 0; i < theTrialList.size(); i++) {
            HashMap<String, String> theTrial = theTrialList.get(i);
            String theDate = theTrial.get("date");
            String theName = theTrial.get("name");
            String theNumSections = theTrial.get("numsections");
            String theNumLaps = theTrial.get("numlaps");
            String _id = theTrial.get("trialid");
            String theEmail = theTrial.get("email");
            String club = theTrial.get("club");
            String mode = theTrial.get("mode");
            String startinterval = theTrial.get("startinterval");
            String created_by = theTrial.get("created_by");

            // Create a ContentValues object where column names are the keys,
            ContentValues values = new ContentValues();

            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NAME, theName);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_DATE, theDate);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL, theEmail);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS, theNumLaps);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS, theNumSections);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID, _id);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_CLUB, club);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_MODE, mode);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_INTERVAL, startinterval);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_CREATED_BY, created_by);
            values.put(TrialContract.TrialEntry._ID, _id);

            db.insertWithOnConflict("trials", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        db.close();
    }
    // Convert returned string to Arraylist for saving in DB
    private ArrayList<HashMap<String, String>> getTrialListFromServer(String json) throws JSONException {
        theTrialList = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);

        for (int index = 0; index < jsonArray.length(); index++) {
            HashMap<String, String> theTrialHash = new HashMap<>();

            theTrialHash.put("trialid", jsonArray.getJSONObject(index).getString("id"));
            theTrialHash.put("date", jsonArray.getJSONObject(index).getString("date"));
            theTrialHash.put("club", jsonArray.getJSONObject(index).getString("club"));
            theTrialHash.put("name", jsonArray.getJSONObject(index).getString("name"));
            theTrialHash.put("numlaps", jsonArray.getJSONObject(index).getString("numlaps"));
            theTrialHash.put("numsections", jsonArray.getJSONObject(index).getString("numsections"));
            theTrialHash.put("starttime", jsonArray.getJSONObject(index).getString("starttime"));
            theTrialHash.put("mode", jsonArray.getJSONObject(index).getString("scoringmode"));
            theTrialHash.put("startinterval", jsonArray.getJSONObject(index).getString("startinterval"));
            theTrialHash.put("email", jsonArray.getJSONObject(index).getString("email"));
            theTrialHash.put("created_by", jsonArray.getJSONObject(index).getString("created_by"));
            theTrialList.add(theTrialHash);
        }
        return theTrialList;
    }

    protected boolean canConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}