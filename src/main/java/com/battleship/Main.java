/**
 * Naval Battle Game
 *
 * Version: 1.0
 * License: OpenGL
 *
 * Authors:
 * - Daniel Andres Micolta (2422033)
 * - Emmanuel Paez Hurtado (2419847)
 */


package com.battleship;

import com.battleship.controllers.GameController;
import com.battleship.models.GameDTO;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application entry point for the Battleship mini project.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Bootstrapping the JavaFX application.</li>
 *   <li>Keeping a reference to the primary {@link Stage} used by the application.</li>
 *   <li>Navigating between the welcome screen and the main game screen.</li>
 * </ul>
 *
 * <p>UI screens are defined using FXML:
 * <ul>
 *   <li>{@code /com/battleship/welcome-view.fxml}</li>
 *   <li>{@code /com/battleship/game-view.fxml}</li>
 * </ul>
 */
public class Main extends Application {

    /**
     * Primary stage for the application.
     * <p>Stored statically to allow simple navigation between scenes from other parts of the app.
     */
    private static Stage primaryStage;

    /**
     * JavaFX lifecycle entry method.
     *
     * <p>Sets up the primary stage and shows the welcome window.
     *
     * @param stage the primary {@link Stage} provided by the JavaFX runtime
     * @throws IOException if the welcome FXML cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        primaryStage.setTitle("Battleship - Mini Project #4");
        showWelcomeWindow();
    }

    /**
     * Loads and displays the welcome window.
     *
     * <p>This method replaces the current scene on the {@link #primaryStage} with the
     * welcome screen.
     *
     * @throws IOException if {@code welcome-view.fxml} cannot be loaded
     */
    public static void showWelcomeWindow() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                Main.class.getResource("/com/battleship/welcome-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Loads and displays the main game window.
     *
     * <p>If a saved game is provided, the controller restores the game state.
     * Otherwise, a new game session is started and the player's nickname is set.
     *
     * @param nickname  the player's nickname (used when {@code savedGame == null})
     * @param savedGame an optional saved game snapshot; if not {@code null}, the game state is loaded
     * @throws IOException if {@code game-view.fxml} cannot be loaded
     */
    public static void showGameWindow(String nickname, GameDTO savedGame) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                Main.class.getResource("/com/battleship/game-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 900, 650);

        GameController controller = fxmlLoader.getController();

        if (savedGame != null) {
            // If a saved game exists, load it into the controller.
            controller.loadSavedGame(savedGame);
        } else {
            // If this is a new game, set the player's nickname.
            controller.setPlayerNickname(nickname);
        }

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }
}
