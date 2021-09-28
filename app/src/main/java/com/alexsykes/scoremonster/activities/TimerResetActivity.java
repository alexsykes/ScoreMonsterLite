package com.alexsykes.scoremonster.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.alexsykes.scoremonster.R;

public class TimerResetActivity extends AppCompatActivity {
    CheckBox confirmResetTimer, confirmDeleteScores;
    SwitchCompat deleteTimes, resetTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_reset);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        confirmResetTimer = findViewById(R.id.confirmTimeReset);
        confirmDeleteScores = findViewById(R.id.confirmDeleteTimes);
        deleteTimes = findViewById(R.id.deleteTimes);
        resetTimer = findViewById(R.id.resetTimer);
        deleteTimes.setChecked(false);
        resetTimer.setChecked(false);

        confirmResetTimer.setVisibility(View.GONE);
        confirmDeleteScores.setVisibility(View.GONE);

        resetTimer.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    confirmResetTimer.setVisibility(View.VISIBLE);
                } else {
                    confirmResetTimer.setVisibility(View.GONE);
                }
            }
        });

        deleteTimes.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    confirmDeleteScores.setVisibility(View.VISIBLE);
                } else {
                    confirmDeleteScores.setVisibility(View.GONE);
                }
            }
        });


    }
}