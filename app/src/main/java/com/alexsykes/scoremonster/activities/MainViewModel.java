package com.alexsykes.scoremonster.activities;

import android.os.AsyncTask;
import android.widget.Toast;

import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

class MainViewModel extends ViewModel {
    int trialid;
    int numsections;
    int numlaps;


    public void setNumLaps(int numlaps) {
        this.numlaps = numlaps;
    }
    public void setNumSections(int numsections) {
        this.numlaps = numlaps;
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
