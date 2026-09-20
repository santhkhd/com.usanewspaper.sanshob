package com.app.webdroid.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.shobmc.san.R;
import com.app.webdroid.activity.MainActivity;
import com.app.webdroid.adapter.AdapterCategory;
import com.app.webdroid.adapter.AdapterNews;
import com.app.webdroid.model.AppConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.database.AppDatabase;
import com.app.webdroid.model.FavoriteItem;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FragmentCategory extends Fragment {

    private RecyclerView recyclerView;
    private AdapterCategory adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private View layoutLoading;
    private TextView textLoadingTitle;
    private TextView textLoadingSub;
    private TextView textError;
    private String jsonUrl;
    private String categoryTitle;

    private MainActivity activity;
    private SharedPref sharedPref;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private List<AppConfig.OverviewItem> allItems; // Filter source

    private void showFilterDialog() {
        if (allItems == null || allItems.isEmpty() || getContext() == null)
            return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_menu, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnClose = dialogView.findViewById(R.id.btn_close_filter);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnYoutube = dialogView.findViewById(R.id.btn_filter_youtube);
        if (btnYoutube != null) {
            btnYoutube.setOnClickListener(v -> {
                dialog.dismiss();
                showYouTubeSearchDialog();
            });
        }

        View btnSearch = dialogView.findViewById(R.id.btn_filter_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                dialog.dismiss();
                showSearchByNameDialog();
            });
        }

        View btnCast = dialogView.findViewById(R.id.btn_filter_cast);
        if (btnCast != null) {
            btnCast.setOnClickListener(v -> {
                dialog.dismiss();
                showPeopleSelectionDialog("cast");
            });
        }

        View btnDirector = dialogView.findViewById(R.id.btn_filter_director);
        if (btnDirector != null) {
            btnDirector.setOnClickListener(v -> {
                dialog.dismiss();
                showPeopleSelectionDialog("director");
            });
        }

        View btnYear = dialogView.findViewById(R.id.btn_filter_year);
        if (btnYear != null) {
            btnYear.setOnClickListener(v -> {
                dialog.dismiss();
                showValueSelectionDialog("year");
            });
        }

        View btnGenre = dialogView.findViewById(R.id.btn_filter_genre);
        if (btnGenre != null) {
            btnGenre.setOnClickListener(v -> {
                dialog.dismiss();
                showValueSelectionDialog("genre");
            });
        }

        View btnReset = dialogView.findViewById(R.id.btn_filter_reset);
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                dialog.dismiss();
                adapter.setItems(injectNativeAds(allItems));
                android.widget.Toast.makeText(getContext(), "Filters reset • Showing all movies", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    public static class WikiArtist {
        public String name;
        public String clean;
        public String type;
        public String image;
        public String wiki_url;
    }

    private static Map<String, WikiArtist> sWikiArtistsMap = null;

    public static synchronized void ensureWikiArtistsLoaded(android.content.Context context) {
        if (sWikiArtistsMap != null || context == null) return;
        sWikiArtistsMap = new java.util.HashMap<>();
        try {
            java.io.InputStream is = context.getAssets().open("malayalam_artists_wiki.json");
            java.io.InputStreamReader reader = new java.io.InputStreamReader(is, "UTF-8");
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<WikiArtist>>() {}.getType();
            List<WikiArtist> list = new com.google.gson.Gson().fromJson(reader, listType);
            reader.close();
            if (list != null) {
                for (WikiArtist wa : list) {
                    if (wa.clean != null) sWikiArtistsMap.put(wa.clean.toLowerCase().trim(), wa);
                    if (wa.name != null) sWikiArtistsMap.put(wa.name.toLowerCase().trim(), wa);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static WikiArtist getWikiArtist(android.content.Context context, String name) {
        if (name == null || name.trim().isEmpty()) return null;
        ensureWikiArtistsLoaded(context);
        String clean = name.trim().toLowerCase();
        if (sWikiArtistsMap != null && sWikiArtistsMap.containsKey(clean)) {
            return sWikiArtistsMap.get(clean);
        }
        return null;
    }

    public static class PersonItem {
        public String name;
        public int count;
        public String imageUrl;
        public String wikiUrl;

        public PersonItem(String name, int count, String imageUrl, String wikiUrl) {
            this.name = name;
            this.count = count;
            this.imageUrl = imageUrl;
            this.wikiUrl = wikiUrl;
        }
    }

    private void showPeopleSelectionDialog(String type) {
        if (getContext() == null || allItems == null || allItems.isEmpty()) return;
        ensureWikiArtistsLoaded(getContext());

        Map<String, Integer> counts = new java.util.HashMap<>();
        for (AppConfig.OverviewItem item : allItems) {
            if ("cast".equals(type) && item.cast != null) {
                for (String c : item.cast) {
                    String clean = c.trim();
                    if (!clean.isEmpty()) {
                        counts.put(clean, counts.getOrDefault(clean, 0) + 1);
                    }
                }
            } else if ("director".equals(type)) {
                String dirs = item.getDirectorString();
                if (dirs != null && !dirs.isEmpty() && !"null".equalsIgnoreCase(dirs)) {
                    for (String d : dirs.split(",")) {
                        String clean = d.trim();
                        if (!clean.isEmpty() && !"null".equalsIgnoreCase(clean)) {
                            counts.put(clean, counts.getOrDefault(clean, 0) + 1);
                        }
                    }
                }
            }
        }

        if (counts.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "No " + type + " found", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        List<PersonItem> allPeople = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String personName = e.getKey();
            WikiArtist wa = getWikiArtist(getContext(), personName);
            String img = (wa != null && wa.image != null && !wa.image.isEmpty()) ? wa.image : null;
            String wikiUrl = (wa != null && wa.wiki_url != null && !wa.wiki_url.isEmpty())
                    ? wa.wiki_url
                    : ("https://en.wikipedia.org/wiki/Special:Search?search=" + Uri.encode(personName));
            allPeople.add(new PersonItem(personName, e.getValue(), img, wikiUrl));
        }

        java.util.Collections.sort(allPeople, (a, b) -> Integer.compare(b.count, a.count));

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_person, null);
        TextView txtTitle = dialogView.findViewById(R.id.dialog_title);
        android.widget.EditText editSearch = dialogView.findViewById(R.id.edit_search_people);
        TextView txtCount = dialogView.findViewById(R.id.text_people_count);
        RecyclerView recycler = dialogView.findViewById(R.id.recycler_people);

        boolean isCast = "cast".equals(type);
        txtTitle.setText(isCast ? "🎭 Select Actor or Actress" : "🎬 Select Director");
        editSearch.setHint(isCast ? "Search actor or actress..." : "Search director...");
        txtCount.setText("Showing " + allPeople.size() + (isCast ? " actors & actresses" : " directors"));

        recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));

        List<PersonItem> displayList = new ArrayList<>(allPeople);

        ImageView btnClosePerson = dialogView.findViewById(R.id.btn_close_person);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnClosePerson != null) {
            btnClosePerson.setOnClickListener(v -> dialog.dismiss());
        }

        class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.ViewHolder> {
            List<PersonItem> items;

            PersonAdapter(List<PersonItem> items) {
                this.items = items;
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_person, parent, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                PersonItem p = items.get(position);
                holder.txtName.setText(p.name);
                holder.txtCount.setText(p.count + (isCast ? (p.count == 1 ? " Movie in Vault" : " Movies in Vault") : (p.count == 1 ? " Film Directed" : " Films Directed")));

                if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(holder.itemView.getContext())
                            .load(p.imageUrl)
                            .placeholder(R.drawable.ic_placeholder_media)
                            .error(R.drawable.ic_placeholder_media)
                            .circleCrop()
                            .into(holder.imgAvatar);
                } else {
                    holder.imgAvatar.setImageResource(R.drawable.ic_placeholder_media);
                }

                // 🌐 Wikipedia Profile Button
                if (holder.btnWiki != null) {
                    holder.btnWiki.setOnClickListener(v -> {
                        Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityWebView.class);
                        intent.putExtra("title", p.name + " - Wikipedia");
                        intent.putExtra("link", p.wikiUrl);
                        startActivity(intent);
                    });
                }

                // Row Tap: Filters all movies in vault
                holder.itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    filterItems(type, p.name);
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Showing " + p.count + " movies for \"" + p.name + "\"", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                android.widget.ImageView imgAvatar;
                TextView txtName, txtCount;
                View btnWiki;

                ViewHolder(View itemView) {
                    super(itemView);
                    imgAvatar = itemView.findViewById(R.id.image_person_avatar);
                    txtName = itemView.findViewById(R.id.text_person_name);
                    txtCount = itemView.findViewById(R.id.text_person_count);
                    btnWiki = itemView.findViewById(R.id.btn_wiki_profile);
                }
            }
        }

        PersonAdapter pAdapter = new PersonAdapter(displayList);
        recycler.setAdapter(pAdapter);

        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase();
                displayList.clear();
                if (q.isEmpty()) {
                    displayList.addAll(allPeople);
                } else {
                    for (PersonItem p : allPeople) {
                        if (p.name.toLowerCase().contains(q)) {
                            displayList.add(p);
                        }
                    }
                }
                txtCount.setText("Showing " + displayList.size() + (isCast ? " actors & actresses" : " directors"));
                pAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        dialog.show();
    }

    private void showSearchByNameDialog() {
        if (getContext() == null) return;
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("e.g. Mohanlal, Mammootty, Manju Warrier, Priyadarshan");
        input.setSingleLine(true);

        android.widget.FrameLayout container = new android.widget.FrameLayout(getContext());
        int padding = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding / 2);
        container.addView(input);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🔍 Search Actors, Actresses & Directors")
                .setView(container)
                .setPositiveButton("Search", (dialog, which) -> {
                    String query = input.getText().toString().trim().toLowerCase();
                    if (!query.isEmpty()) {
                        List<AppConfig.OverviewItem> filtered = new ArrayList<>();
                        for (AppConfig.OverviewItem item : allItems) {
                            boolean match = false;
                            if (item.title != null && item.title.toLowerCase().contains(query)) match = true;
                            if (item.cast != null) {
                                for (String c : item.cast) {
                                    if (c.toLowerCase().contains(query)) { match = true; break; }
                                }
                            }
                            String dir = item.getDirectorString();
                            if (dir != null && dir.toLowerCase().contains(query)) match = true;
                            if (item.genre != null && item.genre.toLowerCase().contains(query)) match = true;
                            if (match) filtered.add(item);
                        }
                        adapter.setItems(injectNativeAds(filtered));
                        android.widget.Toast.makeText(getContext(), "Found " + filtered.size() + " movies for \"" + query + "\"", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showModernYouTubeSearchDialog(String defaultTitle, String defaultUrl) {
        if (getContext() == null || activity == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_youtube_search, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_search_dialog_title);
        android.widget.EditText etQuery = dialogView.findViewById(R.id.et_youtube_search_query);
        ImageView btnClear = dialogView.findViewById(R.id.btn_clear_search_query);
        ImageView btnClose = dialogView.findViewById(R.id.btn_dialog_search_close);

        View chipHits = dialogView.findViewById(R.id.chip_suggest_hits);
        View chipSongs = dialogView.findViewById(R.id.chip_suggest_songs);
        View chipComedy = dialogView.findViewById(R.id.chip_suggest_comedy);
        View chipTrailers = dialogView.findViewById(R.id.chip_suggest_trailers);

        View btnBrowseAll = dialogView.findViewById(R.id.btn_action_browse_all);
        View btnSearch = dialogView.findViewById(R.id.btn_action_search);

        if (defaultTitle != null && !defaultTitle.isEmpty() && tvTitle != null) {
            tvTitle.setText(defaultTitle);
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        if (etQuery != null) {
            etQuery.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnClear != null) {
                        btnClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }
                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (etQuery != null) {
                    etQuery.setText("");
                    etQuery.requestFocus();
                }
            });
        }

        // Suggestions
        View.OnClickListener chipClick = v -> {
            if (etQuery == null) return;
            TextView tv = (TextView) v;
            String text = tv.getText().toString();
            text = text.replaceAll("^[\\p{So}\\p{Sk}\\s]+", "").trim();
            etQuery.setText(text);
            etQuery.setSelection(text.length());
        };
        if (chipHits != null) chipHits.setOnClickListener(chipClick);
        if (chipSongs != null) chipSongs.setOnClickListener(chipClick);
        if (chipComedy != null) chipComedy.setOnClickListener(chipClick);
        if (chipTrailers != null) chipTrailers.setOnClickListener(chipClick);

        Runnable executeSearch = () -> {
            String q = (etQuery != null && etQuery.getText() != null) ? etQuery.getText().toString().trim() : "";
            dialog.dismiss();
            if (!q.isEmpty()) {
                String sUrl = "search:" + q + "|CAM%3D";
                activity.loadWebPage("Search: " + q, "VIDEOS", sUrl, sUrl);
            } else if (defaultUrl != null && !defaultUrl.isEmpty()) {
                activity.loadWebPage(defaultTitle != null ? defaultTitle : "US News Videos", "VIDEOS", defaultUrl, defaultUrl);
            } else {
                android.widget.Toast.makeText(getContext(), "Please enter a search term", android.widget.Toast.LENGTH_SHORT).show();
            }
        };

        if (etQuery != null) {
            etQuery.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    executeSearch.run();
                    return true;
                }
                return false;
            });
        }

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> executeSearch.run());
        }

        if (btnBrowseAll != null) {
            if (defaultUrl != null && !defaultUrl.isEmpty()) {
                btnBrowseAll.setVisibility(View.VISIBLE);
                btnBrowseAll.setOnClickListener(v -> {
                    dialog.dismiss();
                    activity.loadWebPage(defaultTitle != null ? defaultTitle : "All News", "VIDEOS", defaultUrl, defaultUrl);
                });
            } else {
                btnBrowseAll.setVisibility(View.GONE);
            }
        }

        dialog.show();
    }

    private void showYouTubeSearchDialog() {
        showModernYouTubeSearchDialog("Search News Videos", "search:US news live breaking|CAM%3D");
    }

    public void performSearch(String query) {
        if (allItems == null) return;
        if (query == null || query.trim().isEmpty()) {
            adapter.setItems(injectNativeAds(allItems));
            return;
        }
        String q = query.trim().toLowerCase();
        List<AppConfig.OverviewItem> filtered = new ArrayList<>();
        for (AppConfig.OverviewItem item : allItems) {
            boolean match = false;
            if (item.title != null && item.title.toLowerCase().contains(q)) match = true;
            if (!match && item.cast != null) {
                for (String c : item.cast) {
                    if (c.toLowerCase().contains(q)) { match = true; break; }
                }
            }
            if (!match) {
                String dir = item.getDirectorString();
                if (dir != null && dir.toLowerCase().contains(q)) match = true;
            }
            if (!match && item.genre != null && item.genre.toLowerCase().contains(q)) match = true;
            if (!match && item.year != null && item.year.contains(q)) match = true;
            if (match) filtered.add(item);
        }
        adapter.setItems(injectNativeAds(filtered));
    }

    public static class GridItem {
        public String title;
        public int count;

        public GridItem(String title, int count) {
            this.title = title;
            this.count = count;
        }
    }

    private void showValueSelectionDialog(String type) {
        if (getContext() == null || allItems == null || allItems.isEmpty()) return;

        Map<String, Integer> counts = new java.util.HashMap<>();
        for (AppConfig.OverviewItem item : allItems) {
            if ("year".equals(type) && item.year != null && !item.year.trim().isEmpty() && !item.year.equalsIgnoreCase("null")) {
                String y = item.year.trim();
                counts.put(y, counts.getOrDefault(y, 0) + 1);
            } else if ("genre".equals(type) && item.genre != null) {
                String[] split = item.genre.split(",");
                for (String s : split) {
                    String clean = s.replace("[", "").replace("]", "").replace("\"", "").trim();
                    if (!clean.isEmpty() && !clean.equalsIgnoreCase("null")) {
                        counts.put(clean, counts.getOrDefault(clean, 0) + 1);
                    }
                }
            } else if ("cast".equals(type) && item.cast != null) {
                for (String s : item.cast) {
                    String clean = s.trim();
                    if (!clean.isEmpty()) {
                        counts.put(clean, counts.getOrDefault(clean, 0) + 1);
                    }
                }
            } else if ("director".equals(type) && item.getDirectorString() != null) {
                String dirs = item.getDirectorString();
                if (!dirs.isEmpty() && !"null".equalsIgnoreCase(dirs)) {
                    for (String d : dirs.split(",")) {
                        String clean = d.trim();
                        if (!clean.isEmpty() && !"null".equalsIgnoreCase(clean)) {
                            counts.put(clean, counts.getOrDefault(clean, 0) + 1);
                        }
                    }
                }
            }
        }

        if (counts.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "No " + type + " found", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        List<GridItem> allGridItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            allGridItems.add(new GridItem(entry.getKey(), entry.getValue()));
        }

        boolean isYear = "year".equals(type);
        if (isYear) {
            java.util.Collections.sort(allGridItems, (a, b) -> {
                try {
                    int ya = Integer.parseInt(a.title);
                    int yb = Integer.parseInt(b.title);
                    return Integer.compare(yb, ya);
                } catch (Exception e) {
                    return b.title.compareToIgnoreCase(a.title);
                }
            });
        } else {
            java.util.Collections.sort(allGridItems, (a, b) -> {
                int c = Integer.compare(b.count, a.count);
                if (c != 0) return c;
                return a.title.compareToIgnoreCase(b.title);
            });
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_grid_selection, null);
        TextView txtTitle = dialogView.findViewById(R.id.dialog_grid_title);
        TextView txtSubtitle = dialogView.findViewById(R.id.dialog_grid_subtitle);
        android.widget.EditText editSearch = dialogView.findViewById(R.id.edit_search_grid);
        ImageView btnClose = dialogView.findViewById(R.id.btn_close_grid);
        RecyclerView recycler = dialogView.findViewById(R.id.recycler_grid);

        txtTitle.setText(isYear ? "📅 Select Release Year" : "🏷️ Select Movie Genre");
        txtSubtitle.setText("Showing " + allGridItems.size() + (isYear ? " years" : " genres") + " • Tap to filter");
        editSearch.setHint(isYear ? "Search year (e.g. 2024, 1995)..." : "Search genre (e.g. Action, Comedy)...");

        // 3-Column Grid!
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 3));

        List<GridItem> displayList = new ArrayList<>(allGridItems);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        class GridChipAdapter extends RecyclerView.Adapter<GridChipAdapter.ViewHolder> {
            List<GridItem> items;

            GridChipAdapter(List<GridItem> items) {
                this.items = items;
            }

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_chip, parent, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                GridItem item = items.get(position);
                holder.txtTitle.setText(item.title);
                holder.txtCount.setText(item.count + (item.count == 1 ? " movie" : " movies"));

                holder.itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    filterItems(type, item.title);
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Showing " + item.count + " movies for " + item.title, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return items.size();
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                TextView txtTitle, txtCount;

                ViewHolder(View itemView) {
                    super(itemView);
                    txtTitle = itemView.findViewById(R.id.text_chip_title);
                    txtCount = itemView.findViewById(R.id.text_chip_count);
                }
            }
        }

        GridChipAdapter chipAdapter = new GridChipAdapter(displayList);
        recycler.setAdapter(chipAdapter);

        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().trim().toLowerCase();
                displayList.clear();
                if (q.isEmpty()) {
                    displayList.addAll(allGridItems);
                } else {
                    for (GridItem item : allGridItems) {
                        if (item.title.toLowerCase().contains(q)) {
                            displayList.add(item);
                        }
                    }
                }
                txtSubtitle.setText("Showing " + displayList.size() + (isYear ? " years" : " genres"));
                chipAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        dialog.show();
    }

    private void filterItems(String type, String value) {
        List<AppConfig.OverviewItem> filtered = new ArrayList<>();
        for (AppConfig.OverviewItem item : allItems) {
            boolean match = false;
            if ("year".equals(type)) {
                if (item.year != null && item.year.equals(value))
                    match = true;
            } else if ("genre".equals(type)) {
                if (item.genre != null && item.genre.contains(value))
                    match = true;
            } else if ("cast".equals(type)) {
                if (item.cast != null && item.cast.contains(value))
                    match = true;
            } else if ("director".equals(type)) {
                String d = item.getDirectorString();
                if (d != null && d.toLowerCase().contains(value.toLowerCase()))
                    match = true;
            }
            if (match)
                filtered.add(item);
        }
        adapter.setItems(injectNativeAds(filtered));
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        if (getArguments() != null) {
            jsonUrl = getArguments().getString("url");
            categoryTitle = getArguments().getString("name");
        }

        sharedPref = new SharedPref(requireContext());

        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutLoading = view.findViewById(R.id.layout_loading);
        textLoadingTitle = view.findViewById(R.id.text_loading_title);
        textLoadingSub = view.findViewById(R.id.text_loading_sub);
        textError = view.findViewById(R.id.text_error);

        // Grid Layout with minimum 3 columns
        GridLayoutManager glmInitial = new GridLayoutManager(getContext(), 3);
        glmInitial.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter != null && adapter.getItemViewType(position) == AdapterCategory.VIEW_TYPE_AD) {
                    return 3;
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(glmInitial);
        recyclerView.setNestedScrollingEnabled(true);
        adapter = new AdapterCategory(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        AppBarLayout appBarLayout = view.findViewById(R.id.appbar_category);
        if (appBarLayout != null) {
            try {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
                AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
                if (behavior == null) {
                    behavior = new AppBarLayout.Behavior();
                    params.setBehavior(behavior);
                }
                behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                        return true;
                    }
                });
            } catch (Exception ignored) {}

            appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setEnabled(verticalOffset == 0);
                }
            });
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setNestedScrollingEnabled(true);
        }

        setupStateWeatherAndHub(view);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (activity != null) {
                    activity.onScroll(dy);
                }
            }
        });

        // Recursive click handling: if clicked, delegate to MainActivity to switch
        // fragment or open link
        // Recursive click handling: if clicked, delegate to MainActivity to switch
        // fragment or open link
        adapter.setOnItemClickListener((v, obj, position) -> {
            // Save Last Seen for Badge Logic
            if (getContext() != null) {
                getContext().getSharedPreferences("badges", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putLong("last_seen_" + obj.title, System.currentTimeMillis())
                        .apply();
            }

            // High-priority: Handle Category Page / Section Items -> Open ActivityCategoryPage directly
            if ("category_page".equalsIgnoreCase(obj.provider) || "section".equalsIgnoreCase(obj.provider)) {
                String catKey = (obj.arguments != null && !obj.arguments.isEmpty()) ? obj.arguments.get(0) : obj.title;
                com.app.webdroid.activity.ActivityCategoryPage.start(getContext(), catKey, obj.title);
                return;
            }

            // High-priority: Handle RSS / News Items -> Open ActivityNewsDetail directly
            if ("rss_item".equalsIgnoreCase(obj.provider) || "news".equalsIgnoreCase(obj.provider)) {
                ArrayList<String> targetUrls = new ArrayList<>();
                if (obj.arguments != null && !obj.arguments.isEmpty()) {
                    targetUrls.addAll(obj.arguments);
                } else if (obj.link != null) {
                    targetUrls.add(obj.link);
                }
                String targetUrl = targetUrls.isEmpty() ? "" : targetUrls.get(0);

                Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityNewsDetail.class);
                intent.putExtra("title", obj.title != null ? obj.title : "");
                intent.putExtra("description", obj.plot != null && !obj.plot.isEmpty() ? obj.plot : obj.title);
                intent.putExtra("link", targetUrl);
                intent.putExtra("imageUrl", obj.image);
                intent.putExtra("pubDate", obj.year != null ? obj.year : "Live");
                intent.putExtra("sourceName", obj.genre != null ? obj.genre : "Local Press");
                startActivity(intent);
                return;
            }

            // Check if it's a Movie Item and launch ActivityMovieDetail
            boolean isMovieItem = ("movies".equalsIgnoreCase(obj.provider)
                    || ((obj.year != null && !obj.year.isEmpty()) && (obj.plot != null && !obj.plot.isEmpty())))
                    && !"iptv".equalsIgnoreCase(obj.provider)
                    && !"live".equalsIgnoreCase(obj.provider)
                    && !"rss_item".equalsIgnoreCase(obj.provider)
                    && !"news".equalsIgnoreCase(obj.provider);

            if (isMovieItem) {
                com.solodroidx.ads.listener.OnShowAdCompleteListener openMovieAction = () -> {
                    Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityMovieDetail.class);
                    intent.putExtra("title", obj.title != null ? obj.title : "");
                    intent.putExtra("image", obj.image != null ? obj.image : "");
                    intent.putExtra("year", obj.year != null ? obj.year : "");
                    intent.putExtra("runtime", obj.runtime != null ? obj.runtime : "");
                    intent.putExtra("rating", obj.rating != null ? obj.rating : "");
                    intent.putExtra("director", obj.getDirectorString());
                    intent.putExtra("genre", obj.genre != null ? obj.genre : "");
                    intent.putExtra("plot", obj.plot != null ? obj.plot : "");
                    if (obj.cast != null) {
                        intent.putExtra("cast", android.text.TextUtils.join(", ", obj.cast));
                    }
                    String targetOtt = (obj.ottUrl != null && !obj.ottUrl.isEmpty()) ? obj.ottUrl : obj.link;
                    if (targetOtt != null && !targetOtt.isEmpty()) {
                        intent.putExtra("ott_url", targetOtt);
                    }
                    startActivity(intent);
                };

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showInterstitialAdForListItem(openMovieAction);
                } else {
                    openMovieAction.onShowAdComplete();
                }
                return;
            }

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showInterstitialAdForListItem(() -> {

                    // Check provider types and delegate

                    ArrayList<String> targetUrls = new ArrayList<>();
                    if (obj.arguments != null && !obj.arguments.isEmpty()) {
                        targetUrls.addAll(obj.arguments);
                    } else {
                        targetUrls.add("");
                    }

                    String targetUrl = targetUrls.isEmpty() ? "" : targetUrls.get(0);
                    String overrideUrl = com.app.webdroid.util.CustomChannelManager.getChannelOverride(getContext(), obj.title, null);
                    if (overrideUrl != null && !overrideUrl.isEmpty()) {
                        targetUrl = overrideUrl;
                    }
                    final String initialTargetUrl = targetUrl;

                    // Check if it is an RSS News Item -> Open ActivityNewsDetail
                    if ("rss_item".equalsIgnoreCase(obj.provider)) {
                        Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityNewsDetail.class);
                        intent.putExtra("title", obj.title);
                        intent.putExtra("description", obj.plot != null && !obj.plot.isEmpty() ? obj.plot : obj.title);
                        intent.putExtra("link", targetUrl);
                        intent.putExtra("imageUrl", obj.image);
                        intent.putExtra("pubDate", obj.year != null ? obj.year : "Live");
                        intent.putExtra("sourceName", obj.genre != null ? obj.genre : "Local Press");
                        startActivity(intent);
                        return;
                    }

                    // Check if it's an interactive Search action (prompt user) vs preset search query
                    boolean isInteractiveSearch = "search:".equals(targetUrl) || "search:?".equals(targetUrl)
                            || "search".equalsIgnoreCase(obj.provider)
                            || ("Search YouTube Videos".equalsIgnoreCase(obj.title) && (targetUrl.isEmpty() || "search:".equals(targetUrl)));
                    if (isInteractiveSearch) {
                        showModernYouTubeSearchDialog(obj.title, initialTargetUrl);
                        return;
                    }

                    // Check if it's an IPTV HLS stream (.m3u8 or provider "iptv")
                    if (targetUrl.contains(".m3u8") || "iptv".equalsIgnoreCase(obj.provider)) {
                        Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityVideoDetail.class);
                        intent.putExtra("videoId", targetUrl);
                        intent.putExtra("title", obj.title);
                        intent.putExtra("thumbUrl", obj.image);
                        intent.putExtra("date", "Live News Stream • 24/7");
                        startActivity(intent);
                        return;
                    }

                    // Check if it's a direct YouTube video (watch?v= or youtu.be) -> Open ActivityVideoDetail inline
                    String extractedVideoId = null;
                    if (targetUrl.contains("watch?v=")) {
                        extractedVideoId = targetUrl.substring(targetUrl.indexOf("watch?v=") + 8);
                        if (extractedVideoId.contains("&")) {
                            extractedVideoId = extractedVideoId.substring(0, extractedVideoId.indexOf("&"));
                        }
                    } else if (targetUrl.contains("youtu.be/")) {
                        extractedVideoId = targetUrl.substring(targetUrl.indexOf("youtu.be/") + 9);
                        if (extractedVideoId.contains("?")) {
                            extractedVideoId = extractedVideoId.substring(0, extractedVideoId.indexOf("?"));
                        }
                    }

                    if (extractedVideoId != null && !extractedVideoId.isEmpty() && !"live".equalsIgnoreCase(obj.provider)) {
                        Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityVideoDetail.class);
                        intent.putExtra("videoId", extractedVideoId);
                        intent.putExtra("title", obj.title);
                        intent.putExtra("thumbUrl", obj.image);
                        startActivity(intent);
                        return;
                    }

                    // Check if it's a live stream link (e.g. /live or provider "live")
                    if (targetUrl.contains("/live") || "live".equalsIgnoreCase(obj.provider)) {
                        final String liveTarget = targetUrl;
                        final String title = obj.title;
                        final String thumb = obj.image;
                        android.app.ProgressDialog pd = new android.app.ProgressDialog(getContext());
                        pd.setMessage("Connecting to " + title + "...");
                        pd.setCancelable(true);
                        try { pd.show(); } catch (Exception ignored) {}

                        new Thread(() -> {
                            String liveVid = com.app.webdroid.util.YouTubeInnertubeFetcher.resolveLiveVideoId(liveTarget, title);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try { pd.dismiss(); } catch (Exception ignored) {}
                                    Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityVideoDetail.class);
                                    intent.putExtra("videoId", (liveVid != null && !liveVid.isEmpty()) ? liveVid : liveTarget);
                                    intent.putExtra("title", title);
                                    intent.putExtra("thumbUrl", thumb);
                                    intent.putExtra("date", "Live Stream • HD 24/7");
                                    startActivity(intent);
                                });
                            }
                        }).start();
                        return;
                    }

                    String type = "WEB";
                    if ("videos".equalsIgnoreCase(obj.provider) || "video".equalsIgnoreCase(obj.provider)) {
                        type = "VIDEOS";
                    } else if ("songs".equalsIgnoreCase(obj.provider)) {
                        type = "SONGS";
                    } else if ("rss".equalsIgnoreCase(obj.provider)) {
                        if (targetUrl.contains("youtube.com/feeds")) {
                            type = "VIDEOS";
                        } else {
                            type = "RSS";
                        }
                    } else if ("movies".equalsIgnoreCase(obj.provider)) {
                        type = "MOVIES";
                    } else if ("overview".equalsIgnoreCase(obj.provider) || "category".equalsIgnoreCase(obj.provider)) {
                        type = "CATEGORY";
                    } else if ("youtube_channel".equalsIgnoreCase(obj.provider)) {
                        if (targetUrl.startsWith("UC")) {
                            targetUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=" + targetUrl;
                            type = "VIDEOS";
                        } else if (targetUrl.contains("channel_id=")) {
                            type = "VIDEOS";
                        } else {
                            type = "YOUTUBE";
                        }
                    } else if ("youtube_playlist".equalsIgnoreCase(obj.provider)) {
                        if (!targetUrl.startsWith("http")) {
                            targetUrl = "https://www.youtube.com/feeds/videos.xml?playlist_id=" + targetUrl;
                        }
                        type = "VIDEOS";
                    } else if ("latest_videos".equalsIgnoreCase(obj.provider)) {
                        type = "YOUTUBE";
                    } else if ("favorites".equalsIgnoreCase(obj.provider)) {
                        type = "FAVORITES";
                    } else if ("assets".equalsIgnoreCase(obj.provider) || targetUrl.endsWith(".html") || targetUrl.startsWith("file:///")) {
                        type = "assets";
                    }

                    if (!"SONGS".equalsIgnoreCase(type) && (targetUrl.contains("youtube.com/feeds") || targetUrl.contains("channel_id=") || targetUrl.contains("playlist_id=") || targetUrl.startsWith("playlists/") || targetUrl.contains("playlists/") || targetUrl.startsWith("search:"))) {
                        type = "VIDEOS";
                    }

                    if ("radio".equalsIgnoreCase(obj.provider)) {
                        Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityRadioPlayer.class);
                        startActivity(intent);
                        return;
                    }

                    final String finalType = type;
                    final String finalTargetUrl = targetUrl;

                    if ("assets".equalsIgnoreCase(finalType) || (finalTargetUrl != null && finalTargetUrl.endsWith(".html"))) {
                        Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityWebView.class);
                        intent.putExtra("title", obj.title);
                        String assetUrl = finalTargetUrl.startsWith("file:///") ? finalTargetUrl : "file:///android_asset/" + finalTargetUrl;
                        intent.putExtra("link", assetUrl);
                        startActivity(intent);
                        return;
                    }

                    if ("WEB".equalsIgnoreCase(finalType) && finalTargetUrl != null && (finalTargetUrl.startsWith("http://") || finalTargetUrl.startsWith("https://"))) {
                        if (getContext() != null) {
                            com.app.webdroid.util.Tools.showOpenLinkDialog(requireContext(), obj.title, finalTargetUrl,
                                    new com.app.webdroid.util.Tools.OnOpenLinkChoiceListener() {
                                        @Override
                                        public void onOpenInApp() {
                                            Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityWebView.class);
                                            intent.putExtra("title", obj.title);
                                            intent.putExtra("link", finalTargetUrl);
                                            startActivity(intent);
                                        }

                                        @Override
                                        public void onOpenOutside() {
                                            try {
                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(finalTargetUrl));
                                                startActivity(browserIntent);
                                            } catch (Exception e) {
                                                android.widget.Toast.makeText(getContext(), "Could not open browser", android.widget.Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                        return;
                    } else if ("RSS".equals(type)) {
                        activity.loadWebPage(obj.title, type, targetUrls);
                    } else {
                        activity.loadWebPage(obj.title, type, targetUrl, targetUrl);
                    }
                });

            }
        });

        // Favorites Logic
        AppDatabase db = AppDatabase.getDatabase(getContext());
        db.favoriteDao().getAllFavorites().observe(getViewLifecycleOwner(), favorites -> {
            List<String> ids = new ArrayList<>();
            for (FavoriteItem item : favorites) {
                ids.add(item.itemId);
            }
            adapter.setFavoriteIds(ids);
        });

        adapter.setOnFavoriteClickListener((v, obj, position) -> {
            String targetUrl = (obj.arguments != null && !obj.arguments.isEmpty()) ? obj.arguments.get(0) : obj.title;

            String type = "WEB";
            if ("rss".equalsIgnoreCase(obj.provider)) {
                if (targetUrl.contains("youtube.com/feeds"))
                    type = "VIDEOS";
                else
                    type = "RSS";
            } else if ("movies".equalsIgnoreCase(obj.provider))
                type = "MOVIES";
            else if ("youtube_channel".equalsIgnoreCase(obj.provider))
                type = "YOUTUBE";
            else if ("latest_videos".equalsIgnoreCase(obj.provider))
                type = "YOUTUBE";
            else if ("videos".equalsIgnoreCase(obj.provider))
                type = "VIDEOS";

            final String fType = type;

            AppDatabase.databaseWriteExecutor.execute(() -> {
                if (db.favoriteDao().isFavorite(targetUrl, fType) > 0 || db.favoriteDao().isFavoriteByTargetOrTitle(targetUrl, obj.title) > 0) {
                    db.favoriteDao().removeFavoriteComprehensive(targetUrl, targetUrl, obj.title);
                } else {
                    FavoriteItem fav = new FavoriteItem();
                    fav.itemId = targetUrl;
                    fav.type = fType;
                    fav.title = obj.title;
                    if (obj.year != null && !obj.year.isEmpty()) {
                        fav.subtitle = obj.year;
                    } else {
                        fav.subtitle = obj.provider;
                    }
                    fav.imageUrl = obj.image;
                    fav.targetUrl = targetUrl;
                    db.favoriteDao().addFavorite(fav);
                }
            });
        });

        adapter.setOnMoreClickListener((v, obj, position) -> {
            showCardOptionsMenu(v, obj, position);
        });

        adapter.setOnItemLongClickListener((v, obj, position) -> {
            showCardOptionsMenu(v, obj, position);
            return true;
        });

        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddChannel = view.findViewById(R.id.fab_add_channel);
        if (fabAddChannel != null) {
            fabAddChannel.setOnClickListener(v -> {
                if (jsonUrl != null && (jsonUrl.equalsIgnoreCase("youtube_categories.json") || jsonUrl.contains("categories"))) {
                    showCategoryOrChannelChooser();
                } else {
                    showAddChannelDialog();
                }
            });
        }

        swipeRefreshLayout.setOnRefreshListener(this::loadData);

        setupToolbar(view);

        // Setup Dedicated YouTube Inner Search Bar
        android.widget.EditText etYoutubeInnerSearch = view.findViewById(R.id.et_youtube_inner_search);
        ImageView btnYoutubeSearchClear = view.findViewById(R.id.btn_youtube_search_clear);
        ImageView btnYoutubeSearchGo = view.findViewById(R.id.btn_youtube_search_go);

        if (etYoutubeInnerSearch != null) {
            etYoutubeInnerSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnYoutubeSearchClear != null) {
                        btnYoutubeSearchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            Runnable doSearch = () -> {
                String q = etYoutubeInnerSearch.getText() != null ? etYoutubeInnerSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    etYoutubeInnerSearch.clearFocus();
                    if (getContext() != null) {
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(etYoutubeInnerSearch.getWindowToken(), 0);
                    }
                    String searchUrl = "search:" + q + "|CAI%3D";
                    if (activity != null) {
                        activity.loadWebPage("Search: " + q, "VIDEOS", searchUrl, searchUrl);
                    }
                } else {
                    android.widget.Toast.makeText(getContext(), "Type a topic or channel to search YouTube news", android.widget.Toast.LENGTH_SHORT).show();
                }
            };

            etYoutubeInnerSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    doSearch.run();
                    return true;
                }
                return false;
            });

            if (btnYoutubeSearchGo != null) {
                btnYoutubeSearchGo.setOnClickListener(v -> doSearch.run());
            }

            if (btnYoutubeSearchClear != null) {
                btnYoutubeSearchClear.setOnClickListener(v -> {
                    etYoutubeInnerSearch.setText("");
                    etYoutubeInnerSearch.requestFocus();
                });
            }
        }

        if (jsonUrl != null && (jsonUrl.contains("home.json") || "home.json".equalsIgnoreCase(jsonUrl)
                || jsonUrl.contains("home"))) {
            adapter.setFavoritesEnabled(false);
        }

        loadData();

        return view;
    }

    private void loadData() {
        if (jsonUrl == null || jsonUrl.isEmpty()) {
            if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            textError.setVisibility(View.VISIBLE);
            textError.setText("No URL provided");
            return;
        }

        if (layoutLoading != null) {
            layoutLoading.setVisibility(View.VISIBLE);
            if (textLoadingTitle != null) {
                if (categoryTitle != null && !categoryTitle.isEmpty()) {
                    String clean = categoryTitle.replaceAll("^[\\s\\-•★⭐🔥🎬📺🔴🎵📻🍿📰📡😂👶💻✈️🎭🙏📱🧠⚔️🤣]+", "").trim();
                    textLoadingTitle.setText("Loading " + (clean.isEmpty() ? categoryTitle : clean) + "...");
                } else {
                    textLoadingTitle.setText("Loading Content...");
                }
            }
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        textError.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);

        if (com.app.webdroid.util.CustomChannelManager.isCustomCategorySlug(jsonUrl) ||
                (jsonUrl != null && jsonUrl.startsWith("custom_cat_"))) {
            parseAndDisplayJson("[]");
            return;
        }

        Context ctx = getContext();
        if (ctx == null) return;

        com.app.webdroid.util.AppJsonManager.loadJson(ctx, jsonUrl, new com.app.webdroid.util.AppJsonManager.JsonCallback() {
            @Override
            public void onSuccess(String json, boolean fromOnline) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> parseAndDisplayJson(json));
                }
            }

            @Override
            public void onFallback(String json) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> parseAndDisplayJson(json));
                }
            }

            @Override
            public void onError(String error) {
                String assetName = com.app.webdroid.util.AppJsonManager.getAssetName(jsonUrl);
                if (assetName != null) {
                    loadAssetJson(assetName, error);
                } else if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        textError.setVisibility(View.VISIBLE);
                        textError.setText(error);
                    });
                }
            }
        });
    }

    private void loadAssetJson(String assetName, String fallbackError) {
        try {
            if (getContext() != null) {
                if (com.app.webdroid.util.CustomChannelManager.isCustomCategorySlug(assetName) ||
                        (assetName != null && assetName.startsWith("custom_cat_"))) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> parseAndDisplayJson("[]"));
                    }
                    return;
                }
                java.io.InputStream is = getContext().getAssets().open(assetName);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int len;
                while ((len = is.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                is.close();
                String json = baos.toString("UTF-8");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> parseAndDisplayJson(json));
                }
            }
        } catch (Exception e) {
            if (getContext() != null && jsonUrl != null) {
                List<AppConfig.OverviewItem> customChannels = com.app.webdroid.util.CustomChannelManager.getCustomChannels(getContext(), jsonUrl);
                if (customChannels != null && !customChannels.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> parseAndDisplayJson("[]"));
                    }
                    return;
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    textError.setVisibility(View.VISIBLE);
                    textError.setText(fallbackError != null ? fallbackError : ("Asset not found: " + e.getMessage()));
                });
            }
        }
    }

    private void parseAndDisplayJson(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        textError.setVisibility(View.VISIBLE);
                        textError.setText("Empty response");
                    });
                }
                return;
            }

            List<AppConfig.OverviewItem> items = null;
            JsonElement rootElement = JsonParser.parseString(json);

            if (rootElement.isJsonArray()) {
                // Direct List of OverviewItem
                items = new Gson().fromJson(rootElement, new TypeToken<List<AppConfig.OverviewItem>>() {}.getType());
            } else if (rootElement.isJsonObject()) {
                JsonObject rootObj = rootElement.getAsJsonObject();
                if (rootObj.has("overview") && rootObj.get("overview").isJsonArray()) {
                    items = new Gson().fromJson(rootObj.get("overview"), new TypeToken<List<AppConfig.OverviewItem>>() {}.getType());
                } else if (rootObj.has("categories") && rootObj.get("categories").isJsonArray()) {
                    // categories.json object format: { "categories": [ { "id": "...", "name": "...", "icon": "..." } ] }
                    JsonArray catArray = rootObj.getAsJsonArray("categories");
                    items = new ArrayList<>();
                    for (JsonElement catElem : catArray) {
                        if (catElem.isJsonObject()) {
                            JsonObject cObj = catElem.getAsJsonObject();
                            String id = cObj.has("id") ? cObj.get("id").getAsString() : "";
                            String name = cObj.has("name") ? cObj.get("name").getAsString() : (cObj.has("title") ? cObj.get("title").getAsString() : id);
                            String icon = cObj.has("icon") ? cObj.get("icon").getAsString() : "";

                            AppConfig.OverviewItem it = new AppConfig.OverviewItem();
                            it.title = name;
                            it.provider = "category_page";
                            it.arguments = new ArrayList<>();
                            it.arguments.add(id);
                            it.image = mapCategoryIcon(id, icon);
                            items.add(it);
                        }
                    }
                } else {
                    // Try to find any property containing a JSON array of OverviewItem
                    for (Map.Entry<String, JsonElement> entry : rootObj.entrySet()) {
                        if (entry.getValue().isJsonArray()) {
                            try {
                                List<AppConfig.OverviewItem> candidate = new Gson().fromJson(
                                        entry.getValue(), new TypeToken<List<AppConfig.OverviewItem>>() {}.getType());
                                if (candidate != null && !candidate.isEmpty() && candidate.get(0).title != null) {
                                    items = candidate;
                                    break;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            List<AppConfig.OverviewItem> finalItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
            if (getContext() != null && jsonUrl != null) {
                if ("categories.json".equals(jsonUrl)) {
                    List<com.app.webdroid.util.CustomChannelManager.CategoryOption> userCats =
                            com.app.webdroid.util.CustomChannelManager.getUserCategories(getContext());
                    if (userCats != null && !userCats.isEmpty()) {
                        for (int i = userCats.size() - 1; i >= 0; i--) {
                            com.app.webdroid.util.CustomChannelManager.CategoryOption uc = userCats.get(i);
                            AppConfig.OverviewItem catItem = new AppConfig.OverviewItem();
                            catItem.title = "⭐ " + uc.title;
                            catItem.image = "https://img.icons8.com/color/96/folder-invoices.png";
                            catItem.provider = "overview";
                            catItem.arguments = new ArrayList<>();
                            catItem.arguments.add(uc.jsonUrl);
                            finalItems.add(0, catItem);
                        }
                    }
                }
                List<AppConfig.OverviewItem> customChannels = com.app.webdroid.util.CustomChannelManager.getCustomChannels(getContext(), jsonUrl);
                if (customChannels != null && !customChannels.isEmpty()) {
                    finalItems.addAll(0, customChannels);
                }
                // Filter out any hidden channels (built-in or custom)
                finalItems.removeIf(item -> com.app.webdroid.util.CustomChannelManager.isChannelHidden(getContext(), jsonUrl, item));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    if (finalItems != null && !finalItems.isEmpty()) {

                        // Detect correct View Mode
                        boolean isMovies = false;
                        if (!"channels.json".equals(jsonUrl) && !"news_channels.json".equals(jsonUrl)) {
                            for (AppConfig.OverviewItem it : finalItems) {
                                if ("iptv".equalsIgnoreCase(it.provider) || "live".equalsIgnoreCase(it.provider)) {
                                    continue;
                                }
                                if (it.plot != null && !it.plot.isEmpty()) {
                                    isMovies = true;
                                    break;
                                } else if ("movies".equalsIgnoreCase(it.provider)
                                        && (it.arguments == null || it.arguments.isEmpty())) {
                                    isMovies = true;
                                    break;
                                }
                            }
                        }

                        if (isMovies) {
                            adapter.setViewMode(AdapterCategory.VIEW_TYPE_MOVIE);
                            GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
                            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                                @Override
                                public int getSpanSize(int position) {
                                    if (adapter.getItemViewType(position) == AdapterCategory.VIEW_TYPE_AD) {
                                        return 3;
                                    }
                                    return 1;
                                }
                            });
                            recyclerView.setLayoutManager(glm);

                            // Tag movie items so adapter binds correctly
                            for (AppConfig.OverviewItem it : finalItems) {
                                if (it.plot != null || "movies".equalsIgnoreCase(it.provider)) {
                                    it.provider = "movies";
                                }
                            }

                            // Setup Sorting & Filtering Menu
                            requireActivity().addMenuProvider(new androidx.core.view.MenuProvider() {
                                @Override
                                public void onCreateMenu(@NonNull android.view.Menu menu,
                                        @NonNull android.view.MenuInflater menuInflater) {
                                    menuInflater.inflate(R.menu.menu_category_sort, menu);
                                }

                                @Override
                                public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
                                    int id = menuItem.getItemId();
                                    if (id == R.id.action_filter) {
                                        showFilterDialog();
                                        return true;
                                    }
                                    if (id == R.id.action_youtube_search) {
                                        showYouTubeSearchDialog();
                                        return true;
                                    }

                                    // Sorting
                                    if (allItems == null)
                                        return false;

                                    List<AppConfig.OverviewItem> targetList = new ArrayList<>(allItems);

                                    if (id == R.id.sort_az) {
                                        java.util.Collections.sort(targetList,
                                                (o1, o2) -> o1.title.compareToIgnoreCase(o2.title));
                                    } else if (id == R.id.sort_za) {
                                        java.util.Collections.sort(targetList,
                                                (o1, o2) -> o2.title.compareToIgnoreCase(o1.title));
                                    } else if (id == R.id.sort_year_new) {
                                        java.util.Collections.sort(targetList, (o1, o2) -> {
                                            String y1 = o1.year != null ? o1.year : "0";
                                            String y2 = o2.year != null ? o2.year : "0";
                                            return y2.compareTo(y1);
                                        });
                                    } else if (id == R.id.sort_year_old) {
                                        java.util.Collections.sort(targetList, (o1, o2) -> {
                                            String y1 = o1.year != null ? o1.year : "0";
                                            String y2 = o2.year != null ? o2.year : "0";
                                            return y1.compareTo(y2);
                                        });
                                    }

                                    adapter.setItems(injectNativeAds(targetList));
                                    return true;
                                }
                            }, getViewLifecycleOwner(), androidx.lifecycle.Lifecycle.State.RESUMED);

                        } else {
                            adapter.setViewMode(AdapterCategory.VIEW_TYPE_DEFAULT);
                            GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
                            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                                @Override
                                public int getSpanSize(int position) {
                                    if (adapter.getItemViewType(position) == AdapterCategory.VIEW_TYPE_AD) {
                                        return 3;
                                    }
                                    return 1;
                                }
                            });
                            recyclerView.setLayoutManager(glm);
                        }

                        // Apply cached channel logos immediately if enabled
                        if (sharedPref.getShowRealChannelLogo() && getContext() != null) {
                            for (AppConfig.OverviewItem it : finalItems) {
                                String cid = com.app.webdroid.util.ChannelLogoCache.extractChannelId(it);
                                if (cid != null) {
                                    String cachedLogo = com.app.webdroid.util.ChannelLogoCache.getCachedLogo(getContext(), cid);
                                    if (cachedLogo != null && !cachedLogo.isEmpty()) {
                                        it.image = cachedLogo;
                                    }
                                }
                            }
                        }

                        // Save original items to support filtering/sorting on clean data
                        allItems = new ArrayList<>(finalItems);

                        // Inject Ads
                        List<AppConfig.OverviewItem> withAds = injectNativeAds(finalItems);
                        adapter.setItems(withAds);

                        // Badge Logic
                        checkBadges(finalItems);

                        // Load Real Channel Logos if enabled (only fetches missing ones)
                        loadChannelLogos(finalItems);

                    } else {
                        textError.setVisibility(View.VISIBLE);
                        if (com.app.webdroid.util.CustomChannelManager.isCustomCategorySlug(jsonUrl)) {
                            textError.setText("No channels in this category yet.\nTap '+' below to add channels!");
                        } else {
                            textError.setText("No items found");
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    textError.setVisibility(View.VISIBLE);
                    textError.setText("Parse error: " + e.getMessage());
                });
            }
        }
    }

    private String mapCategoryIcon(String id, String icon) {
        if (icon != null && (icon.startsWith("http://") || icon.startsWith("https://"))) {
            return icon;
        }
        if (id != null) {
            String lower = id.toLowerCase(Locale.US);
            if (lower.contains("top") || lower.contains("flag")) return "https://img.icons8.com/color/96/usa.png";
            if (lower.contains("break") || lower.contains("bolt")) return "https://img.icons8.com/color/96/flash-on.png";
            if (lower.contains("white_house") || lower.contains("president")) return "https://img.icons8.com/color/96/capitol.png";
            if (lower.contains("congress") || lower.contains("senate") || lower.contains("house")) return "https://img.icons8.com/color/96/law.png";
            if (lower.contains("court") || lower.contains("gavel") || lower.contains("balance")) return "https://img.icons8.com/color/96/scales.png";
            if (lower.contains("econom") || lower.contains("money") || lower.contains("inflation") || lower.contains("rate")) return "https://img.icons8.com/color/96/us-dollar-circled.png";
            if (lower.contains("stock") || lower.contains("nasdaq") || lower.contains("dow") || lower.contains("sp500") || lower.contains("market")) return "https://img.icons8.com/color/96/bullish.png";
            if (lower.contains("business") || lower.contains("job") || lower.contains("work")) return "https://img.icons8.com/color/96/briefcase.png";
            if (lower.contains("tech") || lower.contains("ai") || lower.contains("cyber")) return "https://img.icons8.com/color/96/artificial-intelligence.png";
            if (lower.contains("sport") || lower.contains("nfl") || lower.contains("nba") || lower.contains("mlb")) return "https://img.icons8.com/color/96/american-football.png";
            if (lower.contains("health") || lower.contains("med")) return "https://img.icons8.com/color/96/caduceus.png";
            if (lower.contains("science") || lower.contains("space")) return "https://img.icons8.com/color/96/rocket.png";
            if (lower.contains("entertain") || lower.contains("hollywood") || lower.contains("movie")) return "https://img.icons8.com/color/96/clapperboard.png";
            if (lower.contains("world") || lower.contains("global")) return "https://img.icons8.com/color/96/globe.png";
            if (lower.contains("weather")) return "https://img.icons8.com/color/96/partly-cloudy-day.png";
            if (lower.contains("state")) return "https://img.icons8.com/color/96/map.png";
            if (lower.contains("radio")) return "https://img.icons8.com/color/96/radio-tower.png";
        }
        return "https://img.icons8.com/color/96/categorize.png";
    }

    private List<AppConfig.OverviewItem> injectNativeAds(List<AppConfig.OverviewItem> items) {
        // Safe check for context
        if (getContext() == null)
            return items;

        com.app.webdroid.database.prefs.AdsPref adsPref = new com.app.webdroid.database.prefs.AdsPref(getContext());
        if (adsPref.getNativeAdIndex() <= 0)
            return items;
        if (!adsPref.getAdStatus())
            return items;

        int interval = adsPref.getNativeAdIndex();
        List<AppConfig.OverviewItem> newItems = new ArrayList<>();
        int count = 0;
        for (AppConfig.OverviewItem item : items) {
            newItems.add(item);
            count++;
            if (count % interval == 0) {
                AppConfig.OverviewItem adItem = new AppConfig.OverviewItem();
                adItem.provider = "native_ad";
                newItems.add(adItem);
            }
        }
        return newItems;
    }

    private void loadChannelLogos(List<AppConfig.OverviewItem> items) {
        if (!sharedPref.getShowRealChannelLogo() || getContext() == null)
            return;

        // Check which items do NOT have a cached logo yet
        List<Integer> missingIndices = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            AppConfig.OverviewItem it = items.get(i);
            String channelId = com.app.webdroid.util.ChannelLogoCache.extractChannelId(it);
            if (channelId != null) {
                String cached = com.app.webdroid.util.ChannelLogoCache.getCachedLogo(getContext(), channelId);
                if (cached == null || cached.isEmpty()) {
                    missingIndices.add(i);
                }
            }
        }

        // If ALL channel logos are already cached, do NOTHING!
        // No threads, no network requests, no flickering on page change!
        if (missingIndices.isEmpty()) {
            return;
        }

        new Thread(() -> {
            String apiKey = sharedPref.getYoutubeApiKey();
            OkHttpClient client = new OkHttpClient();
            for (int i : missingIndices) {
                if (i >= items.size()) continue;
                AppConfig.OverviewItem item = items.get(i);
                String channelId = com.app.webdroid.util.ChannelLogoCache.extractChannelId(item);
                if (channelId == null || channelId.isEmpty()) continue;

                String logoUrl = null;
                if (apiKey != null && !apiKey.isEmpty()) {
                    try {
                        String apiUrl = "https://www.googleapis.com/youtube/v3/channels?part=snippet&id="
                                + channelId + "&key=" + apiKey;
                        Request req = new Request.Builder().url(apiUrl).build();
                        try (Response res = client.newCall(req).execute()) {
                            if (res.isSuccessful() && res.body() != null) {
                                String body = res.body().string();
                                org.json.JSONObject jsonRoot = new org.json.JSONObject(body);
                                if (jsonRoot.has("items")) {
                                    org.json.JSONArray itemsArr = jsonRoot.getJSONArray("items");
                                    if (itemsArr.length() > 0) {
                                        org.json.JSONObject snippet = itemsArr.getJSONObject(0)
                                                .getJSONObject("snippet");
                                        logoUrl = snippet.getJSONObject("thumbnails")
                                                .getJSONObject("high").getString("url");
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // fallback to web scrape
                    }
                }

                if (logoUrl == null) {
                    // Try Innertube browse API directly (Fast, reliable, no API key required)
                    try {
                        org.json.JSONObject payload = new org.json.JSONObject();
                        org.json.JSONObject ctx = new org.json.JSONObject();
                        org.json.JSONObject clientObj = new org.json.JSONObject();
                        clientObj.put("clientName", "WEB");
                        clientObj.put("clientVersion", "2.20240101.00.00");
                        ctx.put("client", clientObj);
                        payload.put("context", ctx);
                        payload.put("browseId", channelId);

                        okhttp3.MediaType jsonType = okhttp3.MediaType.parse("application/json; charset=utf-8");
                        okhttp3.RequestBody body = okhttp3.RequestBody.create(payload.toString(), jsonType);
                        Request itReq = new Request.Builder()
                                .url("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                                .post(body)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .build();
                        try (Response itRes = client.newCall(itReq).execute()) {
                            if (itRes.isSuccessful() && itRes.body() != null) {
                                String bodyStr = itRes.body().string();
                                org.json.JSONObject data = new org.json.JSONObject(bodyStr);
                                if (data.has("metadata") && data.getJSONObject("metadata").has("channelMetadataRenderer")) {
                                    org.json.JSONObject meta = data.getJSONObject("metadata").getJSONObject("channelMetadataRenderer");
                                    if (meta.has("avatar")) {
                                        org.json.JSONArray thumbs = meta.getJSONObject("avatar").getJSONArray("thumbnails");
                                        if (thumbs.length() > 0) {
                                            logoUrl = thumbs.getJSONObject(thumbs.length() - 1).getString("url");
                                            if (logoUrl != null && logoUrl.startsWith("//")) {
                                                logoUrl = "https:" + logoUrl;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (logoUrl == null) {
                    // Fallback: Scrape YouTube Page
                    try {
                        Request webReq = new Request.Builder()
                                .url("https://www.youtube.com/channel/" + channelId)
                                .header("User-Agent",
                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                .build();
                        try (Response webRes = client.newCall(webReq).execute()) {
                            if (webRes.isSuccessful() && webRes.body() != null) {
                                java.io.BufferedReader reader = new java.io.BufferedReader(
                                        webRes.body().charStream());
                                String line;
                                int limit = 0;
                                while ((line = reader.readLine()) != null && limit < 600) {
                                    limit++;
                                    if (line.contains("og:image")) {
                                        int start = line.indexOf("content=\"");
                                        if (start != -1) {
                                            start += 9;
                                            int end = line.indexOf("\"", start);
                                            if (end != -1) {
                                                logoUrl = line.substring(start, end);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (logoUrl != null && !logoUrl.isEmpty()) {
                    // Persist to cache so it is NEVER fetched again across any page!
                    if (getContext() != null) {
                        com.app.webdroid.util.ChannelLogoCache.saveLogo(getContext(), channelId, logoUrl);
                    }
                    item.image = logoUrl;
                    int finalI = i;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> adapter.notifyItemChanged(finalI));
                    }
                }
            }
        }).start();
    }

    private void setupToolbar(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        toolbarTitle = view.findViewById(R.id.toolbar_title);

        if (toolbar == null)
            return;

        toolbar.setTitle("");
        if (toolbarTitle != null) {
            toolbarTitle.setText(categoryTitle != null ? categoryTitle : "");
        }

        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
        }

        View toolbarDivider = view.findViewById(R.id.toolbar_divider);
        if (sharedPref.getToolbar()) {
            toolbar.setVisibility(View.VISIBLE);
            if (toolbarDivider != null) toolbarDivider.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
            if (toolbarDivider != null) toolbarDivider.setVisibility(View.GONE);
        }

        boolean isDark = sharedPref.getIsDarkTheme();
        if (isDark) {
            toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_dark_toolbar));
            toolbar.getContext().setTheme(androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dark);
            if (toolbarTitle != null) {
                toolbarTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.color_dark_title_toolbar));
            }
        } else {
            toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_light_background));
            toolbar.getContext().setTheme(androidx.appcompat.R.style.ThemeOverlay_AppCompat_Light);
            if (toolbarTitle != null) {
                toolbarTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.color_light_text_primary));
            }
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(ContextCompat.getColor(getContext(), R.color.color_light_text_primary));
            }
            if (toolbar.getOverflowIcon() != null) {
                toolbar.getOverflowIcon().setTint(ContextCompat.getColor(getContext(), R.color.color_light_text_primary));
            }
            if (toolbar.getMenu() != null) {
                android.view.MenuItem item = toolbar.getMenu().findItem(R.id.action_search);
                if (item != null && item.getIcon() != null) {
                    item.getIcon().mutate().setTint(ContextCompat.getColor(getContext(), R.color.color_light_text_primary));
                }
            }
        }

        if (getActivity() != null && getActivity().getWindow() != null) {
            androidx.core.view.WindowInsetsControllerCompat controller =
                    new androidx.core.view.WindowInsetsControllerCompat(getActivity().getWindow(), getActivity().getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(!isDark);
        }
    }

    private void checkBadges(List<AppConfig.OverviewItem> items) {
        if (getContext() == null || items == null)
            return;

        android.content.SharedPreferences prefs = getContext().getSharedPreferences("badges",
                android.content.Context.MODE_PRIVATE);

        for (AppConfig.OverviewItem item : items) {
            long lastSeen = prefs.getLong("last_seen_" + item.title, 0);
            if (lastSeen == 0) {
                item.isNew = true;
            } else {
                item.isNew = false;
            }
        }
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }

    private void showCategoryOrChannelChooser() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_category_or_channel, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnChoiceCategory = dialogView.findViewById(R.id.btn_choice_category);
        if (btnChoiceCategory != null) {
            btnChoiceCategory.setOnClickListener(v -> {
                dialog.dismiss();
                com.app.webdroid.util.CustomChannelManager.showCreateCategoryDialog(getContext(), newCat -> {
                    loadData();
                });
            });
        }

        View btnChoiceChannel = dialogView.findViewById(R.id.btn_choice_channel);
        if (btnChoiceChannel != null) {
            btnChoiceChannel.setOnClickListener(v -> {
                dialog.dismiss();
                showAddChannelDialog();
            });
        }

        View btnChoiceNewspaper = dialogView.findViewById(R.id.btn_choice_newspaper);
        if (btnChoiceNewspaper != null) {
            btnChoiceNewspaper.setOnClickListener(v -> {
                dialog.dismiss();
                if (getActivity() != null) {
                    com.app.webdroid.util.CustomChannelManager.showAddNewspaperDialog(getActivity(), jsonUrl, (item, category) -> {
                        loadData();
                    });
                }
            });
        }

        dialog.show();
    }

    private void showCardOptionsMenu(View anchorView, AppConfig.OverviewItem obj, int position) {
        if (getContext() == null || jsonUrl == null || obj == null) return;

        boolean isCategoryList = "youtube_categories.json".equalsIgnoreCase(jsonUrl) || "home.json".equalsIgnoreCase(jsonUrl);
        String itemType = isCategoryList ? "Category" : "Channel";

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_channel_options, null);
        bottomSheet.setContentView(sheetView);

        ImageView imgLogo = sheetView.findViewById(R.id.img_sheet_channel_logo);
        TextView tvFallback = sheetView.findViewById(R.id.tv_sheet_avatar_fallback);
        TextView tvTitle = sheetView.findViewById(R.id.tv_sheet_channel_title);
        TextView tvSubtitle = sheetView.findViewById(R.id.tv_sheet_channel_subtitle);
        View btnClose = sheetView.findViewById(R.id.btn_sheet_close);

        String rawTitle = obj.title != null ? obj.title : "Options";
        String mainTitle = rawTitle;
        String subTitle = categoryTitle != null ? categoryTitle : "Channel Options";

        if (rawTitle.contains("(") && rawTitle.contains(")")) {
            int startParen = rawTitle.indexOf("(");
            int endParen = rawTitle.indexOf(")");
            if (endParen > startParen) {
                mainTitle = rawTitle.substring(0, startParen).trim();
                subTitle = rawTitle.substring(startParen + 1, endParen).trim();
            }
        }

        if (tvTitle != null) tvTitle.setText(mainTitle);
        if (tvSubtitle != null) tvSubtitle.setText(subTitle);

        if (btnClose != null) btnClose.setOnClickListener(v -> bottomSheet.dismiss());

        if (imgLogo != null) {
            String imageUrl = com.app.webdroid.util.ImageUtil.upgradeAmazonImageUrl(obj.image);
            if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.contains("icons8.com")) {
                com.bumptech.glide.Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder_media)
                        .error(R.drawable.ic_placeholder_media)
                        .circleCrop()
                        .into(imgLogo);
            } else {
                int avatarSize = (int) (32 * getResources().getDisplayMetrics().density);
                android.graphics.drawable.Drawable avatar =
                        com.app.webdroid.util.LetterAvatarUtil.createLetterAvatar(requireContext(), mainTitle, avatarSize);
                imgLogo.setImageDrawable(avatar);
            }
        }

        View rowMove = sheetView.findViewById(R.id.row_move_to_category);
        if (rowMove != null) {
            if (isCategoryList) {
                rowMove.setVisibility(View.GONE);
            } else {
                rowMove.setVisibility(View.VISIBLE);
                rowMove.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    showMoveOrCopyCategoryDialog(obj, true);
                });
            }
        }

        View rowCopy = sheetView.findViewById(R.id.row_copy_to_category);
        if (rowCopy != null) {
            if (isCategoryList) {
                rowCopy.setVisibility(View.GONE);
            } else {
                rowCopy.setVisibility(View.VISIBLE);
                rowCopy.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    showMoveOrCopyCategoryDialog(obj, false);
                });
            }
        }

        View rowEdit = sheetView.findViewById(R.id.row_edit_channel);
        if (rowEdit != null) {
            if (isCategoryList) {
                rowEdit.setVisibility(View.GONE);
            } else {
                rowEdit.setVisibility(View.VISIBLE);
                rowEdit.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    String curId = (obj.arguments != null && !obj.arguments.isEmpty()) ? obj.arguments.get(0) : "";
                    com.app.webdroid.util.CustomChannelManager.showEditChannelDialog(requireContext(), obj.title, curId, () -> {
                        String newId = com.app.webdroid.util.CustomChannelManager.getChannelOverride(requireContext(), obj.title, curId);
                        if (obj.arguments != null && !obj.arguments.isEmpty()) {
                            obj.arguments.set(0, newId);
                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    });
                });
            }
        }

        View rowRemove = sheetView.findViewById(R.id.row_remove_channel);
        TextView tvRemoveTitle = sheetView.findViewById(R.id.tv_row_remove_title);
        TextView tvRemoveSubtitle = sheetView.findViewById(R.id.tv_row_remove_subtitle);
        if (tvRemoveTitle != null) tvRemoveTitle.setText("Remove " + itemType);
        if (tvRemoveSubtitle != null) tvRemoveSubtitle.setText("Hide from " + (categoryTitle != null ? categoryTitle : "this list"));

        if (rowRemove != null) {
            rowRemove.setOnClickListener(v -> {
                bottomSheet.dismiss();
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Remove " + itemType)
                        .setMessage("Do you want to remove '" + obj.title + "' from this list?")
                        .setPositiveButton("Remove", (d, w) -> {
                            com.app.webdroid.util.CustomChannelManager.hideChannel(getContext(), jsonUrl, obj);
                            if ("youtube_categories.json".equalsIgnoreCase(jsonUrl)) {
                                String catSlug = (obj.arguments != null && !obj.arguments.isEmpty()) ? obj.arguments.get(0) : "";
                                com.app.webdroid.util.CustomChannelManager.deleteUserCategoryBySlug(getContext(), catSlug);
                            }
                            android.widget.Toast.makeText(getContext(), "✓ Removed '" + obj.title + "'", android.widget.Toast.LENGTH_SHORT).show();
                            loadData();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        View rowRestore = sheetView.findViewById(R.id.row_restore_hidden);
        TextView tvRestoreTitle = sheetView.findViewById(R.id.tv_row_restore_title);
        if (tvRestoreTitle != null) tvRestoreTitle.setText("Restore All Hidden " + (isCategoryList ? "Categories" : "Channels"));

        if (rowRestore != null) {
            if (com.app.webdroid.util.CustomChannelManager.hasHiddenChannels(getContext(), jsonUrl)) {
                rowRestore.setVisibility(View.VISIBLE);
                rowRestore.setOnClickListener(v -> {
                    bottomSheet.dismiss();
                    com.app.webdroid.util.CustomChannelManager.restoreHiddenChannels(getContext(), jsonUrl);
                    android.widget.Toast.makeText(getContext(), "✓ All channels restored", android.widget.Toast.LENGTH_SHORT).show();
                    loadData();
                });
            } else {
                rowRestore.setVisibility(View.GONE);
            }
        }

        View btnCancel = sheetView.findViewById(R.id.btn_sheet_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> bottomSheet.dismiss());
        }

        bottomSheet.show();
    }

    private void showMoveOrCopyCategoryDialog(AppConfig.OverviewItem obj, boolean isMove) {
        if (getContext() == null || obj == null) return;
        List<com.app.webdroid.util.CustomChannelManager.CategoryOption> allCats =
                com.app.webdroid.util.CustomChannelManager.getAvailableCategories(getContext());

        List<com.app.webdroid.util.CustomChannelManager.CategoryOption> targetCats = new ArrayList<>();
        for (com.app.webdroid.util.CustomChannelManager.CategoryOption cat : allCats) {
            if (!isMove || jsonUrl == null || !cat.jsonUrl.equalsIgnoreCase(jsonUrl)) {
                targetCats.add(cat);
            }
        }

        List<String> catTitles = new ArrayList<>();
        for (com.app.webdroid.util.CustomChannelManager.CategoryOption cat : targetCats) {
            catTitles.add("📁  " + cat.title);
        }
        catTitles.add("＋  Create New Category");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle((isMove ? "Move \"" : "Copy \"") + obj.title + "\" to")
                .setItems(catTitles.toArray(new String[0]), (d, which) -> {
                    if (which < targetCats.size()) {
                        com.app.webdroid.util.CustomChannelManager.CategoryOption targetCat = targetCats.get(which);
                        com.app.webdroid.util.CustomChannelManager.saveCustomChannel(getContext(), targetCat.jsonUrl, obj);
                        if (isMove) {
                            com.app.webdroid.util.CustomChannelManager.hideChannel(getContext(), jsonUrl, obj);
                            android.widget.Toast.makeText(getContext(), "✓ Moved to " + targetCat.title, android.widget.Toast.LENGTH_SHORT).show();
                            loadData();
                        } else {
                            android.widget.Toast.makeText(getContext(), "✓ Copied to " + targetCat.title, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // "＋ Create New Category"
                        com.app.webdroid.util.CustomChannelManager.showCreateCategoryDialog(getContext(), newCat -> {
                            if (newCat != null) {
                                com.app.webdroid.util.CustomChannelManager.saveCustomChannel(getContext(), newCat.jsonUrl, obj);
                                if (isMove) {
                                    com.app.webdroid.util.CustomChannelManager.hideChannel(getContext(), jsonUrl, obj);
                                    android.widget.Toast.makeText(getContext(), "✓ Moved to " + newCat.title, android.widget.Toast.LENGTH_SHORT).show();
                                    loadData();
                                } else {
                                    android.widget.Toast.makeText(getContext(), "✓ Copied to " + newCat.title, android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddChannelDialog() {
        if (activity != null) {
            activity.loadWebPage("Add Channel / Playlist", "ADD_CHANNEL", jsonUrl, jsonUrl);
        }
    }

    private void setupStateWeatherAndHub(View rootView) {
        View weatherContainer = rootView.findViewById(R.id.layout_state_weather_container);
        View hubInclude = rootView.findViewById(R.id.include_state_weather_hub);
        if (hubInclude == null || getContext() == null) return;

        boolean isStatesMenu = (jsonUrl != null && jsonUrl.contains("usa_states_newspapers"));
        boolean isSingleState = (jsonUrl != null && jsonUrl.startsWith("states/"));

        if (!isStatesMenu && !isSingleState) {
            if (weatherContainer != null) weatherContainer.setVisibility(View.GONE);
            hubInclude.setVisibility(View.GONE);
            return;
        }

        if (weatherContainer != null) {
            weatherContainer.setVisibility(View.VISIBLE);
            if (weatherContainer.getLayoutParams() instanceof AppBarLayout.LayoutParams) {
                AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) weatherContainer.getLayoutParams();
                lp.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
                weatherContainer.setLayoutParams(lp);
            }
        }
        hubInclude.setVisibility(View.VISIBLE);

        // 1. Search Bar Setup
        android.widget.EditText etSearch = hubInclude.findViewById(R.id.et_state_search);
        ImageView btnClear = hubInclude.findViewById(R.id.btn_state_search_clear);

        if (isStatesMenu) {
            if (etSearch != null) {
                etSearch.setHint("🔍 Search 50 States (e.g. California, Texas, Florida)...");
            }
        } else {
            String resolvedStateName = categoryTitle != null && !categoryTitle.isEmpty() ? categoryTitle : "State";
            if (etSearch != null) {
                etSearch.setHint("🔍 Search " + resolvedStateName + " newspapers & cities...");
            }
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString().trim().toLowerCase() : "";
                    if (btnClear != null) {
                        btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    filterStateItems(query);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (btnClear != null && etSearch != null) {
            btnClear.setOnClickListener(v -> etSearch.setText(""));
        }

        // 2. State Weather Display & 7-Day Forecast (Active when a single state is opened)
        View cardWeather = hubInclude.findViewById(R.id.card_state_weather);
        View chipsScroll = hubInclude.findViewById(R.id.scroll_state_hub_chips);

        if (!isSingleState) {
            if (cardWeather != null) cardWeather.setVisibility(View.GONE);
            if (chipsScroll != null) chipsScroll.setVisibility(View.GONE);
            return;
        }

        if (cardWeather != null) cardWeather.setVisibility(View.VISIBLE);
        if (chipsScroll != null) chipsScroll.setVisibility(View.VISIBLE);

        final String stateKey = jsonUrl;
        final com.app.webdroid.util.StateWeatherManager.StateLocation loc =
                com.app.webdroid.util.StateWeatherManager.resolveLocation(stateKey);

        TextView tvTitle = hubInclude.findViewById(R.id.tv_state_weather_title);
        TextView tvCity = hubInclude.findViewById(R.id.tv_state_weather_city);
        TextView tvTemp = hubInclude.findViewById(R.id.tv_state_weather_temp);
        TextView tvDesc = hubInclude.findViewById(R.id.tv_state_weather_desc);
        TextView tvBadge = hubInclude.findViewById(R.id.tv_state_condition_badge);
        android.widget.LinearLayout container7Day = hubInclude.findViewById(R.id.container_state_7day_forecast);

        if (tvTitle != null) tvTitle.setText(loc.stateName.toUpperCase(java.util.Locale.US) + " WEATHER");
        if (tvCity != null) tvCity.setText(loc.majorCity + " Metro Hub");

        com.app.webdroid.util.StateWeatherManager.fetchStateWeather(requireContext(), stateKey,
                (currentTempF, condition, highF, lowF, weeklyForecast) -> {
                    if (tvTemp != null) tvTemp.setText(currentTempF + "°F");
                    if (tvDesc != null) tvDesc.setText(condition + " • H: " + highF + "°  L: " + lowF + "°");
                    if (tvBadge != null) tvBadge.setText(condition.toUpperCase(java.util.Locale.US));

                    if (container7Day != null && weeklyForecast != null) {
                        container7Day.removeAllViews();
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        for (com.app.webdroid.util.StateWeatherManager.DayForecast df : weeklyForecast) {
                            View dayView = inflater.inflate(R.layout.item_day_forecast, container7Day, false);
                            TextView tvDay = dayView.findViewById(R.id.tv_forecast_day);
                            ImageView imgIcon = dayView.findViewById(R.id.img_forecast_icon);
                            TextView tvHigh = dayView.findViewById(R.id.tv_forecast_high);
                            TextView tvLow = dayView.findViewById(R.id.tv_forecast_low);

                            if (tvDay != null) tvDay.setText(df.dayName);
                            if (tvHigh != null) tvHigh.setText(df.maxTempF + "°");
                            if (tvLow != null) tvLow.setText(df.minTempF + "°");

                            if (imgIcon != null) {
                                if (df.weatherCode >= 71) {
                                    imgIcon.setImageResource(R.drawable.ic_wb_sunny);
                                    imgIcon.setColorFilter(android.graphics.Color.parseColor("#38BDF8"));
                                } else if (df.weatherCode >= 51) {
                                    imgIcon.setImageResource(R.drawable.ic_wb_sunny);
                                    imgIcon.setColorFilter(android.graphics.Color.parseColor("#0284C7"));
                                } else if (df.weatherCode >= 1) {
                                    imgIcon.setImageResource(R.drawable.ic_wb_sunny);
                                    imgIcon.setColorFilter(android.graphics.Color.parseColor("#94A3B8"));
                                } else {
                                    imgIcon.setImageResource(R.drawable.ic_wb_sunny);
                                    imgIcon.setColorFilter(android.graphics.Color.parseColor("#F59E0B"));
                                }
                            }
                            container7Day.addView(dayView);
                        }
                    }
                });

        // 3. Multi-Hub Action Chips Setup
        TextView chipAll = hubInclude.findViewById(R.id.chip_hub_all_newspapers);
        TextView chipLocalRss = hubInclude.findViewById(R.id.chip_hub_local_rss);
        TextView chipWeatherRss = hubInclude.findViewById(R.id.chip_hub_weather_rss);
        TextView chipMainUsa = hubInclude.findViewById(R.id.chip_hub_main_usa);

        if (chipAll != null) {
            chipAll.setOnClickListener(v -> {
                updateHubChipSelection(chipAll, chipLocalRss, chipWeatherRss, chipMainUsa);
                GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
                recyclerView.setLayoutManager(glm);
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setAdapter(adapter);
                if (allItems != null) {
                    adapter.setItems(injectNativeAds(allItems));
                }
            });
        }

        if (chipLocalRss != null) {
            chipLocalRss.setOnClickListener(v -> {
                updateHubChipSelection(chipLocalRss, chipAll, chipWeatherRss, chipMainUsa);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                String rss = com.app.webdroid.util.StateWeatherManager.getStateNewsRssUrl(loc.stateName);
                com.app.webdroid.util.StateWeatherManager.fetchStateRss(rss, loc.stateName + " News", items -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (items != null && !items.isEmpty()) {
                        bindNewsAdapter(items, loc.stateName + " News");
                    } else {
                        android.widget.Toast.makeText(getContext(), "Fetching latest " + loc.stateName + " stories...", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (chipWeatherRss != null) {
            chipWeatherRss.setOnClickListener(v -> {
                updateHubChipSelection(chipWeatherRss, chipAll, chipLocalRss, chipMainUsa);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                String rss = com.app.webdroid.util.StateWeatherManager.getStateWeatherRssUrl(loc.stateName);
                com.app.webdroid.util.StateWeatherManager.fetchStateRss(rss, loc.stateName + " Weather", items -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (items != null && !items.isEmpty()) {
                        bindNewsAdapter(items, loc.stateName + " Weather");
                    } else {
                        android.widget.Toast.makeText(getContext(), "No severe weather alerts for " + loc.stateName, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (chipMainUsa != null) {
            chipMainUsa.setOnClickListener(v -> {
                updateHubChipSelection(chipMainUsa, chipAll, chipLocalRss, chipWeatherRss);
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                String rss = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en";
                com.app.webdroid.util.StateWeatherManager.fetchStateRss(rss, "Main USA News", items -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (items != null && !items.isEmpty()) {
                        bindNewsAdapter(items, "USA National");
                    }
                });
            });
        }

        TextView chipAiPrompts = hubInclude.findViewById(R.id.chip_hub_ai_prompts);
        if (chipAiPrompts != null) {
            chipAiPrompts.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.app.webdroid.activity.ActivityAiPrompts.class);
                startActivity(intent);
            });
        }
    }

    private void bindNewsAdapter(List<com.app.webdroid.model.NewsItem> items, String defaultSource) {
        if (getContext() == null || recyclerView == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(true);
        AdapterNews newsAdapter = new AdapterNews(getContext(), items);
        newsAdapter.setOnItemClickListener((view, newsItem, position) -> {
            if (newsItem == null) return;
            Intent intent = new Intent(getContext(), com.app.webdroid.activity.ActivityNewsDetail.class);
            intent.putExtra("title", newsItem.title != null ? newsItem.title : "");
            intent.putExtra("description", newsItem.description != null ? newsItem.description : "");
            intent.putExtra("link", newsItem.link != null ? newsItem.link : "");
            intent.putExtra("imageUrl", newsItem.imageUrl);
            intent.putExtra("pubDate", newsItem.pubDate != null ? newsItem.pubDate : "Live");
            intent.putExtra("sourceName", newsItem.sourceName != null ? newsItem.sourceName : defaultSource);
            startActivity(intent);
        });

        newsAdapter.setOnFavoriteClickListener((view, newsItem, position) -> {
            if (getContext() == null || newsItem == null || newsItem.link == null) return;
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(getContext());
                int count = db.favoriteDao().isFavoriteByTargetOrTitle(newsItem.link, newsItem.title);
                if (count > 0) {
                    db.favoriteDao().removeFavoriteComprehensive(newsItem.link, newsItem.link, newsItem.title);
                } else {
                    FavoriteItem fav = new FavoriteItem();
                    fav.itemId = newsItem.link;
                    fav.type = FavoriteItem.TYPE_RSS;
                    fav.title = newsItem.title;
                    fav.subtitle = newsItem.sourceName != null ? newsItem.sourceName : defaultSource;
                    fav.imageUrl = newsItem.imageUrl != null ? newsItem.imageUrl : "";
                    fav.targetUrl = newsItem.link;
                    db.favoriteDao().addFavorite(fav);
                }
            });
        });

        recyclerView.setAdapter(newsAdapter);
    }

    private AppConfig.OverviewItem convertNewsToOverview(com.app.webdroid.model.NewsItem item, String genre) {
        AppConfig.OverviewItem oi = new AppConfig.OverviewItem();
        oi.title = item.title != null ? item.title : "News Story";
        oi.provider = "rss_item";
        oi.arguments = new ArrayList<>();
        oi.arguments.add(item.link != null ? item.link : "");
        oi.link = item.link != null ? item.link : "";
        String iconUrl = item.getSourceIconUrl();
        oi.image = item.imageUrl != null && !item.imageUrl.isEmpty() ? item.imageUrl : (iconUrl != null ? iconUrl : "https://img.icons8.com/color/96/news.png");
        oi.year = item.pubDate != null ? item.pubDate : "Live";
        oi.genre = item.sourceName != null ? item.sourceName : genre;
        oi.plot = item.description != null ? item.description : "";
        oi.rating = "5.0";
        return oi;
    }

    private void updateHubChipSelection(TextView selected, TextView... others) {
        boolean isDark = sharedPref != null && sharedPref.getIsDarkTheme();
        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_chip_selected);
            selected.setTextColor(android.graphics.Color.WHITE);
        }
        for (TextView other : others) {
            if (other != null) {
                other.setBackgroundResource(R.drawable.bg_chip_unselected);
                other.setTextColor(isDark ? 0xFFFFFFFF : 0xFF0F172A);
            }
        }
    }

    private void filterStateItems(String query) {
        if (allItems == null) return;
        if (recyclerView.getAdapter() != adapter) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
            recyclerView.setNestedScrollingEnabled(true);
            recyclerView.setAdapter(adapter);
        }
        if (query.isEmpty()) {
            adapter.setItems(injectNativeAds(allItems));
            return;
        }

        List<AppConfig.OverviewItem> filtered = new ArrayList<>();
        for (AppConfig.OverviewItem it : allItems) {
            boolean matches = false;
            if (it.title != null && it.title.toLowerCase().contains(query)) matches = true;
            if (!matches && it.genre != null && it.genre.toLowerCase().contains(query)) matches = true;
            if (!matches && it.year != null && it.year.toLowerCase().contains(query)) matches = true;
            if (!matches && it.rating != null && it.rating.toLowerCase().contains(query)) matches = true;
            if (matches) filtered.add(it);
        }
        adapter.setItems(injectNativeAds(filtered));
    }
}
