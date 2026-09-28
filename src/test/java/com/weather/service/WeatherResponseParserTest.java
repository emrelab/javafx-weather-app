package com.weather.service;

import com.weather.model.WeatherData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherResponseParserTest {

    private final IWeatherResponseParser parser = new WeatherResponseParser();

    private static final String FULL_RESPONSE = """
            {
              "name": "Istanbul",
              "main": {
                "temp": 21.4,
                "feels_like": 20.8,
                "temp_min": 19.5,
                "temp_max": 23.1,
                "humidity": 62
              },
              "weather": [
                {"main": "Clouds", "description": "parçalı bulutlu", "icon": "02d"}
              ],
              "wind": {"speed": 4.1}
            }
            """;

    @Test
    @DisplayName("Tam yanıt tüm alanlarıyla modele dönüşür")
    void tamYanitCozumlenir() throws WeatherServiceException {
        WeatherData data = parser.parse(FULL_RESPONSE);

        assertEquals("Istanbul", data.getCityName());
        assertEquals(21.4, data.getTemperature(), 0.001);
        assertEquals(20.8, data.getFeelsLike(), 0.001);
        assertEquals(19.5, data.getTempMin(), 0.001);
        assertEquals(23.1, data.getTempMax(), 0.001);
        assertEquals(62, data.getHumidity());
        assertEquals(4.1, data.getWindSpeed(), 0.001);
        assertEquals("parçalı bulutlu", data.getDescription());
        assertEquals("02d", data.getIconCode());
    }

    @Test
    @DisplayName("main alanı küçük harfe çevrilir (ikon yedeği için)")
    void mainAlaniKucukHarfeCevrilir() throws WeatherServiceException {
        assertEquals("clouds", parser.parse(FULL_RESPONSE).getMainWeather());
    }

    @Test
    @DisplayName("Eksik alanlar güvenli varsayılanlarla doldurulur, exception fırlatılmaz")
    void eksikAlanlarVarsayilanaDuser() throws WeatherServiceException {
        String minimal = """
                {
                  "name": "Ankara",
                  "main": {"temp": 30.0},
                  "weather": [{"main": "Clear"}]
                }
                """;

        WeatherData data = parser.parse(minimal);

        assertEquals("Ankara", data.getCityName());
        assertEquals(30.0, data.getTemperature(), 0.001);
        // feels_like / temp_min / temp_max yoksa ana sıcaklığa düşülür
        assertEquals(30.0, data.getFeelsLike(), 0.001);
        assertEquals(30.0, data.getTempMin(), 0.001);
        assertEquals(30.0, data.getTempMax(), 0.001);
        assertEquals(0, data.getHumidity());
        assertEquals(0.0, data.getWindSpeed(), 0.001);
        assertEquals("", data.getDescription());
        assertEquals("", data.getIconCode());
    }

    @Test
    @DisplayName("wind nesnesi hiç yoksa rüzgar 0 olur")
    void windYoksaSifir() throws WeatherServiceException {
        String noWind = """
                {"name": "Izmir", "main": {"temp": 25.0}, "weather": [{"main": "Clear", "icon": "01d"}]}
                """;
        assertEquals(0.0, parser.parse(noWind).getWindSpeed(), 0.001);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    @DisplayName("Boş veya null gövde anlamlı bir hata verir")
    void bosGovdeHataVerir(String json) {
        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> parser.parse(json));
        assertTrue(e.getMessage().contains("boş yanıt"), "mesaj: " + e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{",                                     // bozuk JSON
            "not json at all",                       // JSON değil
            "[]",                                    // dizi, nesne bekleniyordu
    })
    @DisplayName("Bozuk JSON ham exception metni sızdırmadan raporlanır")
    void bozukJsonHataVerir(String json) {
        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> parser.parse(json));
        assertTrue(e.getMessage().contains("çözümlenemedi"), "mesaj: " + e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\": \"X\"}",                                     // main ve weather yok
            "{\"main\": {\"temp\": 1.0}}",                           // weather yok
            "{\"main\": {\"temp\": 1.0}, \"weather\": []}",           // weather boş dizi
            "{\"name\": \"X\", \"weather\": [{\"main\": \"Clear\"}]}", // main yok
            "{\"main\": {}, \"weather\": [{\"main\": \"Clear\"}]}",   // temp yok
    })
    @DisplayName("Zorunlu alanlar eksikse anlamlı hata verilir")
    void zorunluAlanlarEksikseHataVerir(String json) {
        assertThrows(WeatherServiceException.class, () -> parser.parse(json));
    }

    @Test
    @DisplayName("Hata nedeni korunur (root cause kaybolmaz)")
    void hataNedeniKorunur() {
        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> parser.parse("{"));
        assertTrue(e.getCause() instanceof org.json.JSONException,
                "beklenen neden JSONException, gelen: " + e.getCause());
    }
}
