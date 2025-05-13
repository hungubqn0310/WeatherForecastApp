package com.example.weatherforecastapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    TextView tvCity, tvDate, tvTemperature, tvWeatherStatus, tvWind, tvHumidity;
    ImageView ivNotification, ivWeatherIcon;
    Button btnForecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gán ID từ layout
        tvCity = findViewById(R.id.tvCity);
        tvDate = findViewById(R.id.tvDate);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvWeatherStatus = findViewById(R.id.tvWeatherStatus);
        tvWind = findViewById(R.id.tvWind);
        tvHumidity = findViewById(R.id.tvHumidity);
        ivNotification = findViewById(R.id.ivNotification);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        btnForecast = findViewById(R.id.btnForecast);

        // Nhận tên thành phố từ Intent (nếu có)
        String city = getIntent().getStringExtra("CITY_NAME");
        if (city == null || city.isEmpty()) {
            city = "Ha Noi"; // fallback mặc định
        }

        fetchWeather(city);

        // Click để mở bản đồ chọn vị trí
        tvCity.setOnClickListener(v -> openLocationPicker());

        // Hiện thông báo
        ivNotification.setOnClickListener(v -> showNotificationPopup());

        // Mở Forecast
        btnForecast.setOnClickListener(v -> openForecastReport());
    }

    private void fetchWeather(String city) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);

        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", city, 1, "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();

                    tvCity.setText(weather.location.name);
                    tvDate.setText("Hôm nay, " + weather.location.localtime);
                    tvTemperature.setText(weather.current.temp_c + "°");

                    // Kiểm tra điều kiện trước khi truy cập
                    if (weather.current.condition != null) {
                        tvWeatherStatus.setText(weather.current.condition.text);
                        String iconUrl = "https:" + weather.current.condition.icon;
                        Glide.with(MainActivity.this)
                                .load(iconUrl)
                                .into(ivWeatherIcon);
                    } else {
                        tvWeatherStatus.setText("Không có thông tin thời tiết");
                        ivWeatherIcon.setImageResource(R.drawable.ic_cloud_sun); // Hình ảnh mặc định
                    }

                    tvWind.setText(weather.current.wind_kph + " km/h");
                    tvHumidity.setText(weather.current.humidity + "%");
                } else {
                    // Xử lý trường hợp API trả về không thành công
                    tvWeatherStatus.setText("Không thể lấy dữ liệu thời tiết");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
                tvWeatherStatus.setText("Lỗi kết nối");
            }
        });
    }

    private void showNotificationPopup() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.notification_popup, null);
        bottomSheetDialog.setContentView(sheetView);

        ImageView ivClose = sheetView.findViewById(R.id.ivClosePopup);
        ivClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void openLocationPicker() {
        Intent intent = new Intent(MainActivity.this, LocationPickerActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0); // không hiệu ứng chuyển
    }

    private void openForecastReport() {
        Intent intent = new Intent(MainActivity.this, ForecastReportActivity.class);
        intent.putExtra("CITY_NAME", tvCity.getText().toString());
        startActivity(intent);
    }
}
