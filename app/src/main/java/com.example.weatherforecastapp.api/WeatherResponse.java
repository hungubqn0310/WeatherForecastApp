package com.example.weatherforecastapp.api;

public class WeatherResponse {
    public Location location;
    public Current current;

    public class Location {
        public String name;
        public String localtime;
    }

    public class Current {
        public double temp_c;
        public Condition condition;
        public double wind_kph;
        public int humidity;
    }

    public class Condition {
        public String text;
        public String icon;
    }
}
