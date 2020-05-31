package com.alexsykes.scoremonster.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.alexsykes.scoremonster.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SummaryScoreActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://android.trialmonster.uk/";
    TableLayout resultTable;
    int trialid, numsections, numlaps;
    ArrayList<HashMap<String, String>> theResultList;
    ProgressDialog dialog = null;
    SharedPreferences localPrefs;
    int backgroundColor = Color.parseColor("#40bdc0d4");
    int white = Color.parseColor("#ffffff");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_score);

        localPrefs = getSharedPreferences("monster", MODE_PRIVATE);

        numlaps = localPrefs.getInt("numlaps", 0);
        numsections = localPrefs.getInt("numsections", 0);

        trialid = getIntent().getExtras().getInt("trialid");
        String URL = BASE_URL + "getSummaryScores.php?id=" + trialid;
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

            //this method will be called before execution

            @Override
            protected void onPreExecute() {

                super.onPreExecute();
                //dialog = ProgressDialog.show(SummaryScoreActivity.this, "Scoremonster", "Getting leaderboard…", true);

                dialog = new ProgressDialog(SummaryScoreActivity.this);
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

            //this method will be called after execution

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                dialog.dismiss();
                theResultList = populateResultArrayList(s);
                // Define table for results
                displayResultTable(theResultList);

            }

            private ArrayList<HashMap<String, String>> populateResultArrayList(String json) {
                ArrayList<HashMap<String, String>> theResultList = new ArrayList<>();
                final int TIMEOUT = 5000;

                try {
                    // Parse string data into JSON
                    JSONArray jsonArray = new JSONArray(json);

                    for (int index = 0; index < jsonArray.length(); index++) {
                        HashMap<String, String> theResult = new HashMap<>();
                        String rider = jsonArray.getJSONObject(index).getString("rider");
                        String sections = jsonArray.getJSONObject(index).getString("sections");
                        String scorelist = jsonArray.getJSONObject(index).getString("scorelists");
                        String totalscore = jsonArray.getJSONObject(index).getString("totalscore");
                        theResult.put("rider", rider);
                        theResult.put("scorelist", scorelist);
                        theResult.put("sections", sections);
                        theResult.put("totalscore", totalscore);
                        theResultList.add(theResult);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return theResultList;
            }

            //in this method we are fetching the json string
            @Override
            protected String doInBackground(Void... voids) {
                final int TIMEOUT = 5000;

                try {
                    //creating a URL
                    URL url = new URL(urlWebService);

                    //Opening the URL using HttpURLConnection
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(TIMEOUT);
                    con.setReadTimeout(TIMEOUT);

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
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }
        }

        //creating asynctask object and executing it
        GetData getJSON = new GetData();
        getJSON.execute();
    }

    private void displayResultTableOld(ArrayList<HashMap<String, String>> theResultList) {
        String colStr;
        TableRow tr = new TableRow(this);
        TextView cell = new TextView(this);
        cell.setText(R.string.rider);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(8, 8, 8, 8);
        tr.addView(cell);


        for (int col = 1; col < numsections + 1; col++) {
            cell = new TextView(this);
            colStr = String.valueOf(col);
            cell.setText(colStr);
            cell.setGravity(Gravity.CENTER);
            resultTable.setColumnShrinkable(col, true);

            cell.setWidth(2000);
            cell.setPadding(8, 8, 8, 8);
            tr.addView(cell);
        }
        cell = new TextView(this);
        cell.setText(R.string.total);
        cell.setPadding(8, 8, 8, 8);
        cell.setWidth(120);
        cell.setGravity(Gravity.CENTER);
        tr.addView(cell);

        resultTable.addView(tr);


        int numRiders = theResultList.size();

        // Iterate through result list
        // Adding a row for each one
        for (int index = 0; index < numRiders; index++) {

            tr = new TableRow(this);
            if (index % 2 != 0) {
                tr.setBackgroundColor(backgroundColor);
            } else {
                tr.setBackgroundColor(white);
            }

            HashMap<String, String> theResult = theResultList.get(index);

            // Get data from arraylist
            String rider = theResult.get("rider");
            String sections = theResult.get("sections");
            String scorelist = theResult.get("scorelist");

            // Then split to create arrays for section scores
            // and create a pointer to go through arrays
            // // to take account of missing sections

            int pointer;
            int numscores;
            String[] theSectionArray = sections.split(",");
            String[] theScoreArray = scorelist.split(",");
            String[] theScoreValues = new String[numsections];

            numscores = theSectionArray.length;

            //
            for (int i = 0; i < numscores; i++) {
                pointer = Integer.valueOf(theSectionArray[i]) - 1;
                theScoreValues[pointer] = theScoreArray[i];
            }

            // Add rider number at start of each line
            cell = new TextView(this);
            cell.setText(rider);
            cell.setGravity(Gravity.END);
            cell.setPadding(8, 8, 8, 8);
            tr.addView(cell);

            // Iterate through sections, adding rider number, section scores, total
            for (int section = 0; section < numsections; section++) {
                cell = new TextView(this);


                if (theScoreValues[section] != null) {
                    cell.setText(theScoreValues[section]);
                }
/*                else {
                    //   cell.setText("|");
                }*/

                cell.setPadding(40, 8, 8, 8);
                //cell.setGravity(Gravity.END);
                tr.addView(cell);
            }

            // Add cell for total
            cell = new TextView(this);
            String total = theResult.get("totalscore");
            cell.setText(total);
            cell.setGravity(Gravity.END);
            cell.setPadding(24, 8, 40, 8);
            tr.addView(cell);

            resultTable.addView(tr);
        }
    }

    private void displayResultTable(ArrayList<HashMap<String, String>> theResultList) {

        TableLayout resultTable;
        TableRow row;
        TextView cell;
        String section, text, rider, total;
        int numResults;
        HashMap<String, String> theResult;

        resultTable = findViewById(R.id.result_table);
        int textColor = getColor(R.color.colorButtonBlue);

        // Setup header row
        row = new TableRow(this);
        cell = new TextView(this);

        cell.setText(R.string.rider);
        cell.setPadding(20, 8, 20, 8);
        cell.setGravity(View.TEXT_ALIGNMENT_CENTER);
        cell.setTextColor(textColor);
        cell.setTypeface(null, Typeface.BOLD);
        row.addView(cell);

        // Setup section header
        for (int sect = 0; sect < numsections; sect++) {
            cell = new TextView(this);
            section = String.valueOf(sect + 1);
            cell.setText(section);
            cell.setTextColor(textColor);
            cell.setTypeface(null, Typeface.BOLD);
            cell.setMinWidth(40);
            cell.setPadding(20, 8, 20, 8);
            cell.setGravity(View.TEXT_ALIGNMENT_CENTER);
            resultTable.setColumnStretchable(sect + 1, true);
            row.addView(cell);
        }
        cell = new TextView(this);
        cell.setText(R.string.total);
        cell.setTextColor(textColor);
        cell.setTypeface(null, Typeface.BOLD);
        cell.setPadding(20, 8, 20, 8);
        cell.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        row.addView(cell);

        // Add row to table
        resultTable.addView(row);


        // Set up data table
        numResults = theResultList.size();

        // Define colours for stripes
        int backgroundColor = Color.parseColor("#40bdc0d4");
        int white = Color.parseColor("#ffffff");

        // Setup one row for each result
        for (int rowIndex = 0; rowIndex < numResults; rowIndex++) {
            // Get current result
            theResult = theResultList.get(rowIndex);
            rider = theResult.get("rider");
            total = theResult.get("totalscore");
            row = new TableRow(this);

            if (rowIndex % 2 != 0) {
                row.setBackgroundColor(backgroundColor);
            } else {
                row.setBackgroundColor(white);
            }

            // Add riding number
            cell = new TextView(this);
            cell.setText(rider);
            cell.setPadding(20, 8, 20, 8);
            cell.setWidth(80);
            cell.setTextColor(textColor);
            cell.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            row.addView(cell);
            for (int sect = 0; sect < numsections; sect++) {
                cell = new TextView(this);
                row.addView(cell);
            }

            // Add totals
            cell = new TextView(this);
            cell.setText(total);
            cell.setWidth(80);
            cell.setPadding(20, 8, 20, 8);
            cell.setTextColor(textColor);
            cell.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

            row.addView(cell);
            resultTable.addView(row);
        }

        // Prepare array of scores
        String[][] theScoreArray = new String[numResults][numsections];
        String sectionList, scoreList;
        String theScore;

        for (int index = 0; index < numResults; index++) {
            theResult = theResultList.get(index);
            sectionList = theResult.get("sections");
            scoreList = theResult.get("scorelist");
            String[] theSectionArray = sectionList.split(",");
            String[] theSectionScoreArray = scoreList.split(",");
            int numItems = theSectionScoreArray.length;

            // Get row for result insertion
            row = (TableRow) resultTable.getChildAt(index + 1);

            // Populate row from result list
            for (int sec = 0; sec < numItems; sec++) {

                int sectionNumber = Integer.valueOf(theSectionArray[sec]);
                theScore = theSectionScoreArray[sec];
                cell = (TextView) row.getChildAt(sectionNumber);
                cell.setText(theScore);
                cell.setTextColor(textColor);
            }
        }
    }
}
