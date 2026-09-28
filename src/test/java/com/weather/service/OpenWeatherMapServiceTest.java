package com.weather.service;

import com.weather.model.WeatherData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gerçek HTTP üzerinden test: {@link StubHttpServer} sahte API olarak kullanılır.
 * Böylece URL kurulumu, durum kodu → kullanıcı mesajı eşlemesi ve JSON çözümleme
 * birlikte doğrulanır; harici bağımlılık veya gerçek ağ erişimi gerekmez.
 */
class OpenWeatherMapServiceTest {

    private static final String OK_RESPONSE = """
            {
              "name": "Istanbul",
              "main": {"temp": 21.4, "feels_like": 20.8, "temp_min": 19.5, "temp_max": 23.1,
                       "humidity": 62},
              "weather": [{"main": "Clouds", "description": "parçalı bulutlu", "icon": "02d"}],
              "wind": {"speed": 4.1}
            }
            """;

    private static final String VALID_KEY = "test-api-key";
    private static final String PATH = "/data/2.5/weather";

    private StubHttpServer server;

    @AfterEach
    void stopServer() throws IOException {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    private IWeatherService serviceAgainst(StubHttpServer stub, String apiKey) {
        return new OpenWeatherMapService(apiKey, stub.baseUrl() + PATH);
    }

    private IWeatherService startAndService(String apiKey, int status, String body) throws IOException {
        server = new StubHttpServer(status, body);
        return serviceAgainst(server, apiKey);
    }

    @Test
    @DisplayName("200 yanıtı çözümlenip modele dönüşür")
    void basariliYanitCozumlenir() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 200, OK_RESPONSE);

        WeatherData data = service.getWeather("Istanbul");

        assertEquals("Istanbul", data.getCityName());
        assertEquals(21.4, data.getTemperature(), 0.001);
        assertEquals(62, data.getHumidity());
        assertEquals(4.1, data.getWindSpeed(), 0.001);
        assertEquals("parçalı bulutlu", data.getDescription());
        assertEquals("02d", data.getIconCode());
    }

    @Test
    @DisplayName("İstek metric + Türkçe dil parametreleriyle ve doğru şehirle kurulur")
    void istekParametreleriDogru() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 200, OK_RESPONSE);

        service.getWeather("Istanbul");

        String query = server.lastQuery();
        assertTrue(query.contains("q=Istanbul"), "query: " + query);
        assertTrue(query.contains("units=metric"), "query: " + query);
        assertTrue(query.contains("lang=tr"), "query: " + query);
        assertTrue(query.contains("appid=" + VALID_KEY), "query: " + query);
    }

    @Test
    @DisplayName("Türkçe karakterli şehir adı URL için kodlanır")
    void turkceKarakterKodlanir() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 200, OK_RESPONSE);

        service.getWeather("İstanbul");

        String query = server.lastQuery();
        assertTrue(query.contains("q=%C4%B0stanbul"), "query: " + query);
        assertFalse(query.contains("İ"), "ham karakter kodlanmamış: " + query);
    }

    @Test
    @DisplayName("404 şehir bulunamadı mesajı verir")
    void sehirBulunamadi() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 404,
                "{\"cod\":\"404\",\"message\":\"city not found\"}");

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Atlantis"));

        assertTrue(e.getMessage().contains("Şehir bulunamadı: Atlantis"), "mesaj: " + e.getMessage());
    }

    @Test
    @DisplayName("401 API anahtarı mesajı verir, anahtarın kendisi mesaja sızmaz")
    void gecersizApiAnahtari() throws Exception {
        String secretKey = "yanlis-anahtar-123";
        IWeatherService service = startAndService(secretKey, 401,
                "{\"cod\":401,\"message\":\"Invalid API key\"}");

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("API anahtarı geçersiz"), "mesaj: " + e.getMessage());
        assertFalse(e.getMessage().contains(secretKey), "anahtar sızdı: " + e.getMessage());
    }

    @Test
    @DisplayName("429 limit mesajı verir")
    void limitAsildi() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 429, "{}");

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("limit"), "mesaj: " + e.getMessage());
    }

    @Test
    @DisplayName("Beklenmeyen durum kodu ham gövde yerine okunabilir mesaj verir")
    void beklenmeyenDurumKodu() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 500, "Internal Server Error");

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("HTTP 500"), "mesaj: " + e.getMessage());
        assertFalse(e.getMessage().contains("Internal Server Error"), "ham gövde sızdı");
    }

    @Test
    @DisplayName("Sunucu erişilemezse bağlantı mesajı verir")
    void sunucuyaUlasilamaz() {
        // Dinlenmeyen bir port: bağlantı anında reddedilir.
        IWeatherService service = new OpenWeatherMapService(VALID_KEY, "http://127.0.0.1:1" + PATH);

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("ulaşılamadı"), "mesaj: " + e.getMessage());
    }

    @Test
    @DisplayName("Boş şehir adı ağ isteği yapılmadan reddedilir")
    void bosSehirReddedilir() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 200, OK_RESPONSE);

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("   "));

        assertTrue(e.getMessage().contains("şehir adı"), "mesaj: " + e.getMessage());
        assertEquals(0, server.requestCount(), "ağ isteği yapılmamalıydı");
    }

    @Test
    @DisplayName("API anahtarı tanımlı değilse net bir mesaj verilir")
    void apiAnahtariYoksaMesaj() throws Exception {
        IWeatherService service = startAndService(null, 200, OK_RESPONSE);

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("OWM_API_KEY"), "mesaj: " + e.getMessage());
        assertEquals(0, server.requestCount(), "ağ isteği yapılmamalıydı");
    }

    @Test
    @DisplayName("Boş yanıt gövdesi çözümleme hatası olarak raporlanır")
    void bosGovdeHataVerir() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 200, "");

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("boş yanıt"), "mesaj: " + e.getMessage());
    }

    @Test
    @DisplayName("Bozuk JSON gövdesi ham exception metni sızdırmaz")
    void bozukGovdeHataVerir() throws Exception {
        IWeatherService service = startAndService(VALID_KEY, 200, "<html>502 Bad Gateway</html>");

        WeatherServiceException e = assertThrows(WeatherServiceException.class,
                () -> service.getWeather("Istanbul"));

        assertTrue(e.getMessage().contains("çözümlenemedi"), "mesaj: " + e.getMessage());
    }
}
