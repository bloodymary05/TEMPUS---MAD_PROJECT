package com.example.temp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.temp.R;
import com.example.temp.model.ClassSlot;

import java.util.List;

public class ClassSlotAdapter extends RecyclerView.Adapter<ClassSlotAdapter.ViewHolder> {

    private List<ClassSlot> slots;

    public ClassSlotAdapter(List<ClassSlot> slots) {
        this.slots = slots;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView classMainText, classTimeText;

        public ViewHolder(View v) {
            super(v);
            classMainText = v.findViewById(R.id.classMainText);
            classTimeText = v.findViewById(R.id.classTimeText);
        }
    }

    @NonNull
    @Override
    public ClassSlotAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class_slot, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassSlot slot = slots.get(position);
        holder.classMainText.setText(slot.subject);
        holder.classTimeText.setText(slot.time);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }
}
