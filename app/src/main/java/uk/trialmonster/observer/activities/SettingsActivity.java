package uk.trialmonster.observer.activities;
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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import uk.trialmonster.observer.R;
import uk.trialmonster.observer.data.ScoreDbHelper;
import uk.trialmonster.observer.data.TimeDbHelper;
import uk.trialmonster.observer.data.TrialDbHelper;

//TODO - update trialList following login - done?
// TODO - reset maual switch following trial selected

public class SettingsActivity extends AppCompatActivity {
    boolean isOnline;
    SharedPreferences localPrefs;
    ArrayList<HashMap<String, String>> theTrialData;
    public ArrayList<HashMap<String, String>> theTrialList;
    ArrayList<HashMap<String, String>> options;
    TrialDbHelper mDbHelper;
    TextView statusLine;

    int loggedInUserID;

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
        loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
        // Get saved trial data
        mDbHelper = new TrialDbHelper(this);

        populateTrialList(loggedInUserID);
        statusLine = findViewById(R.id.statusLine);
        statusLine.setVisibility(View.GONE);
//        statusLine.setText(SettingsFragment.getTrialDetails());
    }

    private void populateTrialList(int loggedInUserID) {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        theTrialList = mDbHelper.getTrialList(loggedInUserID);
        options = mDbHelper.getPrefsOptions(loggedInUserID);

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
        String email, username, password, observer, mobile, trialName;
        int trialid;
        int section;
        int numsections;
        int numlaps;
        int mode;
        int ridingNumber;
        int loggedInUserID;
        long startInterval;
        long penaltyTariff;
        boolean timeMode, isManualTrial, isLoggedInUser;

        public static String getTrialDetails() {

            return "Trial: ";
        }

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
            section = localPrefs.getInt("section", 0);
            numsections = localPrefs.getInt("numsections", 0);
            numlaps = localPrefs.getInt("numlaps", 0);
            ridingNumber = localPrefs.getInt("ridingNumber", 0);
            penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
            startInterval = localPrefs.getLong("startInterval", 60);
            mode = localPrefs.getInt("mode", 0);
            timeMode = localPrefs.getBoolean("timeMode", false);
            trialid = localPrefs.getInt("trialid", 0);
            trialName = localPrefs.getString("trialName", "");
            email = localPrefs.getString("email", "");
            isManualTrial = localPrefs.getBoolean("isManualTrial", true);
            isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
            loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
            username = localPrefs.getString("username", "");
            password = localPrefs.getString("password", "");
            observer = localPrefs.getString("observer", "");
            mobile = localPrefs.getString("mobile", "");

            if (isManualTrial) {
                trialid = -999;
            }

            SharedPreferences.Editor editor = localPrefs.edit();

            editor.putLong("startInterval", startInterval);
            editor.putLong("penaltyTariff", penaltyTariff);
            editor.putInt("section", section);
            editor.putInt("trialid", trialid);
            editor.remove("startIntervalText");
            editor.remove("penaltyText");
            editor.apply();


            ListPreference trialListPref = findPreference("theTrialIndex");
            EditTextPreference usernamePref = findPreference("username");
            EditTextPreference passwordPref = findPreference("password");
            EditTextPreference mobilePref = findPreference("mobile");
            EditTextPreference trialNamePref = findPreference("trialName");
            EditTextPreference startIntervalPref = findPreference("startIntervalText");
            EditTextPreference penaltyTariffPref = findPreference("penaltyTariffText");
            EditTextPreference ridingNumberPref = findPreference("riderText");
            EditTextPreference sectionPref = findPreference("sectionText");
            EditTextPreference numSectionsPref = findPreference("numsectionsText");
            EditTextPreference numLapsPref = findPreference("numlapsText");
            EditTextPreference emailPref = findPreference("email");
            EditTextPreference observerPref = findPreference("observer");
            SwitchPreference manualTrialSwitchPref = findPreference("manualTrial");
            SwitchPreference restartClockSwitchPref = findPreference("restart_clock_preference");
            SwitchPreference resetScoresSwitchPref = findPreference("reset_scores_preference");
            SwitchPreference resetTimesSwitchPref = findPreference("reset_times_preference");
            SwitchPreference timeModeSwitchPref = findPreference("timeMode");
            SwitchPreference advancedSwitchPref = findPreference("show_advanced");
            PreferenceCategory loginPrefCategory = findPreference("loginPrefCategory");

            // observer pref
            assert observerPref != null;
            if (!observer.equals("")) {
                observerPref.setTitle("Observer: " + observer);
            }
            observerPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS));
            observerPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    observer = String.valueOf(newValue).trim();
                    observerPref.setTitle("Observer: " + observer);
                    observerPref.setText(observer);
                    editor.putString("observer", observer);
                    editor.apply();
                    return false;
                }
            });

            // mobile pref
            assert mobilePref != null;
            if (!mobile.equals("")) {
                mobilePref.setTitle("Contact number: " + mobile);
            }
            mobilePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_PHONE));
            mobilePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    mobile = String.valueOf(newValue).trim();
                    mobilePref.setTitle("Contact number: " + mobile);
                    mobilePref.setText(mobile);
                    editor.putString("mobile", mobile);
                    editor.apply();
                    return false;
                }
            });

            // Selected section
            assert sectionPref != null;
            String sectionsRange = "1 to " + numsections;
            sectionPref.setDialogMessage(sectionsRange);
            sectionPref.setText(String.valueOf(section));
            sectionPref.setTitle("Section: " + section);

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
                        sectionPref.setTitle("Section: " + newValue);
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

            // Manual trial pref
            assert manualTrialSwitchPref != null;
            manualTrialSwitchPref.setVisible(isLoggedInUser);
            manualTrialSwitchPref.setChecked(isManualTrial);
            manualTrialSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    boolean isManualTrial = Boolean.valueOf(newValue.toString());

                    Log.i("info", "Manual trial preference changed: ");
                    manualTrialSwitchPref.setChecked(isManualTrial);
                    numSectionsPref.setVisible(isManualTrial);
                    numLapsPref.setVisible(isManualTrial);
                    trialNamePref.setVisible(isManualTrial);
                    emailPref.setVisible(isManualTrial);
                    trialListPref.setVisible(!isManualTrial);
                    if (isManualTrial) {
                        trialid = -999;

                        SharedPreferences.Editor editor = localPrefs.edit();
                        editor.putBoolean("isManualTrial", isManualTrial);
                        editor.putInt("trialid", trialid);
                        editor.apply();
                    }
                    return false;
                }
            });

            //            trialName pref
            assert trialNamePref != null;
            trialNamePref.setVisible(true);
            trialNamePref.setTitle("Trial: " + trialName);
            trialNamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    trialName = String.valueOf(newValue).trim();
                    trialNamePref.setTitle("Trial: " + trialName);
                    trialNamePref.setText(trialName);
                    editor.putString("trialName", trialName);
                    return false;
                }
            });

            //TODO
            // Settings - move
            trialNamePref.setVisible(isManualTrial);
            trialListPref.setVisible(!isManualTrial);
            numLapsPref.setVisible(isManualTrial);
            numSectionsPref.setVisible(isManualTrial);
            emailPref.setVisible(isManualTrial);
            sectionPref.setVisible(true);
            loginPrefCategory.setVisible(false);

