package com.example.temp.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TimetableRow {
    private String timeDay;
    private Map<String, String> dayLessons;

    public TimetableRow(JSONObject jsonObject) throws JSONException {
        this.dayLessons = new HashMap<>();

        if (jsonObject.has("Time/Day")) {
            this.timeDay = jsonObject.getString("Time/Day");
        }

        String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
        for (String day : days) {
            if (jsonObject.has(day)) {
                dayLessons.put(day, jsonObject.getString(day));
            } else {
                dayLessons.put(day, "");
            }
        }
    }

    public String getTimeDay() {
        return timeDay;
    }

    public Map<String, String> getDayLessons() {
        return dayLessons;
    }

    public String getLesson(String day) {
        return dayLessons.getOrDefault(day, "");
    }
}
