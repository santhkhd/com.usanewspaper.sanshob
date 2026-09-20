package com.app.webdroid.news.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import com.app.webdroid.news.model.NewsStory;
import java.util.ArrayList;
import java.util.List;

/**
 * Separate weather architecture.
 * Keep weather provider replaceable.
 * If no weather API is configured, shows weather-related news rather than inventing forecasts.
 * Never fabricates weather information.
 */
public class WeatherRepository {

    public interface WeatherCallback {
        void onWeatherNewsLoaded(List<NewsStory> weatherStories);
        void onError(String error);
    }

    private final NewsRepository newsRepository;

    public WeatherRepository(Context context) {
        this.newsRepository = NewsRepository.getInstance(context);
    }

    /**
     * Retrieves genuine weather-related news stories without fabricating meteorological numbers.
     */
    public void getWeatherRelatedNews(@NonNull WeatherCallback callback) {
        newsRepository.getNews(new NewsRepository.NewsCallback() {
            @Override
            public void onSuccess(List<NewsStory> stories, boolean isFromCache, long lastUpdatedMinutesAgo) {
                List<NewsStory> weatherStories = new ArrayList<>();
                for (NewsStory story : stories) {
                    String catId = story.getCategoryId();
                    String catName = story.getCategory();
                    if ((catId != null && (catId.equals("weather") || catId.equals("storms") ||
                            catId.equals("hurricanes") || catId.equals("tornadoes") ||
                            catId.equals("flooding") || catId.equals("wildfires"))) ||
                            (catName != null && catName.toLowerCase().contains("weather"))) {
                        weatherStories.add(story);
                    }
                }
                callback.onWeatherNewsLoaded(weatherStories);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
