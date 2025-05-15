package com.example.weatherforecastapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.AnimationDrawable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.bumptech.glide.Glide;
import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;

    TextView tvCity, tvDate, tvTemperature, tvWeatherStatus, tvWind, tvHumidity;
    ImageView ivNotification, ivWeatherIcon;
    View notificationBadge;
    Button btnForecast;
    FrameLayout notificationContainer;
    ConstraintLayout rootLayout; // Thêm biến layout gốc
    FrameLayout rainContainer; // Container cho hiệu ứng mưa
    RainView rainView; // View hiệu ứng mưa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tvSlide = findViewById(R.id.tvSlide);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.slide_right_to_left);
        tvSlide.startAnimation(pulse);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button btnMyLocation = findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getLastLocationAndFetchWeather();
            } else {
                requestLocationPermission();
            }
        });

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
        notificationContainer = findViewById(R.id.notificationContainer);
        notificationBadge = findViewById(R.id.notificationBadge);
        rootLayout = findViewById(R.id.rootLayout);
        rainContainer = findViewById(R.id.rainContainer);

        // Khởi tạo hiệu ứng mưa
        setupRainEffect();

        // Luôn hiển thị red dot khi mở app
        notificationBadge.setVisibility(View.VISIBLE);

        // Nhận tên thành phố từ Intent (nếu có)
        String city = getIntent().getStringExtra("CITY_NAME");
        if (city == null || city.isEmpty()) {
            city = "Hanoi"; // fallback mặc định
        }

        fetchWeather(city);

        // Click để mở bản đồ chọn vị trí
        tvCity.setOnClickListener(v -> openLocationPicker());

        // Click thông báo
        notificationContainer.setOnClickListener(v -> {
            showNotificationPopup();
            // Chỉ ẩn red dot khi click vào thông báo, không lưu trạng thái
            notificationBadge.setVisibility(View.GONE);
        });

        // Mở Forecast
        btnForecast.setOnClickListener(v -> openForecastReport());

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            // Vuốt từ phải sang trái
                            Intent intent = new Intent(MainActivity.this, FavoriteLocationsActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
    private void setupRainEffect() {
        // Tạo và thêm RainView vào rainContainer
        rainView = new RainView(this);
        rainContainer.addView(rainView);

        // Ban đầu ẩn hiệu ứng mưa
        rainContainer.setVisibility(View.GONE);
    }
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndFetchWeather();
            } else {
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void getLastLocationAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Đã kiểm tra trước, nên không vào đây
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                // Gọi API theo tọa độ
                fetchWeatherByCoordinates(latitude, longitude);

            } else {
                Toast.makeText(this, "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lấy vị trí thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void fetchWeatherByCoordinates(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);

        String query = latitude + "," + longitude; // API hỗ trợ query theo dạng "lat,long"

        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", query, 1, "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();

                    tvCity.setText(weather.location.name);
                    tvDate.setText("Hôm nay, " + weather.location.localtime);
                    tvTemperature.setText(weather.current.temp_c + "°");
                    tvWeatherStatus.setText(weather.current.condition.text);
                    tvWind.setText(weather.current.wind_kph + " km/h");
                    tvHumidity.setText(weather.current.humidity + "%");

                    // Load icon thời tiết
                    String iconUrl = "https:" + weather.current.condition.icon.replace("64x64", "128x128");
                    Glide.with(MainActivity.this)
                            .load(iconUrl)
                            .into(ivWeatherIcon);

                    updateBackground(weather.location.localtime);
                    updateRainEffect(weather.current.condition.text);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lấy dữ liệu thời tiết thất bại", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
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
                    tvWeatherStatus.setText(weather.current.condition.text);
                    tvWind.setText(weather.current.wind_kph + " km/h");
                    tvHumidity.setText(weather.current.humidity + "%");

                    // Load icon thời tiết rõ hơn
                    String iconUrl = "https:" + weather.current.condition.icon.replace("64x64", "128x128");
                    Glide.with(MainActivity.this)
                            .load(iconUrl)
                            .into(ivWeatherIcon);

                    // Cập nhật background theo thời gian
                    updateBackground(weather.location.localtime);

                    // Kiểm tra và hiển thị hiệu ứng mưa nếu cần
                    updateRainEffect(weather.current.condition.text);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void updateRainEffect(String weatherCondition) {
        // Kiểm tra nếu thời tiết có liên quan đến mưa
        boolean isRaining = weatherCondition.toLowerCase().contains("mưa") ||
                weatherCondition.toLowerCase().contains("rain");

        if (isRaining) {
            // Hiển thị hiệu ứng mưa
            rainContainer.setVisibility(View.VISIBLE);
            rainView.startRain();

            // Điều chỉnh cường độ mưa dựa vào mô tả thời tiết
            int intensity = 150; // Cường độ mặc định

            if (weatherCondition.toLowerCase().contains("nhẹ") ||
                    weatherCondition.toLowerCase().contains("light")) {
                intensity = 80;
            } else if (weatherCondition.toLowerCase().contains("to") ||
                    weatherCondition.toLowerCase().contains("heavy")) {
                intensity = 250;
            }

            rainView.setRainIntensity(intensity);
        } else {
            // Ẩn hiệu ứng mưa nếu không phải thời tiết mưa
            rainContainer.setVisibility(View.GONE);
            rainView.stopRain();
        }
    }

    private void updateBackground(String localtime) {
        // localtime format: "yyyy-MM-dd HH:mm"
        String[] parts = localtime.split(" ");
        if (parts.length == 2) {
            String timePart = parts[1];
            String[] timeSplit = timePart.split(":");
            int hour = Integer.parseInt(timeSplit[0]);

            if (hour >= 6 && hour < 18) {
                // Ban ngày - sử dụng animated background
                rootLayout.setBackgroundResource(R.drawable.animated_background_day);
                // Bắt đầu animation
                AnimationDrawable animationDrawable = (AnimationDrawable) rootLayout.getBackground();
                animationDrawable.setEnterFadeDuration(6000);
                animationDrawable.setExitFadeDuration(6000);
                animationDrawable.start();
            } else {
                // Ban đêm - tạo animation cho ban đêm
                rootLayout.setBackgroundResource(R.drawable.animated_background_night);
                // Bắt đầu animation
                AnimationDrawable animationDrawable = (AnimationDrawable) rootLayout.getBackground();
                animationDrawable.setEnterFadeDuration(6000);
                animationDrawable.setExitFadeDuration(6000);
                animationDrawable.start();
            }
        }
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
        overridePendingTransition(0, 0);
    }

    private void openForecastReport() {
        Intent intent = new Intent(MainActivity.this, ForecastReportActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra xem nếu hiệu ứng mưa đang hiển thị, tiếp tục hiệu ứng
        if (rainContainer.getVisibility() == View.VISIBLE) {
            rainView.startRain();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Tạm dừng hiệu ứng mưa để tiết kiệm tài nguyên
        rainView.stopRain();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đảm bảo dừng hiệu ứng khi activity bị hủy
        if (rainView != null) {
            rainView.stopRain();
        }
    }
}