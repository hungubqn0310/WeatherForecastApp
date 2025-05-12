package com.example.weatherforecastapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ForecastReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kích hoạt chế độ Edge to Edge
        EdgeToEdge.enable(this);

        // Đặt layout cho Activity
        setContentView(R.layout.activity_forecast_report);

        // Xử lý các insets hệ thống và thay đổi padding của view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy tham chiếu đến nút Back
        TextView backButton = findViewById(R.id.back_button);

        // Đặt sự kiện click cho nút Back
        backButton.setOnClickListener(v -> {

            finish();  // Đóng ForecastReportActivity
        });
    }
}
