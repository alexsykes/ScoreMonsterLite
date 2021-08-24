package com.alexsykes.scoremonster.data;

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

    public ArrayList<HashMap<String, String>> getTrials() {
        String _id, numsections, numlaps, email, date, name;
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> theTrialList = new ArrayList<>();

        String query = "SELECT * FROM trials ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        Trial theTrial;

        while (cursor.moveToNext()) {
            HashMap<String, String> trials = new HashMap<>();
            trials.put("id", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry._ID)));
            trials.put("numsections", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS)));
            trials.put("numlaps", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS)));
            trials.put("email", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL)));
            trials.put("name", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NAME)));
            trials.put("trialid", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID)));
            trials.put("date", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_DATE)));
            trials.put("scoringmode", cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_MODE)));

            // theTrial = new Trial(Integer.valueOf(_id), Integer.valueOf(numsections), Integer.valueOf(numlaps), email, date, name);
            theTrialList.add(trials);
        }
        cursor.close();
        return theTrialList;
    }


    public Trial getTrial(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM trials WHERE id = " + trialid;
        Cursor cursor = db.rawQuery(query, null);
        String name = cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NAME));
        String email = cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL));
        int numlaps = cursor.getInt(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS));
        int numsections = cursor.getInt(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS));
        int scoringmode = cursor.getInt(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_MODE));
        String date = cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_DATE));

        Trial theTrial = new Trial(trialid, numsections, numlaps, email, date, name, scoringmode);
        return theTrial;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void clearTrials() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM trials";
        db.execSQL(query);
    }

    public ArrayList<HashMap<String, String>> getPrefsOptions() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> optionList = new ArrayList<>();
        String query = "SELECT group_concat(_id, ','),group_concat(name, ',')  FROM trials ORDER BY date ASC";
        Log.i("Query", query);
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

    public ArrayList<HashMap<String, String>> getTrialList() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> trialList = new ArrayList<>();
        String query = "SELECT * FROM trials ORDER BY date ASC";
        Log.i("Query", query);
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
        Log.i("Query", query);
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
            trialData.add(trial);
        }
        cursor.close();
        return trialData;
    }
}
