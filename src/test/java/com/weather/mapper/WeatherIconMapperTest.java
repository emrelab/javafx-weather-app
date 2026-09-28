package com.weather.mapper;

import com.weather.model.WeatherData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherIconMapperTest {

    private final IWeatherIconMapper mapper = new WeatherIconMapper();

    @ParameterizedTest(name = "icon kodu {0} -> {1}")
    @CsvSource({
            "01d, Icon=Sunny.png",
            "01n, Icon=Sunny.png",
            "02d, Icon=PartlyCloudy.png",
            "02n, Icon=PartlyCloudy.png",
            "03d, Icon=Cloudy.png",
            "03n, Icon=Cloudy.png",
            "04d, Icon=Cloudy.png",
            "09d, Icon=RainywithSun.png",
            "10d, Icon=Rainy.png",
            "10n, Icon=Rainy.png",
            "11d, Icon=Thunderstorm.png",
            "13d, Icon=Snow.png",
            "50d, Icon=Cloudy.png",
    })
    @DisplayName("Ortamdan bağımsız icon kodu doğru ikona eşleşir")
    void iconKodunuIkonaEslestirir(String iconCode, String expected) {
        assertEquals(expected, mapper.getIconFileName(dataWith(iconCode, null)));
    }

    @ParameterizedTest(name = "main={0} -> {1}")
    @CsvSource({
            "clear,        Icon=Sunny.png",
            "clouds,       Icon=Cloudy.png",
            "rain,         Icon=Rainy.png",
            "drizzle,      Icon=LightDrizzle.png",
            "thunderstorm, Icon=Thunderstorm.png",
            "snow,         Icon=Snow.png",
            "mist,         Icon=Cloudy.png",
            "fog,          Icon=Cloudy.png",
            "haze,         Icon=Cloudy.png",
            "smoke,        Icon=Cloudy.png",
            "squall,       Icon=Thunderstorm.png",
            "tornado,      Icon=Thunderstorm.png",
    })
    @DisplayName("icon kodu yoksa main alanına düşer")
    void kodYoksaMainAlaninaDuser(String mainWeather, String expected) {
        assertEquals(expected, mapper.getIconFileName(dataWith(null, mainWeather)));
    }

    @Test
    @DisplayName("main alanı büyük/küçük harf duyarsız eşleşir")
    void mainAlaniHarfDuyarsiz() {
        assertEquals("Icon=Cloudy.png", mapper.getIconFileName(dataWith(null, "Clouds")));
        assertEquals("Icon=Snow.png", mapper.getIconFileName(dataWith(null, "SNOW")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "x", "99", "00d"})
    @DisplayName("Bilinmeyen veya eksik kod varsayılan ikona düşer")
    void bilinmeyenKodVarsayilanaDuser(String iconCode) {
        WeatherData data = dataWith(iconCode, null);
        assertEquals("Icon=Sunny.png", mapper.getIconFileName(data));
    }

    @Test
    @DisplayName("Kod ve main ikisi de bilinmiyorsa varsayılan ikon döner")
    void ikisiDeBilinmiyorsaVarsayilan() {
        assertEquals("Icon=Sunny.png", mapper.getIconFileName(dataWith("99x", "volcanic-ash-cloud")));
        assertEquals("Icon=Sunny.png", mapper.getIconFileName(dataWith(null, null)));
    }

    @Test
    @DisplayName("null veri NPE fırlatmaz")
    void nullVeriGuvenli() {
        assertEquals("Icon=Sunny.png", mapper.getIconFileName(null));
    }

    @Test
    @DisplayName("icon kodu main alanına göre önceliklidir")
    void kodMaindenOnceliklidir() {
        // API "Rain" diyor ama kod "13" (snow) — kod kazanır.
        assertEquals("Icon=Snow.png", mapper.getIconFileName(dataWith("13d", "Rain")));
    }

    private WeatherData dataWith(String iconCode, String mainWeather) {
        WeatherData data = new WeatherData();
        data.setIconCode(iconCode);
        data.setMainWeather(mainWeather);
        return data;
    }
}
