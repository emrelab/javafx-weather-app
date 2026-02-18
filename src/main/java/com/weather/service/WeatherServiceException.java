package com.weather.service;

/**
 * Hava durumu servisi hata sınıfı.
 */
public class WeatherServiceException extends Exception {

    public WeatherServiceException(String message) {
        super(message);
    }

    public WeatherServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
