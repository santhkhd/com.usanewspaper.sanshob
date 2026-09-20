package com.app.webdroid.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterSongs;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.MalayalamSong;
import com.app.webdroid.util.MalayalamSongManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.List;

public class FragmentSongs extends Fragment {

    private static final int PAGE_SIZE = 40;

    private EditText etSearchSongs;
    private ImageView btnClearSearch;
    private TextView tvTotalCountBadge;
    private RecyclerView rvSongs;
    private View layoutLoading;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private ImageView btnBack;

    // Header Sort views
    private View btnToolbarSort;
    private TextView tvToolbarSortLabel;
    private TextView chipSortPicker;
    private TextView chipYearPicker;

    // Active Movie / Year Filter Bar
    private View layoutActiveFilterBar;
    private TextView tvActiveFilterLabel;
    private TextView btnClearMovieFilter;

    private AdapterSongs adapter;
    private SharedPref sharedPref;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Filter & Sort state
    private String currentQuery = "";
    private String selectedDecade = "all";
    private String selectedArtist = "all";
    private String selectedMovie = null;
    private String selectedYear = null;
    private String currentSortOrder = MalayalamSongManager.SORT_YEAR_DESC;

    // Pagination state
    private List<MalayalamSong> currentFilteredList = new ArrayList<>();
    private int currentLoadedCount = 0;
    private boolean isLoadingMore = false;

    // Filter chip views
    private final List<TextView> allChips = new ArrayList<>();
    private TextView chipAll, chip2020s, chip2010s, chip2000s, chip90s, chip80s, chipOldies;
    private TextView chipYesudas, chipChithra, chipJanaki, chipSujatha, chipSreekumar, chipJayachandran;

    public static FragmentSongs newInstance() {
        return new FragmentSongs();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        sharedPref = new SharedPref(requireContext());

        initViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupSortControls();
        setupSearch();

        loadSongDatabase();

        return view;
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        etSearchSongs = view.findViewById(R.id.et_search_songs);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        tvTotalCountBadge = view.findViewById(R.id.tv_total_count_badge);
        rvSongs = view.findViewById(R.id.rv_songs);
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title);

        btnToolbarSort = view.findViewById(R.id.btn_toolbar_sort);
        tvToolbarSortLabel = view.findViewById(R.id.tv_toolbar_sort_label);
        chipSortPicker = view.findViewById(R.id.chip_sort_picker);
        chipYearPicker = view.findViewById(R.id.chip_year_picker);

        layoutActiveFilterBar = view.findViewById(R.id.layout_active_filter_bar);
        tvActiveFilterLabel = view.findViewById(R.id.tv_active_filter_label);
        btnClearMovieFilter = view.findViewById(R.id.btn_clear_movie_filter);

        chipAll = view.findViewById(R.id.chip_all);
        chip2020s = view.findViewById(R.id.chip_2020s);
        chip2010s = view.findViewById(R.id.chip_2010s);
        chip2000s = view.findViewById(R.id.chip_2000s);
        chip90s = view.findViewById(R.id.chip_90s);
        chip80s = view.findViewById(R.id.chip_80s);
        chipOldies = view.findViewById(R.id.chip_oldies);
        chipYesudas = view.findViewById(R.id.chip_yesudas);
        chipChithra = view.findViewById(R.id.chip_chithra);
        chipJanaki = view.findViewById(R.id.chip_janaki);
        chipSujatha = view.findViewById(R.id.chip_sujatha);
        chipSreekumar = view.findViewById(R.id.chip_sreekumar);
        chipJayachandran = view.findViewById(R.id.chip_jayachandran);

