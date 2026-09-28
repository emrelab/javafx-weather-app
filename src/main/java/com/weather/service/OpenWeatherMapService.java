package com.weather.service;

import com.weather.model.WeatherData;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenWeatherMap API implementasyonu.
 *
 * <p>Single Responsibility: Sadece HTTP taşıma ve durum kodu → kullanıcı mesajı
 * eşlemesi. JSON çözümlemesi {@link IWeatherResponseParser}'a devredilmiştir.
 *
 * <p>Open/Closed: Yeni bir sağlayıcı {@link IWeatherService} implement edilerek
 * eklenir; bu sınıf değişmez.
 *
 * <p>Fırlatılan {@link WeatherServiceException} mesajları doğrudan kullanıcıya
 * gösterilebilir Türkçe metinlerdir; ham exception metni sızdırılmaz.
 */
public class OpenWeatherMapService implements IWeatherService {

    private static final String DEFAULT_API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String apiKey;
    private final String apiUrl;
    private final IWeatherResponseParser parser;
    private final HttpClient httpClient;

    public OpenWeatherMapService(String apiKey) {
        this(apiKey, DEFAULT_API_URL, new WeatherResponseParser());
    }

    public OpenWeatherMapService(String apiKey, String apiUrl) {
        this(apiKey, apiUrl, new WeatherResponseParser());
    }

    public OpenWeatherMapService(String apiKey, String apiUrl, IWeatherResponseParser parser) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.parser = parser;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public WeatherData getWeather(String cityName) throws WeatherServiceException {
        if (cityName == null || cityName.isBlank()) {
            throw new WeatherServiceException("Lütfen bir şehir adı girin.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new WeatherServiceException(
                    "API anahtarı tanımlı değil. OWM_API_KEY ortam değişkenini ayarlayın "
                            + "veya proje köküne .env dosyası ekleyin (.env.example şablonuna bakın).");
        }

        HttpRequest request = HttpRequest.newBuilder(buildUri(cityName))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new WeatherServiceException(toUserMessage(response.statusCode(), cityName));
            }

            return parser.parse(response.body());

        } catch (WeatherServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new WeatherServiceException(
                    "Hava durumu servisine ulaşılamadı. İnternet bağlantınızı kontrol edin.", e);
        } catch (InterruptedException e) {
            // Interrupt durumunu koru; yutmak thread pool'u bozar.
            Thread.currentThread().interrupt();
            throw new WeatherServiceException("İstek iptal edildi.", e);
        }
    }

    private URI buildUri(String cityName) {
        return URI.create(apiUrl
                + "?q=" + encode(cityName)
                + "&appid=" + encode(apiKey)
                + "&units=metric&lang=tr");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String toUserMessage(int statusCode, String cityName) {
        return switch (statusCode) {
            case 400 -> "Geçersiz istek. Şehir adını kontrol edin.";
            case 401 -> "API anahtarı geçersiz. OWM_API_KEY değerini kontrol edin.";
            case 404 -> "Şehir bulunamadı: " + cityName;
            case 429 -> "API istek limiti aşıldı. Lütfen biraz bekleyip tekrar deneyin.";
            default -> "Hava durumu servisi şu anda yanıt vermiyor (HTTP " + statusCode + ").";
        };
    }
}
