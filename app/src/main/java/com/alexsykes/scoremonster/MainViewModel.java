package com.alexsykes.scoremonster;

import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private int trialid;
    private int numsections;
    private int numlaps;


    public MainViewModel() {
    }

    public MainViewModel(int trialid, int numsections, int numlaps) {
        this.trialid = trialid;
        this.numsections = numsections;
        this.numlaps = numlaps;
    }

    public void setNumLaps(int numlaps) {
        this.numlaps = numlaps;
    }

    public void setNumSections(int numsections) {
        this.numsections = numsections;
    }

    public int getTrialid() {
        return trialid;
    }

    public int getNumsections() {
        return numsections;
    }

    public int getNumlaps() {
        return numlaps;
    }

    public void setTrialid(int trialid) {
        this.trialid = trialid;
    }
}
