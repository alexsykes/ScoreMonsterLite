package uk.trialmonster.observer.data;

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
        String query = "SELECT * FROM times WHERE trialid = " + trialid + " AND elapsedTime > 0 ORDER BY elapsedTime ASC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> times = new HashMap<>();
            times.put("id", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry._ID)));
            times.put("rider", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_NUMBER)));
            times.put("elapsedTime", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_ELAPSEDTIME)));
            times.put("penalty", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_PENALTY)));
            times.put("finishTime", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_FINISHTIME)));
            timeList.add(times);
        }
        cursor.close();
        return timeList;
    }


    public ArrayList<HashMap<String, String>> getAllRiderTimes(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> timeList = new ArrayList<>();
        String query = "SELECT * FROM times WHERE trialid = " + trialid + " AND elapsedTime > 0 " +
                "ORDER BY number ASC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> times = new HashMap<>();
            times.put("id", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry._ID)));
            times.put("number",
                    cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_NUMBER)));
            times.put("elapsedTime", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_ELAPSEDTIME)));
            times.put("penalty", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_PENALTY)));
            times.put("finishTime", cursor.getString(cursor.getColumnIndex(TimeContract.TimeEntry.COLUMN_TIME_FINISHTIME)));
            timeList.add(times);
        }
        cursor.close();
        return timeList;
    }

    public long getFastestTime(int trialid) {
        long fastestTime = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT MIN(elapsedTime) FROM times WHERE trialid = " + trialid + " AND elapsedTime > 0";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        String value = cursor.getString(0);
        if (value != null) {
            fastestTime = Long.valueOf(value);
        }
        cursor.close();
        Log.i("Info", "FastestTime: " + fastestTime);
        return fastestTime;
    }

    // Lapse times by setting trialid to negative of original trialid
    public void lapseTimes(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        int newid = -trialid;
        String query = "UPDATE times SET trialid = " + newid + " WHERE trialid = " + trialid;
        Log.i("Query", query);
        // Execute the SQL statement
        db.execSQL(query);
        db.close();
    }


    public Cursor getAll(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT  * FROM times WHERE  trialid=" + id + " ORDER BY elapsedTime ASC", new String[]{});
        // db.close();
        return cursor;

    }




    public void updateTimes(int trialid, long startInterval, long penaltyTariff) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.i("Info", "updateTrial: " + trialid);
        long fastestTime = getFastestTime(trialid);

        String query;
        query = "UPDATE times SET penalty = (elapsedTime - " + fastestTime + ")/(1000 * " + penaltyTariff + ") WHERE trialid = " + trialid;
        db.execSQL(query);
        db.close();
    }

    public void remove(String timeID, int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        int newid = -trialid;
        String query = "UPDATE times SET trialid = " + newid + " WHERE _id = " + timeID;
        Log.i("Query", query);
        // Execute the SQL statement
        db.execSQL(query);
        db.close();
    }
}
