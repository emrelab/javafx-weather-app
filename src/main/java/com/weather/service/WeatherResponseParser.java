package com.weather.service;

import com.weather.model.WeatherData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * OpenWeatherMap JSON yanıtını {@link WeatherData} modeline çevirir.
 * Single Responsibility: Sadece JSON → model dönüşümü. Ağ erişimi içermez,
 * bu yüzden ağ olmadan test edilebilir.
 */
public class WeatherResponseParser implements IWeatherResponseParser {

    @Override
    public WeatherData parse(String json) throws WeatherServiceException {
        if (json == null || json.isBlank()) {
            throw new WeatherServiceException("Hava durumu servisinden boş yanıt geldi.");
        }

        try {
            JSONObject root = new JSONObject(json);

            JSONObject main = root.getJSONObject("main");
            JSONObject weather = root.getJSONArray("weather").getJSONObject(0);
            JSONObject wind = root.optJSONObject("wind");

            double temperature = main.getDouble("temp");

            WeatherData data = new WeatherData();
            data.setCityName(root.optString("name", ""));
            data.setTemperature(temperature);
            // hissedilen ve min/max her zaman gelmeyebilir; ana sıcaklığa düş.
            data.setFeelsLike(main.optDouble("feels_like", temperature));
            data.setTempMin(main.optDouble("temp_min", temperature));
            data.setTempMax(main.optDouble("temp_max", temperature));
            data.setHumidity(main.optInt("humidity", 0));
            data.setWindSpeed(wind != null ? wind.optDouble("speed", 0.0) : 0.0);
            data.setDescription(weather.optString("description", ""));
            data.setMainWeather(weather.optString("main", "").toLowerCase(Locale.ROOT));
            // İkon eşlemesi bu dilden bağımsız kod üzerinden yapılır (bkz. WeatherIconMapper).
            data.setIconCode(weather.optString("icon", ""));

            return data;

        } catch (JSONException e) {
            throw new WeatherServiceException("Hava durumu yanıtı çözümlenemedi.", e);
        }
    }
}
