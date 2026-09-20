package com.app.webdroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.webdroid.database.prefs.SharedPref;
import com.app.webdroid.model.AiPromptItem;
import com.shobmc.san.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdapterAiPrompts extends RecyclerView.Adapter<AdapterAiPrompts.ViewHolder> {

    public interface OnPromptActionListener {
        void onChatGPT(AiPromptItem item, String promptText);
        void onPerplexity(AiPromptItem item, String promptText);
        void onGemini(AiPromptItem item, String promptText);
        void onCopy(AiPromptItem item, String promptText);
        void onEdit(AiPromptItem item);
    }

    private final Context context;
    private List<AiPromptItem> items = new ArrayList<>();
    private final Set<Integer> expandedIds = new HashSet<>();
    private final OnPromptActionListener listener;
    private final boolean isDark;

    public AdapterAiPrompts(Context context, OnPromptActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.isDark = new SharedPref(context).getIsDarkTheme();
    }

    public void setItems(List<AiPromptItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_prompt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AiPromptItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView textCategory;
        TextView textId;
        TextView textTitle;
        TextView textDescription;
        TextView textPrompt;
        TextView btnToggleExpand;
        LinearLayout layoutPromptBox;

        View btnAskChatGPT;
        View btnAskPerplexity;
        FrameLayout btnAskGemini;
        FrameLayout btnCopyPrompt;
        FrameLayout btnEditPrompt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.text_category);
            textId = itemView.findViewById(R.id.text_id);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textPrompt = itemView.findViewById(R.id.text_prompt);
            btnToggleExpand = itemView.findViewById(R.id.btn_toggle_expand);
            layoutPromptBox = itemView.findViewById(R.id.layout_prompt_box);

            btnAskChatGPT = itemView.findViewById(R.id.btn_ask_chatgpt);
            btnAskPerplexity = itemView.findViewById(R.id.btn_ask_perplexity);
            btnAskGemini = itemView.findViewById(R.id.btn_ask_gemini);
            btnCopyPrompt = itemView.findViewById(R.id.btn_copy_prompt);
            btnEditPrompt = itemView.findViewById(R.id.btn_edit_prompt);
        }

        public void bind(AiPromptItem item) {
            if (item == null) return;

            textCategory.setText(item.category != null ? item.category.toUpperCase() : "AI PROMPT");
            textId.setText(String.format("#%02d", item.id));
            textTitle.setText(item.title);
            textDescription.setText(item.description);
            textPrompt.setText(item.prompt);

            if (isDark) {
                layoutPromptBox.setBackgroundResource(R.drawable.bg_ai_prompt_box_dark);
            } else {
                layoutPromptBox.setBackgroundResource(R.drawable.bg_ai_prompt_box);
            }

            boolean isExpanded = expandedIds.contains(item.id);
            if (isExpanded) {
                textPrompt.setMaxLines(Integer.MAX_VALUE);
                btnToggleExpand.setText("Collapse ▲");
            } else {
                textPrompt.setMaxLines(3);
                btnToggleExpand.setText("Tap to expand ▼");
            }

            View.OnClickListener toggleClick = v -> {
                if (expandedIds.contains(item.id)) {
                    expandedIds.remove(item.id);
                } else {
                    expandedIds.add(item.id);
                }
                notifyItemChanged(getAdapterPosition());
            };

            layoutPromptBox.setOnClickListener(toggleClick);
            btnToggleExpand.setOnClickListener(toggleClick);

            btnAskChatGPT.setOnClickListener(v -> {
                if (listener != null) listener.onChatGPT(item, item.prompt);
            });

            btnAskPerplexity.setOnClickListener(v -> {
                if (listener != null) listener.onPerplexity(item, item.prompt);
            });

            btnAskGemini.setOnClickListener(v -> {
                if (listener != null) listener.onGemini(item, item.prompt);
            });

            btnCopyPrompt.setOnClickListener(v -> {
                if (listener != null) listener.onCopy(item, item.prompt);
            });

            btnEditPrompt.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(item);
            });
        }
    }
}
