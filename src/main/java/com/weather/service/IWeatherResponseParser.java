package com.weather.service;

import com.weather.model.WeatherData;

/**
 * Ham API yanıtını domain modeline dönüştürür.
 * Interface Segregation: HTTP taşıma katmanından bağımsız, tek sorumluluk.
 */
public interface IWeatherResponseParser {

    /**
     * @param json Hava durumu API'sinden gelen ham JSON gövdesi
     * @return Çözümlenmiş {@link WeatherData}
     * @throws WeatherServiceException Yanıt boş, bozuk veya beklenen alanları içermiyorsa
     */
    WeatherData parse(String json) throws WeatherServiceException;
}
