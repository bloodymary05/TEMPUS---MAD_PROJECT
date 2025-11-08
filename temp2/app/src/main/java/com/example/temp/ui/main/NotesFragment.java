package com.example.temp.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temp.R;
import com.example.temp.adapter.NotesAdapter;
import com.example.temp.model.NotesModel;
import com.example.temp.network.RetrofitClient;
import com.example.temp.network.TempusApi;
import com.example.temp.network.UploadResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotesFragment extends Fragment {

    private RecyclerView recycler;
    private NotesAdapter adapter;
    private ProgressBar progress;
    private EditText search;
    private View fabUpload;

    private Uri selectedPdfUri = null;
    private TextView fileNameDisplay; // reference to update file name in dialog

    private final String API_URL = "https://tempus-api.neurotechh.xyz/notes/";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recycler = view.findViewById(R.id.recyclerNotes);
        progress = view.findViewById(R.id.progressNotes);
        search = view.findViewById(R.id.searchNotes);
        fabUpload = view.findViewById(R.id.fabUpload);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotesAdapter(requireActivity());
        recycler.setAdapter(adapter);

        fabUpload.setOnClickListener(v -> showUploadDialog());

        loadNotes();
        setupSearch();

        return view;
    }

    private void loadNotes() {
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                InputStream is = conn.getInputStream();
                String result = new java.util.Scanner(is).useDelimiter("\\A").next();

                JSONObject obj = new JSONObject(result);
                JSONArray arr = obj.getJSONArray("notes");

                List<NotesModel> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new NotesModel(
                            o.getString("id"),
                            o.getString("name"),
                            o.getString("subject"),
                            o.optString("year", ""),
                            o.getString("uploaded_by"),
                            o.getString("file_path")
                    ));
                }

                requireActivity().runOnUiThread(() -> {
                    adapter.setData(list);
                    progress.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> progress.setVisibility(View.GONE));
            }
        }).start();
    }

    private void setupSearch() {
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ------------------ Upload Dialog ------------------

    private void showUploadDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.dialog_upload_note, null);
        dialog.setContentView(sheet);
        dialog.show();

        EditText subject = sheet.findViewById(R.id.inputSubject);
        EditText year = sheet.findViewById(R.id.inputYear);
        EditText uploader = sheet.findViewById(R.id.inputUploader);
        fileNameDisplay = sheet.findViewById(R.id.textPickFile);
        View uploadBtn = sheet.findViewById(R.id.buttonUpload);

        fileNameDisplay.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pickPdfLauncher.launch(intent);
        });

        uploadBtn.setOnClickListener(v -> {
            if (selectedPdfUri == null) {
                Toast.makeText(getContext(), "Select a PDF first ❗", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadPdf(subject.getText().toString(), year.getText().toString(), uploader.getText().toString());
            dialog.dismiss();
        });
    }

    private final ActivityResultLauncher<Intent> pickPdfLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();

                    String name = "PDF Selected ✅";
                    Cursor c = requireContext().getContentResolver().query(selectedPdfUri, null, null, null, null);
                    if (c != null && c.moveToFirst()) {
                        name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                        c.close();
                    }
                    fileNameDisplay.setText(name + " ✅");
                }
            });

    private void uploadPdf(String subject, String year, String uploader) {
        try {
            // Copy URI → File
            Cursor meta = requireContext().getContentResolver().query(selectedPdfUri, null, null, null, null);
            meta.moveToFirst();
            String fileName = meta.getString(meta.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            meta.close();

            File file = new File(requireContext().getCacheDir(), fileName);
            InputStream in = requireContext().getContentResolver().openInputStream(selectedPdfUri);
            FileOutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024]; int len;
            while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            in.close(); out.close();

            RequestBody fileBody = RequestBody.create(MediaType.parse("application/pdf"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, fileBody);

            RequestBody sub = RequestBody.create(MultipartBody.FORM, subject);
            RequestBody yr = RequestBody.create(MultipartBody.FORM, year);
            RequestBody up = RequestBody.create(MultipartBody.FORM, uploader);

            TempusApi api = RetrofitClient.get().create(TempusApi.class);
            api.uploadNote(body, sub, yr, up).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    Toast.makeText(getContext(), "Uploaded ✅", Toast.LENGTH_SHORT).show();
                    loadNotes();
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(getContext(), "File Error", Toast.LENGTH_SHORT).show();
        }
    }
}
