package com.alexsykes.scoremonster;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class ScoreListAdapter extends RecyclerView.Adapter<ScoreListAdapter.ScoreHolder> {


    ArrayList<HashMap<String, String>> theSummaryScores;

    public ScoreListAdapter(ArrayList<HashMap<String, String>> theSummaryScores) {
        this.theSummaryScores = theSummaryScores;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public ScoreHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.score_holder, viewGroup, false);
        ScoreHolder scoreViewHolder = new ScoreHolder(v);
        return scoreViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreHolder scoreHolder, int i) {

        int backgroundColor = Color.parseColor("#40bdc0d4");
        int white = Color.parseColor("#ffffff");

        // Populate TextViews with data
        scoreHolder.riderTextView.setText(theSummaryScores.get(i).get("rider"));
        scoreHolder.scoresTextView.setText(theSummaryScores.get(i).get("scores"));
        scoreHolder.totalTextView.setText(theSummaryScores.get(i).get("total"));
        scoreHolder.lapsTextView.setText(theSummaryScores.get(i).get("laps"));

        if (i % 2 != 0) {
            scoreHolder.itemView.setBackgroundColor(backgroundColor);
        } else {
            scoreHolder.itemView.setBackgroundColor(white);
        }
    }

    @Override
    public int getItemCount() {
        return theSummaryScores.size();
    }

    public static class ScoreHolder extends RecyclerView.ViewHolder {

        TextView riderTextView;
        TextView scoresTextView;
        TextView totalTextView;
        TextView lapsTextView;

        public ScoreHolder(@NonNull View itemView) {
            super(itemView);

            // Instantiate fields to be populated
            riderTextView = itemView.findViewById(R.id.riderTextView);
            scoresTextView = itemView.findViewById(R.id.scoresTextView);
            totalTextView = itemView.findViewById(R.id.totalTextView);
            lapsTextView = itemView.findViewById(R.id.lapsTextView);
        }
    }
}

