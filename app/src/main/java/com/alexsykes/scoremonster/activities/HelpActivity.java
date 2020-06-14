package com.alexsykes.scoremonster.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.text.Html;

import com.alexsykes.scoremonster.R;

public class HelpActivity extends AppCompatActivity {
    TextView helpTextView;
    String helpText;
    Button nsButton, helpButton, spButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        helpTextView = findViewById(R.id.helpApp);
        helpButton = findViewById(R.id.helpButton);
        nsButton = findViewById(R.id.nonStopButton);
        spButton = findViewById(R.id.stopPermittedButton);
        // helpButton.setEnabled(false);
        //  helpButton.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        helpTextView.setText(Html.fromHtml(getString(R.string.helpApp)));
    }

    public void showHelp(View view) {
        helpTextView = findViewById(R.id.helpApp);
        helpTextView.setText(Html.fromHtml(getString(R.string.helpApp)));
    }

    public void showNS(View view) {
        helpTextView = findViewById(R.id.helpApp);
        helpTextView.setText(Html.fromHtml(getString(R.string.tsr22NS)));
    }

    public void showSP(View view) {
        helpTextView = findViewById(R.id.helpApp);
        helpTextView.setText(Html.fromHtml(getString(R.string.tsr22SP)));
    }
}