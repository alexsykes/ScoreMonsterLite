package com.alexsykes.scoremonster;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class NumberPadFragment extends Fragment implements View.OnClickListener {
    TextView riderNumber;


    public NumberPadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_number_pad, container, false);
    }

    public void setRiderNumber(String theNumber) {
        riderNumber.setText(theNumber);
    }

    public TextView getRiderNumber(){
        return riderNumber;
    }

    @Override
    public void onClick(View v) {

    }

    public void addDigit(View view) {
    }
}
