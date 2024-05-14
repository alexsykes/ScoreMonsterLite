package uk.trialmonster.observer.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class TrialDbHelper extends SQLiteOpenHelper {
    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "monster.db";
    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 2;

    /**
     * Constructs a new instance of {@link ScoreDbHelper}.
     *
     * @param context of the app
     */
    public TrialDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long getStartInterval(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT startInterval FROM trials WHERE trialid = " + trialid;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        String value = cursor.getString(0);
        long startInterval = Long.valueOf(value);
        cursor.close();
        return startInterval;
    }

    public void clearTrials() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM trials";
        db.execSQL(query);
    }

    public ArrayList<HashMap<String, String>> getPrefsOptions(int loggedInUserID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> optionList = new ArrayList<>();
        String query = "SELECT group_concat(_id, ','),group_concat(name, ',')  FROM trials " +
                "WHERE created_by = " + loggedInUserID + " AND date > DATE('now')  ORDER BY date ASC";
//        Log.i("Query", query);
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> option = new HashMap<>();
            option.put("ids", cursor.getString(0));
            option.put("names", cursor.getString(1));
            optionList.add(option);
        }
        cursor.close();
        return optionList;
    }

    public Cursor getTrialOptions(int loggedInUserID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> optionList = new ArrayList<>();

        String query = "SELECT group_concat(_id, ','),group_concat(name, ',')  FROM trials " +
                "WHERE created_by = " + loggedInUserID + "  ORDER BY date ASC";

        Log.i("Info", query);
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }

    public ArrayList<HashMap<String, String>> getTrialList(int loggedInUserID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> trialList = new ArrayList<>();
        String query = "SELECT * FROM trials WHERE created_by = " + loggedInUserID + " ORDER BY " +
                "date ASC";
//        Log.i("Query", query);
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> trial = new HashMap<>();
            trial.put("id", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry._ID)));
            trial.put("numlaps", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS)));
            trial.put("numsections", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS)));
            trial.put("name", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NAME)));
            trial.put("date", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_DATE)));
            trial.put("trialid", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID)));
            trial.put("mode", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_MODE)));
            trial.put("email", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL)));
            trial.put("club", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_CLUB)));
            trial.put("numdays",
                    cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMDAYS)));
            trialList.add(trial);
        }
        cursor.close();
        return trialList;
    }

    public ArrayList<HashMap<String, String>> getTrialData(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> trialData = new ArrayList<>();
        String query = "SELECT * FROM trials WHERE _id = " + trialid;
        // String query = "SELECT * FROM trials ORDER BY date ASC";
//        Log.i("Query", query);
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> trial = new HashMap<>();
            trial.put("id", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry._ID)));
            trial.put("numlaps", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS)));
            trial.put("numsections", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS)));
            trial.put("name", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NAME)));
            trial.put("date", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_DATE)));
            trial.put("trialid", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID)));
            trial.put("mode", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_MODE)));
            trial.put("email", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL)));
            trial.put("club", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_CLUB)));
            trial.put("startInterval", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_INTERVAL)));
            trial.put("numdays",
                    cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMDAYS)));
            trialData.add(trial);
        }
        cursor.close();
        return trialData;
    }
}
