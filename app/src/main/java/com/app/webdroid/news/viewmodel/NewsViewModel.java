package com.app.webdroid.news.viewmodel;

import android.app.Application;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.app.webdroid.news.model.NewsCategoriesResponse.CategoryItem;
import com.app.webdroid.news.model.NewsStory;
import com.app.webdroid.news.repository.NewsRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NewsViewModel extends AndroidViewModel {

    public enum SortOption {
        LATEST,
        MOST_RELEVANT,
        CATEGORY
    }

    private final NewsRepository repository;

    private final MutableLiveData<List<NewsStory>> _stories = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<NewsStory>> stories = _stories;

    private final MutableLiveData<List<CategoryItem>> _categories = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<CategoryItem>> categories = _categories;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isOffline = new MutableLiveData<>(false);
    public final LiveData<Boolean> isOffline = _isOffline;

    private final MutableLiveData<String> _lastUpdatedText = new MutableLiveData<>("");
    public final LiveData<String> lastUpdatedText = _lastUpdatedText;

    private final MutableLiveData<String> _selectedCategoryId = new MutableLiveData<>("all");
    public final LiveData<String> selectedCategoryId = _selectedCategoryId;

    private final MutableLiveData<SortOption> _currentSort = new MutableLiveData<>(SortOption.LATEST);
    public final LiveData<SortOption> currentSort = _currentSort;

    private final List<NewsStory> masterStoryList = new ArrayList<>();
    private String currentSearchQuery = "";

    public NewsViewModel(@NonNull Application application) {
        super(application);
        this.repository = NewsRepository.getInstance(application);
        loadCategories();
        loadNews();
    }

    public void loadCategories() {
        repository.getCategories(new NewsRepository.CategoriesCallback() {
            @Override
            public void onSuccess(List<CategoryItem> cats) {
                List<CategoryItem> fullList = new ArrayList<>();
                fullList.add(new CategoryItem("all", "Top Stories", "flag"));
                if (cats != null) {
                    fullList.addAll(cats);
                }
                _categories.postValue(fullList);
            }

            @Override
            public void onError(String message) {
                List<CategoryItem> fallback = new ArrayList<>();
                fallback.add(new CategoryItem("all", "Top Stories", "flag"));
                _categories.postValue(fallback);
            }
        });
    }

    public void loadNews() {
        _isLoading.setValue(true);
        repository.getNews(new NewsRepository.NewsCallback() {
            @Override
            public void onSuccess(List<NewsStory> newsStories, boolean isFromCache, long lastUpdatedMinutesAgo) {
                _isLoading.postValue(false);
                _isOffline.postValue(isFromCache);

                if (isFromCache) {
                    if (lastUpdatedMinutesAgo > 0) {
                        _lastUpdatedText.postValue("Last updated: " + lastUpdatedMinutesAgo + " min ago");
                    } else {
                        _lastUpdatedText.postValue("Showing cached news");
                    }
                } else {
                    _lastUpdatedText.postValue("Live updated just now");
                }

                masterStoryList.clear();
                if (newsStories != null) {
                    masterStoryList.addAll(newsStories);
                }
                applyFilterAndSort();
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _isOffline.postValue(true);
                _lastUpdatedText.postValue("Failed to connect. Showing cached stories.");
                applyFilterAndSort();
            }
        });
    }

    public void selectCategory(String categoryId) {
        if (categoryId == null) categoryId = "all";
        _selectedCategoryId.setValue(categoryId);
        applyFilterAndSort();
    }

    public void setSearchQuery(String query) {
        this.currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
        applyFilterAndSort();
    }

    public void setSortOption(SortOption option) {
        if (option != null) {
            _currentSort.setValue(option);
            applyFilterAndSort();
        }
    }

    private void applyFilterAndSort() {
        String activeCatId = _selectedCategoryId.getValue();
        if (activeCatId == null) activeCatId = "all";

        List<NewsStory> filtered = new ArrayList<>();
        boolean isAll = "all".equalsIgnoreCase(activeCatId);

        for (NewsStory story : masterStoryList) {
            // Category check
            boolean matchesCategory = isAll;
            if (!isAll) {
                String catId = story.getCategoryId();
                String catName = story.getCategory();
                if (catId != null && catId.equalsIgnoreCase(activeCatId)) {
                    matchesCategory = true;
                } else if (catName != null && catName.equalsIgnoreCase(activeCatId)) {
                    matchesCategory = true;
                }
            }

            if (!matchesCategory) continue;

            // Search query check (title, summary, source, category)
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                String title = story.getTitle() != null ? story.getTitle().toLowerCase() : "";
                String summary = story.getSummary() != null ? story.getSummary().toLowerCase() : "";
                String source = story.getSource() != null ? story.getSource().toLowerCase() : "";
                String category = story.getCategory() != null ? story.getCategory().toLowerCase() : "";

                boolean matchesSearch = title.contains(currentSearchQuery)
                        || summary.contains(currentSearchQuery)
                        || source.contains(currentSearchQuery)
                        || category.contains(currentSearchQuery);

                if (!matchesSearch) continue;
            }

            filtered.add(story);
        }

        // Apply sorting
        SortOption sort = _currentSort.getValue();
        if (sort == null) sort = SortOption.LATEST;

        switch (sort) {
            case LATEST:
                // Sort ascending by age_minutes (lowest age = newest)
                Collections.sort(filtered, Comparator.comparingLong(NewsStory::getAgeMinutes));
                break;

            case MOST_RELEVANT:
                // Sort by source coverage count descending, then age
                Collections.sort(filtered, (s1, s2) -> {
                    int c = Integer.compare(s2.getSourceCount(), s1.getSourceCount());
                    if (c != 0) return c;
                    return Long.compare(s1.getAgeMinutes(), s2.getAgeMinutes());
                });
                break;

            case CATEGORY:
                Collections.sort(filtered, (s1, s2) -> s1.getCategory().compareToIgnoreCase(s2.getCategory()));
                break;
        }

        _stories.postValue(filtered);
    }
}
