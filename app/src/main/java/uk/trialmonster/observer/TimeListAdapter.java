package uk.trialmonster.observer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import uk.trialmonster.observer.activities.TimeListActivity;

public class TimeListAdapter extends RecyclerView.Adapter<TimeListAdapter.TimeHolder> {
    ArrayList<HashMap<String, String>> theTimes;
    HashMap<String, String> theTime;

    public TimeListAdapter(ArrayList<HashMap<String, String>> theTimes) {
        this.theTimes = theTimes;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public TimeHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Point to data holder layout
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rider_time_row, viewGroup, false);
        TimeHolder timeHolder = new TimeHolder(v);
        return timeHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TimeHolder timeHolder, final int i) {
        // Populate TextViews with data
        theTime = theTimes.get(i);

        SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm:ss");
        String finishTimeString = timeFormat.format(Long.valueOf(theTime.get("finishTime")));



        timeHolder.riderNumber.setText(theTime.get("number"));
        timeHolder.time.setText(finishTimeString);
        timeHolder.delay.setText(theTime.get("elapsedTime"));
        timeHolder.penalty.setText(theTime.get("penalty"));

        // if (i % 2 != 0) timeHolder.itemView.setBackgroundColor(R.color.purple_100);
    }

    @Override
    public int getItemCount() {
        return theTimes.size();
    }

    public static class TimeHolder extends RecyclerView.ViewHolder {
        TextView riderNumber;
        TextView time;
        TextView delay;
        TextView penalty;

        public TimeHolder(@NonNull View itemView) {
            super(itemView);
            riderNumber = itemView.findViewById(R.id.riderNumber);
            time = itemView.findViewById(R.id.time);
            delay = itemView.findViewById(R.id.delay);
            penalty = itemView.findViewById(R.id.penalty);
        }
    }

}

