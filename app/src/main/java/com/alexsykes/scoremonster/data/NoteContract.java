package com.alexsykes.scoremonster.data;

import android.provider.BaseColumns;
import androidx.appcompat.app.AppCompatActivity;

public class NoteContract extends AppCompatActivity {

    private NoteContract() {}

    public static final class NoteEntry implements BaseColumns {
        public final static String TABLE_NAME = "notes";

        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_NOTE_OBSERVER = "observer";
        public final static String COLUMN_NOTE_NOTE = "note";
        public final static String COLUMN_NOTE_CREATED = "created";
        public final static String COLUMN_NOTE_SECTION = "section";
    }
}
