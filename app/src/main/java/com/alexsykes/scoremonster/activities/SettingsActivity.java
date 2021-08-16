package com.alexsykes.scoremonster.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.R;

public class SettingsActivity extends AppCompatActivity {
    boolean isOnline;
    SharedPreferences localPrefs;


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

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = localPrefs.edit();
//        isOnline = isOnline();
//        editor.putBoolean("canConnect", isOnline);
//        editor.apply();
        super.onStart();
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
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

            CharSequence[] entries = localPrefs.getString("theNames","Manual Entry").split(",");
            CharSequence[] entryValues = localPrefs.getString("theIds", "0").split(",");
            ListPreference lp = findPreference("theTrialIndex");
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

            ListPreference lp = findPreference("theTrialIndex");
            lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int trialid = Integer.parseInt(lp.getValue());
                    editor.putString("theTrialIndex", newValue.toString());
                    editor.putBoolean("trialHasChanged", true);
                    editor.putInt("trialid",trialid);
                    editor.apply();
                    return true;
                }
            });
        }
    }
}