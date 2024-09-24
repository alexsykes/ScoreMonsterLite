package uk.trialmonster.observer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class LapScoresAdapter extends RecyclerView.Adapter<LapScoresAdapter.ViewHolder> {
    HashMap<String, String> theScore;
    private final ArrayList<HashMap<String, String>> scoreData;

    public LapScoresAdapter(ArrayList<HashMap<String, String>> scoreData) {
        this.scoreData = scoreData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lap_score_row, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        theScore = scoreData.get(position);
        holder.riderTV.setText(theScore.get("rider"));
        holder.lapScoresTV.setText(theScore.get("laps"));
    }

    @Override
    public int getItemCount() {
        return scoreData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView riderTV;
        TextView lapScoresTV;

        public ViewHolder(View view) {
            super(view);
            riderTV = itemView.findViewById(R.id.riderNumberTextView);
            lapScoresTV = itemView.findViewById(R.id.riderScoresTextView);
        }


    }

//        public void bind(){}
}
