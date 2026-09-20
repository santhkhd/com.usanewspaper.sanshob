package com.app.webdroid.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shobmc.san.R;
import com.app.webdroid.adapter.AdapterRadio;
import com.app.webdroid.model.RadioStation;
import com.app.webdroid.service.RadioService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ActivityRadioPlayer extends AppCompatActivity implements RadioService.RadioStateCallback {

    private Toolbar toolbar;
    private ChipGroup chipGroup;
    private RecyclerView recyclerView;
    private AdapterRadio adapter;
    private List<RadioStation> allStations = new ArrayList<>();

    // Bottom player views
    private View bottomPlayerCard;
    private ImageView imgCurrentStation;
    private TextView txtCurrentTitle;
    private TextView txtCurrentStatus;
    private TextView badgeLive;
    private FloatingActionButton btnPlayPause;
    private ProgressBar playerProgress;

    private RadioService radioService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RadioService.RadioBinder binder = (RadioService.RadioBinder) service;
            radioService = binder.getService();
            isBound = true;
            radioService.setCallback(ActivityRadioPlayer.this);

            RadioStation current = radioService.getCurrentStation();
            if (current != null) {
                bottomPlayerCard.setVisibility(View.VISIBLE);
                updatePlayerUI(current, radioService.isPlaying(), radioService.isBuffering());
            } else if (!allStations.isEmpty()) {
                bottomPlayerCard.setVisibility(View.VISIBLE);
                updatePlayerUI(allStations.get(0), false, false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            radioService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.app.webdroid.util.Tools.getTheme(this);
        setContentView(R.layout.activity_radio_player);
        initViews();
        loadStationsFromJson();

        Intent intent = new Intent(this, RadioService.class);
        try {
            startService(intent);
        } catch (Exception ignored) {
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("24/7 US News & Talk Radio");
        }

        chipGroup = findViewById(R.id.chip_group_categories);
        recyclerView = findViewById(R.id.recycler_stations);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));

        adapter = new AdapterRadio(this, station -> {
            if (isBound && radioService != null) {
                bottomPlayerCard.setVisibility(View.VISIBLE);
                radioService.playStation(station);
            }
        });
        recyclerView.setAdapter(adapter);

        bottomPlayerCard = findViewById(R.id.bottom_player_card);
        imgCurrentStation = findViewById(R.id.img_current_station);
        txtCurrentTitle = findViewById(R.id.txt_current_title);
        txtCurrentStatus = findViewById(R.id.txt_current_status);
        badgeLive = findViewById(R.id.badge_live);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        playerProgress = findViewById(R.id.player_progress);

        // Fix Window Insets: Top Status Bar & Bottom Player navigation bar overlap
        View rootCoordinator = findViewById(R.id.coordinator_radio_root);
        com.google.android.material.appbar.AppBarLayout appBar = findViewById(R.id.appbar);
        if (rootCoordinator != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootCoordinator, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                if (appBar != null) {
                    appBar.setPadding(0, bars.top, 0, 0);
                }
                if (bottomPlayerCard != null) {
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams lp =
                            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) bottomPlayerCard.getLayoutParams();
                    int baseMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
                    lp.bottomMargin = baseMargin + bars.bottom;
                    bottomPlayerCard.setLayoutParams(lp);
                }
                if (recyclerView != null) {
                    int baseBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 130, getResources().getDisplayMetrics());
                    recyclerView.setPadding(
                            recyclerView.getPaddingStart(),
                            recyclerView.getPaddingTop(),
                            recyclerView.getPaddingEnd(),
                            baseBottom + bars.bottom
                    );
                }
                return insets;
            });
        }
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        ImageView btnClosePlayer = findViewById(R.id.btn_close_player);
        if (btnClosePlayer != null) {
            btnClosePlayer.setOnClickListener(v -> {
                if (isBound && radioService != null) {
                    radioService.stop();
                }
                bottomPlayerCard.setVisibility(View.GONE);
                adapter.setPlaybackState(null, false);
            });
        }

        btnPlayPause.setOnClickListener(v -> {
            if (isBound && radioService != null) {
                if (radioService.getCurrentStation() == null && !allStations.isEmpty()) {
                    radioService.playStation(allStations.get(0));
                } else {
                    radioService.toggle();
                }
            }
        });
    }

    private void loadStationsFromJson() {
        allStations.clear();
        List<String> categoriesList = new ArrayList<>();
        try {
            InputStream is = getAssets().open("news_radio.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONArray categoriesArray = new JSONArray(jsonStr);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<RadioStation>>(){}.getType();

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject catObj = categoriesArray.getJSONObject(i);
                String catName = catObj.getString("category");
                categoriesList.add(catName);
                JSONArray stationsArray = catObj.getJSONArray("stations");

                List<RadioStation> stations = gson.fromJson(stationsArray.toString(), listType);
                for (RadioStation s : stations) {
                    s.setCategory(catName);
                    allStations.add(s);
                }
            }

            adapter.setItems(allStations);
            setupCategoryChips(categoriesList);
            if (bottomPlayerCard != null && (radioService == null || radioService.getCurrentStation() == null) && !allStations.isEmpty()) {
                bottomPlayerCard.setVisibility(View.VISIBLE);
                updatePlayerUI(allStations.get(0), false, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load radio stations", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCategoryChips(List<String> categories) {
        if (chipGroup == null) return;
        chipGroup.removeAllViews();

        Chip chipAll = new Chip(this);
        chipAll.setId(View.generateViewId());
        chipAll.setText("All Stations (" + allStations.size() + ")");
        chipAll.setCheckable(true);
        chipAll.setChecked(true);
        styleChip(chipAll);
        chipGroup.addView(chipAll);

        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(cat);
            chip.setCheckable(true);
            styleChip(chip);
            chipGroup.addView(chip);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            Chip chip = findViewById(id);
            if (chip != null) {
                String text = chip.getText().toString();
                if (text.startsWith("All Stations")) {
                    filterCategory(null);
                } else {
                    filterCategory(text);
                }
            }
        });
    }

    private void styleChip(Chip chip) {
        chip.setChipBackgroundColorResource(R.color.color_radio_chip_bg);
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.color_radio_chip_text));
        chip.setChipStrokeColorResource(R.color.color_radio_chip_stroke);
        chip.setChipStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        chip.setCheckedIconTint(ContextCompat.getColorStateList(this, R.color.color_radio_chip_icon));
        chip.setChipCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics()));
    }

    private void filterCategory(String category) {
        if (category == null) {
            adapter.setItems(allStations);
            return;
        }

        List<RadioStation> filtered = new ArrayList<>();
        for (RadioStation s : allStations) {
            if (s.getCategory() != null && s.getCategory().equalsIgnoreCase(category.trim())) {
                filtered.add(s);
            }
        }
        adapter.setItems(filtered);
    }

    @Override
    public void onPlaybackStateChanged(RadioStation station, boolean isPlaying, boolean isBuffering) {
        runOnUiThread(() -> {
            if (station != null && (isPlaying || isBuffering)) {
                bottomPlayerCard.setVisibility(View.VISIBLE);
                updatePlayerUI(station, isPlaying, isBuffering);
                adapter.setPlaybackState(station, isPlaying);
            } else if (station != null) {
                updatePlayerUI(station, isPlaying, isBuffering);
                adapter.setPlaybackState(station, isPlaying);
            } else {
                bottomPlayerCard.setVisibility(View.GONE);
                adapter.setPlaybackState(null, false);
            }
        });
    }

    private void updatePlayerUI(RadioStation station, boolean isPlaying, boolean isBuffering) {
        if (station == null) return;

        txtCurrentTitle.setText(station.getName());

        if (isBuffering) {
            txtCurrentStatus.setText("Connecting live stream...");
            badgeLive.setVisibility(View.GONE);
            playerProgress.setVisibility(View.VISIBLE);
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else if (isPlaying) {
            txtCurrentStatus.setText("Playing Live • " + (station.getDescription() != null ? station.getDescription() : "High Quality"));
            badgeLive.setVisibility(View.VISIBLE);
            playerProgress.setVisibility(View.GONE);
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            txtCurrentStatus.setText("Paused");
            badgeLive.setVisibility(View.GONE);
            playerProgress.setVisibility(View.GONE);
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }

        if (station.getImage() != null && !station.getImage().isEmpty()) {
            Glide.with(this)
                    .load(station.getImage())
                    .placeholder(R.drawable.ic_radio)
                    .error(R.drawable.ic_radio)
                    .into(imgCurrentStation);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            if (radioService != null) {
                radioService.setCallback(null);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
