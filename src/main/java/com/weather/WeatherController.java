package com.weather;

import com.weather.config.ApiKeyResolver;
import com.weather.mapper.IWeatherIconMapper;
import com.weather.mapper.WeatherIconMapper;
import com.weather.model.WeatherData;
import com.weather.service.IWeatherService;
import com.weather.service.OpenWeatherMapService;
import com.weather.service.WeatherServiceException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hava durumu UI controller sınıfı.
 * Single Responsibility: Sadece UI güncellemeleri ve kullanıcı etkileşimleri.
 * Dependency Inversion: Somut sınıflara değil interface'lere bağlı.
 */
public class WeatherController {

    /**
     * API anahtarının okunduğu ortam değişkeni.
     * Ortam değişkeni yoksa proje kökündeki {@code .env} dosyası da kullanılır;
     * ayrıntı için {@link ApiKeyResolver}.
     */
    public static final String API_KEY_ENV = ApiKeyResolver.ENV_VAR;

    private static final String DEFAULT_CITY = "Istanbul";
    private static final Locale TURKISH = Locale.forLanguageTag("tr");

    // Bağımlılıklar - Interface'ler üzerinden (Dependency Inversion)
    private final IWeatherService weatherService;
    private final IWeatherIconMapper iconMapper;

    /**
     * Tek iş parçacıklı havuz: istekler sırayla işlenir, böylece geç dönen eski bir
     * yanıt daha yeni bir aramanın sonucunu ezmez. Thread'ler daemon'dur, pencere
     * kapatıldığında JVM kendiliğinden sonlanır.
     */
    private final ExecutorService executor;

    // FXML UI bileşenleri
    @FXML
    private ImageView weatherIcon;
    @FXML
    private TextField cityInput;
    @FXML
    private Text temperatureText;
    @FXML
    private Text cityNameText;
    @FXML
    private Text weatherDescriptionText;
    @FXML
    private Text humidityText;
    @FXML
    private Text windText;
    @FXML
    private Text feelsLikeText;
    @FXML
    private Text tempMinMaxText;
    @FXML
    private Text loadingText;
    @FXML
    private Text errorText;

    /**
     * Varsayılan constructor - bağımlılıkları oluşturur.
     * API anahtarı önce {@value #API_KEY_ENV} ortam değişkeninden, yoksa proje
     * kökündeki {@code .env} dosyasından okunur. Böylece uygulamanın nasıl
     * başlatıldığından (terminal, IntelliJ, Dock) bağımsız çalışır.
     */
    public WeatherController() {
        this(new OpenWeatherMapService(new ApiKeyResolver().resolve()), new WeatherIconMapper());
    }

    /**
     * Test için constructor - bağımlılık enjeksiyonu.
     * Liskov Substitution: Herhangi bir IWeatherService implementasyonu
     * kullanılabilir.
     */
    public WeatherController(IWeatherService weatherService, IWeatherIconMapper iconMapper) {
        this.weatherService = weatherService;
        this.iconMapper = iconMapper;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "weather-fetch");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * FXML initialize metodu - uygulama başlatıldığında çağrılır.
     */
    public void initialize() {
        fetchWeatherData(DEFAULT_CITY);
    }

    /**
     * Arama butonu tıklandığında çağrılır.
     */
    @FXML
    private void handleSearch() {
        String city = cityInput.getText().trim();
        if (!city.isEmpty()) {
            fetchWeatherData(city);
        }
    }

    /**
     * Hava durumu verilerini arka planda çeker, sonucu FX thread'inde uygular.
     */
    private void fetchWeatherData(String cityName) {
        showLoading();

        executor.submit(() -> {
            try {
                WeatherData data = weatherService.getWeather(cityName);
                Platform.runLater(() -> {
                    updateWeatherUI(data);
                    hideLoading();
                });

            } catch (WeatherServiceException e) {
                // Servis katmanı mesajları zaten kullanıcıya gösterilebilir Türkçe
                // metinlerdir; ham exception metni veya hata kodu gösterilmez.
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    hideLoading();
                });
            }
        });
    }

    /**
     * UI bileşenlerini hava durumu verileriyle günceller. FX thread'inde çağrılmalıdır.
     */
    private void updateWeatherUI(WeatherData data) {
        cityNameText.setText(data.getCityName());
        temperatureText.setText(String.format("%.0f°", data.getTemperature()));
        weatherDescriptionText.setText(capitalizeFirstLetter(data.getDescription()));
        humidityText.setText(String.format("Nem: %%%d", data.getHumidity()));
        windText.setText(String.format("Rüzgar: %.1f km/s", data.getWindSpeed() * 3.6));
        feelsLikeText.setText(String.format("Hissedilen: %.0f°", data.getFeelsLike()));
        tempMinMaxText.setText(String.format("Max: %.0f° / Min: %.0f°",
                data.getTempMax(), data.getTempMin()));

        updateWeatherIcon(data);
    }

    /**
     * Hava durumu ikonunu günceller. Kaynak akışı try-with-resources ile kapatılır.
     */
    private void updateWeatherIcon(WeatherData data) {
        String iconFile = iconMapper.getIconFileName(data);

        try (InputStream stream = getClass().getResourceAsStream(iconFile)) {
            if (stream == null) {
                System.err.println("İkon bulunamadı: " + iconFile);
                return;
            }
            weatherIcon.setImage(new Image(stream));
        } catch (Exception e) {
            System.err.println("İkon yüklenemedi: " + iconFile + " (" + e.getMessage() + ")");
        }
    }

    /** FX thread'inde çağrılır. */
    private void showLoading() {
        loadingText.setText("Yükleniyor...");
        loadingText.setVisible(true);
        errorText.setVisible(false);
    }

    /** FX thread'inde çağrılır. */
    private void hideLoading() {
        loadingText.setText("");
        loadingText.setVisible(false);
    }

    /** FX thread'inde çağrılır. */
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Türkçe yerel ayarı açıkça verilir: aksi halde "i" -> "I"/"İ" dönüşümü
        // platformun varsayılan yerel ayarına göre değişir.
        return text.substring(0, 1).toUpperCase(TURKISH) + text.substring(1);
    }
}
