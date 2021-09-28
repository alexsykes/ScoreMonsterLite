package com.alexsykes.scoremonster.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.R;

public class TimerResetActivity extends AppCompatActivity {
    CheckBox confirmResetTimer, confirmDeleteScores;
    SwitchCompat deleteTimes, resetTimer;
    Button resetButton;
    SharedPreferences localPrefs;

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

        // Get prefs
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        deleteTimes = findViewById(R.id.deleteTimes);
        resetTimer = findViewById(R.id.resetTimer);
        resetButton = findViewById(R.id.resetButton);
        deleteTimes.setChecked(false);
        resetTimer.setChecked(false);
        resetButton.setEnabled(false);


        resetTimer.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    resetButton.setEnabled(true);
                } else if (!deleteTimes.isChecked()) {
                    resetButton.setEnabled(false);
                }
            }
        });

        deleteTimes.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    resetButton.setEnabled(true);
                } else if (!resetTimer.isChecked()) {
                    resetButton.setEnabled(false);
                }
            }
        });
    }

    public void reset(View view) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setCancelable(true);
        confirmDialog.setTitle("Confirm reset?");
        confirmDialog.setIcon(R.drawable.ic__warning_48);

        confirmDialog.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Reset Timer
                if (resetTimer.isChecked() == true) {
                    Log.i("Info", "Reset timer");
                    // SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    editor.putLong("clockStartTime", 0);
                    editor.apply();
                }

                // Reset Times
                if (deleteTimes.isChecked() == true) {
                    Log.i("Info", "Delete times");
                }

                TimerResetActivity.this.finish();
            }
        });

        confirmDialog.setNegativeButton("Cancel", new CancelOnClickListener());
        confirmDialog.show();
    }

    private class CancelOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Toast.makeText(getApplicationContext(), "Reset cancelled!",
                    Toast.LENGTH_SHORT).show();
        }
    }
}