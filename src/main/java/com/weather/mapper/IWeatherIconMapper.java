package com.weather.mapper;

/**
 * Hava durumu ikon eşleştirme interface'i.
 * Interface Segregation: Küçük, odaklı interface.
 */
public interface IWeatherIconMapper {

    /**
     * Hava durumu bilgisine göre ikon dosya adını döndürür.
     * 
     * @param mainWeather Ana hava durumu (clear, clouds, rain, vb.)
     * @param description Detaylı açıklama
     * @return İkon dosya adı
     */
    String getIconFileName(String mainWeather, String description);
}
