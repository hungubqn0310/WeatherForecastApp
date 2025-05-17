package com.example.weatherforecastapp.api;

import java.util.List;

public class WeatherResponse {
    public Location location;
    public Current current;
    public Forecast forecast;

    public static class Location {
        public String name;
        public String localtime;
    }

    public static class Current {
        public double temp_c;
        public Condition condition;
        public double wind_kph;
        public int humidity;
    }

    public static class Condition {
        public String text;
        public String icon;
        public int code;
    }

    public static class Forecast {
        public List<ForecastDay> forecastday;
    }

    public static class ForecastDay {
        public Day day;
    }

    public static class Day {
        public double maxtemp_c;
        public double mintemp_c;
    }

    public class Forecast {
        public List<ForecastDay> forecastday;
    }

    public class ForecastDay {
        public String date;
        public Day day;
        public List<Hour> hour; // Danh sách giờ
    }

    public class Day {
        public double maxtemp_c;
        public double mintemp_c;
        public double avgtemp_c;
        public Condition condition;
    }

    public class Hour {
        public String time; // Thời gian
        public double temp_c; // Nhiệt độ
        public Condition condition; // Điều kiện thời tiết

        // Thêm các thuộc tính khác nếu cần
        public double wind_kph; // Tốc độ gió
        public int humidity; // Độ ẩm
        public double feelslike_c; // Nhiệt độ cảm giác
    }
}