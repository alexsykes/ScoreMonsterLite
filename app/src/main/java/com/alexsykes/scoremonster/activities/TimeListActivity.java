package com.alexsykes.scoremonster.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
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
    TextView statusLine;
    private long clockStartTime, penaltyTariff, startInterval, fastestTime;
    private long baseTime;

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
        clockStartTime = localPrefs.getLong("clockStartTime", 0);
        penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
        startInterval = localPrefs.getLong("startInterval", 60);

        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss");
        String dateString = dateFormat.format(clockStartTime);

        statusLine = findViewById(R.id.statusLine);

        // Get data
        trialid = localPrefs.getInt("trialid", -999);
        dbHelper = new TimeDbHelper(this);
        dbHelper.updateTimes(trialid, startInterval, penaltyTariff);
        fastestTime = dbHelper.getFastestTime(trialid);
        baseTime = (fastestTime / 1000) + 1;

        long elapsedTime = fastestTime / 1000;
        long minutes = elapsedTime / 60;
        long seconds = elapsedTime % 60;

        String secondsString = "00" + seconds;
        secondsString = secondsString.substring(secondsString.length() - 2);
        String elapsedTimeString = minutes + ":" + secondsString;
        statusLine.setText("Standard time: " + elapsedTimeString);
        populateTimeList(baseTime);
    }

    private void populateTimeList(long baseTime) {
        theTimeList = dbHelper.getTimeList(trialid);
        timeView = findViewById(R.id.timeView);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        timeView.setLayoutManager(llm);
        timeView.setHasFixedSize(true);
        initializeAdapter();
    }

    private void initializeAdapter() {
        TimeListAdapter adapter = new TimeListAdapter(theTimeList);
        timeView.setAdapter(adapter);

        // Start
        // on below line we are creating a method to create item touch helper
        // method for adding swipe to delete functionality.
        // in this we are specifying drag direction and position to right
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // this method is called
                // when the item is moved.

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // this method is called when we swipe our item to right direction.
                // on below line we are getting the item at a particular position.
                // RecyclerData deletedCourse = theTimeList.get(viewHolder.getAdapterPosition());

                // below line is to get the position
                // of the item at that position.
                int position = viewHolder.getAdapterPosition();
                String timeID = theTimeList.get(position).get("id");
                // this method is called when item is swiped.
                // below line is to remove item from our array list.
                theTimeList.remove(viewHolder.getAdapterPosition());
                dbHelper.remove(timeID, trialid);
                adapter.notifyItemRangeRemoved(position, 1);
                adapter.notifyDataSetChanged();
            }
            // at last we are adding this
            // to our recycler view.
        }).attachToRecyclerView(timeView);


        // End

    }

    public static class TimeHolder extends RecyclerView.ViewHolder {
        TextView riderTextView, finishTimeTextView, elapsedTimeTextView, timePenaltyTextView;

        public TimeHolder(@NonNull View itemView) {
            super(itemView);
            riderTextView = itemView.findViewById(R.id.rider);
            finishTimeTextView = itemView.findViewById(R.id.finishTime);
            elapsedTimeTextView = itemView.findViewById(R.id.elapsedTime);
            timePenaltyTextView = itemView.findViewById(R.id.marksLost);
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
            long deltaTime;
            double fastestTimeInSeconds = fastestTime / 1000;
            theTime = theTimeList.get(position);
            String timeID = theTime.get("id");

            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a");
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss");
            String finishTimeString = timeFormat.format(Long.valueOf(theTime.get("finishTime")));
            // Get elapsedTime, then convert to seconds and minutes
            long elapsedTime = Long.valueOf(theTime.get("elapsedTime")) / 1000;
            long minutes = elapsedTime / 60;
            long seconds = elapsedTime % 60;

            String secondsString = "00" + seconds;
            secondsString = secondsString.substring(secondsString.length() - 2);
            String elapsedTimeString = minutes + ":" + secondsString;

            deltaTime = (long) Math.ceil((elapsedTime - fastestTimeInSeconds) / 60);
            // Calculate lost marks
            // deltaTime = elapsedTime - baseTime;
            // penalties = deltaTime/penaltyTariff;
            holder.riderTextView.setText(theTime.get("rider"));
            holder.finishTimeTextView.setText(finishTimeString);
            holder.elapsedTimeTextView.setText(elapsedTimeString);
            holder.timePenaltyTextView.setText(String.valueOf(deltaTime));
        }

        @Override
        public int getItemCount() {
            return theTimeList.size();
        }
    }
}