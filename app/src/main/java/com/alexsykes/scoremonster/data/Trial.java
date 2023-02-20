package com.alexsykes.scoremonster.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trials")
public class Trial {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    int _id;
    int numsections, numlaps, mode;
    long startinterval;
    String email, date, name, club;

    public Trial(int _id, int numsections, int numlaps, int mode, String email, String date, String name, String club, long startinterval) {
        this._id = _id;
        this.numsections = numsections;
        this.numlaps = numlaps;
        this.mode = mode;
        this.email = email;
        this.date = date;
        this.name = name;
        this.club = club;
        this.startinterval = startinterval;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setNumsections(int numsections) {
        this.numsections = numsections;
    }

    public int getNumlaps() {
        return numlaps;
    }

    public void setNumlaps(int numlaps) {
        this.numlaps = numlaps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
