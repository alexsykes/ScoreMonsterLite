package com.alexsykes.scoremonster;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SummaryListAdapter extends RecyclerView.Adapter<SummaryListAdapter.ResultHolder> {
    ArrayList<HashMap<String, String>> theResultList;

    public SummaryListAdapter(ArrayList<HashMap<String, String>> theResultList) {
        this.theResultList = theResultList;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public ResultHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.summary_row, viewGroup, false);
        SummaryListAdapter.ResultHolder resultHolder = new SummaryListAdapter.ResultHolder(v);
        return resultHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ResultHolder resultHolder, int i) {

        int backgroundColor = Color.parseColor("#40bdc0d4");
        int white = Color.parseColor("#ffffff");

        // Populate TextViews with data
        //   resultHolder.sectionView.setText(theResultList.get(i).get("section".toString()));
        resultHolder.riderView.setText(theResultList.get(i).get("rider"));
        resultHolder.nameView.setText(theResultList.get(i).get("scores"));
        resultHolder.totalView.setText(theResultList.get(i).get("laps"));
        resultHolder.courseView.setText(theResultList.get(i).get("total"));

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

        TextView nameView;
        TextView totalView;
        TextView riderView;
        TextView courseView;

        ResultHolder(@NonNull View itemView) {
            super(itemView);
            // Instantiate fields to be populated
            nameView = itemView.findViewById(R.id.scoreView);
            totalView = itemView.findViewById(R.id.totalView);
            riderView = itemView.findViewById(R.id.riderView);
            courseView = itemView.findViewById(R.id.lapsView);
        }
    }
}
