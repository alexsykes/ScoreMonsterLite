package uk.trialmonster.observer.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import uk.trialmonster.observer.LapScoresAdapter;
import uk.trialmonster.observer.R;
import uk.trialmonster.observer.data.ScoreDbHelper;

public class InfoActivity extends AppCompatActivity {
    ArrayList<HashMap<String, String>> theScoreList;
    RecyclerView lapScoresRV;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScoreDbHelper scoreDbHelper = new ScoreDbHelper(this);
        theScoreList = scoreDbHelper.getLapScores(1, 112);

        lapScoresRV = findViewById(R.id.lapScoresRV);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        lapScoresRV.setLayoutManager(llm);
        lapScoresRV.setHasFixedSize(true);
        initializeAdapter();
        scoreDbHelper.close();
    }

    private void initializeAdapter() {
        LapScoresAdapter adapter = new LapScoresAdapter(theScoreList);
        lapScoresRV.setAdapter(adapter);
    }
}
