package uk.trialmonster.observer.activities;
// TODO - check section validation following read from trialPref
// TODO - check riderNumber on change/lauch in mode 2
// TODO - update numsections and numlaps filed on initial load of trial

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import com.google.gson.Gson;

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
    boolean isOnline, isLoggedInUser, isAdminUser, manualMode, isManualTrial;
    SharedPreferences localPrefs;

    TrialDbHelper mDbHelper;
    TextView statusLine;
    String username, statusLineText;
    int loggedInUserID;
//    ProgressBar progressBar;

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

//        progressBar = findViewById(R.id.progressBar);
//        Get local prefs
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();

//      Get online status
        isOnline = isOnline();
        editor.putBoolean("canConnect", isOnline);
        editor.apply();
        getPrefs();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
            Log.i("Info", "onCreate: Settings");
        }

//      Added to update section number in statusLine on change
        getSupportFragmentManager().setFragmentResultListener("sectionChange", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                        // We use a String here, but any type that can be put in a Bundle is supported.
//                String result = bundle.getString("bundleKey");
//                        int section = bundle.getInt("section", 1);
//                        int day = localPrefs.getInt("dayNum", 1);
//                        // Do something with the result.
//                        String statusLineText = "Section: " + section + " Day: " + day;
//                        statusLine = findViewById(R.id.statusLine);
//                        statusLine.setVisibility(View.VISIBLE);
//                        statusLine.setText(statusLineText);
                    }
                });

