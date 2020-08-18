package com.alexsykes.scoremonster.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.data.ScoreDbHelper;
import com.google.android.material.textfield.TextInputLayout;

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
    int trialid, section, numsections, numlaps, ridingNumber;
    boolean isOnline, isSingleUser;
    String observer, theTrialName, detail, email;
    String[] theTrials, theIDs;
    ArrayList<HashMap<String, String>> theTrialList;

    SharedPreferences localPrefs;

    LinearLayout trialDetailsInput;

    // Database access
    ScoreDbHelper theScoreDB;

    Spinner trialSelect;
    ProgressDialog dialog = null;
    CheckBox resetCheckBox, confirmCheckBox;
    TextView observerTextInput, trialNameTextInput, emailTextInput, numSectionsTextInput, numLapsTextInput, trialDetailView, ridingNumberTextInput;
    ImageView warningImageView;
    TextInputLayout riderNumberTextView;
    Switch modeSwitch;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        theScoreDB = new ScoreDbHelper(this);
        // Set up activity fields
        observerTextInput = findViewById(R.id.observerTextInput);
        trialDetailView = findViewById(R.id.trialDetailView);
        trialNameTextInput = findViewById(R.id.trialNameTextInput);
        emailTextInput = findViewById(R.id.emailTextInput);
        trialDetailsInput = findViewById((R.id.trialDetailsInput));
        numSectionsTextInput = findViewById((R.id.numSectionsTextInput));
        numLapsTextInput = findViewById((R.id.numLapsTextInput));
        resetCheckBox = findViewById(R.id.resetCheckBox);
        confirmCheckBox = findViewById(R.id.confirmCheckBox);
        warningImageView = findViewById(R.id.warningImageView);

        // New stuff
        riderNumberTextView = findViewById(R.id.ridingNumberTextView);
        ridingNumberTextInput = findViewById(R.id.ridingNumberTextInput);
        button = findViewById(R.id.button);
        modeSwitch = findViewById(R.id.modeSwitch);

        confirmCheckBox.setVisibility(View.GONE);
        warningImageView.setVisibility(View.GONE);

        // Set up listeners
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

        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // ridingNumberTextInput.setText(String.valueOf(ridingNumber));
                if (isChecked) {
                    riderNumberTextView.setVisibility(View.VISIBLE);
                } else {
                    riderNumberTextView.setVisibility(View.GONE);
                }
            }
        });

        // Set up spinner
        trialSelect = findViewById(R.id.trialSelect);
        trialSelect.setOnItemSelectedListener(this);

        checkPrefs();
        if (isOnline) {

            // Get trialList from server
            String URL = BASE_URL + "getTrialList.php";
            try {
                getJSONDataset(URL);
            } catch (NullPointerException e) {
                Toast.makeText(SetupActivity.this, "Empty data", Toast.LENGTH_LONG).show();
            }
        }

        // Check
        if (isSingleUser) {
            riderNumberTextView.setVisibility(View.VISIBLE);
            // ridingNumberTextInput.setText(String.valueOf(ridingNumber));
        }

        if (ridingNumber == 0) {
            ridingNumberTextInput.setText("");
        } else {
            ridingNumberTextInput.setText(String.valueOf(ridingNumber));
        }
    }

    @Override
    protected void onStart() {
        // Check network connectivity and set Prefs
        SharedPreferences.Editor editor = localPrefs.edit();
        isOnline = isOnline();
        editor.putBoolean("canConnect", isOnline);
        editor.apply();
        super.onStart();
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

                int size = theTrialList.size();
                theTrials = new String[size];
                theIDs = new String[size];

                for (int index = 0; index < theTrialList.size(); index++) {
                    theTrials[index] = theTrialList.get(index).get("name");
                    theIDs[index] = theTrialList.get(index).get("id");
                }

                if (trialid == 0) {
                    theTrialName = "Manual Entry";
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
                String date, name, id, club, numsections, numlaps, starttime, email;

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

                        // trial = club + " - " + name;
                        theTrial.put("id", id);
                        theTrial.put("date", date);
                        theTrial.put("club", club);
                        theTrial.put("name", name);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("starttime", starttime);
                        theTrial.put("email", email);
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

    private void checkPrefs() {
        // set error flag to true
        boolean prefsSet = true;
        // Get localPrefs and read values
        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
        trialid = localPrefs.getInt("trialid", 0);
        theTrialName = localPrefs.getString("theTrialName", null);
        numsections = localPrefs.getInt("numsections", 0);
        numlaps = localPrefs.getInt("numlaps", 0);
        observer = localPrefs.getString("observer", "");
        email = localPrefs.getString("email", "");
        isOnline = localPrefs.getBoolean("canConnect", false);
        isSingleUser = localPrefs.getBoolean("isSingleUser", false);
        ridingNumber = localPrefs.getInt("ridingNumber", 0);

        if (ridingNumber == 0) {
            ridingNumberTextInput.setText("");
        } else {
            ridingNumberTextInput.setText(String.valueOf(ridingNumber));
        }
        if (isSingleUser) {
            riderNumberTextView.setVisibility(View.VISIBLE);
        } else {
            riderNumberTextView.setVisibility(View.GONE);
        }


        // Set up manual fields
        trialNameTextInput.setText(theTrialName);
        emailTextInput.setText(email);
        if (numlaps > 0) {
            numLapsTextInput.setText(String.valueOf(numlaps));
        }

        if (numsections > 0) {
            numSectionsTextInput.setText(String.valueOf(numsections));
        }


        detail = theTrialName + "\n" + numlaps + " laps \n" + numsections + " sections";
        trialDetailView.setText(detail);
        modeSwitch.setChecked(isSingleUser);

        if (trialid == 0) {
            trialDetailsInput.setVisibility(View.VISIBLE);
        } else {
            trialDetailsInput.setVisibility(View.GONE);
        }
        // Sync inputs to saved values
        observerTextInput.setText(observer);

        if (observer.equals("") || section == 0 || numlaps == 0 || numsections == 0) {
            // If incomplete, set flag to false
            prefsSet = false;
        }
    }

    public void setPrefs(View view) {
        // Field validation routine
        boolean hasErrors = false;
        String response;

        // Set errorMsg with initial message
        String errorMsg = "The following error(s) need to be corrected:";

        // Check that observer field is complete
        observer = observerTextInput.getText().toString();
        if (observer.equals("")) {
            // If empty, then append message
            hasErrors = true;
            errorMsg += "\nThe observer field is empty";
        }


        // Check for manual entries
        if (trialid == 0 ) {
            theTrialName = trialNameTextInput.getText().toString();
            if (theTrialName.equals("")) {
                // If empty, then append message
                hasErrors = true;
                errorMsg += "\nThe trial name field is empty";
            }

            email = emailTextInput.getText().toString();
            if (email.equals("")) {
                // If empty, then append message
                hasErrors = true;
                errorMsg += "\nThe email field is empty";
            }

            response = numSectionsTextInput.getText().toString();
            if(response.equals("")) {
                // If empty, then append message
                hasErrors = true;
                errorMsg += "\nThe number of sections field is empty";

            } else {
                numsections = Integer.parseInt(numSectionsTextInput.getText().toString());
                if (numsections == 0) {
                    // If empty, then append message
                    hasErrors = true;
                    errorMsg += "\nThe number of sections must be more than zero";
                }
            }

            response = numLapsTextInput.getText().toString();
            if(response.equals("")) {
                // If empty, then append message
                hasErrors = true;
                errorMsg += "\nThe number of laps field is empty";

            } else {
                numlaps = Integer.parseInt(numLapsTextInput.getText().toString());
                if (numlaps == 0) {
                    // If empty, then append message
                    hasErrors = true;
                    errorMsg += "\nThe number of laps must be more than zero";
                }
            }
        }

        isSingleUser = modeSwitch.isChecked();

        if (isSingleUser) {
            // Check that observer field is complete
            if (ridingNumberTextInput.getText().toString().equals("")) {
                // If empty, then append message
                hasErrors = true;
                errorMsg += "\nThe riding number field is empty";
            } else {
                ridingNumber = Integer.parseInt(ridingNumberTextInput.getText().toString());
            }
        }

        // Inform user if errors
        if (hasErrors) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        } else {
            // otherwise save values
            localPrefs = getSharedPreferences("monster", MODE_PRIVATE);
            SharedPreferences.Editor editor = localPrefs.edit();
            editor.putString("theTrialName", theTrialName);
            editor.putInt("trialid", trialid);
            editor.putInt("numsections", numsections);
            editor.putInt("numlaps", numlaps);
            editor.putInt("ridingNumber", ridingNumber);
            editor.putString("observer", observer);
            editor.putString("email", email);
            editor.putBoolean("isSingleUser", isSingleUser);
            editor.commit();
            // Read resetCheckBox
            boolean reset = resetCheckBox.isChecked();

            if (reset) {
                theScoreDB.clearResults();
            }
            finish();
        }
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // Reading trial details into variables
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {

        HashMap theTrial = theTrialList.get(position);
        numsections = Integer.parseInt(theTrial.get("numsections").toString());
        numlaps = Integer.parseInt(theTrial.get("numlaps").toString());
        trialid = Integer.parseInt(theTrial.get("id").toString());
        email = theTrial.get("email").toString();
        theTrialName = theTrial.get("name").toString();

        if (trialid == 0 ) {
            trialDetailsInput.setVisibility(View.VISIBLE);
            trialDetailView.setVisibility(View.GONE);
        } else {
            trialDetailsInput.setVisibility(View.GONE);
            trialDetailView.setVisibility(View.VISIBLE);
        }

        detail = theTrialName + "\n" + numlaps + " laps \n" + numsections + " sections";
        trialDetailView.setText(detail);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
