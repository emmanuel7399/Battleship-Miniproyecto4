package com.battleship.controllers;

import com.battleship.Main;
import com.battleship.models.GameDTO;
import com.battleship.models.Serializator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.io.IOException;

public class WelcomeController {

    @FXML
    private TextField nicknameField;

    @FXML
    protected void onPlayButtonClick() throws IOException {
        String nickname = nicknameField.getText().trim();

        if (nickname.isEmpty()) {
            showAlert("Error", "Please enter a nickname to play!");
        } else {
            // Inicia juego nuevo (pasamos null como savedGame)
            Main.showGameWindow(nickname, null);
        }
    }

    @FXML
    protected void onContinueButtonClick() throws IOException {
        GameDTO savedGame = Serializator.loadGame();

        if (savedGame != null) {
            // Inicia juego cargado (pasamos el objeto DTO)
            Main.showGameWindow(savedGame.getNickname(), savedGame);
        } else {
            showAlert("No Save Found", "No saved game file was found.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}