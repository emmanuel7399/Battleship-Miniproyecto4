package com.battleship.exceptions;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class ExceptionHandler {

    private ExceptionHandler() {}

    public static void handle(GameException ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Battleship Error");
            alert.setHeaderText(getHeader(ex.getType()));
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        });
    }

    public static void handle(Exception ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unexpected Error");
            alert.setHeaderText("Something went wrong");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        });
    }

    private static String getHeader(ErrorType type) {
        return switch (type) {
            case PLACEMENT -> "Invalid Ship Placement";
            case SHOT -> "Invalid Shot";
            case SAVE_LOAD -> "Save / Load Error";
            case ASSET -> "Asset Error";
            case SYSTEM -> "System Error";
        };
    }
}
