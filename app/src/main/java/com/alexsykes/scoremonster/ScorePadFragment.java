package com.alexsykes.scoremonster;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class ScorePadFragment extends Fragment implements View.OnClickListener {
    TextView score;

    public ScorePadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_score_pad, container, false);
    }

    @Override
    public void onClick(View v) {

    }

    public void addDigit(View view) {
    }

/*
    public void enterScore(View view) {
        score = view.findViewById(R.id.numberLabel);


        // Get id from clicked button to get clicked digit
        int intID = view.getId();
        Button button = view.findViewById(intID);
        String digit = button.getText().toString();

        score.setText(digit);
    }*/
}
