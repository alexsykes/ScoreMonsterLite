package uk.trialmonster.observer.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

public class ScoreDbHelper extends SQLiteOpenHelper {
    private static final int SYNCED = 0;
    private static final int NOT_SYNCED = -1;
    SharedPreferences localPrefs;
    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "monster.db";
    private static final String TAG = "Info";
    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 2;

    /**
     * Constructs a new instance of {@link ScoreDbHelper}.
     *
     * @param context of the app
     */
    public ScoreDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Get Score Details
    public ArrayList<HashMap<String, String>> getScoreList(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores WHERE trialid = " + trialid + " AND score NOT NULL " +
                "ORDER BY updated DESC";
//       Log.i("Query", query);
        //  String query = "SELECT * FROM scores  ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();
            scores.put("id", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry._ID)));
            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("lap", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_LAP)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("section", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION)));
            scores.put("trialid", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_TRIALID)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC)));
            scores.put("edited", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_EDITED)));
            scoreList.add(scores);
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    // Used in RecyclerView Score List

    public ArrayList getScores() {
        String section, rider, lap, score, _id, observer, created, sync;

        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<Score> theScores = new ArrayList<>();

        String query = "SELECT section, rider, lap, score, _id, observer, sync, created FROM scores ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        Score theScore;

        while (cursor.moveToNext()) {
            section = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION));
            rider = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER));
            lap = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_LAP));
            score = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE));
            observer = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_OBSERVER));
            created = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_CREATED));
            sync = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC));
            _id = cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry._ID));

            theScore = new Score(section, score, rider, lap, Integer.parseInt(_id), observer, created, sync);
            theScores.add(theScore);
        }
        cursor.close();
        db.close();
        return theScores;
    }

    public void clearResults(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query  = "DELETE FROM scores";
        db.execSQL(query);
        db.close();
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

            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("scoredata", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCOREDATA)));
            scores.put("count", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_COUNT)));
            scores.put("total", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_TOTAL)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC)));
            scoreList.add(scores);
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    // method to count lap number for current rider

    public int getRiderLap(int rider, int section, int trialid) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query =
                "SELECT score AS numLaps FROM scores WHERE score IS NOT NULL AND rider = " + rider + " AND section = " + section + " AND trialid = " + trialid;
        Cursor cursor = db.rawQuery(query, null);
        int numLaps = cursor.getCount();
        cursor.close();

        db.close();
        return numLaps;
    }


    public void markAsDone(int trialid) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE scores SET sync = " + SYNCED + " WHERE sync = " + NOT_SYNCED + " AND trialid = " + trialid;
        db.execSQL(query);
        db.close();
    }

    public Cursor getAll(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.rawQuery("SELECT  * FROM scores WHERE  trialid=" + id, new String[]{});
        db.close();
        return result;
    }

    public Cursor getScoresForEmail(int trialid) {
        String rider, section, scores, observer;

        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<ScoreData> theScores = new ArrayList<>();
//        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT rider, section, GROUP_CONCAT(score, '') AS scores, day FROM " +
                "(SELECT " +
                "score, " +
                "section, rider, lap, day FROM scores WHERE trialid = " + trialid +
                " ORDER BY rider, " +
                "section, " +
                "day, lap) GROUP BY  section, rider, day ORDER BY section, rider, lap ASC";
        Cursor cursor = db.rawQuery(sql, new String[]{});
        return cursor;
    }

