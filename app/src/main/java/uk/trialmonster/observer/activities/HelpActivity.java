package uk.trialmonster.observer.activities;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import uk.trialmonster.observer.R;

public class HelpActivity extends AppCompatActivity {
    TextView helpTextView;
    WebView webView;
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
        webView = findViewById(R.id.webView);
        webView.setVisibility(View.VISIBLE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://trialmonster.uk/index.php?option=com_content&view=article&id=9");
    }

    public void showPrivacy(View view) {
        webView.loadUrl("https://trialmonster.uk/index.php?option=com_content&view=article&id=9");
        webView.getSettings().setJavaScriptEnabled(true);
    }

    public void showHelp(View view) {
        webView.loadUrl("https://trialmonster.uk/index.php?option=com_content&view=article&id=12");
        webView.getSettings().setJavaScriptEnabled(true);
    }

    public void showNS(View view) {
        webView.loadUrl("https://trialmonster.uk/index.php?option=com_content&view=article&id=11");
        webView.getSettings().setJavaScriptEnabled(true);
    }

    public void showSP(View view) {
        webView.loadUrl("https://trialmonster.uk/index.php?option=com_content&view=article&id=10");
        webView.getSettings().setJavaScriptEnabled(true);
    }
}