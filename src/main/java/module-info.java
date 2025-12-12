module com.battleship {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop; // A veces necesario para ciertas utilidades de AWT si se usan

    opens com.battleship to javafx.fxml;
    exports com.battleship;

    // Exportamos y abrimos el paquete controllers para que JavaFX pueda acceder a ellos
    exports com.battleship.controllers;
    opens com.battleship.controllers to javafx.fxml;
}