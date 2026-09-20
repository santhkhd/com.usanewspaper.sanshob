package com.app.webdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.shobmc.san.R;
import com.app.webdroid.model.RadioStation;

import java.util.ArrayList;
import java.util.List;

public class AdapterRadio extends RecyclerView.Adapter<AdapterRadio.ViewHolder> {

    private final Context context;
    private List<RadioStation> items = new ArrayList<>();
    private RadioStation currentPlayingStation = null;
    private boolean isPlaying = false;
    private OnStationClickListener listener;

    public interface OnStationClickListener {
        void onStationClick(RadioStation station);
    }

    public AdapterRadio(Context context, OnStationClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<RadioStation> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void setPlaybackState(RadioStation station, boolean playing) {
        this.currentPlayingStation = station;
        this.isPlaying = playing;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_radio_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RadioStation station = items.get(position);
        holder.txtName.setText(station.getName());
        holder.txtDesc.setText(station.getDescription() != null ? station.getDescription() : station.getCategory());

        if (station.getImage() != null && !station.getImage().isEmpty()) {
            Glide.with(context)
                    .load(station.getImage())
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(200))
                    .placeholder(R.drawable.ic_radio)
                    .error(R.drawable.ic_radio)
                    .fitCenter()
                    .into(holder.imgStation);
        } else {
            holder.imgStation.setImageResource(R.drawable.ic_radio);
        }

        boolean isCurrent = currentPlayingStation != null && currentPlayingStation.getStreamUrl().equals(station.getStreamUrl());

        if (isCurrent) {
            holder.cardStation.setStrokeColor(ContextCompat.getColor(context, R.color.color_light_primary));
            holder.cardStation.setStrokeWidth(4);
            if (isPlaying) {
                holder.btnIndicator.setImageResource(R.drawable.ic_pause);
                holder.btnIndicator.setColorFilter(ContextCompat.getColor(context, R.color.color_light_primary));
            } else {
                holder.btnIndicator.setImageResource(R.drawable.ic_play_circle);
                holder.btnIndicator.setColorFilter(ContextCompat.getColor(context, R.color.color_light_primary));
            }
        } else {
            holder.cardStation.setStrokeColor(ContextCompat.getColor(context, R.color.color_card_border));
            holder.cardStation.setStrokeWidth(1);
            holder.btnIndicator.setImageResource(R.drawable.ic_play_circle);
            holder.btnIndicator.setColorFilter(ContextCompat.getColor(context, R.color.color_light_primary));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStationClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardStation;
        ImageView imgStation;
        TextView txtName;
        TextView txtDesc;
        ImageView btnIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardStation = itemView.findViewById(R.id.card_station);
            imgStation = itemView.findViewById(R.id.img_station);
            txtName = itemView.findViewById(R.id.txt_station_name);
            txtDesc = itemView.findViewById(R.id.txt_station_desc);
            btnIndicator = itemView.findViewById(R.id.btn_play_indicator);
        }
    }
}
