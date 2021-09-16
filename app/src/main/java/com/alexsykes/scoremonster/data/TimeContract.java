package com.alexsykes.scoremonster.data;

import android.provider.BaseColumns;

import androidx.appcompat.app.AppCompatActivity;

public class TimeContract extends AppCompatActivity {
    private TimeContract() {
    }

    /**
     * Inner class that defines constant values for the trials database table.
     * Each entry in the table represents a single trials.
     */
    public static final class TimeEntry implements BaseColumns {
        public final static String TABLE_NAME = "times";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_TIME_NUMBER = "number";
        public final static String COLUMN_TIME_FINISHTIME = "finishTime";
        public final static String COLUMN_TIME_ELAPSEDTIME = "elapsedTime";
        public final static String COLUMN_TIME_CREATED = "created";
        public final static String COLUMN_TIME_TRIALID = "trialid";
    }
}
