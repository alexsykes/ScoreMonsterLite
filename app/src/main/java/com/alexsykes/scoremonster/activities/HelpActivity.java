package com.alexsykes.scoremonster.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;
import android.text.Html;

import com.alexsykes.scoremonster.R;

public class HelpActivity extends AppCompatActivity {
    TextView helpTextView;
    String helpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        helpTextView = findViewById(R.id.helpApp);
        helpText = getResources().getString(R.string.tsr22SP);
        helpTextView.setText(Html.fromHtml((helpText)));
    }
}