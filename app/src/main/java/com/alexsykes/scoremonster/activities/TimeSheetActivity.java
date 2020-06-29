package com.alexsykes.scoremonster.activities;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.alexsykes.scoremonster.R;
import com.alexsykes.scoremonster.TimeListAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class TimeSheetActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://android.trialmonster.uk/";
    ArrayList<HashMap<String, String>> theTimeList;
    RecyclerView rvTimes;
    LinearLayoutManager llm;
    int trialid;
    ProgressDialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_sheet);
        llm = new LinearLayoutManager(this);

        // Get trialid form Intent
        trialid = getIntent().getExtras().getInt("trialid");
        String URL = BASE_URL + "getTimes.php?id=" + trialid;
        getJSONDataset(URL);
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

            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                dialog = ProgressDialog.show(TimeSheetActivity.this, "Scoremonster", "Getting times…", true);
            }

            //this method will be called after execution

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();
                theTimeList = populateResultArrayList(s);
                displayResultArrayList();
            }

            private ArrayList<HashMap<String, String>> populateResultArrayList(String json) {
                ArrayList<HashMap<String, String>> theList = new ArrayList<>();

                try {
                    // Parse string data into JSON
                    JSONArray jsonArray = new JSONArray(json);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        HashMap<String, String> theResult = new HashMap<>();
                        String name = jsonArray.getJSONObject(index).getString("name");
                        String number = jsonArray.getJSONObject(index).getString("number");
                        String ridetime = jsonArray.getJSONObject(index).getString("ridetime");
                        String timepenalty = jsonArray.getJSONObject(index).getString("timepenalty");

                        theResult.put("name", name);
                        theResult.put("number", number);
                        theResult.put("ridetime", ridetime);
                        theResult.put("timepenalty", timepenalty);
                        theList.add(theResult);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return theList;
            }

            @Override
            protected String doInBackground(Void... voids) {

                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    //StringBuilder object to read the string from the service
                    StringBuilder sb = new StringBuilder();

                    //We will use a buffered reader to read the string from service
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    //A simple string to read values from each line
                    String json;

                    //reading until we don't find null
                    while ((json = bufferedReader.readLine()) != null) {

                        //appending it to string builder
                        sb.append(json + "\n");
                    }

                    //finally returning the read string
                    return sb.toString().trim();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        //creating asynctask object and executing it
        GetData getJSON = new GetData();
        getJSON.execute();
    }

    private void displayResultArrayList() {

        rvTimes = findViewById(R.id.rvTimes);
        rvTimes.setLayoutManager(llm);
        rvTimes.setHasFixedSize(true);
        initializeAdapter();
    }

    private void initializeAdapter() {
        TimeListAdapter adapter = new TimeListAdapter(theTimeList);
        rvTimes.setAdapter(adapter);
    }
}
