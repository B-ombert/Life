module com.life {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.life to javafx.fxml;
    exports com.life;
}