//      Added to update section number in statusLine on change
        getSupportFragmentManager().setFragmentResultListener("login", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                        // We use a String here, but any type that can be put in a Bundle is supported.
//                String result = bundle.getString("bundleKey");
                        String statusLineText;
                        isLoggedInUser = bundle.getBoolean("isLoggedInUser");
                        if (isLoggedInUser) {
                            // Do something with the result.
                            statusLineText = "Logged in as: " + username;
                        } else {
                            statusLineText = "You are not logged in. Manual mode ONLY";
                        }
                        statusLine = findViewById(R.id.statusLine);
                        statusLine.setVisibility(View.VISIBLE);
                        statusLine.setText(statusLineText);
                    }
                });
        // Get saved trial data for user
        mDbHelper = new TrialDbHelper(this);
        populateTrialList(loggedInUserID);

        int section = localPrefs.getInt("section", 1);
        int day = localPrefs.getInt("dayNum", 1);
        username = localPrefs.getString("username", "");
        if (!isManualTrial) {
            statusLineText = "Logged in as: " + username;
        } else {
            statusLineText = "You are not logged in. Manual mode ONLY";
        }
        statusLine = findViewById(R.id.statusLine);
        statusLine.setVisibility(View.VISIBLE);
        statusLine.setText(statusLineText);
        mDbHelper.close();
    }

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        super.onStart();
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();
        editor.putBoolean("canConnect", isOnline);
        editor.apply();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            String observer = localPrefs.getString("observer", "");
            String email = localPrefs.getString("email", "");
            String trialName = localPrefs.getString("trialName", "");
            String mobile = localPrefs.getString("mobile", "");
            if (observer.equals("") || mobile.equals("")) {
                Toast.makeText(this, "Observer name and contact details cannot be left empty", Toast.LENGTH_LONG).show();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getPrefs() {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
        isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
        isManualTrial = localPrefs.getBoolean("isManualTrial", true);
    }

    private void populateTrialList(int loggedInUserID) {
        localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = localPrefs.edit();

        Cursor cursor = mDbHelper.getTrialOptions(loggedInUserID);
        if (cursor.moveToFirst()) {
            String theIds = cursor.getString(0);
            String theNames = cursor.getString(1);
            editor.putString("theIds", cursor.getString(0));
            editor.putString("theNames", cursor.getString(1));
        } else {
            editor.putString("theIds", "-999");
            editor.putString("theNames", "My Trial");
        }
        editor.apply();
        cursor.close();
        mDbHelper.close();
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        //      Define variables
        public static final String TAG = "Info";
        String email, username, password, observerName, mobile, trialName, adminLockPass,
                adminLockNewPass, adminLockConfirmPass, dayText;
        int trialid, section, numsections, numlaps, numdays, dayNum, mode, ridingNumber,
                loggedInUserID,
                initialUserId;
        long startInterval, penaltyTariff;
        boolean timeMode, isLoggedInUser, incomplete, isManualTrial, isAdminUser;

        SharedPreferences localPrefs;
        SharedPreferences.Editor editor;

        PreferenceCategory loginPrefCategory, trialDetailsPrefCategory,
                observerDetailsPrefCategory, timeModePrefCategory, trialSelectCategory, recoveryModeCategory;

        Preference isManualTrialPref;

        ListPreference trialListPref;
        EditTextPreference trialNamePref, numLapsPref, daysPref, numSectionsPref, emailPref,
                usernamePref, passwordPref, ridingNumberPref, observerPref, mobilePref, sectionPref, adminLockPassPref, adminLockNewPassPref, adminLockConfirmPassPref, startIntervalPref, penaltyTariffPref;

        SwitchPreference timeModeSwitchPref, restartClockSwitchPref, resetScoresSwitchPref,
                resetTimesSwitchPref, advancedSwitchPref, restoreTrialScoresPref, dumpTrialScoresPref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
//          Set up prefs and editor
            localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            editor = localPrefs.edit();
            isAdminUser = localPrefs.getBoolean("isAdminUser", false);
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setupInitialPrefs();
            saveInitialValuesToPrefs();
//            setTrials();
            setupPrefs();
            setupTrialPrefOptions(loggedInUserID);
        }

        private void setupInitialPrefs() {
//          Define pref categories
            loginPrefCategory = findPreference("loginPrefCategory");
            trialDetailsPrefCategory = findPreference("trialDetails");
            observerDetailsPrefCategory = findPreference("observerDetails");
            timeModePrefCategory = findPreference("timingModeCategory");
            trialSelectCategory = findPreference("trialSelectCategory");
            recoveryModeCategory = findPreference("recoveryModeCategory");

//          Define pref widgets
            isManualTrialPref = findPreference("isManualTrial");
//            advancedSwitchPref = findPreference("show_advanced");
            trialListPref = findPreference("theTrialIndex");
            trialNamePref = findPreference("trialName");
            numLapsPref = findPreference("numlapsText");
            numSectionsPref = findPreference("numsectionsText");
            emailPref = findPreference("email");
            usernamePref = findPreference("username");
            passwordPref = findPreference("password");
            ridingNumberPref = findPreference("riderText");
            resetTimesSwitchPref = findPreference("reset_times_preference");
            resetScoresSwitchPref = findPreference("reset_scores_preference");
            restartClockSwitchPref = findPreference("restart_clock_preference");
            daysPref = findPreference("dayText");
            startIntervalPref = findPreference("startIntervalText");
            penaltyTariffPref = findPreference("penaltyTariffText");
            observerPref = findPreference("observer");
            mobilePref = findPreference("mobile");
            sectionPref = findPreference("sectionText");
            adminLockPassPref = findPreference("adminLockPass");
            adminLockNewPassPref = findPreference("adminLockNewPass");
            adminLockConfirmPassPref = findPreference("adminLockConfirmPass");
            timeModeSwitchPref = findPreference("timeMode");
            restoreTrialScoresPref = findPreference("restore_trial_scores");
            dumpTrialScoresPref = findPreference("dump_trial_scores");

            // Get initial values from localPrefs
            // Setup current values
//           Trial details
            trialid = localPrefs.getInt("trialid", -999);
            trialName = localPrefs.getString("trialName", "");
            isManualTrial = localPrefs.getBoolean("isManualTrial", true);
            numsections = localPrefs.getInt("numsections", 1);
            numlaps = localPrefs.getInt("numlaps", 1);
            numdays = localPrefs.getInt("numdays", 1);
            penaltyTariff = localPrefs.getLong("penaltyTariff", 60);
            startInterval = localPrefs.getLong("startInterval", 60);
            isAdminUser = localPrefs.getBoolean("isAdminUser", false);
//          Score data
            section = localPrefs.getInt("section", 1);
            dayText = localPrefs.getString("dayText", "1");
            dayNum = localPrefs.getInt("dayNum", 1);

            timeMode = localPrefs.getBoolean("timeMode", false);
            mode = localPrefs.getInt("mode", 0);

//          User prefs
            isLoggedInUser = localPrefs.getBoolean("isLoggedInUser", false);
            loggedInUserID = localPrefs.getInt("loggedInUserID", 0);
            username = localPrefs.getString("username", "");
            password = localPrefs.getString("password", "");
            ridingNumber = localPrefs.getInt("ridingNumber", 0);
            isAdminUser = localPrefs.getBoolean("isAdminUser", false);

//          Set up initial visibilities
            userLoggedIn(isLoggedInUser);

//          Observer prefs
            observerName = localPrefs.getString("observer", "");
            mobile = localPrefs.getString("mobile", "");
            email = localPrefs.getString("email", "");

            incomplete = localPrefs.getBoolean("incomplete", false);

//          Admin access
            adminLockPass = localPrefs.getString("adminLockPass", "13151");
            if (adminLockPass.length() < 1) {
                adminLockPass = "13151";
            }

            adminLockNewPass = "";
            adminLockConfirmPass = "";
//          Set inital visibility

//            isManualTrialPref.setSummary(trialName);


            isManualTrialPref.setVisible(false);
            loginPrefCategory.setVisible(true);
            observerDetailsPrefCategory.setVisible(true);
            timeModePrefCategory.setVisible(true);
            trialSelectCategory.setVisible(isAdminUser);
//            isManualTrialPref.setTitle(trialName);

            usernamePref.setVisible(true);
            passwordPref.setVisible(true);

            adminUserLoggedIn(isAdminUser);
            usernamePref.setVisible(true);
            passwordPref.setVisible(true);

            daysPref.setVisible(numdays > 1);
//            TODO - add manual scoring mode
            ridingNumberPref.setVisible(false);
        }

        //        Initial visibilities for admin users
        private void adminUserLoggedIn(boolean isAdminUser) {
            restartClockSwitchPref.setVisible(isAdminUser);
            resetScoresSwitchPref.setVisible(isAdminUser);
            resetTimesSwitchPref.setVisible(isAdminUser);
//            advancedSwitchPref.setChecked(isAdminUser);
            loginPrefCategory.setVisible(isAdminUser);
            timeModeSwitchPref.setVisible(isAdminUser);
            timeModePrefCategory.setVisible(isAdminUser);
            adminLockNewPassPref.setVisible(isAdminUser);
            trialSelectCategory.setVisible(isAdminUser);
            dumpTrialScoresPref.setVisible(isAdminUser);
            restoreTrialScoresPref.setVisible(isAdminUser);
            recoveryModeCategory.setVisible(isAdminUser);
        }

        //      Initial visibilities
        private void userLoggedIn(boolean isLoggedInUser) {
            numLapsPref.setVisible(!isLoggedInUser);
            numSectionsPref.setVisible(!isLoggedInUser);
            trialListPref.setVisible(isLoggedInUser);
            trialNamePref.setVisible(!isLoggedInUser);
            emailPref.setVisible(!isLoggedInUser);
            trialDetailsPrefCategory.setVisible(!isLoggedInUser);
            trialSelectCategory.setVisible(isLoggedInUser);
        }

//        private void setTrials() {
//            // Get saved values from prefers file
//            CharSequence[] entries = localPrefs.getString("theNames", "My Trial").split(",");
//            CharSequence[] entryValues = localPrefs.getString("theIds", "-999").split(",");
////            ListPreference lp = findPreference("theTrialIndex");
////            assert lp != null;
//            trialListPref.setEntries(entries);
//            trialListPref.setEntryValues(entryValues);
//        }

        private void saveInitialValuesToPrefs() {
            editor.putLong("startInterval", startInterval);
            editor.putLong("penaltyTariff", penaltyTariff);
            editor.putInt("section", section);
            editor.putInt("trialid", trialid);
            editor.remove("startIntervalText");
            editor.remove("penaltyText");
            editor.apply();
        }

        public void setupPrefs() {
            // observer pref
            assert observerPref != null;
            observerPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS));
            observerPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    observerName = String.valueOf(newValue).trim();
                    observerPref.setTitle("Observer: " + observerName);
                    observerPref.setText(observerName);
                    editor.putString("observer", observerName);

                    if (observerName.equals("")) {
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
                    mobilePref.setTitle("Contact number");
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
            daysPref.setTitle("Day");
            if ((dayNum > numdays) || (dayNum == 0)) {
                daysPref.setTitle("Invalid day number");
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

                    daysPref.setTitle("Day");
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
            sectionPref.setTitle("Section");
            if ((section > numsections) || (section == 0)) {
                sectionPref.setTitle("Invalid section number");
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
            trialNamePref.setTitle("Trial");
            trialNamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    trialName = String.valueOf(newValue).trim();
                    trialNamePref.setText(trialName);
                    editor.putString("trialName", trialName);
                    trialNamePref.setTitle("Trial");
                    editor.apply();
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
//            usernamePref.setVisible(false);
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
//            passwordPref.setVisible(false);
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

                    isAdminUser = returnedValue.equals(adminLockPass);
                    editor.putString("adminLockPass", adminLockPass);
                    editor.putBoolean("isAdminUser", isAdminUser);
                    editor.apply();

                    adminUserLoggedIn(isAdminUser);
//                    trialDetailsPrefCategory.setVisible(goAhead);
//                    restartClockSwitchPref.setVisible(goAhead);
//                    resetScoresSwitchPref.setVisible(goAhead);
//                    resetTimesSwitchPref.setVisible(goAhead);
//                    advancedSwitchPref.setChecked(goAhead);
//                    loginPrefCategory.setVisible(goAhead);
//                    timeModeSwitchPref.setVisible(goAhead);
//                    timeModeSwitchPref.setVisible(goAhead);
//                    usernamePref.setVisible(goAhead);
//                    passwordPref.setVisible(goAhead);
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

            numSectionsPref.setTitle("Number of sections");
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
            numLapsPref.setTitle("Number of laps");
            numLapsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numLapsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numLapsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int numlaps = Integer.parseInt(newValue.toString());
                    numLapsPref.setTitle("Number of laps");
                    editor.putInt("numlaps", numlaps);
                    editor.apply();
                    return false;
                }
            })
            ;

            // email pref
            assert emailPref != null;
