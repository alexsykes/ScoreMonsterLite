package com.alexsykes.scoremonster.data;

import android.provider.BaseColumns;
import androidx.appcompat.app.AppCompatActivity;

public class ScoreContract extends AppCompatActivity {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ScoreContract() {}

    /**
     * Inner class that defines constant values for the scores database table.
     * Each entry in the table represents a single scores.
     */
    public static final class ScoreEntry implements BaseColumns {

        /** Name of database table for scores */
        public final static String TABLE_NAME = "scores";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_SCORE_SECTION ="section";
        public final static String COLUMN_SCORE_SCORE = "score";
        public final static String COLUMN_SCORE_RIDER = "rider";
        public final static String COLUMN_SCORE_LAP = "lap";
        public final static String COLUMN_SCORE_OBSERVER = "observer";
        public final static String COLUMN_SCORE_TRIALID = "trialid";
        public final static String COLUMN_SCORE_CREATED = "created";
        public final static String COLUMN_SCORE_UPDATED = "updated";
        public final static String COLUMN_SCORE_EDITED = "edited";
        public final static String COLUMN_SCORE_TOTAL = "total";
        public final static String COLUMN_SCORE_SCOREDATA = "scoredata";
        public final static String COLUMN_SCORE_SYNC = "sync";
        public final static String COLUMN_SCORE_COUNT = "count";
    }
}
