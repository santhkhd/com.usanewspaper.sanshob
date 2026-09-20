package com.app.webdroid.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.util.MalayalamSongManager;
import com.google.android.material.card.MaterialCardView;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.List;

public class AdapterYearPicker extends RecyclerView.Adapter<AdapterYearPicker.YearViewHolder> {

    private final Context context;
    private final List<String> allYears = new ArrayList<>();
    private final List<String> displayYears = new ArrayList<>();
    private String selectedYear = null;

    public interface OnYearSelectedListener {
        void onYearSelected(String year);
    }

    private final OnYearSelectedListener listener;

    public AdapterYearPicker(Context context, OnYearSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setYears(List<String> years, String currentSelected) {
        this.allYears.clear();
        this.displayYears.clear();
        this.selectedYear = currentSelected;
        if (years != null) {
            this.allYears.addAll(years);
            this.displayYears.addAll(years);
        }
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String query) {
        displayYears.clear();
        if (query == null || query.trim().isEmpty()) {
            displayYears.addAll(allYears);
        } else {
            String q = query.trim();
            for (String y : allYears) {
                if (y.contains(q)) {
                    displayYears.add(y);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public YearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_year_chip, parent, false);
        return new YearViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull YearViewHolder holder, int position) {
        String year = displayYears.get(position);
        holder.bind(year);
    }

    @Override
    public int getItemCount() {
        return displayYears.size();
    }

    public class YearViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final TextView tvYear;
        final TextView tvCount;

        public YearViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_year_root);
            tvYear = itemView.findViewById(R.id.tv_year_text);
            tvCount = itemView.findViewById(R.id.tv_year_song_count);
        }

        public void bind(String year) {
            tvYear.setText(year);
            int count = MalayalamSongManager.getInstance().getSongCountForYear(year);
            tvCount.setText(count + " songs");

            boolean isSelected = (selectedYear != null && selectedYear.equals(year));
            int accentColor = androidx.core.content.ContextCompat.getColor(context, R.color.colorAccent);
            if (isSelected) {
                cardRoot.setStrokeColor(accentColor);
                cardRoot.setStrokeWidth(3);
                cardRoot.setCardBackgroundColor(Color.parseColor("#1AE11D48"));
                tvYear.setTextColor(accentColor);
                tvCount.setTextColor(accentColor);
            } else {
                cardRoot.setStrokeColor(Color.parseColor("#25888888"));
                cardRoot.setStrokeWidth(1);
                cardRoot.setCardBackgroundColor(Color.TRANSPARENT);
                tvYear.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.color_light_text_primary));
                tvCount.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.color_light_text_secondary));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onYearSelected(year);
                }
            });
        }
    }
}