//            emailPref.setTitle("Email");
            emailPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS));
            emailPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    email = newValue.toString().trim();
//                    emailPref.setTitle("Email: " + email);
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

//            Score dump preference
            assert dumpTrialScoresPref != null;

            dumpTrialScoresPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                                                                  @Override
                                                                  public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                                                                      Log.i("info", "Dump trial scores: " + trialid);
                                                                      dumpAllScores(trialid);
                                                                      return false;
                                                                  }
                                                              }

            );



            // Score reset pref
            assert restoreTrialScoresPref != null;

            restoreTrialScoresPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override

                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    Log.i("info", "Restore trial scores: " + trialid);
                    boolean state = Boolean.parseBoolean(newValue.toString());
                    if (state) {
                        restoresScores(trialid);
                    }
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
/*            assert advancedSwitchPref != null;
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
            });*/

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

                    if (trialid == 0) {
                        Log.i(TAG, "trialid: " + trialid);
                        editor.putString("trialName", "Manual Entry");
                        editor.putInt("trialid", 0);
                        editor.apply();
                        trialListPref.setVisible(false);
                        return true;
                    }
                    if (trialid != oldTrialId) {
//                        Get score data from server
                        getScoreData(trialid);
                        trialListPref.setVisible(true);
                    }

//                    getScoreData(trialid);

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

        private void restoresScores(int trialid) {
//            Log.i(TAG, "restoresScores: " + trialid);
            TrialDbHelper trialDbHelper = new TrialDbHelper(getContext());
            trialDbHelper.restoreScores(trialid);
            trialDbHelper.close();
        }

        private void dumpAllScores(int trialid) {
            ArrayList<HashMap<String, String>> theTrialScores;
            ScoreDbHelper scoreDbHelper = new ScoreDbHelper(getContext());
            theTrialScores = scoreDbHelper.dumpTrialScores(trialid);
            String scoreJSON = new Gson().toJson(theTrialScores);

            postTrialScoreData(scoreJSON);
            scoreDbHelper.close();
        }

        private void getScoreData(int trialid) {
            // Instantiate the RequestQueue.
            ProgressDialog progress = new ProgressDialog(getContext());
            progress.setTitle("Loading");
            progress.setMessage("Fetching scores...");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

            RequestQueue queue = Volley.newRequestQueue(getContext());

            String url = "https://android.trialmonster.uk/getScoreListScoreMonsterLive" +
                    ".php?trialid=" + trialid;

//            Log.i("Info", "URL:" + url);

// Request a string response from the provided URL
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            updateScoresDB(response);
                            progress.dismiss();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // Log.i("Info", "That didn't work!");

                }
            });
