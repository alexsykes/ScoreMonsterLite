package com.alexsykes.scoremonster.activities;
// TODO - check section validation following read from trialPref
// TODO - check riderNumber on change/lauch in mode 2
// TODO - update numsections and numlaps filed on initial load of trial

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.data.ScoreDbHelper;
import com.alexsykes.scoremonster.data.TimeDbHelper;
import com.alexsykes.scoremonster.data.TrialDbHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    boolean isOnline;
    SharedPreferences localPrefs;
    ArrayList<HashMap<String, String>> theTrialData;
    public ArrayList<HashMap<String, String>> theTrialList;
    ArrayList<HashMap<String, String>> options;
    TrialDbHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        isOnline = isOnline();
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Get saved trial data
        mDbHelper = new TrialDbHelper(this);
        populateTrialList();
    }

    private void populateTrialList() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        theTrialList = mDbHelper.getTrialList();
        options = mDbHelper.getPrefsOptions();

        editor.putString("theIds", options.get(0).get("ids"));
        editor.putString("theNames", options.get(0).get("names"));
        editor.apply();
        mDbHelper.close();
    }

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline);
        editor.apply();
        super.onStart();
    }
    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SharedPreferences localPrefs;
        String sectionPrefText, email;
        int trialid;
        int section;
        int numsections;
        int numlaps;
        int mode;
        int ridingNumber;
        long startInterval;
        long penaltyTariff;
        boolean timeMode;

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
            assert lp != null;
            lp.setEntries(entries);
            lp.setEntryValues(entryValues);
        }

        private void setup() {
            // Setup known values
            section = localPrefs.getInt("section", 1);
            numsections = localPrefs.getInt("numsections", 1);
            numlaps = localPrefs.getInt("numlaps", 1);
            ridingNumber = localPrefs.getInt("ridingNumber", 1);
            penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
            startInterval = localPrefs.getLong("startInterval", 60);
            mode = localPrefs.getInt("mode", 0);
            timeMode = localPrefs.getBoolean("timeMode", false);
            trialid = localPrefs.getInt("trialid", 0);
            email = localPrefs.getString("email", "");

            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putLong("startInterval", startInterval);
            editor.putLong("penaltyTariff", penaltyTariff);
            editor.putInt("section", section);
            editor.remove("startIntervalText");
            editor.remove("penaltyText");
            editor.apply();

            // mobile pref
            EditTextPreference mobilePref = findPreference("mobile");
            assert mobilePref != null;
            mobilePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_PHONE));

            // startInterval pref
            EditTextPreference startIntervalPref = findPreference("startIntervalText");
            assert startIntervalPref != null;
            startIntervalPref.setVisible(timeMode);
            startIntervalPref.setText(String.valueOf(startInterval));
            startIntervalPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            startIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    long startInterval;                    // Add check to empty return
                    if (newValue.toString().trim().length() == 0) {
                        Log.i("Note", "Empty");
                        return false;
                    }
                    startInterval = Long.parseLong(newValue.toString());
                    editor.putLong("startInterval", startInterval);
                    startIntervalPref.setText(newValue.toString());
                    editor.apply();
                    return false;
                }
            });

            // penaltyTariff pref
            EditTextPreference penaltyTariffPref = findPreference("penaltyTariffText");
            assert penaltyTariffPref != null;
            penaltyTariffPref.setVisible(timeMode);
            penaltyTariffPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            penaltyTariffPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    long penaltyTariff;                    // Add check to empty return
                    if (newValue.toString().trim().length() == 0) {
                        Log.i("Note", "Empty");
                        return false;
                    }
                    penaltyTariff = Long.parseLong(newValue.toString());
                    editor.putLong("penaltyTariff", penaltyTariff);
                    penaltyTariffPref.setText(newValue.toString());
                    editor.apply();
                    return false;
                }
            });

            // ridingNumber pref
            EditTextPreference ridingNumberPref = findPreference("riderText");
            assert ridingNumberPref != null;
            ridingNumberPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            ridingNumberPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int ridingNumber;                    // Add check to empty return
                    if (newValue.toString().trim().length() == 0) {
                        Log.i("Note", "Empty");
                        return false;
                    }
                    ridingNumber = Integer.parseInt(newValue.toString());
                    editor.putInt("ridingNumber", ridingNumber);
                    ridingNumberPref.setText(newValue.toString());
                    editor.apply();
                    return false;
                }
            });

            // section pref
            EditTextPreference sectionPref = findPreference("sectionText");
            numsections = localPrefs.getInt("numsections", 1);
            assert sectionPref != null;
            String sectionsRange = "1 to " + numsections;
            sectionPref.setDialogMessage(sectionsRange);
            sectionPref.setText(String.valueOf(section));

            sectionPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            sectionPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Add check to empty return
                    if (newValue.toString().trim().length() == 0) {
                        Log.i("Note", "Empty");
                        return false;
                    }
                    section = Integer.parseInt(newValue.toString());
                    if (0 < section && section <= numsections) {
                        sectionPref.setText(newValue.toString());
                        editor.putInt("section", section);
//                        editor.putString("sectionText", String.valueOf(section));
                        editor.apply();
                        sectionPref.setIcon(null);
                        return false;
                    } else {
                        sectionPref.setText("Invalid choice");
                        sectionPref.setIcon(R.drawable.ic_warning_red_48dp);
                        return false;
                    }
                }
            });

            // numsections pref
            EditTextPreference numSectionsPref = findPreference("numsectionsText");
            assert numSectionsPref != null;
            Boolean show = !timeMode;
            numSectionsPref.setVisible(show);
            numSectionsPref.setText(String.valueOf(numsections));
            numSectionsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numSectionsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numSectionsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    numsections = Integer.parseInt(newValue.toString());
                    String sectionsRange = "1 to " + numsections;
                    sectionPref.setDialogMessage(sectionsRange);
                    editor.putInt("numsections", numsections);
                    editor.apply();
                    return false;
                }
            })
            ;

            // numlaps pref
            EditTextPreference numLapsPref = findPreference("numlapsText");
            assert numLapsPref != null;
            show = !timeMode;
            numLapsPref.setVisible(show);
            numLapsPref.setText(String.valueOf(numlaps));
            numLapsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numLapsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numLapsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int numlaps = Integer.parseInt(newValue.toString());
                    editor.putInt("numlaps", numlaps);
                    editor.apply();
                    return false;
                }
            })
            ;

            // email pref
            EditTextPreference emailPref = findPreference("email");
            assert emailPref != null;
            emailPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));

            // observer pref
            EditTextPreference observerPref = findPreference("observer");
            assert observerPref != null;
            observerPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS));



            // Timer reset pref
            SwitchPreference restartClockSwitchPref = findPreference("restart_clock_preference");
            assert restartClockSwitchPref != null;
            restartClockSwitchPref.setVisible(false);

            restartClockSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "Time reset changed: ");
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

                    alert.setTitle("Warning");
                    alert.setMessage("Data will be destroyed");
                    alert.setIcon(R.drawable.ic_warning_red_48dp);

                    alert.setCancelable(true);
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    });
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Clock restart
                            SharedPreferences.Editor editor = localPrefs.edit();
                            editor.putLong("clockStartTime", 0);
                            editor.apply();
                        }
                    });
                    alert.show();
                    return false;
                }
            });

            // Score reset pref
            SwitchPreference resetScoresSwitchPref = findPreference("reset_scores_preference");
            assert resetScoresSwitchPref != null;
            resetScoresSwitchPref.setVisible(false);

            resetScoresSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "Time reset changed: ");
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

                    alert.setTitle("Warning");
                    alert.setMessage("Data will be destroyed");
                    alert.setIcon(R.drawable.ic_warning_red_48dp);

                    alert.setCancelable(true);
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    });
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ScoreDbHelper scoreDbHelper = new ScoreDbHelper(getContext());
                            scoreDbHelper.lapseScores(trialid);
                            Log.i("Info", "Delete scores - trialid: " + trialid);
                            dialog.dismiss();
                        }
                    });
                    alert.show();
                    return false;
                }
            });
            
            // Time reset pref
            SwitchPreference resetTimesSwitchPref = findPreference("reset_times_preference");
            assert resetTimesSwitchPref != null;
            resetTimesSwitchPref.setVisible(false);

            resetTimesSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "Time reset changed: ");
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

                    alert.setTitle("Warning");
                    alert.setMessage("Data will be destroyed");
                    alert.setIcon(R.drawable.ic_warning_red_48dp);

                    alert.setCancelable(true);
                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    });
                    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TimeDbHelper timeDbHelper = new TimeDbHelper(getContext());
                            timeDbHelper.lapseTimes(trialid);
                            Log.i("Info", "Delete times - trialid: " + trialid);
                            dialog.dismiss();
                        }
                    });
                    alert.show();
                    return false;
                }
            });

            // timeMode pref
            SwitchPreference timeModeSwitchPref = findPreference("timeMode");
            assert timeModeSwitchPref != null;


            // Advanced mode
            SwitchPreference advancedSwitchPref = findPreference("show_advanced");
            assert advancedSwitchPref != null;
            advancedSwitchPref.setChecked(false);

            advancedSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean goAhead = Boolean.valueOf(newValue.toString());
                    restartClockSwitchPref.setVisible(goAhead);
                    resetScoresSwitchPref.setVisible(goAhead);
                    resetTimesSwitchPref.setVisible(goAhead);
                    advancedSwitchPref.setChecked(goAhead);
                    return false;
                }
            });

            // timeMode pref
            timeModeSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "TimeMode changed: ");
                    boolean isTimeMode = Boolean.valueOf(newValue.toString());

                    timeModeSwitchPref.setChecked(isTimeMode);
                    editor.putBoolean("timeMode", isTimeMode);
                    editor.apply();
                    startIntervalPref.setVisible(isTimeMode);
                    penaltyTariffPref.setVisible(isTimeMode);
                    numSectionsPref.setVisible(!isTimeMode);
                    numLapsPref.setVisible(!isTimeMode);
                    return false;
                }
            });

            // trialid pref
            int trialid = 0;
            ListPreference trialListPref = findPreference("theTrialIndex");
            if (trialListPref.getValue() != null) {
                trialid = Integer.valueOf(trialListPref.getValue());
            }
            if (trialid == 0) {
                Log.i("Note", "Manual Entry selected");
                emailPref.setVisible(true);
                numSectionsPref.setVisible(true);
                numLapsPref.setVisible(true);
            } else {
                emailPref.setVisible(false);
                numSectionsPref.setVisible(false);
                numLapsPref.setVisible(false);
            }

            trialListPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int trialid = Integer.parseInt(newValue.toString());

                    HashMap<String, String> theTrialData;
                    theTrialData = getTrialData(trialid).get(0);
                    numsections = Integer.parseInt(Objects.requireNonNull(theTrialData.get("numsections")));
                    numlaps = Integer.parseInt(Objects.requireNonNull(theTrialData.get("numlaps")));
                    mode = Integer.parseInt(Objects.requireNonNull(theTrialData.get("mode")));
                    startInterval = Long.parseLong(theTrialData.get("startInterval"));
                    String email = theTrialData.get("email");
                    String date = theTrialData.get("date");
                    String name = theTrialData.get("name");
                    String club = theTrialData.get("club");

                    editor.putString("theTrialIndex", newValue.toString());  // Check if this is necessary
                    editor.putBoolean("trialHasChanged", true);
                    editor.putInt("trialid", trialid);
                    editor.putInt("numsections", numsections);
                    editor.putInt("numlaps", numlaps);
                    editor.putInt("mode", mode);
                    editor.putString("date", date);
                    editor.putString("email", email);
                    editor.putString("name", name);
                    editor.putString("club", club);
                    editor.putLong("startInterval", startInterval);
                    editor.apply();
                    Log.i("Note", "Trial selection changed");

                    String sectionsRange = "1 to " + numsections;
                    sectionPref.setDialogMessage(sectionsRange);

                    numLapsPref.setText(String.valueOf(numlaps));
                    numSectionsPref.setText(String.valueOf(numsections));

                    if (trialid == 0) {
                        Log.i("Note", "Manual Entry selected");
                        emailPref.setVisible(true);
                        // numSectionsPref.setVisible(true);
                        // numLapsPref.setVisible(true);
                        // theTrialSettings.setVisible(true);
                    } else {
                        emailPref.setVisible(false);
                        // numSectionsPref.setVisible(false);
                        // numLapsPref.setVisible(false);
                        // theTrialSettings.setVisible(false);
                    }
                    // Setup modes
                    ridingNumberPref.setVisible(mode == 2);
                    sectionPref.setVisible(mode != 4);

                    return true;
                }

                private ArrayList<HashMap<String, String>> getTrialData(int trialid) {
                    ArrayList<HashMap<String, String>> theData;
                    TrialDbHelper trialDbHelper = new TrialDbHelper(getContext());
                    theData = trialDbHelper.getTrialData(trialid);
                    return theData;
                }
            });

            // Setup modes
            ridingNumberPref.setVisible(mode == 2);
            sectionPref.setVisible(mode != 4);

        }
    }
}