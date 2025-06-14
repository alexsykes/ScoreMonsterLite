package uk.trialmonster.observer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PadFragment extends Fragment {

    public PadFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PadFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PadFragment newInstance(String param1, String param2) {
        PadFragment fragment = new PadFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public void scoreClean(View view) {
        Log.i("Info", "scoreClean: ");
        // Get id from clicked button to get clicked digit
//        int intID = view.getId();
//        Button button = view.findViewById(intID);
//        String digit = button.getText().toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pad, container, false);
    }
}