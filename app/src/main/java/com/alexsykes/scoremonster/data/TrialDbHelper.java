package com.alexsykes.scoremonster.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
        String date = cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_DATE));

        Trial theTrial = new Trial(trialid, numsections, numlaps, email, date, name);
        return theTrial;
    }

    public void insert(String name, String email) {

        SQLiteDatabase db = this.getWritableDatabase();

        String query = "INSERT INTO trials (name, email) VALUES ('" + name + "','" + email + "')";
        db.execSQL(query);

    }

    public Cursor getCount() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT COUNT(*), * FROM trials";
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }

    public void describe() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT sql FROM monster WHERE name = 'trials'";
        Cursor c = db.rawQuery(query, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
