package uk.trialmonster.observer.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import uk.trialmonster.observer.LapScoresAdapter;
import uk.trialmonster.observer.R;
import uk.trialmonster.observer.data.ScoreDbHelper;

public class SummaryActivity extends AppCompatActivity {
    ArrayList<HashMap<String, String>> theScoreList;
    RecyclerView summaryRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_summary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        ScoreDbHelper scoreDbHelper = new ScoreDbHelper(this);
        theScoreList = scoreDbHelper.getLapScores(1, 112);
        summaryRV = findViewById(R.id.summaryRV);
//        LinearLayoutManager llm = new LinearLayoutManager(this);
//        summaryRV.setLayoutManager(llm);
        GridLayoutManager glm;
        glm = new GridLayoutManager(this, 3);
        summaryRV.setLayoutManager(glm);
        summaryRV.setHasFixedSize(true);
        initializeAdapter();
        scoreDbHelper.close();

    }

    private void initializeAdapter() {
        LapScoresAdapter adapter = new LapScoresAdapter(theScoreList);
        summaryRV.setAdapter(adapter);
    }
}