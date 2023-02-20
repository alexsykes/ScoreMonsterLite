package com.alexsykes.scoremonster.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "times")
public class Time {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    int _id;
    String number,
            sequence,
            created;
    int trialid;

    @Ignore
    public Time(Integer _id, String number, String sequence, String created, int trialid) {
        this._id = _id;
        this.number = number;
        this.sequence = sequence;
        this.created = created;
        this.trialid = trialid;
    }

    public Time(String number, String sequence, String created) {
        this.number = number;
        this.sequence = sequence;
        this.created = created;
    }

    public int getTrialid() {
        return trialid;
    }

    public void setTrialid(int trialid) {
        this.trialid = trialid;
    }

    public Integer get_id() {
        return _id;
    }

    public void set_id(Integer _id) {
        this._id = _id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }
}
