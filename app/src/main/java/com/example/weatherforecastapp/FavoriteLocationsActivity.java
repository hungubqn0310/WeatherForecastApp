package com.example.weatherforecastapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FavoriteLocationsActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GestureDetector gestureDetector;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_locations);
        TextView tvSlide = findViewById(R.id.tvSlide);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.slide_left_to_right);
        tvSlide.startAnimation(pulse);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Vuốt từ trái sang phải (quay lại MainActivity)
                            finish(); // Tốt hơn là finish thay vì startActivity lại MainActivity
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        LinearLayout locationContainer = findViewById(R.id.locationContainer);

        String[] favoriteCities = {"Vị trí của tôi", "Hanoi", "Ho Chi Minh City", "Da Nang", "New York", "Tokyo"};

        for (String city : favoriteCities) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.location_card, locationContainer, false);

            TextView tvLocationTitle = cardView.findViewById(R.id.tvLocationTitle);
            TextView tvCityName = cardView.findViewById(R.id.tvCityName);
            TextView tvTime = cardView.findViewById(R.id.tvTime);
            TextView tvWeatherStatus = cardView.findViewById(R.id.tvWeatherStatus);
            TextView tvTemperature = cardView.findViewById(R.id.tvTemperature);
            TextView tvHighTemp = cardView.findViewById(R.id.tvHighTemp);
            TextView tvLowTemp = cardView.findViewById(R.id.tvLowTemp);

            if (city.equals("Vị trí của tôi")) {
                tvLocationTitle.setText("Vị trí của tôi");
                tvCityName.setText("Đang tải...");

                // Yêu cầu quyền và lấy vị trí thực tế rồi gọi API
                requestLocationAndFetchWeather(tvCityName, tvTime, tvWeatherStatus, tvTemperature, tvHighTemp, tvLowTemp);

            } else {
                tvLocationTitle.setText(city);
                tvCityName.setText(city);
                fetchWeather(city, tvTime, tvWeatherStatus, tvTemperature, tvHighTemp, tvLowTemp);
            }

            locationContainer.addView(cardView);
        }
    }

    private void requestLocationAndFetchWeather(TextView tvCityName, TextView tvTime, TextView tvWeatherStatus,
                                                TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Chưa có quyền, yêu cầu người dùng cấp quyền
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Đã có quyền, lấy vị trí
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();

                        // Gọi API theo tọa độ
                        fetchWeatherByCoordinates(lat, lon, tvCityName, tvTime, tvWeatherStatus, tvTemperature, tvHighTemp, tvLowTemp);
                    } else {
                        // Nếu không lấy được vị trí, hiển thị lỗi hoặc fallback
                        tvCityName.setText("Không xác định được vị trí");
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    tvCityName.setText("Lỗi lấy vị trí");
                });
    }

    // Xử lý kết quả cấp quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                // Người dùng cấp quyền, bạn có thể tải lại activity hoặc gọi lại hàm lấy vị trí nếu muốn
                recreate(); // Tải lại activity để lấy vị trí
            } else {
                // Người dùng không cấp quyền, xử lý phù hợp
            }
        }
    }

    private void fetchWeatherByCoordinates(double lat, double lon, TextView tvCityName, TextView tvTime,
                                           TextView tvWeatherStatus, TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);

        // Gọi API với tham số q là "lat,lon"
        String latlon = lat + "," + lon;

        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", latlon, 1, "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();

                    tvCityName.setText(weather.location.name);

                    String time = weather.location.localtime.split(" ")[1];
                    tvTime.setText(time);

                    tvWeatherStatus.setText(weather.current.condition.text);
                    tvTemperature.setText(weather.current.temp_c + "°");
                    tvHighTemp.setText("C:" + weather.forecast.forecastday.get(0).day.maxtemp_c + "°");
                    tvLowTemp.setText("T:" + weather.forecast.forecastday.get(0).day.mintemp_c + "°");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void fetchWeather(String city, TextView tvTime, TextView tvWeatherStatus, TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
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

                    String time = weather.location.localtime.split(" ")[1];
                    tvTime.setText(time);

                    tvWeatherStatus.setText(weather.current.condition.text);
                    tvTemperature.setText(weather.current.temp_c + "°");
                    tvHighTemp.setText("C:" + weather.forecast.forecastday.get(0).day.maxtemp_c + "°");
                    tvLowTemp.setText("T:" + weather.forecast.forecastday.get(0).day.mintemp_c + "°");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}
