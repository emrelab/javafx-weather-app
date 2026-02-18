module com.weather {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    opens com.weather to javafx.fxml;

    exports com.weather;

    exports com.weather.model;
    exports com.weather.service;
    exports com.weather.mapper;
}