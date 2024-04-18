package uk.trialmonster.observer.activities;
// TODO - check section validation following read from trialPref
// TODO - check riderNumber on change/lauch in mode 2
// TODO - update numsections and numlaps filed on initial load of trial

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentResultListener;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import uk.trialmonster.observer.R;
import uk.trialmonster.observer.data.ScoreContract;
import uk.trialmonster.observer.data.ScoreDbHelper;
import uk.trialmonster.observer.data.TimeDbHelper;
import uk.trialmonster.observer.data.TrialDbHelper;

//TODO - update trialList following login - done?
// TODO - reset maual switch following trial selected

public class SettingsActivity extends AppCompatActivity {
    boolean isOnline, isLoggedInUser, manualMode;
    SharedPreferences localPrefs;
    ArrayList<HashMap<String, String>> theTrialData;
    public ArrayList<HashMap<String, String>> theTrialList;
    ArrayList<HashMap<String, String>> options;
    TrialDbHelper mDbHelper;
    TextView statusLine;
    String username, statusLineText;
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
        isOnline = isOnline();
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
        isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
        manualMode = localPrefs.getBoolean("manualMode", true);


        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
            Log.i("Info", "onCreate: ");
        }

//      Added to update section number in statusLine on change
        getSupportFragmentManager().setFragmentResultListener("sectionChange", this,
                new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                // We use a String here, but any type that can be put in a Bundle is supported.
//                String result = bundle.getString("bundleKey");
                int section = bundle.getInt("section", 1);
                int day = localPrefs.getInt("dayNum", 1);
                // Do something with the result.
                String statusLineText = "Section: " + section + " Day: " + day;
                statusLine = findViewById(R.id.statusLine);
                statusLine.setVisibility(View.VISIBLE);
                statusLine.setText(statusLineText);
            }
        });
        // Get saved trial data
        mDbHelper = new TrialDbHelper(this);
        populateTrialList(loggedInUserID);
        int section = localPrefs.getInt("section", 1);
        int day = localPrefs.getInt("dayNum", 1);
        username = localPrefs.getString("username", "");
        if (!manualMode) {
            statusLineText = "Logged in as : " + username;
        } else {
            statusLineText = "You are not logged in. Manual mode ONLY";
        }
        statusLine = findViewById(R.id.statusLine);
        statusLine.setVisibility(View.VISIBLE);
        statusLine.setText(statusLineText);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            String observer = localPrefs.getString("observer", "");
            String email = localPrefs.getString("email", "");
            String trialName = localPrefs.getString("trialName", "");
            String mobile = localPrefs.getString("mobile", "");
            if (observer.equals("") || email.equals("") || mobile.equals("") || trialName.equals("")) {
                Toast.makeText(this, "Additional data needed", Toast.LENGTH_LONG).show();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateTrialList(int loggedInUserID) {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
//        theTrialList = mDbHelper.getTrialList(loggedInUserID);

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
        mDbHelper.close();
    }
    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        public static final String TAG = "Info";
        SharedPreferences localPrefs;
        String email, username, password, observer, mobile, trialName, adminLockPass,
                adminLockNewPass, adminLockConfirmPass, dayText;
        int trialid;
        int section;
        int numsections;
        int numlaps;
        int numdays;
        int dayNum;
        int mode;
        int ridingNumber;
        int loggedInUserID;
        long startInterval;
        long penaltyTariff;
        boolean timeMode, isLoggedInUser, incomplete;

        PreferenceCategory loginPrefCategory = findPreference("loginPrefCategory");

        SwitchPreference advancedSwitchPref = findPreference("show_advanced");
        ListPreference trialListPref = findPreference("theTrialIndex");
        EditTextPreference trialNamePref = findPreference("trialName");
        EditTextPreference numLapsPref = findPreference("numlapsText");
        EditTextPreference numSectionsPref = findPreference("numsectionsText");
        EditTextPreference emailPref = findPreference("email");
        EditTextPreference usernamePref = findPreference("username");
        EditTextPreference passwordPref = findPreference("password");

        EditTextPreference ridingNumberPref = findPreference("riderText");
        SwitchPreference restartClockSwitchPref = findPreference("restart_clock_preference");
        SwitchPreference resetScoresSwitchPref = findPreference("reset_scores_preference");
        SwitchPreference resetTimesSwitchPref = findPreference("reset_times_preference");
//        boolean isManualTrial;

        public static String getTrialDetails() {

            return "Trial: ";
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
            isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
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
            adminLockNewPass = "";
            adminLockConfirmPass = "";
            SharedPreferences.Editor editor = localPrefs.edit();

            EditTextPreference observerPref = findPreference("observer");
            EditTextPreference mobilePref = findPreference("mobile");
            EditTextPreference sectionPref = findPreference("sectionText");
            EditTextPreference adminLockPassPref = findPreference("adminLockPass");
            EditTextPreference adminLockNewPassPref = findPreference("adminLockNewPass");
            EditTextPreference adminLockConfirmPassPref = findPreference("adminLockConfirmPass");

            ListPreference trialListPref = findPreference("theTrialIndex");

            EditTextPreference trialNamePref = findPreference("trialName");
            EditTextPreference numLapsPref = findPreference("numlapsText");
            EditTextPreference daysPref = findPreference("dayText");
            EditTextPreference numSectionsPref = findPreference("numsectionsText");
            EditTextPreference emailPref = findPreference("email");

            SwitchPreference timeModeSwitchPref = findPreference("timeMode");
            EditTextPreference startIntervalPref = findPreference("startIntervalText");
            EditTextPreference penaltyTariffPref = findPreference("penaltyTariffText");

            SwitchPreference advancedSwitchPref = findPreference("show_advanced");
            EditTextPreference usernamePref = findPreference("username");
            EditTextPreference passwordPref = findPreference("password");

            EditTextPreference ridingNumberPref = findPreference("riderText");
            SwitchPreference restartClockSwitchPref = findPreference("restart_clock_preference");
            SwitchPreference resetScoresSwitchPref = findPreference("reset_scores_preference");
            SwitchPreference resetTimesSwitchPref = findPreference("reset_times_preference");
            PreferenceCategory loginPrefCategory = findPreference("loginPrefCategory");
            PreferenceCategory modePrefCategory = findPreference("modePrefCategory");
            PreferenceCategory timingModeCategory = findPreference("timingModeCategory");
            PreferenceCategory trialDetailsCategory = findPreference("trial_details");

            // Setup current values
            section = localPrefs.getInt("section", 1);
            numsections = localPrefs.getInt("numsections", 1);
            numlaps = localPrefs.getInt("numlaps", 1);
            numdays = localPrefs.getInt("numdays", 1);
            ridingNumber = localPrefs.getInt("ridingNumber", 0);
            penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
            startInterval = localPrefs.getLong("startInterval", 60);
            mode = localPrefs.getInt("mode", 0);
            timeMode = localPrefs.getBoolean("timeMode", false);
            trialid = localPrefs.getInt("trialid", 0);
            trialName = localPrefs.getString("trialName", "");
            email = localPrefs.getString("email", "");
//            isManualTrial = localPrefs.getBoolean("isManualTrial", true);
            isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
            loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
            username = localPrefs.getString("username", "");
            password = localPrefs.getString("password", "");
            observer = localPrefs.getString("observer", "");
            mobile = localPrefs.getString("mobile", "");
            incomplete = localPrefs.getBoolean("incomplete", false);
            adminLockPass = localPrefs.getString("adminLockPass", "13151");
            dayText = localPrefs.getString("dayText", "1");
            dayNum = localPrefs.getInt("dayNum", 1);


            if (adminLockPass.length() < 1) {
                adminLockPass = "13151";
            }


//          Initial visibility settings
            observerPref.setVisible(true);
            mobilePref.setVisible(true);
            sectionPref.setVisible(true);
            loginPrefCategory.setVisible(false);
            timingModeCategory.setVisible(false);
            trialDetailsCategory.setVisible(false);
            resetScoresSwitchPref.setVisible(false);
            resetTimesSwitchPref.setVisible(false);
            restartClockSwitchPref.setVisible(false);

            usernamePref.setVisible(true);
            passwordPref.setVisible(true);

            if (!isLoggedInUser) {
                trialid = -999;
                emailPref.setVisible(true);
                trialDetailsCategory.setVisible(true);
            }

            if (isLoggedInUser) {
//                manualTrialSwitchPref.setVisible(false);
                trialListPref.setVisible(true);
                trialNamePref.setVisible(false);
                numLapsPref.setVisible(false);
                numSectionsPref.setVisible(false);
                emailPref.setVisible(false);
            } else {
//                manualTrialSwitchPref.setVisible(true);
                trialListPref.setVisible(false);
                trialNamePref.setVisible(true);
                numLapsPref.setVisible(true);
                numSectionsPref.setVisible(true);
                emailPref.setVisible(true);
            }

            editor.putLong("startInterval", startInterval);
            editor.putLong("penaltyTariff", penaltyTariff);
            editor.putInt("section", section);
            editor.putInt("trialid", trialid);
            editor.remove("startIntervalText");
            editor.remove("penaltyText");
            editor.apply();


            // observer pref
            assert observerPref != null;
//            if (!observer.equals("")) {
//                observerPref.setTitle("Observer: " + observer);
//            }
            observerPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS));
            observerPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    observer = String.valueOf(newValue).trim();
                    observerPref.setTitle("Observer: " + observer);
                    observerPref.setText(observer);
                    editor.putString("observer", observer);

                    if (observer.equals("")) {
                        observerPref.setIcon(R.drawable.ic_baseline_warning_24);
                    } else {
                        observerPref.setIcon(null);
                    }
                    editor.apply();
                    return false;
                }
            });

            // mobile pref
            assert mobilePref != null;
