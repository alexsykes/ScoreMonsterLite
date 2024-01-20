package uk.trialmonster.observer.activities;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import uk.trialmonster.observer.R;

public class HelpActivity extends AppCompatActivity {
    TextView helpTextView;
    String helpText;
    Button nsButton, helpButton, spButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        helpTextView = findViewById(R.id.helpApp);
        helpButton = findViewById(R.id.helpButton);
        nsButton = findViewById(R.id.nonStopButton);
        spButton = findViewById(R.id.stopPermittedButton);
        // helpButton.setEnabled(false);
        //  helpButton.setBackgroundColor(getResources().getColor(R.color.colorWhite));
        helpTextView.setText(Html.fromHtml(getString(R.string.helpApp)));

/*        LinearLayout scrollView = findViewById(R.id.layout);
        // Create TextView programmatically.
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setGravity(Gravity.CENTER);
        textView.setText("Added");
        scrollView.addView(textView);
        ImageView mainScreenView = new ImageView(this);
        mainScreenView.setMaxHeight(12);
        mainScreenView.setImageDrawable(getResources().getDrawable(R.drawable.main_screen, null));
        scrollView.addView(mainScreenView);*/

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