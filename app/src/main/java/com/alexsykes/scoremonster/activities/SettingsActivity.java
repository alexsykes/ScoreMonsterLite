package com.alexsykes.scoremonster.activities;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.data.TrialContract;
import com.alexsykes.scoremonster.data.TrialDbHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
        isOnline = isOnline();
        if (isOnline) {
            String theURL = "http://android.trialmonster.uk/getTrialListScoreMonster.php";
            getTrialsData(theURL);
        }

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

    private void getTrialsData(final String urlWebService) {
        /*
         * As fetching the json string is a network operation
         * And we cannot perform a network operation in main thread
         * so we need an AsyncTask
         * The constrains defined here are
         * Void -> We are not passing anything
         * Void -> Nothing at progress update as well
         * String -> After completion it should return a string and it will be the json string
         * */
        class GetData extends AsyncTask<Void, Void, String> {

            //this method will be called before execution

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                // Populate ArrayList with JSON data
                theTrialData = populateResultArrayList(s);
                addToDatabase(theTrialData);
            }

            private void addToDatabase(ArrayList<HashMap<String, String>> theTrialData) {
                final String DATABASE_NAME = "monster.db";

                SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);
                ContentValues values = new ContentValues();
                for (int i = 0; i < theTrialData.size(); i++) {
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NAME, theTrialData.get(i).get("name"));
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_TRIALID, theTrialData.get(i).get("id"));
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMLAPS, theTrialData.get(i).get("numlaps"));
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_NUMSECTIONS, theTrialData.get(i).get("numsections"));
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_DATE, theTrialData.get(i).get("date"));
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_EMAIL, theTrialData.get(i).get("email"));
                    values.put(TrialContract.TrialEntry.COLUMN_TRIAL_MODE, theTrialData.get(i).get("mode"));
                    values.put(TrialContract.TrialEntry._ID, theTrialData.get(i).get("id"));
                    db.insertWithOnConflict("trials", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            /*
            @param String json JSON string returned from MySQL
            @return ArrayList of trials data
             */
            private ArrayList<HashMap<String, String>> populateResultArrayList(String json) {
                ArrayList<HashMap<String, String>> theTrialList = new ArrayList<>();
                String date, name, id, club, numsections, numlaps, starttime, email, mode;

                try {
                    // Parse string data into JSON
                    JSONArray jsonArray = new JSONArray(json);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        HashMap<String, String> theTrial = new HashMap<>();
                        id = jsonArray.getJSONObject(index).getString("id");
                        date = jsonArray.getJSONObject(index).getString("date");
                        club = jsonArray.getJSONObject(index).getString("club");
                        name = jsonArray.getJSONObject(index).getString("name");
                        numsections = jsonArray.getJSONObject(index).getString("numsections");
                        numlaps = jsonArray.getJSONObject(index).getString("numlaps");
                        starttime = jsonArray.getJSONObject(index).getString("starttime");
                        email = jsonArray.getJSONObject(index).getString("email");
                        mode = jsonArray.getJSONObject(index).getString("mode");

                        // trial = club + " - " + name;
                        theTrial.put("id", id);
                        theTrial.put("date", date);
                        theTrial.put("club", club);
                        theTrial.put("name", name);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("starttime", starttime);
                        theTrial.put("email", email);
                        theTrial.put("mode", mode);
                        theTrialList.add(theTrial);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // AT this stage, theTrialList is populated with trial data - now add to database
                return theTrialList;
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {
                int TIMEOUT_VALUE = 1000;
                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(TIMEOUT_VALUE);
                    con.setReadTimeout(TIMEOUT_VALUE);
                    //StringBuilder object to read the string from the service
                    StringBuilder sb = new StringBuilder();

                    //We will use a buffered reader to read the string from service
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    //A simple string to read values from each line
                    String json;

                    //reading until we don't find null
                    while ((json = bufferedReader.readLine()) != null) {
                        json = json + "\n";
                        //appending it to string builder
                        sb.append(json);
                    }

                    //finally returning the read string
                    return sb.toString().trim();
                } catch (SocketTimeoutException e) {
                    // TODO handle timeout
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        //creating asynctask object and executing it
        GetData getJSON = new GetData();
        getJSON.execute();
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
            assert lp != null;
            lp.setEntries(entries);
            lp.setEntryValues(entryValues);
        }

        private void setup() {

            // mobile pref
            EditTextPreference mobilePref = findPreference("mobile");
            assert mobilePref != null;
            mobilePref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_PHONE));

            EditTextPreference sectionPref = findPreference("sectionText");
            assert sectionPref != null;
            sectionPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));


            // numsections pref
            EditTextPreference numSectionsPref = findPreference("numsectionsText");
            assert numSectionsPref != null;
            numSectionsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numSectionsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numSectionsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int numsections = Integer.parseInt(newValue.toString());
                    editor.putInt("numsections", numsections);
                    editor.putString("numsectionsText", newValue.toString());
                    editor.apply();
                    return false;
                }
            })
            ;

            // numlaps pref
            EditTextPreference numLapsPref = findPreference("numlapsText");
            assert numLapsPref != null;
            numLapsPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            numLapsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    numLapsPref.setText(newValue.toString());
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int numlaps = Integer.parseInt(newValue.toString());
                    editor.putInt("numlaps", numlaps);
                    editor.putString("numlapsText", newValue.toString());
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

            // trialid pref
            int trialid = 0;
            ListPreference lp = findPreference("theTrialIndex");
            if (lp.getValue() != null) {
                trialid = Integer.valueOf(lp.getValue());
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

            lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences.Editor editor = localPrefs.edit();
                    int trialid = Integer.parseInt(newValue.toString());

                    HashMap<String, String> theTrialData;
                    theTrialData = getTrialData(trialid).get(0);
                    int numsections = Integer.valueOf(theTrialData.get("numsections"));
                    int numlaps = Integer.valueOf(theTrialData.get("numlaps"));
                    int mode = Integer.valueOf(theTrialData.get("mode"));
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
                    editor.apply();
                    Log.i("Note", "Trial selection changed");

                    if (trialid == 0) {
                        Log.i("Note", "Manual Entry selected");
                        emailPref.setVisible(true);
                        numSectionsPref.setVisible(true);
                        numLapsPref.setVisible(true);
                        // theTrialSettings.setVisible(true);
                    } else {
                        emailPref.setVisible(false);
                        numSectionsPref.setVisible(false);
                        numLapsPref.setVisible(false);
                        //  theTrialSettings.setVisible(false);
                    }
                    return true;
                }

                private ArrayList<HashMap<String, String>> getTrialData(int trialid) {
                    ArrayList<HashMap<String, String>> theData;
                    TrialDbHelper trialDbHelper = new TrialDbHelper(getContext());
                    theData = trialDbHelper.getTrialData(trialid);
                    return theData;
                }
            });
        }
    }
}