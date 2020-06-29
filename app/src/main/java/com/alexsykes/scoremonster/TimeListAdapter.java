package com.alexsykes.scoremonster;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class TimeListAdapter extends RecyclerView.Adapter<TimeListAdapter.ResultHolder> {
    ArrayList<HashMap<String, String>> theResultList;

    public TimeListAdapter(ArrayList<HashMap<String, String>> theResultList) {
        this.theResultList = theResultList;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }


    @NonNull
    @Override
    public ResultHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.time_holder, viewGroup, false);
        TimeListAdapter.ResultHolder resultHolder = new TimeListAdapter.ResultHolder(v);
        return resultHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TimeListAdapter.ResultHolder resultHolder, int i) {

        int backgroundColor = Color.parseColor("#40bdc0d4");
        int white = Color.parseColor("#ffffff");

        // Get time as String
        String ridetime = theResultList.get(i).get("ridetime");

        // Construct new Date
        Date date = new Date(Long.valueOf(ridetime));

        // Define formatting and apply
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("H:mm:ss");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateUTC = dateFormatGmt.format(date);

        // Populate TextViews with data
        resultHolder.numberView.setText(theResultList.get(i).get("number"));
        resultHolder.nameView.setText(theResultList.get(i).get("name"));
        resultHolder.timeView.setText(dateUTC);
        resultHolder.penaltyView.setText(theResultList.get(i).get("timepenalty"));

        if (i % 2 != 0) {
            resultHolder.itemView.setBackgroundColor(backgroundColor);
        } else {
            resultHolder.itemView.setBackgroundColor(white);
        }
    }

    @Override
    public int getItemCount() {
        return theResultList.size();
    }

    class ResultHolder extends RecyclerView.ViewHolder {

        TextView numberView;
        TextView nameView;
        TextView timeView;
        TextView penaltyView;

        ResultHolder(@NonNull View itemView) {
            super(itemView);
            // Instantiate fields to be populated
            numberView = itemView.findViewById(R.id.numberView);
            nameView = itemView.findViewById(R.id.nameView);
            timeView = itemView.findViewById(R.id.timeView);
            penaltyView = itemView.findViewById(R.id.penaltyView);
        }
    }
}
