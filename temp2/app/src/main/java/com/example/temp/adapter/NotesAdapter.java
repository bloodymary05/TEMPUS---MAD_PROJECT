package com.example.temp.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temp.R;
import com.example.temp.model.NotesModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private final List<NotesModel> visibleList = new ArrayList<>();
    private final List<NotesModel> fullList = new ArrayList<>();
    private final Context context;

    public NotesAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<NotesModel> newData) {
        fullList.clear();
        visibleList.clear();
        if (newData != null) {
            fullList.addAll(newData);
            visibleList.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_notes, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesAdapter.ViewHolder holder, int position) {
        NotesModel m = visibleList.get(position);

        holder.noteName.setText(m.getName());

        String meta = "Subject: " + safe(m.getSubject());
        if (m.getYear() != null && !m.getYear().trim().isEmpty()) {
            meta += " • Year: " + m.getYear();
        }
        meta += " • Uploader: " + safe(m.getUploadedBy());
        holder.noteMeta.setText(meta);

        holder.itemView.setOnClickListener(v -> openNote(m));
    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView noteName, noteMeta;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            noteName = itemView.findViewById(R.id.noteName);
            noteMeta  = itemView.findViewById(R.id.noteMeta);
        }
    }

    private String safe(String s) { return s == null ? "" : s; }

    public void filter(String query) {
        visibleList.clear();
        if (query == null || query.trim().isEmpty()) {
            visibleList.addAll(fullList);
        } else {
            String q = query.toLowerCase();
            for (NotesModel m : fullList) {
                if ((m.getName() != null && m.getName().toLowerCase().contains(q)) ||
                        (m.getSubject() != null && m.getSubject().toLowerCase().contains(q)) ||
                        (m.getYear() != null && m.getYear().toLowerCase().contains(q)) ||
                        (m.getUploadedBy() != null && m.getUploadedBy().toLowerCase().contains(q))) {
                    visibleList.add(m);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void openNote(NotesModel m) {
        // API endpoint designed for viewing
        String viewUrl = "https://tempus-api.neurotechh.xyz/notes/view/" + m.getId();

        // Wrap in Google Docs Viewer for perfect streaming
        String googleViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + viewUrl;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleViewerUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "No browser found to open PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodePathPreservingSlashes(String rawPath) {
        if (rawPath == null) return "";
        String[] parts = rawPath.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            try {
                sb.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
            } catch (UnsupportedEncodingException e) {
                sb.append(parts[i]);
            }
            if (i < parts.length - 1) sb.append("/");
        }
        return sb.toString();
    }
}
