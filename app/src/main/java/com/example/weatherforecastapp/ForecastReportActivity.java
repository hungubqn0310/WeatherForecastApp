package com.example.weatherforecastapp;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForecastReportActivity extends AppCompatActivity {
    private static final String API_KEY = "da7aaf6a73cd4196a8121617251005";
    private static final String API_URL = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY + "&q=Hanoi&days=7&aqi=no&alerts=no";
    private static final String TAG = "WeatherApp";
    private TextView dateLabel;
    ConstraintLayout rootLayoutForecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forecast_report);
        String cityName = getIntent().getStringExtra("CITY_NAME");
        if (cityName != null) {
            // Sử dụng cityName để gọi API dự báo thời tiết cho thành phố này
            fetchForecast(cityName);
        }
        // Tìm TextView hiển thị ngày
        dateLabel = findViewById(R.id.date_label);

        // Gọi API để lấy dữ liệu thời tiết
        fetchWeatherData();
        // Gán layout gốc
        rootLayoutForecast = findViewById(R.id.rootLayoutForecast);

        // Xử lý padding hệ thống
        ViewCompat.setOnApplyWindowInsetsListener(rootLayoutForecast, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Gán nút back
        TextView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Lấy thời gian hiện tại và cập nhật nền
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String currentTime = String.format("%02d:%02d", hour, minute);
        String currentDate = String.format("%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        String localtime = currentDate + " " + currentTime;
        updateBackground(localtime);
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
                rootLayoutForecast.setBackgroundResource(R.drawable.animated_background_day);
                // Bắt đầu animation
                AnimationDrawable animationDrawable = (AnimationDrawable) rootLayoutForecast.getBackground();
                animationDrawable.setEnterFadeDuration(6000);
                animationDrawable.setExitFadeDuration(6000);
                animationDrawable.start();
            } else {
                // Ban đêm - tạo animation cho ban đêm
                rootLayoutForecast.setBackgroundResource(R.drawable.animated_background_night);
                // Bắt đầu animation
                AnimationDrawable animationDrawable = (AnimationDrawable) rootLayoutForecast.getBackground();
                animationDrawable.setEnterFadeDuration(6000);
                animationDrawable.setExitFadeDuration(6000);
                animationDrawable.start();
            }
        }
    }
    private void fetchForecast(String cityName) {
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY + "&q=" + cityName + "&days=7&aqi=no&alerts=no";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gọi API dự báo thời tiết: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ForecastReportActivity.this, "Không thể kết nối đến máy chủ thời tiết", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        // Phân tích dữ liệu JSON
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonArray forecastDays = jsonObject.getAsJsonObject("forecast").getAsJsonArray("forecastday");

                        List<DailyForecast> forecasts = new ArrayList<>();
                        List<HourlyForecast> hourlyForecasts = new ArrayList<>();

                        // Lấy dữ liệu cho 7 ngày
                        for (int i = 0; i < forecastDays.size(); i++) {
                            JsonObject dayForecast = forecastDays.get(i).getAsJsonObject();
                            String date = dayForecast.get("date").getAsString();
                            JsonObject dayInfo = dayForecast.getAsJsonObject("day");
                            double avgTemp = dayInfo.get("avgtemp_c").getAsDouble();
                            String iconUrl = "https:" + dayInfo.getAsJsonObject("condition").get("icon").getAsString();

                            // Định dạng lại ngày tháng (từ YYYY-MM-DD thành MMM DD)
                            String formattedDate = formatDate(date);

                            // Tạo đối tượng dự báo
                            DailyForecast forecast = new DailyForecast(formattedDate, iconUrl, String.format("%.1f°C", avgTemp));
                            forecasts.add(forecast);

                            // Nếu là ngày đầu tiên, lấy dữ liệu dự báo theo giờ
                            if (i == 0) {
                                JsonArray hoursArray = dayForecast.getAsJsonArray("hour");
                                for (int j = 0; j < hoursArray.size(); j++) {
                                    JsonObject hourData = hoursArray.get(j).getAsJsonObject();
                                    String time = hourData.get("time").getAsString();
                                    double tempC = hourData.get("temp_c").getAsDouble();
                                    String hourIconUrl = "https:" + hourData.getAsJsonObject("condition").get("icon").getAsString();

                                    // Chỉ lấy giờ từ chuỗi thời gian (định dạng: yyyy-MM-dd HH:mm)
                                    String hourOnly = time.substring(11, 16);

                                    HourlyForecast hourlyForecast = new HourlyForecast(
                                            hourOnly,
                                            hourIconUrl,
                                            String.format("%.0f°C", tempC)
                                    );
                                    hourlyForecasts.add(hourlyForecast);
                                }
                            }
                        }

                        // Cập nhật UI trên main thread
                        runOnUiThread(() -> {
                            updateUI(forecasts);
                            updateHourlyForecast(hourlyForecasts);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu JSON dự báo: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(ForecastReportActivity.this, "Lỗi khi xử lý dữ liệu dự báo thời tiết", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ForecastReportActivity.this, "Không thể lấy dữ liệu dự báo thời tiết", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void fetchWeatherData() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gọi API: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ForecastReportActivity.this, "Không thể kết nối đến máy chủ thời tiết", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        // Phân tích dữ liệu JSON
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonObject location = jsonObject.getAsJsonObject("location");
                        String currentDate = location.get("localtime").getAsString().split(" ")[0]; // Lấy phần ngày từ localtime

                        JsonArray forecastDays = jsonObject.getAsJsonObject("forecast").getAsJsonArray("forecastday");

                        List<DailyForecast> forecasts = new ArrayList<>();
                        List<HourlyForecast> hourlyForecasts = new ArrayList<>();

                        // Lấy dữ liệu cho 7 ngày
                        for (int i = 0; i < forecastDays.size(); i++) {
                            JsonObject dayForecast = forecastDays.get(i).getAsJsonObject();
                            String date = dayForecast.get("date").getAsString();
                            JsonObject dayInfo = dayForecast.getAsJsonObject("day");
                            double avgTemp = dayInfo.get("avgtemp_c").getAsDouble();
                            String iconUrl = "https:" + dayInfo.getAsJsonObject("condition").get("icon").getAsString();

                            // Định dạng lại ngày tháng (từ YYYY-MM-DD thành MMM DD)
                            String formattedDate = formatDate(date);

                            // Tạo đối tượng dự báo
                            DailyForecast forecast = new DailyForecast(formattedDate, iconUrl, String.format("%.1f°C", avgTemp));
                            forecasts.add(forecast);

                            // Nếu là ngày đầu tiên, lấy dữ liệu dự báo theo giờ
                            if (i == 0) {
                                JsonArray hoursArray = dayForecast.getAsJsonArray("hour");
                                for (int j = 0; j < hoursArray.size(); j++) {
                                    JsonObject hourData = hoursArray.get(j).getAsJsonObject();
                                    String time = hourData.get("time").getAsString();
                                    double tempC = hourData.get("temp_c").getAsDouble();
                                    String hourIconUrl = "https:" + hourData.getAsJsonObject("condition").get("icon").getAsString();

                                    // Chỉ lấy giờ từ chuỗi thời gian (định dạng: yyyy-MM-dd HH:mm)
                                    String hourOnly = time.substring(11, 16);

                                    HourlyForecast hourlyForecast = new HourlyForecast(
                                            hourOnly,
                                            hourIconUrl,
                                            String.format("%.0f°C", tempC)
                                    );
                                    hourlyForecasts.add(hourlyForecast);
                                }
                            }
                        }

                        // Định dạng ngày hiện tại (từ YYYY-MM-DD thành MMM DD, YYYY)
                        final String formattedCurrentDate = formatCurrentDate(currentDate);

                        // Cập nhật UI trên main thread
                        runOnUiThread(() -> {
                            // Cập nhật ngày hiện tại
                            dateLabel.setText(formattedCurrentDate);

                            updateUI(forecasts);
                            updateHourlyForecast(hourlyForecasts);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu JSON: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(ForecastReportActivity.this, "Lỗi khi xử lý dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ForecastReportActivity.this, "Không thể lấy dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Phương thức để định dạng ngày hiện tại
    private String formatCurrentDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi định dạng ngày hiện tại: " + e.getMessage());
            return inputDate; // Trả về ngày gốc nếu có lỗi
        }
    }

    // Phương thức để cập nhật dự báo theo giờ
    private void updateHourlyForecast(List<HourlyForecast> hourlyForecasts) {
        // Giới hạn số lượng dự báo theo giờ hiển thị (tối đa 24 giờ)
        int maxHours = Math.min(hourlyForecasts.size(), 24);

        for (int i = 0; i < maxHours; i++) {
            // Tìm LinearLayout tương ứng với giờ hiện tại
            int hourlyItemId = getResources().getIdentifier("hourly_forecast_item" + (i + 1), "id", getPackageName());
            LinearLayout hourlyItem = findViewById(hourlyItemId);

            if (hourlyItem != null) {
                HourlyForecast forecast = hourlyForecasts.get(i);

                // Lấy các view con trong LinearLayout
                TextView tempTextView = (TextView) hourlyItem.getChildAt(0);
                ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
                TextView timeTextView = (TextView) hourlyItem.getChildAt(2);

                // Cập nhật dữ liệu
                tempTextView.setText(forecast.getTemperature());
                timeTextView.setText(forecast.getTime());

                // Tải hình ảnh biểu tượng thời tiết
                Glide.with(this)
                        .load(forecast.getIconUrl())
                        .into(iconImageView);
            }
        }
    }

    // Phương thức để cập nhật UI với dữ liệu thời tiết hàng ngày
    private void updateUI(List<DailyForecast> forecasts) {
        for (int i = 0; i < forecasts.size(); i++) {
            // Lấy container cho ngày tương ứng
            int containerId = getResources().getIdentifier("day" + (i + 1) + "_container", "id", getPackageName());
            LinearLayout container = findViewById(containerId);

            if (container != null) {
                // Lấy các view con trong container
                TextView dateTextView = (TextView) container.getChildAt(0);
                ImageView iconImageView = (ImageView) container.getChildAt(1);
                TextView tempTextView = (TextView) container.getChildAt(2);

                // Cập nhật dữ liệu
                DailyForecast forecast = forecasts.get(i);
                dateTextView.setText(forecast.getDate());
                tempTextView.setText(forecast.getTemperature());

                // Sử dụng Glide để tải hình ảnh
                Glide.with(this)
                        .load(forecast.getConditionIconUrl())
                        .into(iconImageView);
            }
        }
    }

    // Phương thức để định dạng lại ngày tháng
    private String formatDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi định dạng ngày tháng: " + e.getMessage());
            return inputDate; // Trả về ngày gốc nếu có lỗi
        }
    }

    // Lớp để lưu trữ dữ liệu dự báo thời tiết theo ngày
    private static class DailyForecast {
        private final String date;
        private final String conditionIconUrl;
        private final String temperature;

        public DailyForecast(String date, String conditionIconUrl, String temperature) {
            this.date = date;
            this.conditionIconUrl = conditionIconUrl;
            this.temperature = temperature;
        }

        public String getDate() {
            return date;
        }

        public String getConditionIconUrl() {
            return conditionIconUrl;
        }

        public String getTemperature() {
            return temperature;
        }
    }

    // Lớp để lưu trữ dữ liệu dự báo thời tiết theo giờ
    private static class HourlyForecast {
        private final String time;
        private final String iconUrl;
        private final String temperature;

        public HourlyForecast(String time, String iconUrl, String temperature) {
            this.time = time;
            this.iconUrl = iconUrl;
            this.temperature = temperature;
        }

        public String getTime() {
            return time;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public String getTemperature() {
            return temperature;
        }
    }

}