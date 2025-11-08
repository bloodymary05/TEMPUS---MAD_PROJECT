package com.example.temp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    private static final String PREF_NAME = "timetable_prefs";
    private static final String KEY_TIMETABLE = "saved_timetable";

    public static void saveTimetable(Context context, String json) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TIMETABLE, json).apply();
    }

    public static String getTimetable(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TIMETABLE, null);
    }

    public static void clearTimetable(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_TIMETABLE).apply();
    }
}