// Add the request to the RequestQueue.
            queue.add(stringRequest);
            progress.show();
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
                } else {
                    values.put(ScoreContract.ScoreEntry.COLUMN_SCORE_SCORE, ".");
                }

                db.insertWithOnConflict("scores", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
//            Toast.makeText(getContext(), "Scores downloaded", Toast.LENGTH_LONG).show();
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

                    String theIds = "";
                    String theNames = "";
                    int id = Integer.parseInt(response);
                    editor.putInt("loggedInUserID", id);
                    isLoggedInUser = (id != 0);
                    editor.putBoolean("isLoggedInUser", isLoggedInUser);
                    editor.putBoolean("isManualTrial", !isLoggedInUser);

                    Bundle result = new Bundle();
                    result.putBoolean("isManualTrial", !isLoggedInUser);
                    result.putBoolean("isLoggedInUser", isLoggedInUser);
                    result.putString("username", username);
                    result.putInt("userID", id);
                    getParentFragmentManager().setFragmentResult("login", result);


//                  Set pref visibilities on isLoggedInUser state
                    userLoggedIn(isLoggedInUser);

//                  Only change trial details for new user
//                  Start of working area
                    if (isLoggedInUser && (initialUserId != id)) {
                        loggedInUserID = id;

                        setupTrialPrefOptions(loggedInUserID);
//                        // Get users trial data
//                        TrialDbHelper mDbHelper = new TrialDbHelper(getContext());
//                        Cursor cursor = mDbHelper.getTrialOptions(loggedInUserID);
//                        if (cursor.moveToFirst()) {
//                            theIds = cursor.getString(0);
//                            theNames = cursor.getString(1);
//                            trialListPref.setVisible(true);
//                            trialNamePref.setVisible(false);
//                        } else {
//                            trialListPref.setVisible(false);
//                            trialNamePref.setVisible(true);
//                        }
//
//                        editor.putString("theIds", theIds);
//                        editor.putString("theNames", theNames);
//                        editor.apply();
//                        cursor.close();
//                        mDbHelper.close();
//
////                     Update trialListPref
//                        CharSequence[] entries = theNames.split(",");
//                        CharSequence[] entryValues = theIds.split(",");
//
//                        if (entries.length > 0) {
//                            trialListPref.setEntries(entries);
//                            trialListPref.setEntryValues(entryValues);
//                        }
                    } else {
                        editor.putInt("trialid", -999);
                        editor.putString("theTrialIndex", "-999");
                    }

//                  End of working area

                    editor.apply();

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

        private void setupTrialPrefOptions(int loggedInUserID) {
            if (loggedInUserID > 0) {
                TrialDbHelper trialDbHelper = new TrialDbHelper(getContext());
                ArrayList<HashMap<String, String>> data = trialDbHelper.getUserTrialList(loggedInUserID);
                int numTrials = data.size();

                if (numTrials > 0) {
                    String[] theIDS = new String[numTrials];
                    String[] theTrialNames = new String[numTrials];
                    for (int i = 0; i < numTrials; i++) {
                        theIDS[i] = data.get(i).get("id");
                        theTrialNames[i] = data.get(i).get("name");
                    }
                    trialListPref.setEntries(theTrialNames);
                    trialListPref.setEntryValues(theIDS);

                    editor.putBoolean("isManualTrial", false);
                    editor.apply();
                }
            } else {
//          Setup manual trial
                trialid = -999;
                isManualTrial = true;
                trialName = "Manual trial";

                editor.putInt("trialid", trialid);
//                editor.putString("trialName", trialName);
                editor.putBoolean("isManualTrial", isManualTrial);
                editor.apply();
            }
        }

        private void postTrialScoreData(String jsonData) {
            String url = "https://android.trialmonster.uk/processScoreDump.php";

            // creating a new variable for our request queue
            RequestQueue queue = Volley.newRequestQueue(getContext());

            // on below line we are calling a string
            // request method to post the data to our API
            // in this we are calling a post method.
            StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
                @Override
                public void onResponse(String response) {


                    // on below line we are displaying a success toast message.
                    Toast.makeText(getContext(), "Scores uploaded for trialid: " + trialid,
                            Toast.LENGTH_LONG).show();
                    try {
                        // on below line we are parsing the response
                        // to json object to extract data from it.
                        Log.i(TAG, "Response: " + response);
//                    JSONObject respObj = new JSONObject(response);
                        // below are the strings which we
                        // extract from our json object.
//                    String name = respObj.getString("name");
//                    String job = respObj.getString("job");

                        // on below line we are setting this string s to our text view.
//                    responseTV.setText("Name : " + name + "\n" + "Job : " + job);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new com.android.volley.Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // method to handle errors.
                    Toast.makeText(getContext(), "Fail to get response = " + error, Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    // below line we are creating a map for
                    // storing our values in key and value pair.
                    Map<String, String> params = new HashMap<String, String>();

                    // on below line we are passing our key
                    // and value pair to our parameters.
                    params.put("data", jsonData);
                    params.put("trialid", String.valueOf(trialid));
                    params.put("numlaps", String.valueOf(numlaps));
                    params.put("numsections", String.valueOf(numsections));

                    return params;
                }
            };
            // below line is to make
            // a json object request.
            queue.add(request);
        }
    }
}

