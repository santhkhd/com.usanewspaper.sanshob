package com.app.webdroid.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.adapter.AdapterAiCategoryChips;
import com.app.webdroid.adapter.AdapterAiPrompts;
import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.AiPromptItem;
import com.app.webdroid.util.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shobmc.san.R;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ActivityAiPrompts extends AppCompatActivity implements AdapterAiPrompts.OnPromptActionListener {

    private Toolbar toolbar;
    private EditText editSearch;
    private ImageView btnClearSearch;
    private RecyclerView rvCategories;
    private RecyclerView rvPrompts;
    private TextView textPromptsCount;
    private View layoutEmptyState;
    private TextView textEmptyQuery;
    private Button btnResetSearch;

    private AdapterAiPrompts adapterPrompts;
    private AdapterAiCategoryChips adapterCategories;

    private List<AiPromptItem> allPrompts = new ArrayList<>();
    private List<AiPromptItem> filteredPrompts = new ArrayList<>();
    private String currentCategory = "All Prompts";
    private String currentQuery = "";
    private SharedPref sharedPref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_ai_prompts);
        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);

        initViews();
        setupToolbar();
        setupRecyclerViews();
        loadPromptsFromAssets();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editSearch = findViewById(R.id.edit_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        rvCategories = findViewById(R.id.rv_categories);
        rvPrompts = findViewById(R.id.rv_prompts);
        textPromptsCount = findViewById(R.id.text_prompts_count);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        textEmptyQuery = findViewById(R.id.text_empty_query);
        btnResetSearch = findViewById(R.id.btn_reset_search);

        btnClearSearch.setOnClickListener(v -> {
            editSearch.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });

        btnResetSearch.setOnClickListener(v -> {
            editSearch.setText("");
            currentCategory = "All Prompts";
            if (adapterCategories != null) {
                adapterCategories.selectFirst();
            }
            applyFilters();
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        rvPrompts.setLayoutManager(new LinearLayoutManager(this));
        adapterPrompts = new AdapterAiPrompts(this, this);
        rvPrompts.setAdapter(adapterPrompts);

        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadPromptsFromAssets() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream is = getAssets().open("ai_news_prompts.json");
                Type listType = new TypeToken<List<AiPromptItem>>() {}.getType();
                List<AiPromptItem> parsed = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), listType);
                is.close();

                runOnUiThread(() -> {
                    if (parsed != null && !parsed.isEmpty()) {
                        allPrompts = parsed;
                        setupCategories();
                        applyFilters();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupCategories() {
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        categoryCounts.put("All Prompts", allPrompts.size());

        for (AiPromptItem item : allPrompts) {
            if (item.category != null) {
                int count = categoryCounts.containsKey(item.category) ? categoryCounts.get(item.category) : 0;
                categoryCounts.put(item.category, count + 1);
            }
        }

        List<AdapterAiCategoryChips.CategoryChip> chipList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            chipList.add(new AdapterAiCategoryChips.CategoryChip(entry.getKey(), entry.getValue()));
        }

        adapterCategories = new AdapterAiCategoryChips(this, chipList, categoryName -> {
            currentCategory = categoryName;
            applyFilters();
        });
        rvCategories.setAdapter(adapterCategories);
    }

    private void applyFilters() {
        filteredPrompts.clear();
        boolean isAllCategories = "All Prompts".equalsIgnoreCase(currentCategory);

        for (AiPromptItem item : allPrompts) {
            boolean categoryMatch = isAllCategories || (item.category != null && item.category.equalsIgnoreCase(currentCategory));
            if (categoryMatch && item.matches(currentQuery)) {
                filteredPrompts.add(item);
            }
        }

        adapterPrompts.setItems(filteredPrompts);
        textPromptsCount.setText(String.format("Showing %d Prompts", filteredPrompts.size()));

        if (filteredPrompts.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvPrompts.setVisibility(View.GONE);
            if (!currentQuery.isEmpty()) {
                textEmptyQuery.setText(String.format("No prompts found for \"%s\"", currentQuery));
            } else {
                textEmptyQuery.setText(String.format("No prompts found in \"%s\"", currentCategory));
            }
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvPrompts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- AI Action Handlers ---

    @Override
    public void onChatGPT(AiPromptItem item, String promptText) {
        launchChatGPT(promptText);
    }

    @Override
    public void onPerplexity(AiPromptItem item, String promptText) {
        launchPerplexity(promptText);
    }

    @Override
    public void onGemini(AiPromptItem item, String promptText) {
        launchGemini(promptText);
    }

    @Override
    public void onCopy(AiPromptItem item, String promptText) {
        copyPromptToClipboard(promptText);
    }

    @Override
    public void onEdit(AiPromptItem item) {
        showEditPromptDialog(item);
    }

    public void launchChatGPT(String prompt) {
        try {
            String encoded = URLEncoder.encode(prompt, "UTF-8");
            String chatgptUrl = "https://chatgpt.com/?q=" + encoded;
            openBrowserOrCustomTab(chatgptUrl, "ChatGPT");
            Toast.makeText(this, "Opening in ChatGPT...", Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void launchPerplexity(String prompt) {
        try {
            String encoded = URLEncoder.encode(prompt, "UTF-8");
            String perplexityUrl = "https://www.perplexity.ai/search?q=" + encoded;
            openBrowserOrCustomTab(perplexityUrl, "Perplexity AI");
            Toast.makeText(this, "Searching live news on Perplexity...", Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void launchGemini(String prompt) {
        try {
            String encoded = URLEncoder.encode(prompt, "UTF-8");
            String geminiUrl = "https://gemini.google.com/app";
            // Also copy prompt to clipboard so user can paste immediately if Gemini redirects to app home
            copyPromptToClipboard(prompt, false);
            openBrowserOrCustomTab(geminiUrl, "Google Gemini");
            Toast.makeText(this, "Prompt copied! Opening Gemini...", Toast.LENGTH_LONG).show();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void copyPromptToClipboard(String prompt) {
        copyPromptToClipboard(prompt, true);
    }

    private void copyPromptToClipboard(String prompt, boolean showNotification) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("AI News Prompt", prompt);
            clipboard.setPrimaryClip(clip);
            if (showNotification) {
                Snackbar.make(findViewById(R.id.parent_view), "✅ AI Prompt copied to clipboard! Ready to paste into ChatGPT, Perplexity, or Gemini.", Snackbar.LENGTH_LONG)
                        .setAction("Open ChatGPT", v -> launchChatGPT(prompt))
                        .show();
            }
        }
    }

    private void openBrowserOrCustomTab(String url, String title) {
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build();
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            // Fallback to external view intent or in-app webview
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception ex) {
                Intent webIntent = new Intent(this, ActivityWebView.class);
                webIntent.putExtra("title", title);
                webIntent.putExtra("link", url);
                startActivity(webIntent);
            }
        }
    }

    private void showEditPromptDialog(AiPromptItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_ai_prompt, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText editPrompt = dialogView.findViewById(R.id.edit_custom_prompt);
        TextView textTitle = dialogView.findViewById(R.id.dialog_title);
        ImageView btnClose = dialogView.findViewById(R.id.dialog_btn_close);

        Button btnChatGPT = dialogView.findViewById(R.id.btn_dialog_chatgpt);
        Button btnPerplexity = dialogView.findViewById(R.id.btn_dialog_perplexity);
        Button btnGemini = dialogView.findViewById(R.id.btn_dialog_gemini);
        Button btnCopy = dialogView.findViewById(R.id.btn_dialog_copy);

        if (item.title != null) {
            textTitle.setText(item.title);
        }
        editPrompt.setText(item.prompt);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnChatGPT.setOnClickListener(v -> {
            String edited = editPrompt.getText().toString().trim();
            if (!edited.isEmpty()) {
                dialog.dismiss();
                launchChatGPT(edited);
            }
        });

        btnPerplexity.setOnClickListener(v -> {
            String edited = editPrompt.getText().toString().trim();
            if (!edited.isEmpty()) {
                dialog.dismiss();
                launchPerplexity(edited);
            }
        });

        btnGemini.setOnClickListener(v -> {
            String edited = editPrompt.getText().toString().trim();
            if (!edited.isEmpty()) {
                dialog.dismiss();
                launchGemini(edited);
            }
        });

        btnCopy.setOnClickListener(v -> {
            String edited = editPrompt.getText().toString().trim();
            if (!edited.isEmpty()) {
                dialog.dismiss();
                copyPromptToClipboard(edited);
            }
        });

        dialog.show();
    }
}
