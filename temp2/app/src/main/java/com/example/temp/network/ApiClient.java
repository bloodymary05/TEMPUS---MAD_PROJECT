package com.example.temp.network;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ApiClient {
    private static final String BASE_URL = "https://tempus-api.neurotechh.xyz";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();

    // Crowd Management API endpoints
    public static String getCrowdEndpoint() {
        return BASE_URL + "/crowd";
    }

    // Timetable API endpoints
    public static String getTimetableEndpoint() {
        return BASE_URL + "/ocr/extract-timetable";
    }

    // Navigation API endpoints
    public static String getNavigationEndpoint() {
        return BASE_URL + "/floor";
    }

    // Notes API endpoints
    public static String getNotesEndpoint() {
        return BASE_URL + "/notes";
    }

    public static String getNotesMetadataEndpoint() {
        return BASE_URL + "/app/notes/metadata.json";
    }

    public static Request.Builder getRequestBuilder() {
        return new Request.Builder()
                .addHeader("Content-Type", "application/json");
    }

    public static OkHttpClient getClient() {
        return client;
    }
}
