package com.example.weatherforecastapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.AnimationDrawable;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.bumptech.glide.Glide;
import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.content.pm.PackageManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FavoriteLocationsPrefs";
    private static final String KEY_FAVORITE_CITIES = "favoriteCities";
    private static final String PREFS_APP = "WeatherAppPrefs";
    TextView tvCity, tvDate, tvTemperature, tvWeatherStatus, tvWind, tvHumidity;
    ImageView ivNotification, ivWeatherIcon;
    ImageView ivLove;
    View notificationBadge;
    Button btnForecast;
    FrameLayout notificationContainer;
    ConstraintLayout rootLayout;
    FrameLayout rainContainer;
    RainView rainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tvSlide = findViewById(R.id.tvSlide);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.slide_right_to_left);
        tvSlide.startAnimation(pulse);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Button btnMyLocation = findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getLastLocationAndFetchWeather();
            } else {
                requestLocationPermission();
            }
        });

        // Khởi tạo ivLove
        ivLove = findViewById(R.id.ivLove);
        final Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_animation);

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

        // Kiểm tra Intent từ FavoriteLocationsActivity
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("USE_CURRENT_LOCATION", false)) {
                // Sử dụng vị trí hiện tại
                if (checkLocationPermission()) {
                    getLastLocationAndFetchWeather();
                } else {
                    requestLocationPermission();
                }
            } else if (intent.hasExtra("SELECTED_CITY")) {
                // Lấy tên thành phố từ Intent
                String selectedCity = intent.getStringExtra("SELECTED_CITY");
                tvCity.setText(selectedCity);
                fetchWeather(selectedCity);
            } else {
                // Mặc định: sử dụng thành phố từ Intent trước đó hoặc fallback
                String city = intent.getStringExtra("CITY_NAME");
                if (city == null || city.isEmpty()) {
                    city = "Hanoi"; // fallback mặc định
                }
                tvCity.setText(city);
                fetchWeather(city);
            }
        } else {
            // Mặc định: sử dụng thành phố Hanoi
            String city = "Hanoi";
            tvCity.setText(city);
            fetchWeather(city);
        }

        // Kiểm tra trạng thái yêu thích ban đầu
        Set<String> favoriteCities = sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>());
        ivLove.setSelected(favoriteCities.contains(tvCity.getText().toString()));

        // Xử lý nhấp vào icon love
        ivLove.setOnClickListener(v -> {
            v.startAnimation(scaleAnimation);
            Set<String> updatedFavorites = new HashSet<>(sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>()));
            String currentCity = tvCity.getText().toString();
            if (ivLove.isSelected()) {
                updatedFavorites.remove(currentCity);
                Toast.makeText(this, "Đã gỡ " + currentCity + " khỏi địa điểm yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                updatedFavorites.add(currentCity);
                Toast.makeText(this, "Đã thêm " + currentCity + " vào địa điểm yêu thích", Toast.LENGTH_SHORT).show();
            }
            ivLove.setSelected(!ivLove.isSelected());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_FAVORITE_CITIES, updatedFavorites);
            editor.apply();
        });

        // Click để mở bản đồ chọn vị trí
        tvCity.setOnClickListener(v -> openLocationPicker());

        // Click thông báo
        notificationContainer.setOnClickListener(v -> {
            showNotificationPopup();
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
        rainView = new RainView(this);
        rainContainer.addView(rainView);
        rainContainer.setVisibility(View.GONE);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndFetchWeather();
            } else {
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối, sử dụng Hà Nội", Toast.LENGTH_SHORT).show();
                tvCity.setText("Hanoi");
                fetchWeather("Hanoi");
            }
        }
    }

    private void getLastLocationAndFetchWeather() {
        if (!checkLocationPermission()) {
            Log.d("MainActivity", "Location permissions not granted");
            return;
        }

        Log.d("MainActivity", "Attempting to get last location");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Log.d("MainActivity", "Location received: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                fetchWeatherByCoordinates(latitude, longitude);
            } else {
                Log.d("MainActivity", "Location is null");
                Toast.makeText(this, "Không thể lấy vị trí hiện tại, sử dụng Hà Nội", Toast.LENGTH_SHORT).show();
                tvCity.setText("Hanoi");
                fetchWeather("Hanoi");
            }
        }).addOnFailureListener(e -> {
            Log.e("MainActivity", "Failed to get location: " + e.getMessage());
            Toast.makeText(this, "Lấy vị trí thất bại, sử dụng Hà Nội: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            tvCity.setText("Hanoi");
            fetchWeather("Hanoi");
        });
    }

    private void fetchWeatherByCoordinates(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        String query = latitude + "," + longitude;

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

                    String iconUrl = "https:" + weather.current.condition.icon.replace("64x64", "128x128");
                    Glide.with(MainActivity.this)
                            .load(iconUrl)
                            .into(ivWeatherIcon);

                    // Cập nhật trạng thái yêu thích
                    Set<String> favoriteCities = sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>());
                    ivLove.setSelected(favoriteCities.contains(weather.location.name));

                    updateBackground(weather.location.localtime);
                    updateRainEffect(weather.current.condition.text);
                } else {
                    Toast.makeText(MainActivity.this, "Lấy dữ liệu thời tiết thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lấy dữ liệu thời tiết thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

                    String iconUrl = "https:" + weather.current.condition.icon.replace("64x64", "128x128");
                    Glide.with(MainActivity.this)
                            .load(iconUrl)
                            .into(ivWeatherIcon);

                    // Cập nhật trạng thái yêu thích
                    Set<String> favoriteCities = sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>());
                    ivLove.setSelected(favoriteCities.contains(weather.location.name));

                    updateBackground(weather.location.localtime);
                    updateRainEffect(weather.current.condition.text);
                } else {
                    Toast.makeText(MainActivity.this, "Lấy dữ liệu thời tiết thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lấy dữ liệu thời tiết thất bại: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void updateRainEffect(String weatherCondition) {
        boolean isRaining = weatherCondition.toLowerCase().contains("mưa") ||
                weatherCondition.toLowerCase().contains("rain");

        if (isRaining) {
            rainContainer.setVisibility(View.VISIBLE);
            rainView.startRain();
            int intensity = 150;
            if (weatherCondition.toLowerCase().contains("nhẹ") ||
                    weatherCondition.toLowerCase().contains("light")) {
                intensity = 80;
            } else if (weatherCondition.toLowerCase().contains("to") ||
                    weatherCondition.toLowerCase().contains("heavy")) {
                intensity = 250;
            }
            rainView.setRainIntensity(intensity);
        } else {
            rainContainer.setVisibility(View.GONE);
            rainView.stopRain();
        }
    }

    private void updateBackground(String localtime) {
        String[] parts = localtime.split(" ");
        if (parts.length == 2) {
            String timePart = parts[1];
            String[] timeSplit = timePart.split(":");
            int hour = Integer.parseInt(timeSplit[0]);

            SharedPreferences appPrefs = getSharedPreferences(PREFS_APP, MODE_PRIVATE);
            SharedPreferences.Editor editor = appPrefs.edit();
            AnimationDrawable animationDrawable;

            if (hour >= 6 && hour < 18) {
                rootLayout.setBackgroundResource(R.drawable.animated_background_day);
                editor.putBoolean("isDayBackground", true);
            } else {
                rootLayout.setBackgroundResource(R.drawable.animated_background_night);
                editor.putBoolean("isDayBackground", false);
            }
            editor.apply();

            animationDrawable = (AnimationDrawable) rootLayout.getBackground();
            animationDrawable.setEnterFadeDuration(6000);
            animationDrawable.setExitFadeDuration(6000);
            animationDrawable.start();
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
        String currentCity = tvCity.getText().toString();
        Intent intent = new Intent(MainActivity.this, ForecastReportActivity.class);
        intent.putExtra("CITY_NAME", currentCity);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rainContainer.getVisibility() == View.VISIBLE) {
            rainView.startRain();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        rainView.stopRain();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rainView != null) {
            rainView.stopRain();
        }
    }
}