//            if (!mobile.equals("")) {
//                mobilePref.setTitle("Contact number: " + mobile);
//            }
            mobilePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_PHONE));
            mobilePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    mobile = String.valueOf(newValue).trim();
                    mobilePref.setTitle("Contact number: " + mobile);
                    mobilePref.setText(mobile);
                    editor.putString("mobile", mobile);
                    editor.apply();
                    if (mobile.equals("")) {
                        mobilePref.setIcon(R.drawable.ic_baseline_warning_24);
                    } else {
                        mobilePref.setIcon(null);
                    }
                    return false;
                }
            });

//          Day selection
            assert daysPref != null;
            String daysRange = "1 to " + numdays;
            daysPref.setDialogMessage(daysRange);
            daysPref.setTitle("Day: " + dayText);
            if ((dayNum > numdays) || (dayNum == 0)) {
                daysPref.setTitle("Invalid day number: " + dayNum);
                daysPref.setText(dayText);
                daysPref.setIcon(R.drawable.ic_warning_red_48dp);
            }
            daysPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            daysPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Add check to empty return
                    dayText = newValue.toString().trim();

                    if (dayText.equals("")) {
                        daysPref.setIcon(R.drawable.ic_warning_red_48dp);
                        editor.putString("dayText", "1");
                        editor.putInt("dayNum", 1);
                        editor.apply();
                        return false;
                    }
                    dayNum = Integer.parseInt(dayText);
                    if (dayNum == 0 || dayNum > numdays) {

                        daysPref.setIcon(R.drawable.ic_warning_red_48dp);
                        return false;
                    }

                    daysPref.setTitle("Day: " + dayText);
                    daysPref.setIcon(null);

                    editor.putString("dayText", dayText);
                    editor.putInt("dayNum", dayNum);
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
            if ((section > numsections) || (section == 0)) {
                sectionPref.setTitle("Invalid section number: " + section);
                sectionPref.setText("");
                sectionPref.setIcon(R.drawable.ic_warning_red_48dp);
            }

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
                        editor.putBoolean("incomplete", false);
                        sectionPref.setIcon(null);

                        Bundle result = new Bundle();
