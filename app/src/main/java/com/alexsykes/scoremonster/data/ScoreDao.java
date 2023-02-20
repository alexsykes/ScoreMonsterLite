package com.alexsykes.scoremonster.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Score score);

    @Query("Delete FROM scores")
    void deleteAll();

    @Query("SELECT * FROM scores ORDER BY _id DESC")
    LiveData<List<Score>> getAllScores();

    @Query("SELECT * FROM scores WHERE rider = :rider AND trialid = :trialid ORDER BY lap ASC")
    LiveData<List<Score>> getCurrentRiderScores(int rider, int trialid);
}
