package com.app.webdroid.news.network;

import com.app.webdroid.news.model.NewsCategoriesResponse;
import com.app.webdroid.news.model.NewsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface NewsApiService {

    @GET
    Call<NewsResponse> getNewsFeed(@Url String fullUrl);

    @GET
    Call<NewsCategoriesResponse> getCategories(@Url String fullUrl);
}
