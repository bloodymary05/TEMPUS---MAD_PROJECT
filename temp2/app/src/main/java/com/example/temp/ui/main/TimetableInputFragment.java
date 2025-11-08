package com.example.temp.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.temp.R;
import com.example.temp.utils.Prefs;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

public class TimetableInputFragment extends Fragment {

    private static final int PICK_FILE_REQUEST_CODE = 101;
    private static final String API_URL = "https://tempus-api.neurotechh.xyz/ocr/extract-timetable";

    private Button uploadButton;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timetable_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String savedJson = Prefs.getTimetable(requireContext());
        if (savedJson != null) {
            Bundle bundle = new Bundle();
            bundle.putString("json", savedJson);

            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.timetableDayWiseFragment, bundle);
            return; // <- Avoid reloading upload UI
        }

        uploadButton = view.findViewById(R.id.uploadButton);
        progressBar = view.findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);

        uploadButton.setOnClickListener(v -> openFilePicker());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            uploadTimetable(data.getData());
        }
    }

    private void uploadTimetable(@NonNull Uri fileUri) {
        uploadButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                File file = getFileFromUri(fileUri);
                if (file == null) {
                    showError("Failed to read file");
                    return;
                }

                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("application/pdf")))
                        .build();

                Request request = new Request.Builder().url(API_URL).post(requestBody).build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    showError("API Error: " + response.code());
                    return;
                }

                String body = response.body().string();
                JSONObject root = new JSONObject(body);

                JSONObject dataObj = root
                        .getJSONArray("results")
                        .getJSONObject(0)
                        .getJSONObject("data");

                // ✅ Save timetable permanently
                Prefs.saveTimetable(requireContext(), dataObj.toString());


                requireActivity().runOnUiThread(() -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("json", dataObj.toString());

                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.timetableDayWiseFragment, bundle);

                    showSuccess("Timetable Loaded ✅");
                });

            } catch (Exception e) {
                Log.e("ERROR", "Upload failed", e);
                showError("Error: " + e.getMessage());
            } finally {
                requireActivity().runOnUiThread(() -> {
                    uploadButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private File getFileFromUri(@NonNull Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            String fileName = getFileName(uri);
            File file = new File(requireContext().getCacheDir(), fileName);

            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            outputStream.close();
            inputStream.close();
            return file;

        } catch (Exception e) {
            return null;
        }
    }

    private String getFileName(@NonNull Uri uri) {
        String result = null;
        var cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst())
            result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        if (cursor != null) cursor.close();
        return result != null ? result : "temp.pdf";
    }

    private void showError(String msg) {
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void showSuccess(String msg) {
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show());
    }
}
