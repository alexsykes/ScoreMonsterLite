package com.alexsykes.scoremonster.activities;

import static java.lang.String.join;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
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

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SharedPreferences localPrefs;
        private static final String BASE_URL = "https://android.trialmonster.uk/";
        String URL = BASE_URL + "getTrialList.php";
        private String[] theTrials, theIDs;
        private String theTrialName;

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
            ListPreference lp = (ListPreference)findPreference("theTrialIndex");
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


            ListPreference lp = (ListPreference)findPreference("theTrialIndex");
            lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences.Editor editor = localPrefs.edit();
                    editor.putBoolean("trialHasChanged", true);
                    editor.apply();
                    return true;
                }
            });
        }


        /*private void getJSONDataset(final String urlWebService) {
            *//*
             * As fetching the json string is a network operation
             * And we cannot perform a network operation in main thread
             * so we need an AsyncTask
             * The constrains defined here are
             * Void -> We are not passing anything
             * Void -> Nothing at progress update as well
             * String -> After completion it should return a string and it will be the json string
             * *//*
            class GetData extends AsyncTask<Void, Void, String> {
                private ArrayList<HashMap<String, String>> theTrialList;

                //this method will be called before execution

                @Override
                protected void onPreExecute() {

                    super.onPreExecute();
                    // Show dialog during server transaction
                    // dialog = ProgressDialog.show(SetupActivity.this, "Scoremonster", "Getting trial list", true);
//                dialog = new ProgressDialog(MainActivity.this);
//                dialog.setMessage("Loading…");
//                dialog.setCancelable(false);
//                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> dialog.dismiss());
//                dialog.show();
                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                private void setTrialsList(ArrayList<HashMap<String, String>> theTrialList) {
                    SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    // Need name and id from theTrialList
                    int size = theTrialList.size();
                    String[] theTrialNames = new String[size];
                    String[] theTrialIds = new String[size];
                    for (int index = 0; index < size; index++) {
                        theTrialNames[index] = theTrialList.get(index).get("name");
                        theTrialIds[index] =  theTrialList.get(index).get("id");
                    }
                    String theTrialListNames = join(",", theTrialNames);
                    String theTrialListIds = join(",", theTrialIds);

                    SharedPreferences.Editor editor = localPrefs.edit();

                    editor.putString("theNames", theTrialListNames);
                    editor.putString("theIds", theTrialListIds);
                    editor.apply();
                }
            *//* this method will be called after execution

                s contains trial details in JSON string
             *//*

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    //  dialog.dismiss();

                    // Populate ArrayList with JSON data
                    theTrialList = populateResultArrayList(s);


                    int size = theTrialList.size();
                    theTrials = new String[size];
                    theIDs = new String[size];
                    String id;

                    for (int index = 0; index < theTrialList.size(); index++) {
                        theTrialName= theTrialList.get(index).get("name");
                        id = theTrialList.get(index).get("id");
                        theTrials[index] = theTrialName;
                        theIDs[index] = id;
                    }

                    setTrialsList(theTrialList);

                }

                *//*
                @param String json JSON string returned from MySQL
                @return ArrayList of trials data
                 *//*
                private ArrayList<HashMap<String, String>> populateResultArrayList(String json) {
                    ArrayList<HashMap<String, String>> theTrialList = new ArrayList<>();
                    String date, name, id, club, numsections, numlaps, starttime, email, scoringmode;

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
                            scoringmode = jsonArray.getJSONObject(index).getString("scoringmode");

                            // trial = club + " - " + name;
                            theTrial.put("id", id);
                            theTrial.put("date", date);
                            theTrial.put("club", club);
                            theTrial.put("name", name);
                            theTrial.put("numsections", numsections);
                            theTrial.put("numlaps", numlaps);
                            theTrial.put("starttime", starttime);
                            theTrial.put("email", email);
                            theTrial.put("scoringmode", scoringmode);
                            theTrialList.add(theTrial);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                      //  Toast.makeText(SettingsFragment, "SocketTimeoutException", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
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
        }*/
    }
}