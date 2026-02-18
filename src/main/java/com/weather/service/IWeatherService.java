package com.weather.service;

import com.weather.model.WeatherData;

/**
 * Hava durumu servisi için interface.
 * Dependency Inversion: Controller somut sınıfa değil bu interface'e bağlı.
 * Open/Closed: Yeni API servisleri bu interface'i implement ederek eklenebilir.
 */
public interface IWeatherService {

    /**
     * Belirtilen şehir için hava durumu verilerini getirir.
     * 
     * @param cityName Şehir adı
     * @return WeatherData nesnesi
     * @throws WeatherServiceException Hata durumunda
     */
    WeatherData getWeather(String cityName) throws WeatherServiceException;
}
