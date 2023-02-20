package com.alexsykes.scoremonster.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Time time);

    @Query("Delete FROM times")
    void deleteAll();

    @Query("SELECT * FROM times ORDER BY _id DESC")
    LiveData<List<Time>> getAllTimes();
}