//    public void update(String scoreid, String score) {
//
//        SQLiteDatabase db = this.getReadableDatabase();
//
//        String query = "UPDATE scores SET score = '" + score + "', edited = 1, updated = DATETIME" +
//                "('now'), sync = " + NOT_SYNCED + " WHERE _id = " + scoreid;
//        db.execSQL(query);
//        db.close();
//    }


    public void lapseScores(int trialid, int section, int dayNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        int newid = -trialid;
        String query = "UPDATE scores SET trialid = " + newid + " WHERE trialid = " + trialid +
                " AND DAY = " + dayNum + " AND section = " + section;
//        Log.i("Query", query);
        // Execute the SQL statement
        db.execSQL(query);
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public ArrayList<HashMap<String, String>> getScoreListForUpload(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores WHERE trialid = " + trialid + " AND sync = -1 ORDER " +
                "BY _id DESC";
//        Log.i("Query", query);
        //  String query = "SELECT * FROM scores  ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();
            scores.put("id", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry._ID)));
            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("lap", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_LAP)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("section", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION)));
            scores.put("trialid", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_TRIALID)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC)));
            scores.put("edited", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_EDITED)));
            scores.put("created",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_CREATED)));
            scores.put("updated",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_UPDATED)));
            scoreList.add(scores);
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    public JSONArray getTrialData(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        JSONArray trialDetail = new JSONArray();
        String query = "SELECT * FROM trials WHERE trialid = " + trialid ;
        Cursor cursor = db.rawQuery(query, null);
        String id = cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry._ID));
        String name = cursor.getString(cursor.getColumnIndex(TrialContract.TrialEntry.COLUMN_TRIAL_NAME));
//        trialDetail.add(cursor.getString(cursor.getColumnIndex(ScoreEntry._ID));
        trialDetail.put(id);
        trialDetail.put(name);

        cursor.close();
        db.close();
        return trialDetail;
    }

    public ArrayList<HashMap<String, String>> getNewScoreListForUpload(int trialid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores WHERE trialid = " + trialid + " AND sync = -1 ORDER " +
                "BY _id DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();
            scores.put("id", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry._ID)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("updated",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_UPDATED)));
            scores.put("created",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_CREATED)));
            scoreList.add(scores);
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    public ArrayList<HashMap<String, String>> getScoreList(int trialid, int day, int section) {

        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores WHERE trialid = " + trialid +
                " AND section = " + section +
                " AND day = " + day +
                " AND score != \".\" " +
                " ORDER BY updated DESC";
//        String query = "SELECT * FROM scores WHERE trialid = " + trialid +
//                " AND section = " + section +
//                " AND day = " + day +
//                " AND sync = -1 " +
//                " ORDER BY updated DESC";
//       Log.i("Query", query);
        //  String query = "SELECT * FROM scores  ORDER BY _id DESC";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();
            scores.put("id", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry._ID)));
            scores.put("rider", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER)));
            scores.put("lap", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_LAP)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("section", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION)));
            scores.put("trialid", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_TRIALID)));
            scores.put("sync", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SYNC)));
            scores.put("edited", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_EDITED)));
            scores.put("day",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_DAY)));
            scoreList.add(scores);
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    public ArrayList<HashMap<String, String>> getNewScoreListForUpload(int trialid, int section, int day) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> scoreList = new ArrayList<>();
        String query = "SELECT * FROM scores WHERE trialid = " + trialid +
                " AND day = " + day +
                " AND section = " + section +
                " AND sync = -1 ORDER " +
                "BY _id DESC";

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> scores = new HashMap<>();
            scores.put("id", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry._ID)));
            scores.put("score", cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE)));
            scores.put("updated",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_UPDATED)));
            scores.put("created",
                    cursor.getString(cursor.getColumnIndex(ScoreContract.ScoreEntry.COLUMN_SCORE_CREATED)));
            scoreList.add(scores);
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    public void markAsDone(int trialid, int day, int section) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE scores SET sync = " + SYNCED +
                " WHERE  trialid = " + trialid +
                " AND section = " + section +
                " AND day = " + day;
        db.execSQL(query);
        db.close();
    }

    public void enterInAll(int trialid, int day, int section) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE scores SET score  = '" + "1" +
                "', sync = " + NOT_SYNCED + " WHERE  trialid = " + trialid +
                " AND section = " + section +
                " AND day = " + day;
        db.execSQL(query);
        db.close();
    }

    public int getRiderLap(int rider, int section, int trialid, int day) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query =
                "SELECT score AS numLaps FROM scores WHERE score != \".\"" +
                        " AND rider = " + rider +
                        " AND section = " + section +
                        " AND day = " + day +
                        " AND trialid = " + trialid;
        Cursor cursor = db.rawQuery(query, null);
        int numLaps = cursor.getCount();
        cursor.close();

