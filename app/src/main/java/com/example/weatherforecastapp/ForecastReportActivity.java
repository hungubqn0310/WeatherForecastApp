package com.example.weatherforecastapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForecastReportActivity extends AppCompatActivity {

    private LinearLayout forecastContainer;
    private LinearLayout todayForecast;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forecast_report);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        forecastContainer = findViewById(R.id.forecast_container);
        todayForecast = findViewById(R.id.today_forecast);
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        String city = getIntent().getStringExtra("CITY_NAME");
        if (city == null || city.isEmpty()) city = "Ha Noi";

        fetchForecast(city);

    }

    private void fetchForecast(String city) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);

        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", city, 7, "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    runOnUiThread(() -> {
                        for (WeatherResponse.ForecastDay forecastDay : response.body().forecast.forecastday) {
                            View itemView = getLayoutInflater().inflate(R.layout.forecast_item, null);

                            TextView dateText = itemView.findViewById(R.id.date_text);
                            TextView tempText = itemView.findViewById(R.id.temp_text);
                            ImageView icon = itemView.findViewById(R.id.weather_icon);

                            dateText.setText(forecastDay.date);
                            tempText.setText(forecastDay.day.avgtemp_c + "°C");

                            String iconUrl = "https:" + forecastDay.day.condition.icon;
                            Glide.with(ForecastReportActivity.this).load(iconUrl).into(icon);

                            forecastContainer.addView(itemView);
                        }
                    });
                }
                if (response.isSuccessful() && response.body() != null) {
                    runOnUiThread(() -> {
                        todayForecast.removeAllViews(); // Xóa các item cũ

                        // Lặp qua các giờ trong ngày hôm nay
                        for (WeatherResponse.Hour hour : response.body().forecast.forecastday.get(0).hour) {
                            View itemView = getLayoutInflater().inflate(R.layout.forecast_item1, null);

                            TextView tempText = itemView.findViewById(R.id.temp_text);
                            ImageView icon = itemView.findViewById(R.id.weather_icon);
                            TextView timeText = itemView.findViewById(R.id.time_text);

                            tempText.setText(hour.temp_c + "°C");
                            timeText.setText(hour.time.split(" ")[1]); // Lấy giờ từ chuỗi thời gian

                            String iconUrl = "https:" + hour.condition.icon;
                            Glide.with(ForecastReportActivity.this).load(iconUrl).into(icon);

                            todayForecast.addView(itemView);
                        }
                    });
                }
            }





            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
}