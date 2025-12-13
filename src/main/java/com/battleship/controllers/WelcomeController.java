package com.battleship.controllers;

import com.battleship.Main;
import com.battleship.models.GameDTO;
import com.battleship.models.Serializator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller for the welcome screen.
 *
 * <p>This controller handles user input on the initial menu, including:</p>
 * <ul>
 *   <li>Starting a new game using the provided nickname.</li>
 *   <li>Continuing from a previously saved game (if available).</li>
 * </ul>
 *
 * <p>This class belongs to the <b>Controller</b> layer in the MVC architecture.
 * It delegates screen navigation to {@link Main} and save/load operations to
 * {@link Serializator}.</p>
 */
public class WelcomeController {

    /** Text field where the player enters their nickname. */
    @FXML
    private TextField nicknameField;

    /**
     * Triggered when the user clicks the "Play" (new game) button.
     *
     * <p>Validates that the nickname is not empty and then navigates to the game screen
     * starting a new mission (no saved game data is provided).</p>
     *
     * @throws IOException if the game view FXML cannot be loaded
     */
    @FXML
    protected void onPlayButtonClick() throws IOException {
        String nickname = nicknameField.getText().trim();

        if (nickname.isEmpty()) {
            showAlert("Error", "Please enter a nickname to play!");
        } else {
            // Start a new game (pass null as savedGame)
            Main.showGameWindow(nickname, null);
        }
    }

    /**
     * Triggered when the user clicks the "Continue" button.
     *
     * <p>Attempts to load a saved game using {@link Serializator#loadGame()}.
     * If a save is found, navigates to the game screen and restores the state.
     * Otherwise, shows a warning to the user.</p>
     *
     * @throws IOException if the game view FXML cannot be loaded
     */
    @FXML
    protected void onContinueButtonClick() throws IOException {
        GameDTO savedGame = Serializator.loadGame();

        if (savedGame != null) {
            // Start a loaded game (pass the DTO object)
            Main.showGameWindow(savedGame.getNickname(), savedGame);
        } else {
            showAlert("No Save Found", "No saved game file was found.");
        }
    }

    /**
     * Shows a warning alert dialog to the user.
     *
     * @param title   the alert window title
     * @param content the alert message content
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