        allChips.add(chipAll);
        if (chipYearPicker != null) allChips.add(chipYearPicker);
        allChips.add(chip2020s);
        allChips.add(chip2010s);
        allChips.add(chip2000s);
        allChips.add(chip90s);
        allChips.add(chip80s);
        allChips.add(chipOldies);
        allChips.add(chipYesudas);
        allChips.add(chipChithra);
        if (chipJanaki != null) allChips.add(chipJanaki);
        allChips.add(chipSujatha);
        allChips.add(chipSreekumar);
        if (chipJayachandran != null) allChips.add(chipJayachandran);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        if (btnClearMovieFilter != null) {
            btnClearMovieFilter.setOnClickListener(v -> {
                selectedMovie = null;
                selectedYear = null;
                if (chipYearPicker != null) chipYearPicker.setText("📆 Select Year ▾");
                layoutActiveFilterBar.setVisibility(View.GONE);
                highlightChip(chipAll);
                applyFilter();
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new AdapterSongs(requireContext());
        adapter.setInteractionListener(new AdapterSongs.OnSongInteractionListener() {
            @Override
            public void onMovieClick(String movieName) {
                filterByMovie(movieName);
            }

            @Override
            public void onShareSong(MalayalamSong song) {
                adapter.shareSong(song);
            }
        });

        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        rvSongs.setLayoutManager(lm);
        rvSongs.setAdapter(adapter);

        rvSongs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onScroll(dy);
                }
                if (dy > 0 && !isLoadingMore) {
                    int visibleCount = lm.getChildCount();
                    int totalCount = lm.getItemCount();
                    int firstVisible = lm.findFirstVisibleItemPosition();

                    if ((visibleCount + firstVisible) >= totalCount - 5) {
                        loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSortControls() {
        View.OnClickListener sortClickListener = v -> showSortBottomSheet();

        if (btnToolbarSort != null) {
            btnToolbarSort.setOnClickListener(sortClickListener);
        }
        if (chipSortPicker != null) {
            chipSortPicker.setOnClickListener(sortClickListener);
        }
    }

    private void showSortBottomSheet() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort_songs, null);
        dialog.setContentView(view);

        ImageView btnClose = view.findViewById(R.id.btn_close_sort);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // Option rows
        View optYearDesc = view.findViewById(R.id.opt_sort_year_desc);
        ImageView checkYearDesc = view.findViewById(R.id.check_sort_year_desc);

        View optYearAsc = view.findViewById(R.id.opt_sort_year_asc);
        ImageView checkYearAsc = view.findViewById(R.id.check_sort_year_asc);

        View optSongAsc = view.findViewById(R.id.opt_sort_song_asc);
        ImageView checkSongAsc = view.findViewById(R.id.check_sort_song_asc);

        View optSongDesc = view.findViewById(R.id.opt_sort_song_desc);
        ImageView checkSongDesc = view.findViewById(R.id.check_sort_song_desc);

        View optMovieAsc = view.findViewById(R.id.opt_sort_movie_asc);
        ImageView checkMovieAsc = view.findViewById(R.id.check_sort_movie_asc);

        View optSingerAsc = view.findViewById(R.id.opt_sort_singer_asc);
        ImageView checkSingerAsc = view.findViewById(R.id.check_sort_singer_asc);

        View optMusicianAsc = view.findViewById(R.id.opt_sort_musician_asc);
        ImageView checkMusicianAsc = view.findViewById(R.id.check_sort_musician_asc);

        View optLyricistAsc = view.findViewById(R.id.opt_sort_lyricist_asc);
        ImageView checkLyricistAsc = view.findViewById(R.id.check_sort_lyricist_asc);

        // Highlight current selection
        if (checkYearDesc != null) checkYearDesc.setVisibility(MalayalamSongManager.SORT_YEAR_DESC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkYearAsc != null) checkYearAsc.setVisibility(MalayalamSongManager.SORT_YEAR_ASC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkSongAsc != null) checkSongAsc.setVisibility(MalayalamSongManager.SORT_SONG_ASC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkSongDesc != null) checkSongDesc.setVisibility(MalayalamSongManager.SORT_SONG_DESC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkMovieAsc != null) checkMovieAsc.setVisibility(MalayalamSongManager.SORT_MOVIE_ASC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkSingerAsc != null) checkSingerAsc.setVisibility(MalayalamSongManager.SORT_SINGER_ASC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkMusicianAsc != null) checkMusicianAsc.setVisibility(MalayalamSongManager.SORT_MUSICIAN_ASC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);
        if (checkLyricistAsc != null) checkLyricistAsc.setVisibility(MalayalamSongManager.SORT_LYRICIST_ASC.equals(currentSortOrder) ? View.VISIBLE : View.GONE);

        // Click listeners
        optYearDesc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_YEAR_DESC, "📅 Year: Newest ▾", dialog));
        optYearAsc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_YEAR_ASC, "📅 Year: Oldest ▴", dialog));
        optSongAsc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_SONG_ASC, "🎵 Song: A → Z ▾", dialog));
        optSongDesc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_SONG_DESC, "🎵 Song: Z → A ▾", dialog));
        optMovieAsc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_MOVIE_ASC, "🎬 Movie: A → Z ▾", dialog));
        optSingerAsc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_SINGER_ASC, "🎙️ Singer: A → Z ▾", dialog));
        optMusicianAsc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_MUSICIAN_ASC, "🎼 Music: A → Z ▾", dialog));
        optLyricistAsc.setOnClickListener(v -> selectSort(MalayalamSongManager.SORT_LYRICIST_ASC, "✍️ Lyricist: A → Z ▾", dialog));

        dialog.show();
    }

    private void selectSort(String sortOrder, String chipLabel, BottomSheetDialog dialog) {
        currentSortOrder = sortOrder;
        if (chipSortPicker != null) {
            chipSortPicker.setText(chipLabel);
        }
        if (tvToolbarSortLabel != null) {
            tvToolbarSortLabel.setText(sortOrder.contains("year") ? "Year" : (sortOrder.contains("song") ? "Song" : "Sort"));
        }
        if (dialog != null) {
            dialog.dismiss();
        }
        applyFilter();
    }

    public void filterByMovie(String movieName) {
        if (movieName == null || movieName.trim().isEmpty()) return;
        selectedMovie = movieName.trim();
        if (layoutActiveFilterBar != null) {
            layoutActiveFilterBar.setVisibility(View.VISIBLE);
        }
        if (tvActiveFilterLabel != null) {
            tvActiveFilterLabel.setText("🎬 Movie: " + selectedMovie);
        }
        applyFilter();
    }

    private void setupFilterChips() {
        if (chipYearPicker != null) {
            chipYearPicker.setOnClickListener(v -> showYearPickerBottomSheet());
        }

        chipAll.setOnClickListener(v -> {
            selectedDecade = "all";
            selectedArtist = "all";
            selectedMovie = null;
            selectedYear = null;
            if (chipYearPicker != null) chipYearPicker.setText("📆 Select Year ▾");
            if (layoutActiveFilterBar != null) layoutActiveFilterBar.setVisibility(View.GONE);
            highlightChip(chipAll);
            applyFilter();
        });

        chip2020s.setOnClickListener(v -> selectDecade("2020s", chip2020s));
        chip2010s.setOnClickListener(v -> selectDecade("2010s", chip2010s));
        chip2000s.setOnClickListener(v -> selectDecade("2000s", chip2000s));
        chip90s.setOnClickListener(v -> selectDecade("90s", chip90s));
        chip80s.setOnClickListener(v -> selectDecade("80s", chip80s));
        chipOldies.setOnClickListener(v -> selectDecade("oldies", chipOldies));

        chipYesudas.setOnClickListener(v -> selectArtist("യേശുദാസ്", chipYesudas));
        chipChithra.setOnClickListener(v -> selectArtist("ചിത്ര", chipChithra));
        if (chipJanaki != null) chipJanaki.setOnClickListener(v -> selectArtist("ജാനകി", chipJanaki));
        chipSujatha.setOnClickListener(v -> selectArtist("സുജാത", chipSujatha));
        chipSreekumar.setOnClickListener(v -> selectArtist("ശ്രീകുമാർ", chipSreekumar));
        if (chipJayachandran != null) chipJayachandran.setOnClickListener(v -> selectArtist("ജയചന്ദ്രൻ", chipJayachandran));
    }

    private void showYearPickerBottomSheet() {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_year, null);
        dialog.setContentView(view);

        ImageView btnClose = view.findViewById(R.id.btn_close_year_picker);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        EditText etSearchYear = view.findViewById(R.id.et_search_year);
        ImageView btnClearYearSearch = view.findViewById(R.id.btn_clear_year_search);
        View cardSelectAllYears = view.findViewById(R.id.card_select_all_years);
        ImageView checkAllYears = view.findViewById(R.id.check_all_years);
        RecyclerView rvYearsGrid = view.findViewById(R.id.rv_years_grid);

        if (checkAllYears != null) {
            checkAllYears.setVisibility(selectedYear == null ? View.VISIBLE : View.GONE);
        }

        if (cardSelectAllYears != null) {
            cardSelectAllYears.setOnClickListener(v -> {
                selectedYear = null;
                if (chipYearPicker != null) chipYearPicker.setText("📆 Select Year ▾");
                if (layoutActiveFilterBar != null && selectedMovie == null) {
                    layoutActiveFilterBar.setVisibility(View.GONE);
                }
                highlightChip(chipAll);
                dialog.dismiss();
                applyFilter();
            });
        }

        com.app.webdroid.adapter.AdapterYearPicker yearAdapter =
                new com.app.webdroid.adapter.AdapterYearPicker(requireContext(), year -> {
                    selectedYear = year;
                    if (chipYearPicker != null) {
                        chipYearPicker.setText("📆 " + year + " ▾");
                        highlightChip(chipYearPicker);
                    }
                    if (layoutActiveFilterBar != null) {
                        layoutActiveFilterBar.setVisibility(View.VISIBLE);
                    }
                    if (tvActiveFilterLabel != null) {
                        int count = MalayalamSongManager.getInstance().getSongCountForYear(year);
                        tvActiveFilterLabel.setText("📆 Year: " + year + " (" + count + " songs)");
                    }
                    dialog.dismiss();
                    applyFilter();
                });

        if (rvYearsGrid != null) {
            rvYearsGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3));
            rvYearsGrid.setAdapter(yearAdapter);
        }
        yearAdapter.setYears(MalayalamSongManager.getInstance().getDistinctYears(), selectedYear);

        if (etSearchYear != null) {
            etSearchYear.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String q = s != null ? s.toString().trim() : "";
                    if (btnClearYearSearch != null) btnClearYearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                    yearAdapter.filter(q);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearYearSearch != null) {
            btnClearYearSearch.setOnClickListener(v -> {
                if (etSearchYear != null) etSearchYear.setText("");
            });
        }

        dialog.show();
    }

    private void selectDecade(String decade, TextView chip) {
        if (selectedDecade.equalsIgnoreCase(decade)) {
            selectedDecade = "all";
            highlightChip(chipAll);
        } else {
            selectedDecade = decade;
            selectedArtist = "all";
            highlightChip(chip);
        }
        applyFilter();
    }

    private void selectArtist(String artist, TextView chip) {
        if (selectedArtist.equalsIgnoreCase(artist)) {
            selectedArtist = "all";
            highlightChip(chipAll);
        } else {
            selectedArtist = artist;
            selectedDecade = "all";
            highlightChip(chip);
        }
        applyFilter();
    }

    private void highlightChip(TextView activeChip) {
        boolean isDark = sharedPref.getIsDarkTheme();
        int inactiveBg = isDark ? R.drawable.bg_search_chip_inactive_dark : R.drawable.bg_search_chip_inactive_light;
        int inactiveTextColor = isDark ? Color.parseColor("#9CA3AF") : Color.parseColor("#64748B");

        for (TextView c : allChips) {
            if (c == null) continue;
            if (c == activeChip) {
                c.setBackgroundResource(R.drawable.bg_search_chip_active);
                c.setTextColor(Color.WHITE);
            } else {
                c.setBackgroundResource(inactiveBg);
                c.setTextColor(inactiveTextColor);
            }
        }
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> {
            etSearchSongs.setText("");
            btnClearSearch.setVisibility(View.GONE);
            hideKeyboard();
        });

        etSearchSongs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    currentQuery = q;
                    applyFilter();
                };
                searchHandler.postDelayed(searchRunnable, 250);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadSongDatabase() {
        layoutLoading.setVisibility(View.VISIBLE);
        rvSongs.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        MalayalamSongManager.getInstance().loadSongsAsync(requireContext(), new MalayalamSongManager.OnLoadedCallback() {
            @Override
            public void onLoaded(List<MalayalamSong> songs) {
                if (getActivity() == null) return;
                layoutLoading.setVisibility(View.GONE);
                rvSongs.setVisibility(View.VISIBLE);

                tvTotalCountBadge.setText(String.format(java.util.Locale.US, "%,d", songs.size()));

                applyFilter();
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                layoutLoading.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText("Failed to load songs");
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilter() {
        if (!MalayalamSongManager.getInstance().isLoaded()) {
            return;
        }

        currentFilteredList = MalayalamSongManager.getInstance().filter(
                currentQuery, selectedDecade, selectedArtist, selectedMovie, selectedYear, currentSortOrder);
        currentLoadedCount = 0;

        if (currentFilteredList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSongs.setVisibility(View.GONE);
            tvEmptyTitle.setText(currentQuery.isEmpty() ? "No songs found for this filter" : "No results for \"" + currentQuery + "\"");
            tvTotalCountBadge.setText("0");
            adapter.setSongs(new ArrayList<>());
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvSongs.setVisibility(View.VISIBLE);
            tvTotalCountBadge.setText(String.format(java.util.Locale.US, "%,d", currentFilteredList.size()));

            int toLoad = Math.min(PAGE_SIZE, currentFilteredList.size());
            List<MalayalamSong> firstPage = new ArrayList<>(currentFilteredList.subList(0, toLoad));
            currentLoadedCount = toLoad;
            adapter.setSongs(firstPage);
            rvSongs.scrollToPosition(0);
        }
    }

    private void loadNextPage() {
        if (currentLoadedCount >= currentFilteredList.size()) {
            return;
        }

        isLoadingMore = true;
        int nextLimit = Math.min(currentLoadedCount + PAGE_SIZE, currentFilteredList.size());
        List<MalayalamSong> nextPage = new ArrayList<>(currentFilteredList.subList(currentLoadedCount, nextLimit));
        currentLoadedCount = nextLimit;
        adapter.addMoreSongs(nextPage);
        isLoadingMore = false;
    }

    private void hideKeyboard() {
        if (getContext() != null && etSearchSongs != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearchSongs.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
