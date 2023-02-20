package com.alexsykes.scoremonster.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Score score);

    @Query("Delete FROM score_table")
    void deleteAll();

    @Query("SELECT * FROM score_table ORDER BY _id DESC")
    List<Score> getAllScores();

    @Query("SELECT * FROM score_table WHERE rider = :rider AND trialid = :trialid ORDER BY lap ASC")
    List<Score> getCurrentRiderScores(int rider, int trialid);
}
