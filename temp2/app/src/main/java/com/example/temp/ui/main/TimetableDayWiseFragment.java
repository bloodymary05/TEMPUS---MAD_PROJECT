package com.example.temp.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.temp.R;
import com.example.temp.adapter.ClassSlotAdapter;
import com.example.temp.model.ClassSlot;
import com.example.temp.utils.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimetableDayWiseFragment extends Fragment {

    private JSONObject timetableData;
    private RecyclerView classRecyclerView;
    private TextView emptyStateText;
    private Button selectedDayButton = null;

    private final String[] days = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (getArguments() != null && getArguments().containsKey("json")) {
                timetableData = new JSONObject(getArguments().getString("json"));
            } else {
                String savedJson = Prefs.getTimetable(requireContext());
                if (savedJson != null) timetableData = new JSONObject(savedJson);
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable_daywise, container, false);

        classRecyclerView = view.findViewById(R.id.classRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        Button deleteBtn = view.findViewById(R.id.deleteTimetableBtn);
        LinearLayout daySelector = view.findViewById(R.id.daySelector);

        classRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupDayButtons(daySelector);
        loadDay(getToday()); // Highlight current day automatically

        // ✅ Delete timetable & go back to upload screen
        deleteBtn.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Timetable?")
                    .setMessage("Your timetable will be permanently removed. This cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        Prefs.clearTimetable(requireContext());
                        NavController navController = Navigation.findNavController(requireView());
                        navController.navigate(R.id.timetableInputFragment);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        // ✅ Apply Edge-to-Edge padding respecting system bars
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom);
            return insets;
        });

        return view;
    }

    private void setupDayButtons(LinearLayout selector) {
        for (String day : days) {
            Button btn = new Button(getContext());
            btn.setText(day.substring(0, 3).toUpperCase());
            btn.setAllCaps(false);
            btn.setPadding(30, 16, 30, 16);

            // ✅ Make text black
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));

            btn.setBackgroundResource(R.drawable.day_selector_button);

            btn.setOnClickListener(v -> {
                highlightButton(btn);
                loadDay(day);
            });

            selector.addView(btn);

            // ✅ Auto-select today
            if (day.equalsIgnoreCase(getToday())) selectedDayButton = btn;
        }

        // Default highlight
        if (selectedDayButton != null) highlightButton(selectedDayButton);
        else highlightButton((Button) selector.getChildAt(0));
    }

    private void highlightButton(Button btn) {
        if (selectedDayButton != null) {
            selectedDayButton.setBackgroundResource(R.drawable.day_selector_button);
            selectedDayButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        }

        btn.setBackgroundResource(R.drawable.day_selector_button_selected);
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        selectedDayButton = btn;
    }

    private void loadDay(String day) {
        try {
            JSONArray arr = timetableData.getJSONArray("Timetable");
            List<ClassSlot> slots = new ArrayList<>();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject row = arr.getJSONObject(i);
                String time = row.getString("Time/Day");
                String subject = row.optString(day, "").trim();

                if (!subject.isEmpty() && !subject.equalsIgnoreCase("BREAK")) {
                    slots.add(new ClassSlot(time, subject));
                }
            }

            if (slots.isEmpty()) {
                emptyStateText.setVisibility(View.VISIBLE);
                classRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                classRecyclerView.setVisibility(View.VISIBLE);
                classRecyclerView.setAdapter(new ClassSlotAdapter(slots));
            }

        } catch (Exception ignored) {}
    }

    private String getToday() {
        return new SimpleDateFormat("EEEE").format(new Date());
    }
}
