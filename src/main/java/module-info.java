module com.weather {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    // OpenWeatherMapService, JDK'nın yerleşik HTTP istemcisini kullanır.
    requires java.net.http;

    // FXML reflection ile controller'ı bağlar; paket açık olmalı.
    opens com.weather to javafx.fxml;

    exports com.weather;

    exports com.weather.model;
    exports com.weather.service;
    exports com.weather.mapper;
}
