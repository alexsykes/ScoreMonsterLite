package com.alexsykes.scoremonster.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.data.TimeDbHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class TimeListActivity extends AppCompatActivity {
    RecyclerView timeView;
    ArrayList<HashMap<String, String>> theTimeList;
    SharedPreferences localPrefs;
    HashMap<String, String> theTime;
    private TimeDbHelper dbHelper;
    private int trialid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        // Get shared preferences for trialid, section
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get data
        trialid = localPrefs.getInt("trialid", -999);
        dbHelper = new TimeDbHelper(this);
        populateTimeList();

    }

    private void populateTimeList() {
        theTimeList = dbHelper.getTimeList(trialid);
        Log.i("trialid", "" + trialid);
        timeView = findViewById(R.id.timeView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        timeView.setLayoutManager(llm);
        timeView.setHasFixedSize(true);
        initializeAdapter();
    }

    private void initializeAdapter() {
        TimeListAdapter adapter = new TimeListAdapter(theTimeList);
        timeView.setAdapter(adapter);
    }

    public static class TimeHolder extends RecyclerView.ViewHolder {
        TextView rider, finishTime, elapsedTime;

        public TimeHolder(@NonNull View itemView) {
            super(itemView);
            rider = itemView.findViewById(R.id.rider);
            finishTime = itemView.findViewById(R.id.finishTime);
            elapsedTime = itemView.findViewById(R.id.elapsedTime);
        }
    }

    private class TimeListAdapter extends RecyclerView.Adapter<TimeHolder> {
        ArrayList<HashMap<String, String>> theTimeList;
        HashMap<String, String> theTime;

        public TimeListAdapter(ArrayList<HashMap<String, String>> theTimeList) {
            this.theTimeList = theTimeList;
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        @NonNull
        @Override
        public TimeHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            // Point to data holder layout
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.time_row, viewGroup, false);
            TimeHolder timeHolder = new TimeHolder(v);
            return timeHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull TimeHolder holder, int position) {
            theTime = theTimeList.get(position);

            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss");
            String finishTimeString = dateFormat.format(Long.valueOf(theTime.get("finishTime")));
            String elapsedTimeString = timeFormat.format(Long.valueOf(theTime.get("elapsedTime")));

            holder.rider.setText(theTime.get("rider"));
            holder.finishTime.setText(finishTimeString);
            holder.elapsedTime.setText(elapsedTimeString);
        }

        @Override
        public int getItemCount() {
            return theTimeList.size();
        }
    }
}