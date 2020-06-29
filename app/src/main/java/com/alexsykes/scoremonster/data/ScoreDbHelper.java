package com.alexsykes.scoremonster.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alexsykes.scoremonster.data.ScoreContract.ScoreEntry;

import java.util.ArrayList;
import java.util.HashMap;

public class ScoreDbHelper extends SQLiteOpenHelper {



    private static final int SYNCED = 0;
    private static final int NOT_SYNCED = -1;
    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "monster.db";
    private static final String TAG = "ScoreDbHelper";
    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 2;
    private ArrayList<Score> theScores;

    /**
     * Constructs a new instance of {@link ScoreDbHelper}.
     *
     * @param context of the app
     */
    public ScoreDbHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the scores table
        String SQL_CREATE_SCORES_TABLE = "CREATE TABLE " + ScoreEntry.TABLE_NAME + " ("
                + ScoreEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ScoreEntry.COLUMN_SCORE_OBSERVER + " TEXT NOT NULL, "
                + ScoreEntry.COLUMN_SCORE_SECTION + " INTEGER NOT NULL, "
                + ScoreEntry.COLUMN_SCORE_RIDER + " INTEGER NOT NULL, "
                + ScoreEntry.COLUMN_SCORE_LAP + " INTEGER NOT NULL DEFAULT 0, "
                + ScoreEntry.COLUMN_SCORE_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + ScoreEntry.COLUMN_SCORE_UPDATED + " TEXT , "
                + ScoreEntry.COLUMN_SCORE_EDITED + " INTEGER NOT NULL DEFAULT 0, "
                + ScoreEntry.COLUMN_SCORE_TRIALID + " INTEGER NOT NULL DEFAULT 0, "
                + ScoreEntry.COLUMN_SCORE_SYNC + " INTEGER NOT NULL DEFAULT 1, "
                + ScoreEntry.COLUMN_SCORE_SCORE + " INTEGER NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_SCORES_TABLE);

        // Create a String that contains the SQL statement to create the finishtimes table
        String SQL_CREATE_FINISHTIMES_TABLE = "CREATE TABLE " + FinishTimeContract.FinishTimeEntry.TABLE_NAME + " ("
                + FinishTimeContract.FinishTimeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FinishTimeContract.FinishTimeEntry.COLUMN_FINISHTIME_TIME + " TEXT NOT NULL, "
                + FinishTimeContract.FinishTimeEntry.COLUMN_FINISHTIME_SYNC + " INTEGER NOT NULL DEFAULT 1, "
                + FinishTimeContract.FinishTimeEntry.COLUMN_FINISHTIME_RIDER + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_FINISHTIMES_TABLE);

    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + ScoreContract.ScoreEntry.TABLE_NAME;
        db.execSQL(sql);

        onCreate(db);
    }


    /**
     * Section imported from tutlane
     *
     * @return ArrayList of Score data
     * @param trialid the trial id
     * @param section the section
     */

    // Get Score Details
    public ArrayList<HashMap<String, String>> getScoreList(int trialid, int section) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores WHERE trialid =" + trialid + " AND section = " + section + " ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();
            scores.put("id", cursor.getString(cursor.getColumnIndex(ScoreEntry._ID)));
            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("lap", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_LAP)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("trialid", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_TRIALID)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SYNC)));
            scores.put("edited", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_EDITED)));
            scoreList.add(scores);
        }
        cursor.close();
        return scoreList;
    }

    // Used in RecyclerView Score List

    public ArrayList getScores() {
        String section, rider, lap, score, _id, observer, created, sync;

        SQLiteDatabase db = this.getWritableDatabase();
        theScores = new ArrayList<>();

        String query = "SELECT section, rider, lap, score, _id, observer, sync, created FROM scores ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        Score theScore;

        while (cursor.moveToNext()) {

            section = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SECTION));
            rider = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_RIDER));
            lap = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_LAP));
            score = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SCORE));
            observer = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_OBSERVER));
            created = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_CREATED));
            sync = cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SYNC));
            _id = cursor.getString(cursor.getColumnIndex(ScoreEntry._ID));

            theScore = new Score(section, score, rider, lap, Integer.parseInt(_id), observer, created, sync);
            theScores.add(theScore);
        }
        cursor.close();
        return theScores;
    }


    // Unused
    public ArrayList<HashMap<String, String>> GetRidersScores() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores ORDER BY rider, _id ASC ";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();

            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("_id", cursor.getString(cursor.getColumnIndex(ScoreEntry._ID)));
            scores.put("lap", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_LAP)));
            scores.put("section", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SECTION)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SYNC)));
            scores.put("edited", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_EDITED)));
            scoreList.add(scores);
        }
        cursor.close();
        return scoreList;
    }

    //

    public void clearResults(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query  = "DELETE FROM scores";
        db.execSQL(query);
    }

    // Used in Score List

    /**
     * Get riders summary scores
     *
     * @return ArrayList of summary data
     */
    public ArrayList<HashMap<String, String>> getRidersSummaryScores() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT rider, SUM(score) AS total, COUNT(score) as count, GROUP_CONCAT(score,' • ') AS scoredata, sync FROM scores GROUP BY rider ORDER BY rider, _id ASC ";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();

            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("scoredata", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SCOREDATA)));
            scores.put("count", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_COUNT)));
            scores.put("total", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_TOTAL)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreEntry.COLUMN_SCORE_SYNC)));
            scoreList.add(scores);
        }
        cursor.close();
        return scoreList;
    }

    // method to count lap number for current rider

    public int getRiderLap(int rider, int section, int trialid) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT score AS numLaps FROM scores WHERE rider = " + rider + " AND section = " + section + " AND trialid = " + trialid;
        Cursor cursor = db.rawQuery(query, null);
        int numLaps = cursor.getCount();
        cursor.close();

        return numLaps;
    }


    public void markAsDone(int trialid) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE scores SET sync = " + SYNCED + " WHERE sync = " + NOT_SYNCED + " AND trialid = " + trialid;
        db.execSQL(query);
    }

    public Cursor getUnSynced(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT  * FROM scores WHERE sync = " + NOT_SYNCED + " AND trialid=" + id, new String[]{});
    }

    public void delete(int id) {
        // Unused
        SQLiteDatabase db = this.getReadableDatabase();
        //String query = "DELETE FROM scores WHERE _id = " + id;
        String query = "UPDATE scores SET edited = 1 WHERE _id = " + id;
        db.execSQL(query);
    }

    public void update(String scoreid, String score) {

        SQLiteDatabase db = this.getReadableDatabase();
        // String query = "UPDATE scores SET score = " + score + ", edited = 1, updated = DATETIME('now','localtime'), sync = " + NOT_SYNCED + " WHERE _id = " + scoreid;
        String query = "UPDATE scores SET score = " + score + ", edited = 1, updated = DATETIME('now'), sync = " + NOT_SYNCED + " WHERE _id = " + scoreid;
        db.execSQL(query);

    }
}