//        db.close();
        return numLaps;
    }

    public void update(String scoreid, String score, String observer) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "";
        if (score.equals("N")) {
            query = "UPDATE scores SET score = NULL, edited = 1," +
                    " observer = '" + observer +
                    "', updated = DATETIME" +
                    "('now'), sync = " + NOT_SYNCED + " WHERE _id = " + scoreid;
        } else {
            query = "UPDATE scores SET score = '" + score + "', edited = 1," +
                    " observer = '" + observer +
                    "', updated = DATETIME" +
                    "('now'), sync = " + NOT_SYNCED + " WHERE _id = " + scoreid;
        }
        db.execSQL(query);
        db.close();
    }

    public Cursor getScoresForSaving(int trialid, int section) {
//        ArrayList<HashMap<String, String>> theScoreList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT rider, sectionscores FROM  (SELECT rider, " +
                "section, " +
                "GROUP_CONCAT(score, '') AS sectionscores FROM scores " +
                "WHERE trialid = " + trialid + " AND section = " + section +
                " GROUP BY rider ORDER by rider, " +
                "lap) " +
                "GROUP BY rider";

        Cursor cursor = db.rawQuery(sql, null);

//        if (cursor.moveToFirst()) {
//            do {
//                HashMap<String, String> scores = new HashMap<>();
//                scores.put("rider", cursor.getString(0));
//                scores.put("scores", cursor.getString(1));
////                theScoreList.add(scores);
//            } while (cursor.moveToNext());
//        }
//        cursor.close();
        return cursor;
    }


    public void deleteAllTrials() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM trials";
        Log.i("Query", query);
        // Execute the SQL statement
        db.execSQL(query);
//        db.close();
    }

    public ArrayList<HashMap<String, String>> dumpTrialScores(int trialid) {
        ArrayList<HashMap<String, String>> theScoreList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT rider, GROUP_CONCAT(sectionscores, '|') FROM  (SELECT rider, " +
                "section, " +
                "GROUP_CONCAT(score, '') AS sectionscores FROM scores " +
                "WHERE trialid = " + trialid + " GROUP BY rider, section ORDER by rider, section, " +
                "lap) " +
                "GROUP BY rider";

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> scores = new HashMap<>();
                scores.put("rider", cursor.getString(0));
                scores.put("scores", cursor.getString(1));
                theScoreList.add(scores);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return theScoreList;
    }

    public void deleteScore(String idToDelete, int numlaps, String observer) {
        SQLiteDatabase db = this.getWritableDatabase();

//      Get entry details
        int rider, section, day, trialid, lap;
        String getScoreIds =
                "SELECT rider, trialid, day, section, lap FROM scores WHERE _id = " + idToDelete;
        Cursor theEntry = db.rawQuery(getScoreIds, null);
        if (theEntry.moveToFirst()) {
            rider = theEntry.getInt(0);
            trialid = theEntry.getInt(1);
            day = theEntry.getInt(2);
            section = theEntry.getInt(3);
            lap = theEntry.getInt(4);

//          Shuffle later laps into nulled score
            for (int lapToMove = lap; lapToMove < numlaps; lapToMove++) {
                Log.i(TAG, "LapToMove: " + lapToMove);
                int source = lapToMove + 1;
                String query = "UPDATE scores SET updated = DATETIME('now'), edited = 1, sync = " +
                        "-1, score = (SELECT score FROM scores WHERE trialid" +
                        " = " + trialid + " AND rider = " + rider + " AND section = " + section +
                        " AND lap = " + source +
                        ") WHERE trialid = " + trialid + " AND rider = " + rider + " AND section " +
                        "= " + section + " AND lap = " + lapToMove;

                db.execSQL(query);
//                Log.i(TAG, "query: " + query);
            }

//          then delete removed score
            String fillLastLapQuery =
                    "UPDATE scores SET updated = DATETIME('now'), edited = 1, sync = -1 , score =" +
                            " " +
                            "'.'" + " " +
                            "WHERE trialid" +
                            " = " + trialid + " AND rider = " + rider + " AND section = " + section +
                            " AND lap = " + numlaps;
            Log.i(TAG, "query: " + fillLastLapQuery);
            db.execSQL(fillLastLapQuery);
        }
        theEntry.close();
        db.close();
    }
}