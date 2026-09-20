package com.app.webdroid.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.database.prefs.SharedPref;
import com.shobmc.san.R;

import java.util.List;

public class AdapterAiCategoryChips extends RecyclerView.Adapter<AdapterAiCategoryChips.ViewHolder> {

    public static class CategoryChip {
        public String name;
        public int count;

        public CategoryChip(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    public interface OnCategorySelectedListener {
        void onCategorySelected(String categoryName);
    }

    private final Context context;
    private final List<CategoryChip> chips;
    private final OnCategorySelectedListener listener;
    private int selectedPosition = 0;
    private final boolean isDark;

    public AdapterAiCategoryChips(Context context, List<CategoryChip> chips, OnCategorySelectedListener listener) {
        this.context = context;
        this.chips = chips;
        this.listener = listener;
        this.isDark = new SharedPref(context).getIsDarkTheme();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_category_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryChip chip = chips.get(position);
        boolean isSelected = position == selectedPosition;

        holder.textName.setText(chip.name);
        holder.textCount.setText(String.valueOf(chip.count));

        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.bg_search_chip_active);
            holder.textName.setTextColor(Color.WHITE);
            holder.textCount.setBackgroundResource(R.drawable.bg_badge_breaking);
            holder.textCount.setTextColor(Color.WHITE);
        } else {
            if (isDark) {
                holder.container.setBackgroundResource(R.drawable.bg_search_chip_inactive_dark);
                holder.textName.setTextColor(ContextCompat.getColor(context, R.color.color_dark_text_primary));
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_search_chip_inactive_light);
                holder.textName.setTextColor(ContextCompat.getColor(context, R.color.color_light_text_primary));
            }
            holder.textCount.setBackgroundResource(R.drawable.bg_badge);
            holder.textCount.setTextColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategorySelected(chip.name);
            }
        });
    }

    public void selectFirst() {
        int oldPos = selectedPosition;
        selectedPosition = 0;
        notifyItemChanged(oldPos);
        notifyItemChanged(0);
    }

    @Override
    public int getItemCount() {
        return chips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        TextView textName;
        TextView textCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.chip_container);
            textName = itemView.findViewById(R.id.text_chip_name);
            textCount = itemView.findViewById(R.id.text_chip_count);
        }
    }
}
