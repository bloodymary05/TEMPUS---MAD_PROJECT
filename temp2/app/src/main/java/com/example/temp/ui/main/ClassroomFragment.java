package com.example.temp.ui.main;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.temp.R;
import com.example.temp.network.ApiClient;
import com.example.temp.utils.CacheHelper;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClassroomFragment extends Fragment {

    private Spinner typeSpinner;
    private EditText numberInput;
    private Button searchButton;
    private ImageView classroomImage;
    private TextView statusTextView;
    private static final String CACHE_KEY = "navigation_cache";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_classroom, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        typeSpinner = view.findViewById(R.id.typeSpinner);
        numberInput = view.findViewById(R.id.numberInput);
        searchButton = view.findViewById(R.id.searchButton);
        classroomImage = view.findViewById(R.id.classroomImage);
        statusTextView = view.findViewById(R.id.statusTextView);

        // setup spinner values
        String[] types = new String[]{"CC", "CL", "CR", "TR"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, types) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(Color.BLACK); // Selected item text color
                textView.setTextSize(16);
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);

                typeSpinner.setPopupBackgroundDrawable(new ColorDrawable(Color.WHITE)); // or any color

                // Change color per item (optional)
                switch (position) {
                    case 0: textView.setTextColor(Color.BLACK); break;
                    case 1: textView.setTextColor(Color.BLACK); break;
                    case 2: textView.setTextColor(Color.BLACK); break;
                    case 3: textView.setTextColor(Color.BLACK); break;
                }

                textView.setTextSize(16);
                return textView;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);


        searchButton.setOnClickListener(v -> searchClassroom());

        // If navigated here from Home with a query, prefill and search
        Bundle args = getArguments();
        if (args != null) {
            String q = args.getString("search_query", "").trim();
            if (!q.isEmpty()) {
                // Try to parse a room type prefix if provided (e.g., cc101 or cr301)
                String letters = q.replaceAll("[^A-Za-z]", "");
                String digits = q.replaceAll("[^0-9]", "");
                if (!letters.isEmpty()) {
                    // set spinner to the letters if they match available types
                    for (int i = 0; i < typeSpinner.getCount(); i++) {
                        String item = (String) typeSpinner.getItemAtPosition(i);
                        if (item.equalsIgnoreCase(letters)) {
                            typeSpinner.setSelection(i);
                            break;
                        }
                    }
                }
                if (!digits.isEmpty()) {
                    numberInput.setText(digits);
                    // trigger search after fields are filled
                    searchClassroom();
                }
            }
        }
    }
    private void searchClassroom() {
        String type = typeSpinner.getSelectedItem().toString().toLowerCase();
        String number = numberInput.getText().toString().trim();
        if (number.isEmpty()) {
            Toast.makeText(requireContext(), "Enter classroom number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construct drawable name (e.g. cc101 -> cc101)
        String resourceName = (type + number).toLowerCase();

        int resId = requireContext().getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());
        if (resId != 0) {
            classroomImage.setImageResource(resId);
            statusTextView.setText("Found: " + resourceName);
        } else {
            // Attempt to fetch from backend: /floor/image/{floor_number}/{filename}
            classroomImage.setImageDrawable(null);
            // Derive floor number from room number's leading digit
            char firstChar = number.charAt(0);
            String floor = String.valueOf(firstChar);
            String filename = resourceName + ".png";
            String url = ApiClient.getNavigationEndpoint() + "/image/" + floor + "/" + filename;
            statusTextView.setText("Fetching from server...");
            // download image asynchronously
            OkHttpClient client = ApiClient.getClient();
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> statusTextView.setText("No image found for: " + resourceName));
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        requireActivity().runOnUiThread(() -> statusTextView.setText("No image found for: " + resourceName));
                        return;
                    }
                    byte[] bytes = response.body().bytes();
                    final Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    requireActivity().runOnUiThread(() -> {
                        if (bmp != null) {
                            classroomImage.setImageBitmap(bmp);
                            statusTextView.setText("Loaded from server: " + resourceName);
                        } else {
                            statusTextView.setText("No image found for: " + resourceName);
                        }
                    });
                }
            });
        }
    }
}
