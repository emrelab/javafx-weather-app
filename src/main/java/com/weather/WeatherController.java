package com.weather;

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

/**
 * Hava durumu UI controller sınıfı.
 * Single Responsibility: Sadece UI güncellemeleri ve kullanıcı etkileşimleri.
 * Dependency Inversion: Somut sınıflara değil interface'lere bağlı.
 */
public class WeatherController {

    // API Key - güvenlik için environment variable'dan alınabilir
    private static final String API_KEY = "YOUR_API_KEY";

    // Bağımlılıklar - Interface'ler üzerinden (Dependency Inversion)
    private final IWeatherService weatherService;
    private final IWeatherIconMapper iconMapper;

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
     */
    public WeatherController() {
        this.weatherService = new OpenWeatherMapService(API_KEY);
        this.iconMapper = new WeatherIconMapper();
    }

    /**
     * Test için constructor - bağımlılık enjeksiyonu.
     * Liskov Substitution: Herhangi bir IWeatherService implementasyonu
     * kullanılabilir.
     */
    public WeatherController(IWeatherService weatherService, IWeatherIconMapper iconMapper) {
        this.weatherService = weatherService;
        this.iconMapper = iconMapper;
    }

    /**
     * FXML initialize metodu - uygulama başlatıldığında çağrılır.
     */
    public void initialize() {
        fetchWeatherData("Istanbul");
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
     * Hava durumu verilerini asenkron olarak çeker.
     */
    private void fetchWeatherData(String cityName) {
        showLoading(true);
        hideError();

        new Thread(() -> {
            try {
                WeatherData data = weatherService.getWeather(cityName);

                Platform.runLater(() -> {
                    updateWeatherUI(data);
                    showLoading(false);
                });

            } catch (WeatherServiceException e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    showLoading(false);
                });
            }
        }).start();
    }

    /**
     * UI bileşenlerini hava durumu verileriyle günceller.
     */
    private void updateWeatherUI(WeatherData data) {
        cityNameText.setText(data.getCityName());
        temperatureText.setText(String.format("%.0f°", data.getTemperature()));
        weatherDescriptionText.setText(capitalizeFirstLetter(data.getDescription()));
        humidityText.setText(String.format("Nem: %%%.0f", (double) data.getHumidity()));
        windText.setText(String.format("Rüzgar: %.1f km/s", data.getWindSpeed() * 3.6));
        feelsLikeText.setText(String.format("Hissedilen: %.0f°", data.getFeelsLike()));
        tempMinMaxText.setText(String.format("Max: %.0f° / Min: %.0f°", data.getTempMax(), data.getTempMin()));

        updateWeatherIcon(data.getMainWeather(), data.getDescription());
    }

    /**
     * Hava durumu ikonunu günceller.
     */
    private void updateWeatherIcon(String mainWeather, String description) {
        String iconFile = iconMapper.getIconFileName(mainWeather, description);

        try {
            Image image = new Image(getClass().getResourceAsStream(iconFile));
            weatherIcon.setImage(image);
        } catch (Exception e) {
            System.err.println("İkon yüklenemedi: " + iconFile);
        }
    }

    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            loadingText.setVisible(show);
            loadingText.setText(show ? "Yükleniyor..." : "");
        });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
    }

    private void hideError() {
        Platform.runLater(() -> errorText.setVisible(false));
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