//                        result.putString("bundleKey", "result");
                        result.putInt("section", section);
                        getParentFragmentManager().setFragmentResult("sectionChange", result);
                    } else {
                        sectionPref.setTitle("Invalid section number: " + section);
                        sectionPref.setText("");
                        sectionPref.setIcon(R.drawable.ic_warning_red_48dp);
                        editor.putBoolean("incomplete", true);
                    }
                    editor.apply();
                    return false;
                }
            });


            //            trialName pref
            assert trialNamePref != null;
//            trialNamePref.setVisible(true);
            trialNamePref.setTitle("Trial: " + trialName);
            trialNamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    trialName = String.valueOf(newValue).trim();
                    trialNamePref.setText(trialName);
                    editor.putString("trialName", trialName);
                    trialNamePref.setTitle("Trial: " + trialName);
                    editor.commit();
                    if (trialName.equals("")) {
                        trialNamePref.setIcon(R.drawable.ic_baseline_warning_24);
                    } else {
                        trialNamePref.setIcon(null);
                    }
                    return false;
                }
            });

            //TODO

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

//          Admin lock password
            assert adminLockPassPref != null;
            adminLockPassPref.setVisible(true);
            adminLockPassPref.setText("");
            adminLockPassPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            adminLockPassPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    // Add check to empty return
                    if (newValue.toString().trim().length() == 0) {
                        Log.i("Note", "Empty");
                        return false;
                    }
                    String returnedValue = newValue.toString().trim();

                    boolean goAhead = returnedValue.equals(adminLockPass);
                    editor.putString("adminLockPass", adminLockPass);
                    editor.apply();
                    adminLockNewPassPref.setVisible(goAhead);

                    trialDetailsCategory.setVisible(goAhead);
                    restartClockSwitchPref.setVisible(goAhead);
                    resetScoresSwitchPref.setVisible(goAhead);
                    resetTimesSwitchPref.setVisible(goAhead);
                    advancedSwitchPref.setChecked(goAhead);
                    loginPrefCategory.setVisible(goAhead);
                    timeModeSwitchPref.setVisible(goAhead);
                    timingModeCategory.setVisible(goAhead);
                    usernamePref.setVisible(goAhead);
                    passwordPref.setVisible(goAhead);
                    return false;
                }
            });

            //          Admin lock password
            assert adminLockNewPassPref != null;
            adminLockNewPassPref.setText("");
            adminLockNewPassPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            adminLockNewPassPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    adminLockNewPass = newValue.toString().trim();
                    adminLockNewPassPref.setVisible(false);
                    adminLockConfirmPassPref.setVisible(true);
                    return false;
                }
            });

            //          Admin lock password
            assert adminLockConfirmPassPref != null;
            adminLockConfirmPassPref.setText("");
            adminLockConfirmPassPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
            adminLockConfirmPassPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    String message = "";
                    adminLockConfirmPass = newValue.toString().trim();
                    if (adminLockNewPass.length() == 0) {
                        message = "Password must not be left empty!";
                    } else if (adminLockNewPass.equals(adminLockConfirmPass)) {
                        Log.i(TAG, "Passwords match");
                        editor.putString("adminLockPass", adminLockNewPass);
                        adminLockPass = adminLockNewPass;
                        editor.apply();
                        message = "Admin password changed to " + adminLockNewPass;
                        adminLockConfirmPassPref.setVisible(false);
                    } else {
                        message = "Passwords do not match!";
                    }
                    Toast.makeText(getContext(), message,
                            Toast.LENGTH_LONG).show();

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
                    if ((section > numsections) || (section == 0)) {
                        sectionPref.setTitle("Invalid section number: " + section);
                        sectionPref.setText("");
                        sectionPref.setIcon(R.drawable.ic_warning_red_48dp);
                    } else {
                        sectionPref.setTitle("Section: " + section);
                        sectionPref.setText(String.valueOf(section));
                        sectionPref.setIcon(null);
                    }
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
                    email = newValue.toString().trim();
                    emailPref.setTitle("Email: " + email);
                    emailPref.setText(email);
