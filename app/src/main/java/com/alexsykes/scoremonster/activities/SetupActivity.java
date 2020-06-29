package com.alexsykes.scoremonster.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.data.FinishTimeDbHelper;
import com.alexsykes.scoremonster.data.ScoreDbHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class SetupActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    // Set up data fields
    private static final String BASE_URL = "https://android.trialmonster.uk/";
    int trialid, section, numsections, numlaps;
    boolean showDabPad;
    String observer, theTrialName, detail;
    // long startTime;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialList;

    SharedPreferences localPrefs;

    // Database access
    ScoreDbHelper theScoreDB;
    FinishTimeDbHelper theFinishTimeDB;

    // Interface widgets
    RadioGroup modeSwitch;
    int modeIdx;

    RadioButton dabPadSelect, numberPadSelect;
    Spinner trialSelect;
    ProgressDialog dialog = null;
    CheckBox resetCheckBox, confirmCheckBox;
    TextView observerTextInput, sectionTextInput, trialDetailView;
    ImageView warningImageView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        theScoreDB = new ScoreDbHelper(this);
        theFinishTimeDB = new FinishTimeDbHelper(this);
        // Set up activity fields
        observerTextInput = findViewById(R.id.observerTextInput);
        sectionTextInput = findViewById(R.id.sectionTextInput);
        trialDetailView = findViewById(R.id.trialDetailView);
        modeSwitch = findViewById(R.id.padViewGroup);
        dabPadSelect = findViewById(R.id.dabPadSelect);
        numberPadSelect = findViewById(R.id.numberPadSelect);
        resetCheckBox = findViewById(R.id.resetCheckBox);
        confirmCheckBox = findViewById(R.id.confirmCheckBox);
        warningImageView = findViewById(R.id.warningImageView);
        button = findViewById(R.id.button);

        confirmCheckBox.setVisibility(View.GONE);
        warningImageView.setVisibility(View.GONE);

        resetCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    confirmCheckBox.setVisibility(View.VISIBLE);
                    warningImageView.setVisibility(View.VISIBLE);
                    button.setEnabled(false);

                } else {
                    confirmCheckBox.setVisibility(View.GONE);
                    warningImageView.setVisibility(View.GONE);
                    button.setEnabled(true);
                }
            }
        });


        confirmCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    confirmCheckBox.setVisibility(View.VISIBLE);
                    button.setEnabled(true);

                } else {
                    button.setEnabled(false);
                }
            }
        });


        // Set up spinner
        trialSelect = findViewById(R.id.trialSelect);
        trialSelect.setOnItemSelectedListener(this);

        checkPrefs();

        // Get trialList from server
        String URL = BASE_URL + "getTrialList.php";
        try {
            getJSONDataset(URL);
        } catch (NullPointerException e) {
            Toast.makeText(SetupActivity.this, "Empty data", Toast.LENGTH_LONG).show();
        }
    }

    private void getJSONDataset(final String urlWebService) {
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
            protected void onPreExecute() {

                super.onPreExecute();
                // Show dialog during server transaction
                // dialog = ProgressDialog.show(SetupActivity.this, "Scoremonster", "Getting trial list", true);
                dialog = new ProgressDialog(SetupActivity.this);
                dialog.setMessage("Loading…");
                dialog.setCancelable(false);
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }


            /* this method will be called after execution

                s contains trial details in JSON string
             */

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();

                // Populate ArrayList with JSON data
                theTrialList = populateResultArrayList(s);

                theTrials = new String[theTrialList.size()];
                theIDs = new String[theTrialList.size()];

                for (int index = 0; index < theTrialList.size(); index++) {
                    theTrials[index] = theTrialList.get(index).get("name");
                    theIDs[index] = theTrialList.get(index).get("id");
                }

                // Set up Spinner
                ArrayAdapter aa = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_item, theTrials);
                aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // Setting the ArrayAdapter data on the Spinner
                trialSelect.setAdapter(aa);
                trialSelect.setSelection(aa.getPosition(theTrialName));
            }

            /*
            @param String json JSON string returned from MySQL
            @return ArrayList of trials data
             */
            private ArrayList<HashMap<String, String>> populateResultArrayList(String json) {
                ArrayList<HashMap<String, String>> theTrialList = new ArrayList<>();
                String date, name, id, club, numsections, numlaps, starttime;

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

                        // trial = club + " - " + name;
                        theTrial.put("id", id);
                        theTrial.put("date", date);
                        theTrial.put("club", club);
                        theTrial.put("name", name);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("starttime", starttime);
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
                    Toast.makeText(SetupActivity.this, "SocketTimeoutException", Toast.LENGTH_LONG).show();
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
    }

    private boolean checkPrefs() {
        // set error flag to true
        boolean prefsSet = true;
        String sectionNumber;
        // Get localPrefs and read values
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        trialid = localPrefs.getInt("trialid", 0);
        theTrialName = localPrefs.getString("theTrialName", null);
        numsections = localPrefs.getInt("numsections", 0);
        numlaps = localPrefs.getInt("numlaps", 0);
        observer = localPrefs.getString("observer", "");
        section = localPrefs.getInt("section", 0);
        //showDabPad = localPrefs.getBoolean("showDabPad", false);
        modeIdx = localPrefs.getInt("modeIndex", 0);

        if (section == 0) {
            sectionNumber = "";
        } else {
            sectionNumber = String.valueOf(section);
        }

        // Sync inputs to saved values
        observerTextInput.setText(observer);
        sectionTextInput.setText(sectionNumber);

        RadioButton selected = (RadioButton) modeSwitch.getChildAt(modeIdx);
        selected.setChecked(true);
/*
        if (showDabPad) {
            dabPadSelect.setChecked(true);
        } else {
            numberPadSelect.setChecked(true);
        }*/

        // Check for missing values
        if (observer.equals("") || section == 0 || trialid == 0 || numlaps == 0 || numsections == 0) {
            // If incomplete, set flag to false
            prefsSet = false;
        }
        return prefsSet;
    }

    public void setPrefs(View view) {
        // Field validation routine
        boolean hasErrors = false;

        // Set errorMsg with initial message
        String errorMsg = "The following error(s) need to be corrected:";

        // Check that observer field is complete
        observer = observerTextInput.getText().toString();
        if (observer.equals("")) {
            // If empty, then append message
            hasErrors = true;
            errorMsg += "\nThe observer field is empty";
        }

        // Check that section field is populated
        if (sectionTextInput.getText().toString().equals("")) {
            // If empty, then append message
            hasErrors = true;
            errorMsg += "\nThe section field is empty";
        } else {
            // Check section number validity against numsections
            section = Integer.parseInt(sectionTextInput.getText().toString());
            // If out of range, then append message
            if (section > numsections || section <= 0) {
                hasErrors = true;
                errorMsg += "\nInvalid section number";
            }
        }

        // Inform user if errors
        if (hasErrors) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        } else {
            // otherwise save values
            int radioButtonID = modeSwitch.getCheckedRadioButtonId();
            View radioButton = modeSwitch.findViewById(radioButtonID);
            int idx = modeSwitch.indexOfChild(radioButton);

            localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putString("theTrialName", theTrialName);
            editor.putInt("trialid", trialid);
            editor.putInt("numsections", numsections);
            editor.putInt("numlaps", numlaps);
            editor.putString("observer", observer);
            editor.putInt("section", section);
            //editor.putBoolean("showDabPad", dabPadSelect.isChecked());
            //editor.putBoolean("showNumberPad", numberPadSelect.isChecked());
            // editor.putLong("starttime", startTime);
            editor.putInt("modeIndex", idx);

            /* Moving time setting to TimerActivity
            if (startTime > 0) {
                editor.putBoolean("isStartTimeSet", true);
            } else {
                editor.putBoolean("isStartTimeSet", false);
            }

             */

            boolean success = editor.commit();


            // Read resetCheckBox
            boolean reset = resetCheckBox.isChecked();

            if (reset) {
                theScoreDB.clearResults();
                theFinishTimeDB.clearTimes();
            }
            finish();
        }
    }

    // Reading trial details into variables
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {

        HashMap theTrial = theTrialList.get(position);
        numsections = Integer.parseInt(theTrial.get("numsections").toString());
        numlaps = Integer.parseInt(theTrial.get("numlaps").toString());
        trialid = Integer.parseInt(theTrial.get("id").toString());
        theTrialName = theTrial.get("name").toString();

//         Moving startTime to TimerActivity
//        try {
//             startTime = Long.valueOf(theTrial.get("starttime").toString());
//        } catch (Exception e) {
//             startTime = -1;
//         }

        detail = theTrialName + "\n" + numlaps + " laps \n" + numsections + " sections";
        trialDetailView.setText(detail);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
