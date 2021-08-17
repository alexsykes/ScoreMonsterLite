package com.alexsykes.scoremonster;

import static java.lang.String.join;

import android.os.AsyncTask;

import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MainViewModel extends ViewModel {
    private int trialid;
    private int numsections;
    private int numlaps;
    private int ridingNumber;
    private int score;
    private int section;

    private boolean isRefreshed;
    private ArrayList<HashMap<String, String>> theTrialData;
    private String[] theTrials, theIDs;
    private String theTrialName, data;

    // URL constants
    private final String upLoadServerUri = "http://android.trialmonster.uk/sendMailWithFile.php";
    private final String sendMailURL = "http://android.trialmonster.uk/sendMailWithFile.php";
    private static final String BASE_URL = "https://android.trialmonster.uk/";

    public MainViewModel() {
        String theURL = BASE_URL + "getTrialListScoreMonster.php";
        getTrialList(theURL);
    }

    public boolean isRefreshed() {
        return isRefreshed;
    }

    public void setRefreshed(boolean refreshed) {
        isRefreshed = refreshed;
    }

    public MainViewModel(int trialid, int numsections, int numlaps) {
        this.trialid = trialid;
        this.numsections = numsections;
        this.numlaps = numlaps;
    }

    public void setNumLaps(int numlaps) {
        this.numlaps = numlaps;
    }

    public void setNumSections(int numsections) {
        this.numsections = numsections;
    }

    public int getTrialid() {
        return trialid;
    }

    public int getNumsections() {
        return numsections;
    }

    public int getNumlaps() {
        return numlaps;
    }

    public void setTrialid(int trialid) {
        this.trialid = trialid;
    }


    public int getRidingNumber() {
        return ridingNumber;
    }

    public void setRidingNumber(int ridingNumber) {
        this.ridingNumber = ridingNumber;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getSection() {
        return section;
    }

    public void setSection(int section) {
        this.section = section;
    }

    public String getCurrentTrialData() {
        return data;
    }


    public void getTrialList(final String urlWebService) {
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
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                // Populate ArrayList with JSON data
                theTrialData = populateResultArrayList(s);

                int size = theTrialData.size();
                theTrials = new String[size];
                theIDs = new String[size];
                String id;

                for (int index = 0; index < theTrialData.size(); index++) {
                    theTrialName = theTrialData.get(index).get("name");
                    id = theTrialData.get(index).get("id");
                    theTrials[index] = theTrialName;
                    theIDs[index] = id;
                }

                data = setTrialsList(theTrialData);
                isRefreshed = true;

                if (trialid == 0) {
                    theTrialName = "Manual Entry";
                }
            }

            /*
            @param String json JSON string returned from MySQL
            @return ArrayList of trials data
             */
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

                        // trial = club + " - " + name;
                        theTrial.put("id", id);
                        theTrial.put("date", date);
                        theTrial.put("club", club);
                        theTrial.put("name", name);
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
                   // Toast.makeText(MainActivity.this, "SocketTimeoutException", Toast.LENGTH_LONG).show();
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

    private String setTrialsList(ArrayList<HashMap<String, String>> theTrialList) {
        // SharedPreferences localPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Need name and id from theTrialList
        int size = theTrialList.size();
        String[] theTrialNames = new String[size];
        String[] theTrialIds = new String[size];
        for (int index = 0; index < size; index++) {
            theTrialNames[index] = theTrialList.get(index).get("name");
            theTrialIds[index] = theTrialList.get(index).get("id");
        }
        String theTrialListNames = join(",", theTrialNames);
        String theTrialListIds = join(",", theTrialIds);
        String theData = theTrialListIds + ":" + theTrialListNames;
        return theData;
//        SharedPreferences.Editor editor = localPrefs.edit();
//
//        editor.putString("theNames", theTrialListNames);
//        editor.putString("theIds", theTrialListIds);
//        editor.apply();
    }
    private void getTrialDetails(final String urlWebService) {
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
//                dialog = new ProgressDialog(MainActivity.this);
//                dialog.setMessage("Loading…");
//                dialog.setCancelable(false);
//                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> dialog.dismiss());
//                dialog.show();
            }


            /* this method will be called after execution

                s contains trial details in JSON string
             */

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                //  dialog.dismiss();

                // Populate ArrayList with JSON data
                theTrialData = readTrialData(s);


                theTrialName = theTrialData.get(0).get("name");
                trialid = Integer.parseInt(theTrialData.get(0).get("id"));
                numlaps = Integer.valueOf(theTrialData.get(0).get("numlaps"));
                numsections  = Integer.valueOf(theTrialData.get(0).get("numsections"));


                //setTrialsList(theTrialData);

                if (trialid == 0) {
                    theTrialName = "Manual Entry";
                }
            }

            private ArrayList<HashMap<String, String>> readTrialData(String json) {

                ArrayList<HashMap<String, String>> theTrialData = new ArrayList<>();
                String id, numsections, numlaps, name;

                try {
                    // Parse string data into JSON
                    JSONArray jsonArray = new JSONArray(json);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        HashMap<String, String> theTrial = new HashMap<>();
                        id = jsonArray.getJSONObject(index).getString("id");
                        numsections = jsonArray.getJSONObject(index).getString("numsections");
                        numlaps = jsonArray.getJSONObject(index).getString("numlaps");
                        name = jsonArray.getJSONObject(index).getString("name");

                        theTrial.put("id", id);
                        theTrial.put("numsections", numsections);
                        theTrial.put("numlaps", numlaps);
                        theTrial.put("name", name);
                        theTrialData.add(theTrial);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return theTrialData;
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
                    //Toast.makeText(MainActivity.this, "SocketTimeoutException", Toast.LENGTH_LONG).show();
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


    public void saveCurrentValuesToModel(int ridingNumber, int score, int section, int trialid, int numlaps, int numsections) {
        this.ridingNumber = ridingNumber;
        this.score = score;
        this.section = section;
        this.trialid = trialid;
        this.numlaps = numlaps;
        this.numsections = numsections;
    }

}
