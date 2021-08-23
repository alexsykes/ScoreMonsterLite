package com.alexsykes.scoremonster;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.alexsykes.scoremonster.data.TrialContract;
import com.alexsykes.scoremonster.data.TrialDbHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class Bits {
    TrialDbHelper trialDbHelper;

    public Bits(TrialDbHelper mDbHelper) {     // Create database connection
//        mDbHelper = new ScoreDbHelper(this);
//        trialDbHelper = new TrialDbHelper(this);
    }

    private void saveTrialData(ArrayList<HashMap<String, String>> theTrialData) {
        // Database operations - https://www.tutorialspoint.com/android/android_sqlite_database.htm
        // First, get your database
        //   final String DATABASE_NAME = "monster.db";
        //   SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);

        SQLiteDatabase db = trialDbHelper.getWritableDatabase();

        for (int i = 0; i < theTrialData.size(); i++) {
            HashMap<String, String> theTrial = theTrialData.get(i);
            String theDate = theTrial.get("date");
            String theName = theTrial.get("name");
            String theNumSections = theTrial.get("numsections");
            String theNumLaps = theTrial.get("numlaps");
            String theID = theTrial.get("id");
            String theEmail = theTrial.get("email");

            // Create a ContentValues object where column names are the keys,
            ContentValues values = new ContentValues();
            // String dateString = currentTimeStamp;
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NAME, theName);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_DATE, theDate);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL, theEmail);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS, theNumLaps);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS, theNumSections);
            values.put(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID, theID);

            long result = db.insert(TrialContract.TrialEntry.TABLE_NAME, null, values);

            Log.i("Note", "Result: " + result);
        }

    }

    private void setTrialsList(ArrayList<HashMap<String, String>> theTrialList) {
//        SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//        // Need name and id from theTrialList
//        int size = theTrialList.size();
//        String[] theTrialNames = new String[size];
//        String[] theTrialIds = new String[size];
//        for (int index = 0; index < size; index++) {
//            theTrialNames[index] = theTrialList.get(index).get("name");
//            theTrialIds[index] = theTrialList.get(index).get("id");
//        }
//        String theTrialListNames = join(",", theTrialNames);
//        String theTrialListIds = join(",", theTrialIds);
//
//        SharedPreferences.Editor editor = localPrefs.edit();
//
//        editor.putString("theNames", theTrialListNames);
//        editor.putString("theIds", theTrialListIds);
//        editor.apply();
    }

}
