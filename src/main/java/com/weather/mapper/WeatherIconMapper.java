package com.weather.mapper;

import com.weather.model.WeatherData;

import java.util.Locale;
import java.util.Map;

/**
 * Hava durumu verisini yerel ikon dosyalarına eşleştirir.
 * Single Responsibility: Sadece ikon eşleştirme işlemi.
 *
 * <p>Öncelik sırası:
 * <ol>
 *   <li>OpenWeatherMap {@code icon} kodu (örn. {@code "10d"}) — dilden bağımsız,
 *       birincil kaynak.</li>
 *   <li>{@code main} alanı (clear/clouds/rain/…) — kod eksikse yedek.</li>
 * </ol>
 *
 * <p>Neden kod: API {@code lang=tr} ile çağrıldığı için {@code description}
 * Türkçe gelir ("parçalı bulutlu"), dolayısıyla İngilizce anahtar kelime
 * araması ("few", "light", "shower") hiçbir zaman eşleşmez.
 */
public class WeatherIconMapper implements IWeatherIconMapper {

    private static final String DEFAULT_ICON = "Icon=Sunny.png";

    /**
     * OpenWeatherMap ikon kodunun ilk iki hanesi → ikon dosyası.
     * 01 clear · 02 few clouds · 03 scattered · 04 broken clouds ·
     * 09 shower rain · 10 rain · 11 thunderstorm · 13 snow · 50 mist
     */
    private static final Map<String, String> ICON_BY_CODE = Map.ofEntries(
            Map.entry("01", "Icon=Sunny.png"),
            Map.entry("02", "Icon=PartlyCloudy.png"),
            Map.entry("03", "Icon=Cloudy.png"),
            Map.entry("04", "Icon=Cloudy.png"),
            Map.entry("09", "Icon=RainywithSun.png"),
            Map.entry("10", "Icon=Rainy.png"),
            Map.entry("11", "Icon=Thunderstorm.png"),
            Map.entry("13", "Icon=Snow.png"),
            Map.entry("50", "Icon=Cloudy.png"));

    /** Kod yoksa kullanılan yedek eşleme (dilden bağımsız alan). */
    private static final Map<String, String> ICON_BY_MAIN = Map.ofEntries(
            Map.entry("clear", "Icon=Sunny.png"),
            Map.entry("clouds", "Icon=Cloudy.png"),
            Map.entry("rain", "Icon=Rainy.png"),
            Map.entry("drizzle", "Icon=LightDrizzle.png"),
            Map.entry("thunderstorm", "Icon=Thunderstorm.png"),
            Map.entry("snow", "Icon=Snow.png"),
            Map.entry("mist", "Icon=Cloudy.png"),
            Map.entry("fog", "Icon=Cloudy.png"),
            Map.entry("haze", "Icon=Cloudy.png"),
            Map.entry("smoke", "Icon=Cloudy.png"),
            Map.entry("dust", "Icon=Cloudy.png"),
            Map.entry("sand", "Icon=Cloudy.png"),
            Map.entry("ash", "Icon=Cloudy.png"),
            Map.entry("squall", "Icon=Thunderstorm.png"),
            Map.entry("tornado", "Icon=Thunderstorm.png"));

    @Override
    public String getIconFileName(WeatherData data) {
        if (data == null) {
            return DEFAULT_ICON;
        }

        String byCode = iconByCode(data.getIconCode());
        if (byCode != null) {
            return byCode;
        }
        return iconByMain(data.getMainWeather());
    }

    private String iconByCode(String iconCode) {
        if (iconCode == null || iconCode.length() < 2) {
            return null;
        }
        return ICON_BY_CODE.get(iconCode.substring(0, 2));
    }

    private String iconByMain(String mainWeather) {
        if (mainWeather == null) {
            return DEFAULT_ICON;
        }
        // Map.of... null anahtarla get() çağrısında NPE fırlatır, bu yüzden yukarıda korunuyor.
        return ICON_BY_MAIN.getOrDefault(mainWeather.toLowerCase(Locale.ROOT), DEFAULT_ICON);
    }
}
