package com.alexsykes.scoremonster.data;

import android.provider.BaseColumns;
import androidx.appcompat.app.AppCompatActivity;

public class EntryContract extends AppCompatActivity {
    private EntryContract() {}


    public static final class EntryEntry implements BaseColumns {

        public final static String TABLE_NAME = "entries";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_ENTRY_SURNAME = "surname";
        public final static String COLUMN_ENTRY_FIRSTNAME = "firstname";
        public final static String COLUMN_ENTRY_ADDRESS = "address";
        public final static String COLUMN_ENTRY_POSTCODE = "postcode";
        public final static String COLUMN_ENTRY_TELEPHONE = "telephone";
        public final static String COLUMN_ENTRY_DOB = "dob";
        public final static String COLUMN_ENTRY_EMAIL = "email";
        public final static String COLUMN_ENTRY_CLUB = "club";
        public final static String COLUMN_ENTRY_ACU = "acu";
        public final static String COLUMN_ENTRY_COURSE = "course";
        public final static String COLUMN_ENTRY_CLASS = "class";
        public final static String COLUMN_ENTRY_MAKE = "make";
        public final static String COLUMN_ENTRY_SIZE = "size";
        public final static String COLUMN_ENTRY_TYPE = "type";
        public final static String COLUMN_ENTRY_ISYOUTH = "isYouth";
        public final static String COLUMN_ENTRY_NUMBER = "number";
        public final static String COLUMN_ENTRY_GUARDIAN = "guardian";
        public final static String COLUMN_ENTRY_GUARDIANADDRESS = "guardianAddress";
        public final static String COLUMN_ENTRY_CREATED = "created";

        public final static String COLUMN_ENTRY_COUNT = "count";
    }
}