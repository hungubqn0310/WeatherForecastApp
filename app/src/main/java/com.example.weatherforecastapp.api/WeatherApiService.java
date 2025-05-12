package com.example.weatherforecastapp.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("forecast.json")
    Call<WeatherResponse> getForecast(
            @Query("key") String apiKey,
            @Query("q") String city,
            @Query("days") int days,
            @Query("lang") String lang
    );
}
