package com.app.webdroid.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.shobmc.san.R;
import com.app.webdroid.activity.ActivitySlider;
import com.app.webdroid.model.Slider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class AdapterSlider extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Slider> items;
    Context context;
    boolean loading;

    public AdapterSlider(Context context, List<Slider> items) {
        this.items = items;
        this.context = context;
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {

        ImageView imgIntro;
        TextView txtIntroTitle;
        TextView txtIntroMessage;
        LinearLayout lytBackground;
        Button btnGetStarted;

        public OriginalViewHolder(View v) {
            super(v);
            imgIntro = v.findViewById(R.id.img_intro);
            txtIntroTitle = v.findViewById(R.id.txt_intro_title);
            txtIntroMessage = v.findViewById(R.id.txt_intro_message);
            lytBackground = v.findViewById(R.id.lyt_background);
            btnGetStarted = v.findViewById(R.id.btn_get_started);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro, parent, false);
        vh = new OriginalViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Slider slider = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.txtIntroTitle.setText(slider.title);
            vItem.txtIntroMessage.setText(slider.message);

            vItem.lytBackground.setBackgroundColor(Color.parseColor("#" + slider.background_color));

            Glide.with(context)
                    .load(slider.image.replace(" ", "%20"))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(vItem.imgIntro);

            if (position == (items.size() - 1)) {
                vItem.btnGetStarted.setVisibility(View.VISIBLE);
                vItem.btnGetStarted.setOnClickListener(view -> ((ActivitySlider) context).startMainActivity());
            } else {
                vItem.btnGetStarted.setVisibility(View.GONE);
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void insertData(List<Slider> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

}