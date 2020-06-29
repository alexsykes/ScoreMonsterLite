package com.alexsykes.scoremonster;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alexsykes.scoremonster.activities.SyncActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class ScoreListSyncAdapter extends RecyclerView.Adapter<ScoreListSyncAdapter.ScoreHolder> {
    ArrayList<HashMap<String, String>> theScores;
    HashMap<String, String> theScore;
    OnItemClickListener listener;

    public ScoreListSyncAdapter(ArrayList<HashMap<String, String>> theScores) {
        this.theScores = theScores;
    }

    public ScoreListSyncAdapter(ArrayList<HashMap<String, String>> theScores, OnItemClickListener listener) {
        this.theScores = theScores;
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public ScoreHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        // Point to data holder layout
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.score_row, viewGroup, false);
        ScoreHolder scoreHolder = new ScoreHolder(v);
        return scoreHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreHolder scoreHolder, final int i) {
        // Populate TextViews with data
        theScore = theScores.get(i);
        String syncState;
        if(theScore.get("sync").equals("-1")){
            syncState = "Pending";
        } else {
            syncState = "OK";
        }

        int backgroundColor = Color.parseColor("#40bdc0d4");
        int white = Color.parseColor("#ffffff");

        scoreHolder.score.setText(theScore.get("score"));
        scoreHolder.lap.setText(theScore.get("lap"));
        scoreHolder.rider.setText(theScore.get("rider"));
        scoreHolder.rider.setText(theScore.get("rider"));
        scoreHolder.trial.setText(theScore.get("trialid"));
        // scoreHolder.trial.setText("trial");
        scoreHolder.sync.setText(syncState);
        scoreHolder.bind(theScore, listener);

        if (i % 2 != 0) {
            scoreHolder.itemView.setBackgroundColor(backgroundColor);
        } else {
            scoreHolder.itemView.setBackgroundColor(white);
        }
    }

    @Override
    public int getItemCount() {
        return theScores.size();
    }

    public interface OnItemClickListener {
        void onItemClick(HashMap<String, String> theScores);
    }

    public static class ScoreHolder extends RecyclerView.ViewHolder {
        TextView rider;
        TextView lap;
        TextView score;
        TextView sync;
        TextView trial;

        public ScoreHolder(@NonNull View itemView) {
            super(itemView);
            score = itemView.findViewById(R.id.score);
            lap = itemView.findViewById(R.id.lap);
            rider = itemView.findViewById(R.id.rider);
            sync = itemView.findViewById(R.id.sync);
            trial = itemView.findViewById(R.id.trial);
        }

        public void bind(final HashMap<String, String> theScore, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = theScore.get("id");
                    String score = theScore.get("score");
                    int scoreIndex;
                    scoreIndex = 0;
                    switch (score) {
                        case "0":
                            scoreIndex = 0;
                            break;
                        case "1":
                            scoreIndex = 1;
                            break;
                        case "2":
                            scoreIndex = 2;
                            break;
                        case "3":
                            scoreIndex = 3;
                            break;
                        case "5":
                            scoreIndex = 4;
                            break;
                        case "10":
                            scoreIndex = 5;
                            break;
                    }

                    Context context = v.getContext();
                    ((SyncActivity) context).onClickCalled(id, scoreIndex);
                }
            });
        }
    }
}
