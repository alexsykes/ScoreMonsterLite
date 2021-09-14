package com.alexsykes.scoremonster;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;


/**
 * A simple {@link Fragment} subclass.
 * <p>
 * Just more to do
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

    @Override
    public void onClick(View v) {

    }

    public void addDigit(View view) {
    }
}
