package com.alexsykes.scoremonster.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Score.class}, version = 1, exportSchema = false)
public abstract class ScoreRoomDatabase extends RoomDatabase {
    public static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private static volatile ScoreRoomDatabase INSTANCE;
    private static final RoomDatabase.Callback sScoreDatabaseCallback =
            new RoomDatabase.Callback() {

                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);


                    databaseWriteExecutor.execute(() -> {
                        ScoreDao scoreDao = INSTANCE.scoreDao();
                        scoreDao.deleteAll();
                    });

                }
            };

    static ScoreRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ScoreRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ScoreRoomDatabase.class,
                                    "score_database")
                            .addCallback(sScoreDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    abstract ScoreDao scoreDao();
}
