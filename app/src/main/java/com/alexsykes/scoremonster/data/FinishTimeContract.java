package com.alexsykes.scoremonster.data;

import android.provider.BaseColumns;
import android.support.v7.app.AppCompatActivity;

public class FinishTimeContract extends AppCompatActivity {
    public FinishTimeContract(){}

    public static final class FinishTimeEntry implements BaseColumns {

        public static final String TABLE_NAME = "finishTimes";

        public static final String _ID = BaseColumns._ID;
        public final static String COLUMN_FINISHTIME_RIDER = "rider";
        public final static String COLUMN_FINISHTIME_TIME = "finishtime";
        public final static String COLUMN_FINISHTIME_RIDE_TIME = "ridetime";
        public final static String COLUMN_FINISHTIME_SYNC = "sync";
    }
}