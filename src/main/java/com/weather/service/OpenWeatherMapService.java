package com.weather.service;

import com.weather.model.WeatherData;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * OpenWeatherMap API implementasyonu.
 * Single Responsibility: Sadece API çağrısı ve JSON parsing.
 * Open/Closed: IWeatherService interface'ini implement eder.
 */
public class OpenWeatherMapService implements IWeatherService {

    private final String apiKey;
    private final String apiUrl;

    public OpenWeatherMapService(String apiKey) {
        this.apiKey = apiKey;
        this.apiUrl = "https://api.openweathermap.org/data/2.5/weather";
    }

    public OpenWeatherMapService(String apiKey, String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @Override
    public WeatherData getWeather(String cityName) throws WeatherServiceException {
        try {
            String urlString = String.format("%s?q=%s&appid=%s&units=metric&lang=tr",
                    apiUrl, cityName, apiKey);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                throw new WeatherServiceException("Şehir bulunamadı: " + cityName);
            }

            StringBuilder response = new StringBuilder();
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            return parseWeatherResponse(response.toString());

        } catch (WeatherServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherServiceException("API hatası: " + e.getMessage(), e);
        }
    }

    private WeatherData parseWeatherResponse(String jsonResponse) {
        JSONObject json = new JSONObject(jsonResponse);

        JSONObject main = json.getJSONObject("main");
        JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
        JSONObject wind = json.getJSONObject("wind");

        WeatherData data = new WeatherData();
        data.setCityName(json.getString("name"));
        data.setTemperature(main.getDouble("temp"));
        data.setFeelsLike(main.getDouble("feels_like"));
        data.setTempMin(main.getDouble("temp_min"));
        data.setTempMax(main.getDouble("temp_max"));
        data.setHumidity(main.getInt("humidity"));
        data.setWindSpeed(wind.getDouble("speed"));
        data.setDescription(weather.getString("description"));
        data.setMainWeather(weather.getString("main").toLowerCase());

        return data;
    }
}
