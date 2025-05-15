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
}
