package com.example.temp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.temp.network.RetrofitClient;
import com.example.temp.network.TempusApi;
import com.example.temp.network.UploadResponse;
import com.example.temp.ui.main.NotesFragment;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadNotesFragment extends Fragment {

    private EditText etSubject, etYear, etUploader;
    private TextView tvSelected;
    private Button btnPick, btnUpload;
    private ProgressBar progress;

    private Uri selectedUri;

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    selectedUri = uri;
                    tvSelected.setText(getDisplayName(requireContext(), uri));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_upload_notes, container, false);

        etSubject = v.findViewById(R.id.etSubject);
        etYear = v.findViewById(R.id.etYear);
        etUploader = v.findViewById(R.id.etUploader);
        tvSelected = v.findViewById(R.id.tvSelectedFile);
        btnPick = v.findViewById(R.id.btnPickFile);
        btnUpload = v.findViewById(R.id.btnUpload);
        progress = v.findViewById(R.id.progressUpload);

        btnPick.setOnClickListener(view -> {
            // PDFs primarily, but allow any note file if needed
            filePicker.launch(new String[]{"application/pdf", "*/*"});
        });

        btnUpload.setOnClickListener(view -> tryUpload());

        return v;
    }

    private void tryUpload() {
        String subject = etSubject.getText().toString().trim();
        String year = etYear.getText().toString().trim();
        String uploader = etUploader.getText().toString().trim();

        if (selectedUri == null) {
            toast("Please select a file.");
            return;
        }
        if (TextUtils.isEmpty(subject)) {
            etSubject.setError("Enter subject");
            return;
        }
        if (TextUtils.isEmpty(year)) {
            etYear.setError("Enter course year");
            return;
        }
        if (TextUtils.isEmpty(uploader)) {
            etUploader.setError("Enter your name");
            return;
        }

        setLoading(true);

        TempusApi api = RetrofitClient.get().create(TempusApi.class);

        try {
            String fileName = getDisplayName(requireContext(), selectedUri);
            MediaType mediaType = guessMediaType(requireContext(), selectedUri);

            RequestBody fileBody = new InputStreamRequestBody(
                    mediaType != null ? mediaType : MediaType.parse("application/octet-stream"),
                    requireContext().getContentResolver(),
                    selectedUri
            );

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", fileName, fileBody
            );

            RequestBody subjectPart = RequestBody.create(MediaType.parse("text/plain"), subject);
            RequestBody yearPart = RequestBody.create(MediaType.parse("text/plain"), year);
            RequestBody uploaderPart = RequestBody.create(MediaType.parse("text/plain"), uploader);

            Call<UploadResponse> call = api.uploadNote(filePart, subjectPart, yearPart, uploaderPart);
            call.enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        UploadResponse res = response.body();
                        if (res.success) {
                            toast("Uploaded successfully");
                            // Optional: pop back to notes list
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.contentContainer, new NotesFragment())
                                    .commit();
                        } else {
                            toast(res.message != null ? res.message : "Upload failed");
                        }
                    } else {
                        toast("Server error: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                    setLoading(false);
                    toast("Upload error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            setLoading(false);
            toast("Failed to read file: " + e.getMessage());
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnUpload.setEnabled(!loading);
        btnPick.setEnabled(!loading);
        etSubject.setEnabled(!loading);
        etYear.setEnabled(!loading);
        etUploader.setEnabled(!loading);
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    /** Guess content-type via ContentResolver and file extension */
    @Nullable
    private MediaType guessMediaType(Context ctx, Uri uri) {
        ContentResolver cr = ctx.getContentResolver();
        String type = cr.getType(uri);
        if (type != null) return MediaType.parse(type);

        String name = getDisplayName(ctx, uri);
        String ext = name != null && name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : null;
        if (ext != null) {
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            if (mime != null) return MediaType.parse(mime);
        }
        return MediaType.parse("application/octet-stream");
    }

    /** SAF-friendly filename lookup */
    private String getDisplayName(Context ctx, Uri uri) {
        String result = null;
        Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIdx >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(nameIdx);
            }
            cursor.close();
        }
        if (result == null) result = uri.getLastPathSegment();
        return result;
    }

    /** Stream a ContentResolver InputStream as a RequestBody (efficient for large files) */
    private static class InputStreamRequestBody extends RequestBody {
        private final MediaType contentType;
        private final ContentResolver resolver;
        private final Uri uri;

        InputStreamRequestBody(MediaType contentType, ContentResolver resolver, Uri uri) {
            this.contentType = contentType;
            this.resolver = resolver;
            this.uri = uri;
        }

        @Nullable
        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) throws IOException {
            try (InputStream is = resolver.openInputStream(uri)) {
                if (is == null) throw new IOException("Cannot open input stream");
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    sink.write(buffer, 0, read);
                }
            }
        }
    }
}