//            Username preference
            assert usernamePref != null;
            usernamePref.setVisible(false);
            usernamePref.setText(username);
            usernamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    // Add check to empty return
                    username = newValue.toString().trim();
                    if (username.length() == 0) {
                        Log.i("Note", "Empty");
                        return true;
                    }
                    editor.putString("username", username);
                    usernamePref.setText(username);
                    editor.apply();

                    if (password != "") {
                        checkLogin(password, username);
                    }
                    return false;
                }
            });


//            Password preference
            assert passwordPref != null;
            passwordPref.setVisible(false);
            passwordPref.setText(password);
            passwordPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

            passwordPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    // Add check to empty return
                    if (newValue.toString().trim().length() == 0) {
                        Log.i("Note", "Empty");
                        return false;
                    }
                    password = newValue.toString().trim();
                    editor.putString("password", password);
                    passwordPref.setText(password);
                    editor.apply();
                    if (username != "") {
                        checkLogin(password, username);
                    }
                    return false;
                }
            });

            // startInterval pref
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
            assert ridingNumberPref != null;
            Boolean show = (mode == 1);
            ridingNumberPref.setVisible(show);
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

            // numsections pref
            assert numSectionsPref != null;

            numSectionsPref.setTitle("Number of sections: " + numsections);
            numSectionsPref.setText(String.valueOf(numsections));
            numSectionsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numSectionsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numSectionsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    numsections = Integer.parseInt(newValue.toString());
                    numSectionsPref.setTitle("Number of sections: " + numsections);
                    String sectionsRange = "1 to " + numsections;
                    sectionPref.setDialogMessage(sectionsRange);
                    editor.putInt("numsections", numsections);
                    editor.apply();
                    return false;
                }
            })
            ;

            // numlaps pref
            assert numLapsPref != null;

            numLapsPref.setText(String.valueOf(numlaps));
            numLapsPref.setTitle("Number of laps: " + numlaps);
            numLapsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numLapsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numLapsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int numlaps = Integer.parseInt(newValue.toString());
                    numLapsPref.setTitle("Number of laps: " + numlaps);
                    editor.putInt("numlaps", numlaps);
                    editor.apply();
                    return false;
                }
            })
            ;

            // email pref
            assert emailPref != null;
            emailPref.setTitle("Email: " + email);
            emailPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
            emailPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    email = newValue.toString();
                    emailPref.setTitle("Email: " + email);
                    emailPref.setText(email);
                    editor.putString("email", email);
                    editor.apply();
                    return false;
                }
            });


            // Timer reset pref
            assert restartClockSwitchPref != null;
            restartClockSwitchPref.setVisible(false);

            restartClockSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "Time reset changed: ");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                    builder.setTitle("Warning - Timing clock will be reset");
                    builder.setIcon(R.drawable.ic_warning_red_48dp);

                    builder.setCancelable(false);
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                        }
                    });
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Clock restart
                            SharedPreferences.Editor editor = localPrefs.edit();
                            editor.putLong("clockStartTime", 0);
                            editor.apply();
                        }
                    });
                    builder.show();
                    return false;
                }
            });

            // Score reset pref
            assert resetScoresSwitchPref != null;
            resetScoresSwitchPref.setVisible(false);

            resetScoresSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "Time reset changed: ");
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

                    alert.setTitle("Warning - Score data will be destroyed");
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
            assert resetTimesSwitchPref != null;
            resetTimesSwitchPref.setVisible(false);

            resetTimesSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.i("info", "Time reset changed: ");
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

                    alert.setTitle("Warning - Time data will be destroyed");
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
            assert timeModeSwitchPref != null;


            // Advanced mode
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
                    loginPrefCategory.setVisible(goAhead);
                    usernamePref.setVisible(goAhead);
                    passwordPref.setVisible(goAhead);
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
                    sectionPref.setVisible(!isTimeMode);
