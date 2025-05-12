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

public class ForecastReportActivity extends AppCompatActivity {

    ConstraintLayout rootLayoutForecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forecast_report);

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
}