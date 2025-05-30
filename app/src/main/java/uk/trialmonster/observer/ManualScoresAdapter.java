package uk.trialmonster.observer;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class ManualScoresAdapter extends RecyclerView.Adapter<ManualScoresAdapter.ViewHolder> {
    private final int numColumns = 3;
    private final int numScores;
    private final ArrayList<HashMap<String, String>> scoreData;
    HashMap<String, String> theScore1, theScore2, theScore3;
    private int numRows;

    public ManualScoresAdapter(ArrayList<HashMap<String, String>> scoreData) {
        this.scoreData = scoreData;
        numScores = scoreData.size();
        numRows = numScores / numColumns;
        if (numScores % numColumns > 0) {
            numRows++;
        }
        Log.i("info", "numScores: " + numScores);
        Log.i("info", "numRows: " + numRows);
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
        int index;

//        First cell always full
        theScore1 = scoreData.get(position);
        holder.riderTV1.setText(theScore1.get("rider"));
        holder.lapScoresTV1.setText(theScore1.get("laps"));

//        index = (position * 3) + 1;
        index = numRows + position;
//        int offset1 = position + numRows;
        if (index < numScores - 1) {
            theScore2 = scoreData.get(index);
            holder.riderTV2.setText(theScore2.get("rider"));
            holder.lapScoresTV2.setText(theScore2.get("laps"));
        } else {
            holder.riderTV2.setText("");
            holder.lapScoresTV2.setText("");
        }

        index = (numRows * 2) + position;
        if (index < numScores) {
            theScore3 = scoreData.get(index);
            holder.riderTV3.setText(theScore3.get("rider"));
            holder.lapScoresTV3.setText(theScore3.get("laps"));
        } else {
            holder.riderTV3.setText("");
            holder.lapScoresTV3.setText("");
        }
    }

    @Override
    public int getItemCount() {
        int numScores = scoreData.size();
        int part1 = numScores / numColumns;
        int mod1 = numScores % numColumns;
        int count = part1 + mod1;
        return count;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView riderTV1;
        TextView lapScoresTV1;
        TextView riderTV2;
        TextView lapScoresTV2;
        TextView riderTV3;
        TextView lapScoresTV3;

        public ViewHolder(View view) {
            super(view);
            riderTV1 = itemView.findViewById(R.id.rider1NumberTextView);
            lapScoresTV1 = itemView.findViewById(R.id.rider1ScoresTextView);
            riderTV2 = itemView.findViewById(R.id.rider2NumberTextView);
            lapScoresTV2 = itemView.findViewById(R.id.rider2ScoresTextView);
            riderTV3 = itemView.findViewById(R.id.rider3NumberTextView);
            lapScoresTV3 = itemView.findViewById(R.id.rider3ScoresTextView);
        }


    }

//        public void bind(){}
}
