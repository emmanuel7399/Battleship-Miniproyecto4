package com.battleship;

import com.battleship.controllers.GameController;
import com.battleship.models.GameDTO;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Battleship - Mini Project #4");
        showWelcomeWindow();
    }

    public static void showWelcomeWindow() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/battleship/welcome-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    // Modificado para aceptar un GameDTO opcional
    public static void showGameWindow(String nickname, GameDTO savedGame) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/battleship/game-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 650);

        GameController controller = fxmlLoader.getController();

        if (savedGame != null) {
            // Si hay partida guardada, la cargamos
            controller.loadSavedGame(savedGame);
        } else {
            // Si es juego nuevo, solo ponemos el nombre
            controller.setPlayerNickname(nickname);
        }

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}