//                    editor.putString("email", email);
                    editor.apply();

                    if (email.equals("")) {
                        emailPref.setIcon(R.drawable.ic_baseline_warning_24);
                    } else {
                        emailPref.setIcon(null);
                    }
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
                            scoreDbHelper.lapseScores(trialid, section, dayNum);
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
//            resetTimesSwitchPref.setVisible(false);

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
//            advancedSwitchPref.setChecked(false);
//            advancedSwitchPref.setVisible(false);
            advancedSwitchPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean goAhead = Boolean.valueOf(newValue.toString());
//                    restartClockSwitchPref.setVisible(goAhead);
//                    resetScoresSwitchPref.setVisible(goAhead);
//                    resetTimesSwitchPref.setVisible(goAhead);
//                    advancedSwitchPref.setChecked(goAhead);
//                    loginPrefCategory.setVisible(goAhead);
                    usernamePref.setVisible(goAhead);
                    passwordPref.setVisible(goAhead);
                    timeModeSwitchPref.setVisible(goAhead);
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
                    int oldTrialId = localPrefs.getInt("trialid", 0);
                    int trialid = Integer.parseInt(newValue.toString());


                    if (trialid != oldTrialId) {
//                        Get score data from server
                        getScoreData(trialid);
                    }

                    getScoreData(trialid);

