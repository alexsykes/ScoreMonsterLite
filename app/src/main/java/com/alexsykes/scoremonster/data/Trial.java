package com.alexsykes.scoremonster.data;

public class Trial {
    int _id;
    int numsections;
    int numlaps;
    int scoringmode;

    public Trial(int _id, int numsections, int numlaps, String email, String date, String name, int scoringmode) {
        this._id = _id;
        this.numsections = numsections;
        this.numlaps = numlaps;
        this.email = email;
        this.date = date;
        this.name = name;
        this.scoringmode = scoringmode;
    }

    public int getScoringMode() {
        return scoringmode;
    }

    String email, date, name;

    public void setScoringMode(int scoringMode) {
        this.scoringmode = scoringMode;
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
