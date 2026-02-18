package com.weather.mapper;

/**
 * Hava durumu kodlarını ikon dosyalarına eşleştirir.
 * Single Responsibility: Sadece ikon eşleştirme işlemi.
 */
public class WeatherIconMapper implements IWeatherIconMapper {

    private static final String DEFAULT_ICON = "Icon=Sunny.png";

    @Override
    public String getIconFileName(String mainWeather, String description) {
        if (mainWeather == null) {
            return DEFAULT_ICON;
        }

        String lowerDescription = description != null ? description.toLowerCase() : "";

        switch (mainWeather.toLowerCase()) {
            case "clear":
                return "Icon=Sunny.png";

            case "clouds":
                if (lowerDescription.contains("few") || lowerDescription.contains("scattered")) {
                    return "Icon=PartlyCloudy.png";
                }
                return "Icon=Cloudy.png";

            case "rain":
                if (lowerDescription.contains("light") || lowerDescription.contains("drizzle")) {
                    return "Icon=LightDrizzle.png";
                } else if (lowerDescription.contains("shower")) {
                    return "Icon=RainywithSun.png";
                }
                return "Icon=Rainy.png";

            case "thunderstorm":
                return "Icon=Thunderstorm.png";

            case "snow":
                return "Icon=Snow.png";

            case "drizzle":
                return "Icon=LightDrizzle.png";

            case "mist":
            case "fog":
            case "haze":
                return "Icon=Cloudy.png";

            default:
                return DEFAULT_ICON;
        }
    }
}
