package com.alexsykes.scoremonster.data;

import android.provider.BaseColumns;

import androidx.appcompat.app.AppCompatActivity;

public class TrialContract extends AppCompatActivity {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private TrialContract() {
    }

    /**
     * Inner class that defines constant values for the trials database table.
     * Each entry in the table represents a single trials.
     */
    public static final class TrialEntry implements BaseColumns {

        /**
         * Name of database table for trials
         */
        public final static String TABLE_NAME = "trials";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_TRIAL_EMAIL = "email";
        public final static String COLUMN_TRIAL_NAME = "name";
        public final static String COLUMN_TRIAL_TRIALID = "trialid";
        public final static String COLUMN_TRIAL_NUMSECTIONS = "numsections";
        public final static String COLUMN_TRIAL_NUMLAPS = "numlaps";
        public final static String COLUMN_TRIAL_DATE = "date";
        public final static String COLUMN_TRIAL_MODE = "mode";
        public final static String COLUMN_TRIAL_CLUB = "club";
        public final static String COLUMN_TRIAL_INTERVAL = "startinterval";
    }


}
