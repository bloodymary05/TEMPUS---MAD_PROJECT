package com.example.temp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheHelper {
    private static final String PREFS_NAME = "tempus_cache";

    public static void saveJson(Context context, String key, String json) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(key, json).apply();
    }

    public static String getJson(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static void clear(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }
}
