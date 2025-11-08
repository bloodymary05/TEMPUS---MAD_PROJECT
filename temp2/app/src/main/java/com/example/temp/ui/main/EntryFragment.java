package com.example.temp.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.temp.MainActivity;
import com.example.temp.R;

public class EntryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Delay transition by 3 seconds
        new Handler().postDelayed(this::switchToMainGraph, 3000);
        
    }

    private void switchToMainGraph() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToMainGraph();
        }
    }
}
