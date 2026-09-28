package com.weather.mapper;

import com.weather.model.WeatherData;

/**
 * Hava durumu verisini yerel ikon dosyası adına eşleştirir.
 * Interface Segregation: Küçük, odaklı interface.
 */
public interface IWeatherIconMapper {

    /**
     * @param data Hava durumu verisi
     * @return Sınıf yoluna göre ikon dosya adı (örn. {@code "Icon=Sunny.png"})
     */
    String getIconFileName(WeatherData data);
}