//                    getScoreData(trialid);
                    HashMap<String, String> theTrialData;
                    theTrialData = getTrialData(trialid).get(0);
                    numsections = Integer.parseInt(Objects.requireNonNull(theTrialData.get("numsections")));
                    numdays =
                            Integer.parseInt(Objects.requireNonNull(theTrialData.get("numdays")));
                    numlaps = Integer.parseInt(Objects.requireNonNull(theTrialData.get("numlaps")));

                    if (numsections == 0) {
                        numsections = 1;
                    }
                    if (numdays == 0) {
                        numdays = 1;
                    }
                    if (numlaps == 0) {
                        numlaps = 1;
                    }

                    mode = Integer.parseInt(Objects.requireNonNull(theTrialData.get("mode")));
                    startInterval = Long.parseLong(theTrialData.get("startInterval"));
                    String email = theTrialData.get("email");
                    String date = theTrialData.get("date");
                    String trialName = theTrialData.get("name");
                    String club = theTrialData.get("club");

                    trialListPref.setTitle("Trial: " + trialName);

                    editor.putString("theTrialIndex", newValue.toString());  // Check if this is necessary
                    editor.putBoolean("trialHasChanged", true);
                    editor.putInt("trialid", trialid);
                    editor.putInt("numsections", numsections);
                    editor.putInt("numlaps", numlaps);
                    editor.putInt("numdays", numdays);
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

        private void getScoreData(int trialid) {
            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(getContext());
//            String url = "https://android.trialmonster.uk/getScoreListScoreMonsterLive" +
//                    ".php?trialid=" + 94 + "&section=" + 1 + "&day=" + 1;
            String url = "https://android.trialmonster.uk/getScoreListScoreMonsterLive" +
                    ".php?trialid=" + trialid;

            Log.i("Info", "URL:" + url);

// Request a string response from the provided URL
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            updateScoresDB(response);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // Log.i("Info", "That didn't work!");

                }
            });
