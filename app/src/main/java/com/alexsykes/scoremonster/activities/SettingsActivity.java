package com.alexsykes.scoremonster.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    boolean isOnline;
    SharedPreferences localPrefs;
    ProgressDialog dialog = null;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialList;
    private static final String BASE_URL = "https://android.trialmonster.uk/";
    int trialid, section, numsections, numlaps, ridingNumber, numberInGroup;
    String observer, theTrialName, detail, email, scoringmode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SharedPreferences localPrefs;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setup();
            setTrials();
        }

        private void setTrials() {
            localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            CharSequence[] entries = localPrefs.getString("theNames","").split(",");
            CharSequence[] entryValues = localPrefs.getString("theIds","").split(",");
            ListPreference lp = (ListPreference)findPreference("thetrialid");
            lp.setEntries(entries);
            lp.setEntryValues(entryValues);
        }

        private void setup() {
            EditTextPreference mobilePref = findPreference("mobile");
            assert mobilePref != null;
            mobilePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_PHONE));

            EditTextPreference sectionPref = findPreference("sectionText");
            assert sectionPref != null;
            sectionPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            EditTextPreference numSectionsPref = findPreference("numsectionsText");
            assert numSectionsPref != null;
            numSectionsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            EditTextPreference numLapsPref = findPreference("numlapsText");
            assert numLapsPref != null;
            numLapsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            EditTextPreference trialidPref = findPreference("trialid");
            assert trialidPref != null;
            trialidPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            EditTextPreference emailPref = findPreference("email");
            assert emailPref != null;
            emailPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));

            EditTextPreference observerPref = findPreference("observer");
            assert observerPref != null;
            observerPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS));
        }
                                                                                    }

    // Imported from earlier Activity

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        isOnline = isOnline();
        editor.putBoolean("canConnect", isOnline);
        editor.apply();
        super.onStart();
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}