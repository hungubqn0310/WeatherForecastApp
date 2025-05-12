package com.example.weatherforecastapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Nạp bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Không thể tải bản đồ", Toast.LENGTH_SHORT).show();
        }

        // Nút quay lại
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // Xử lý nhập tên thành phố
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String city = etSearch.getText().toString().trim();
                if (!city.isEmpty()) {
                    saveRecentSearch(city); // ✅ lưu recent
                    Intent intent = new Intent(LocationPickerActivity.this, MainActivity.class);
                    intent.putExtra("CITY_NAME", city);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Vui lòng nhập tên thành phố", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // ✅ Hiển thị recent search khi mở activity
        showRecentSearches();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Mặc định về Hà Nội
        LatLng hanoi = new LatLng(21.0285, 105.8542);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hanoi, 10));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Vị trí đã chọn"));

            Geocoder geocoder = new Geocoder(LocationPickerActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                String cityName = null;
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    cityName = address.getLocality();
                    if (cityName == null || cityName.isEmpty()) cityName = address.getSubAdminArea();
                    if (cityName == null || cityName.isEmpty()) cityName = address.getAdminArea();
                }

                String latlon = latLng.latitude + "," + latLng.longitude;

                if (cityName != null && !cityName.isEmpty()) {
                    Toast.makeText(this, "Đã chọn: " + cityName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đã chọn tọa độ: " + latlon, Toast.LENGTH_SHORT).show();
                }

                saveRecentSearch(cityName != null ? cityName : latlon); // ✅ lưu recent theo tên nếu có

                Intent intent = new Intent(LocationPickerActivity.this, MainActivity.class);
                intent.putExtra("CITY_NAME", latlon); // 🔥 gọi API bằng lat,lon
                startActivity(intent);
                finish();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lấy địa chỉ", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void saveRecentSearch(String cityName) {
        SharedPreferences prefs = getSharedPreferences("recent_searches", MODE_PRIVATE);
        Set<String> recent = new LinkedHashSet<>(prefs.getStringSet("cities", new LinkedHashSet<>()));
        recent.remove(cityName); // tránh trùng
        recent.add(cityName);
        if (recent.size() > 5) {
            String first = recent.iterator().next();
            recent.remove(first);
        }
        prefs.edit().putStringSet("cities", recent).apply();
    }

    private List<String> getRecentSearches() {
        SharedPreferences prefs = getSharedPreferences("recent_searches", MODE_PRIVATE);
        return new ArrayList<>(prefs.getStringSet("cities", new LinkedHashSet<>()));
    }

    private void showRecentSearches() {
        LinearLayout recentContainer = findViewById(R.id.recentContainer);
        recentContainer.removeAllViews();

        List<String> recentList = getRecentSearches();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String city : recentList) {
            View itemView = inflater.inflate(R.layout.item_recent_search, recentContainer, false);
            TextView tvCity = itemView.findViewById(R.id.tvCity);
            tvCity.setText(city);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(LocationPickerActivity.this, MainActivity.class);
                intent.putExtra("CITY_NAME", city);
                startActivity(intent);
                finish();
            });

            recentContainer.addView(itemView);
        }
    }
}