//                    numSectionsPref.setVisible(!isTimeMode);
//                    numLapsPref.setVisible(!isTimeMode);
                    return false;
                }
            });

            // trialList pref
            int trialid = 0;
            if (trialListPref.getValue() != null) {
                trialid = Integer.valueOf(trialListPref.getValue());
            }

            trialListPref.setTitle("Trial: " + trialName);
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
                    String trialName = theTrialData.get("name");
                    String club = theTrialData.get("club");

                    trialListPref.setTitle("Trial: " + trialName);

                    editor.putString("theTrialIndex", newValue.toString());  // Check if this is necessary
                    editor.putBoolean("trialHasChanged", true);
                    editor.putBoolean("isManualTrial", false);
                    editor.putInt("trialid", trialid);
                    editor.putInt("numsections", numsections);
                    editor.putInt("numlaps", numlaps);
                    editor.putInt("mode", mode);
                    editor.putString("date", date);
                    editor.putString("email", email);
                    editor.putString("trialName", trialName);
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
                    } else {
                        emailPref.setVisible(false);
                    }
                    // Setup modes
                    ridingNumberPref.setVisible(mode == 1);
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
            // Electronic scoring = 2
            ridingNumberPref.setVisible(mode == 1);
        }

        private void checkLogin(String newValue, String username) {
            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            String URL = "https://android.trialmonster.uk/joomlaAuth.php";

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d("Volley", "Response: " + response);
                    SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int id = Integer.parseInt(response);
                    editor.putInt("loggedInUserID", id);
                    editor.putBoolean("isLoggedInUser", id != 0);
                    isLoggedInUser = (id != 0);
                    SwitchPreference manualTrialSwitchPref = findPreference("manualTrial");
                    manualTrialSwitchPref.setVisible(isLoggedInUser);
                    manualTrialSwitchPref.setChecked(isManualTrial);
                    editor.commit();

                    String message = "You are not logged in - check your username and password";
                    if (id != 0) {
                        message = "You are now logged in as " + username;
                    }
                    Toast.makeText(getContext(), message,
                            Toast.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    params.put("username", username);
                    params.put("password", newValue);
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    return params;
                }
            };

            Log.d("string", stringRequest.toString());
            int MY_SOCKET_TIMEOUT_MS = 5000;

            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(stringRequest);
        }
    }
}