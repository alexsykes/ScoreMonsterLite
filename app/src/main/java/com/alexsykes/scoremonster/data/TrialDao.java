package com.alexsykes.scoremonster.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TrialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Trial trial);

    @Query("Delete FROM trials")
    void deleteAll();

    @Query("SELECT * FROM trials ORDER BY _id DESC")
    List<Trial> getAllTrials();
}