// Add the request to the RequestQueue.
            queue.add(stringRequest);
        }

        private void updateScoresDB(String response) {
            // Get saved trial data
            TrialDbHelper mDbHelper = new TrialDbHelper(getContext());
            ArrayList<HashMap<String, String>> theScoreList = new ArrayList<HashMap<String,
                    String>>();
            try {
                theScoreList =
                        getScoreListFromResponse(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            for (int i = 0; i < theScoreList.size(); i++) {
                HashMap<String, String> theScore = theScoreList.get(i);
                String _id = theScore.get("_id");
                String lap = theScore.get("lap");
                String rider = theScore.get("rider");
                String day = theScore.get("day");
                String section = theScore.get("section");
                String trialid = theScore.get("trialid");
                String score = theScore.get("score");
//            String _id = theScore.get("id");

                // Create a ContentValues object where column names are the keys,
                ContentValues values = new ContentValues();

                values.put(ScoreContract.ScoreEntry._ID, _id);
                values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_RIDER, rider);
                values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_SECTION, section);
                values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_LAP, lap);
                values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_DAY, day);
                values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_TRIALID, trialid);
                if (score != "null") {
                    values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE, score);
                }

                db.insertWithOnConflict("scores", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            Toast.makeText(getContext(), "Scores downloaded", Toast.LENGTH_LONG).show();
            db.close();
        }

        private ArrayList<HashMap<String, String>> getScoreListFromResponse(String json) throws JSONException {
            ArrayList<HashMap<String, String>> theScoreList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(json);

            for (int index = 0; index < jsonArray.length(); index++) {
                HashMap<String, String> theScoreHash = new HashMap<>();

                theScoreHash.put("_id", jsonArray.getJSONObject(index).getString("id"));
                theScoreHash.put("lap", jsonArray.getJSONObject(index).getString("lap"));
                theScoreHash.put("rider", jsonArray.getJSONObject(index).getString("rider"));
                theScoreHash.put("section", jsonArray.getJSONObject(index).getString("section"));
                theScoreHash.put("day", jsonArray.getJSONObject(index).getString("day"));
                theScoreHash.put("score", jsonArray.getJSONObject(index).getString("score"));
                theScoreHash.put("trialid", jsonArray.getJSONObject(index).getString("trialid"));
                theScoreList.add(theScoreHash);
            }
            return theScoreList;
        }

        //        TODO Login check
        private void checkLogin(String newValue, String username) {
            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            String URL = "https://android.trialmonster.uk/joomlaAuthLive.php";

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    Log.d("Volley", "Response: " + response);
                    SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int initialUserId = localPrefs.getInt("loggedInUserID", -1);



                    int id = Integer.parseInt(response);
                    editor.putInt("loggedInUserID", id);
                    isLoggedInUser = (id != 0);
                    editor.putBoolean("isLoggedInUser", isLoggedInUser);
                    editor.putBoolean("manualMode", !isLoggedInUser);
                    Bundle result = new Bundle();
                    result.putBoolean("manualMode", !isLoggedInUser);
                    getParentFragmentManager().setFragmentResult("login", result);



                    trialNamePref.setVisible(!isLoggedInUser);
                    trialListPref.setVisible(isLoggedInUser);

                    numLapsPref.setVisible(!isLoggedInUser);
                    numSectionsPref.setVisible(!isLoggedInUser);
                    emailPref.setVisible(!isLoggedInUser);
                    advancedSwitchPref.setChecked(!isLoggedInUser);
                    restartClockSwitchPref.setVisible(!isLoggedInUser);
                    resetScoresSwitchPref.setVisible(!isLoggedInUser);
                    resetTimesSwitchPref.setVisible(!isLoggedInUser);
                    advancedSwitchPref.setChecked(!isLoggedInUser);
                    loginPrefCategory.setVisible(true);
                    usernamePref.setVisible(true);
                    passwordPref.setVisible(true);


//                  Only change trial details for new user
                    if (isLoggedInUser && (initialUserId != id)) {

                        // Get users trial data
                        TrialDbHelper mDbHelper = new TrialDbHelper(getContext());

                        ArrayList<HashMap<String, String>> options = mDbHelper.getPrefsOptions(loggedInUserID);
                        mDbHelper.close();

                        editor.putString("theIds", options.get(0).get("ids"));
                        editor.putString("theNames", options.get(0).get("names"));

//                     Update trialListPref
                        CharSequence[] entries = localPrefs.getString("theNames", "Manual Entry").split(",");
                        CharSequence[] entryValues = localPrefs.getString("theIds", "0").split(",");

                        trialListPref.setEntries(entries);
                        trialListPref.setEntryValues(entryValues);
                    } else {
                        editor.putInt("trialid", -999);
                        editor.putString("theTrialIndex", "-999");
                    }

                    editor.apply();


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