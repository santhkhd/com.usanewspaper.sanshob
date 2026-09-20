package com.app.webdroid.news.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.app.webdroid.Config;
import com.app.webdroid.news.model.NewsResponse;
import com.app.webdroid.news.network.NewsApiClient;
import com.app.webdroid.news.repository.NewsRepository;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;

public class NewsSyncWorker extends Worker {

    private static final String WORK_NAME = "us_news_periodic_sync_work";

    public NewsSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String url = Config.US_NEWS_GITHUB_RAW_URL;
        if (url == null || url.trim().isEmpty()) {
            return Result.success();
        }

        try {
            Response<NewsResponse> response = NewsApiClient.getApiService().getNewsFeed(url).execute();
            if (response.isSuccessful() && response.body() != null) {
                // NewsRepository caches successfully
                NewsRepository.getInstance(getApplicationContext()).getNews(new NewsRepository.NewsCallback() {
                    @Override
                    public void onSuccess(java.util.List<com.app.webdroid.news.model.NewsStory> stories, boolean isFromCache, long lastUpdatedMinutesAgo) {}

                    @Override
                    public void onError(String message) {}
                });
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /**
     * Enqueues periodic background refresh respecting battery and network constraints.
     * Refreshes every 3 to 6 hours rather than aggressively polling.
     */
    public static void enqueuePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(NewsSyncWorker.class, 3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}
