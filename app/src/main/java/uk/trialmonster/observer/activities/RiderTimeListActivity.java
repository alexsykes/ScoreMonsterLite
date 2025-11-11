package uk.trialmonster.observer.activities;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

import uk.trialmonster.observer.R;
import uk.trialmonster.observer.TimeListAdapter;
import uk.trialmonster.observer.data.TimeDbHelper;

public class RiderTimeListActivity extends AppCompatActivity {
    ArrayList<HashMap<String, String>> theTimeList;
    SharedPreferences localPrefs;
    private int trialid;
    private TimeDbHelper timeDbHelper;
    RecyclerView timeViewRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rider_time_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();


        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        trialid = localPrefs.getInt("trialid", -999);
        timeDbHelper = new TimeDbHelper(this);
//        long fastestTime = timeDbHelper.getFastestTime(trialid);

        populateTimeList();
    }

    private void populateTimeList() {
        theTimeList = timeDbHelper.getAllRiderTimes(trialid);
        timeViewRV = findViewById(R.id.riderTimeView);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        timeViewRV.setLayoutManager(llm);
        timeViewRV.setHasFixedSize(true);
        initializeAdapter();
        timeDbHelper.close();
    }
    private void initializeAdapter() {
        TimeListAdapter adapter = new TimeListAdapter(theTimeList, timeDbHelper.getFastestTime(trialid));
        timeViewRV.setAdapter(adapter);
    }
}