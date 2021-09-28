package com.alexsykes.scoremonster.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class TimeDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "monster.db";
    private static final int DATABASE_VERSION = 2;

    public TimeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Get Score Details
    public ArrayList<HashMap<String, String>> getTimeList(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> timeList = new ArrayList<>();
        String query = "SELECT * FROM times WHERE trialid = " + trialid + " ORDER BY elapsedTime ASC";
        Log.i("Query", query);
        //  String query = "SELECT * FROM scores  ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> times = new HashMap<>();
            times.put("id", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry._ID)));
            times.put("rider", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_NUMBER)));
            times.put("elapsedTime", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_ELAPSEDTIME)));
            times.put("finishTime", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_FINISHTIME)));
            timeList.add(times);
        }
        cursor.close();
        return timeList;
    }

    // Lapse times by setting trialid to negative of original trialid
    public void lapseTimes(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        int newid = -trialid;
        String query = "UPDATE times SET trialid = " + newid + " WHERE trialid = " + trialid;
        Log.i("Query", query);
        // Execute the SQL statement
        db.execSQL(query);
    }
}
