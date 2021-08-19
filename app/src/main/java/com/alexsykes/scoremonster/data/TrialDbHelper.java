package com.alexsykes.scoremonster.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    public Trial getTrial(int trialid) {
        SQLiteDatabase db = this.getReadableDatabase();
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

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
