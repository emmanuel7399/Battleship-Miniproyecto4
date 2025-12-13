package com.battleship.controllers;

import com.battleship.exceptions.*;
import com.battleship.models.*;
import com.battleship.views.BoardView;
import com.battleship.views.CellView;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.Map;

/**
 * Main controller for the Battleship game screen.
 *
 * <p>This class belongs to the <b>Controller</b> layer in the MVC architecture and
 * coordinates interactions between:</p>
 * <ul>
 *   <li><b>Model</b>: {@link Board}, {@link Ship}, {@link Coordinate}, AI opponent, save/load state.</li>
 *   <li><b>View</b>: {@link BoardView} and {@link CellView}.</li>
 * </ul>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Ship placement phase (selection, orientation, hover preview, validation).</li>
 *   <li>Battle phase (player shots, enemy AI shots, win/lose detection).</li>
 *   <li>Rendering ships as sprites spanning multiple cells (via {@link BoardView#addShipSprite}).</li>
 *   <li>Timer tracking and display.</li>
 *   <li>Background music control.</li>
 *   <li>Save/load integration and auto-save after turns.</li>
 *   <li>Centralized error reporting through {@link ExceptionHandler} and {@link GameException}.</li>
 * </ul>
 */
public class GameController {

    // =========================
    // FXML (Injected UI controls)
    // =========================

    /** Container holding both boards (player board and enemy board). */
    @FXML private HBox boardsContainer;

    /** Status label used to display messages to the user (instructions, results, etc.). */
    @FXML private Label statusLabel;

    /** Label that shows the current placement orientation (horizontal/vertical). */
    @FXML private Label orientationLabel;

    /** Label that shows the elapsed game time. */
    @FXML private Label timerLabel;

    /** Button to start the battle phase after ships are placed. */
    @FXML private Button btnStart;

    /** Button to restart the game after game over. */
    @FXML private Button btnRestart;

    // =========================
    // View References
    // =========================

    /** Player board view ("My Fleet"). */
    private BoardView playerBoardView;

    /** Enemy board view ("Enemy Waters"). */
    private BoardView enemyBoardView;

    // =========================
    // Model References
    // =========================

    /** Player board model. */
    private Board playerBoard;

    /** Enemy board model. */
    private Board enemyBoard;

    /** Player nickname displayed in victory/defeat dialogs and status messages. */
    private String playerNickname;

    // =========================
    // Game State Flags
    // =========================

    /** True while the player is placing ships; false once battle starts. */
    private boolean isPlacingShips = true;

    /** True if it is the player's turn to shoot; false when AI is shooting. */
    private boolean isMyTurn = false;

    /** True if placement orientation is horizontal; false if vertical. */
    private boolean isHorizontal = true;

    /** True while the game is running; used to stop timer thread on game over/restart. */
    private boolean isGameRunning = false;

    /** Debug flag that toggles whether enemy ships are revealed. */
    private boolean isDebugMode = false;

    // =========================
    // Timer
    // =========================

    /** Elapsed seconds since the battle started (or since the saved game timer value). */
    private int elapsedSeconds = 0;

    // =========================
    // Ship Placement
    // =========================

    /**
     * Remaining ships to place per type.
     * Example: Carrier=1, Submarine=2, Destroyer=3, Frigate=4.
     */
    private java.util.EnumMap<Ship.Type, Integer> shipsRemaining;

    /** Currently selected ship type for placement. */
    private Ship.Type selectedShipType;

    /** Cells currently highlighted as placement preview (hover). */
    private final java.util.List<Coordinate> currentPreviewCells = new java.util.ArrayList<>();

    // =========================
    // Ship Sprites
    // =========================

    /** Ship sprites in horizontal and vertical orientation for each ship type. */
    private Image frigateH, frigateV;
    private Image destroyerH, destroyerV;
    private Image submarineH, submarineV;
    private Image carrierH, carrierV;

    // =========================
    // Audio
    // =========================

    /** Media player used to loop background music during the match. */
    private MediaPlayer backgroundMusicPlayer;

    // =========================
    // Initialization
    // =========================

    /**
     * JavaFX controller initialization hook.
     *
     * <p>Creates model instances, builds the board views, wires placement events, loads ship sprites,
     * and prepares background music. Exceptions are delegated to {@link ExceptionHandler}.</p>
     */
    @FXML
    public void initialize() {
        try {
            playerBoard = new Board();
            enemyBoard = new Board();

            playerBoardView = new BoardView("My Fleet");
            enemyBoardView = new BoardView("Enemy Waters");
            boardsContainer.getChildren().addAll(playerBoardView, enemyBoardView);

            initShipsToPlace();
            setupPlacementEvents();

            initShipSprites();
            initBackgroundMusic();

        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        } catch (Exception ex) {
            ExceptionHandler.handle(ex);
        }
    }

    // ---------------------- SHIP / SPRITE UTILITIES ----------------------

    /**
     * Determines whether a ship is placed horizontally based on its occupied coordinates.
     *
     * @param ship the ship to evaluate
     * @return {@code true} if the ship is horizontal or size 1; {@code false} if vertical
     */
    private boolean isShipHorizontal(Ship ship) {
        var positions = ship.getPositions();
        if (positions == null || positions.size() <= 1) return true;

        int row0 = positions.get(0).getRow();
        for (Coordinate c : positions) {
            if (c.getRow() != row0) return false;
        }
        return true;
    }

    /**
     * Loads a sprite image from the resources folder.
     *
     * @param fileName sprite file name inside {@code /com/battleship/assets/}
     * @return loaded {@link Image}
     * @throws GameException if the resource cannot be found
     */
    private Image loadSprite(String fileName) {
        var url = getClass().getResource("/com/battleship/assets/" + fileName);
        if (url == null) {
            throw new GameException(ErrorType.ASSET, "Missing sprite: " + fileName);
        }
        return new Image(url.toExternalForm());
    }

    /**
     * Loads all ship sprites for both orientations.
     *
     * @throws GameException if any required sprite is missing
     */
    private void initShipSprites() {
        frigateH = loadSprite("fragataderecha.png");
        frigateV = loadSprite("fragataabajo.png");

        destroyerH = loadSprite("destructorderecha.png");
        destroyerV = loadSprite("destructorabajo.png");

        submarineH = loadSprite("submarinoderecha.png");
        submarineV = loadSprite("submarinoabajo.png");

        carrierH = loadSprite("portavionesderecha.png");
        carrierV = loadSprite("portavionesabajo.png");
    }

    /**
     * Returns the sprite that matches the given ship type and orientation.
     *
     * @param type       ship type
     * @param horizontal {@code true} for horizontal sprite; {@code false} for vertical
     * @return sprite image for that ship type and orientation
     */
    private Image getSpriteForShip(Ship.Type type, boolean horizontal) {
        return switch (type) {
            case FRIGATE -> horizontal ? frigateH : frigateV;
            case DESTROYER -> horizontal ? destroyerH : destroyerV;
            case SUBMARINE -> horizontal ? submarineH : submarineV;
            case AIRCRAFT_CARRIER -> horizontal ? carrierH : carrierV;
        };
    }

    // ------------------------------- LOAD GAME ----------------------------

    /**
     * Loads a previously saved game state into the controller.
     *
     * <p>Restores boards, nickname, and elapsed time. Then re-renders both boards,
     * re-wires battle events, starts background music, and resumes the timer.</p>
     *
     * @param data saved game data transfer object
     */
    public void loadSavedGame(GameDTO data) {
        try {
            if (data == null) {
                throw new GameException(ErrorType.SAVE_LOAD, "Saved game data is null.");
            }

            this.playerBoard = data.getPlayerBoard();
            this.enemyBoard = data.getEnemyBoard();
            this.playerNickname = data.getNickname();
            this.elapsedSeconds = data.getElapsedSeconds();

            this.isPlacingShips = false;
            this.isGameRunning = true;
            this.isMyTurn = true;

            btnStart.setDisable(true);
            updateStatus("Game Loaded! Welcome back, " + playerNickname);

            renderBoardFromModel(playerBoard, playerBoardView, true);
            renderBoardFromModel(enemyBoard, enemyBoardView, false);

            setupBattleEvents();
            initBackgroundMusic();
            playBackgroundMusic();
            startTimerThread();

        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        } catch (Exception ex) {
            ExceptionHandler.handle(ex);
        }
    }

    // ----------------------------- BOARD RENDERING ------------------------

    /**
     * Renders a {@link Board} model into a {@link BoardView}.
     *
     * <p>Ship sprites are cleared and optionally re-drawn (depending on {@code showShips}).
     * Shots are then drawn on top of the view.</p>
     *
     * @param board     model board containing ships and shot history
     * @param view      view to be updated
     * @param showShips {@code true} to draw ship sprites; {@code false} to hide ships
     */
    private void renderBoardFromModel(Board board, BoardView view, boolean showShips) {
        if (board == null || view == null) return;

        view.clearShipSprites();

        if (showShips) {
            for (Ship ship : board.getFleet()) {
                if (ship == null || ship.getPositions() == null || ship.getPositions().isEmpty()) continue;

                boolean horizontal = isShipHorizontal(ship);
                Image sprite = getSpriteForShip(ship.getType(), horizontal);
                Coordinate start = ship.getPositions().get(0);

                view.addShipSprite(
                        start.getRow(),
                        start.getCol(),
                        ship.getType().getSize(),
                        horizontal,
                        sprite
                );
            }
        }

        for (Coordinate shot : board.getShotsFired()) {
            if (shot == null) continue;
            CellView cell = view.getCell(shot.getRow(), shot.getCol());
            if (cell == null) continue;

            Map<Coordinate, Ship> grid = board.getGrid();
            if (grid != null && grid.containsKey(shot)) {
                Ship hitShip = grid.get(shot);
                if (hitShip != null && hitShip.isSunk()) cell.markAsSunk();
                else cell.markAsHit();
            } else {
                cell.markAsWater();
            }
        }
    }

    // ------------------------------- AUTO-SAVE -----------------------------

    /**
     * Saves the current game state asynchronously.
     *
     * <p>This method is typically called after turns (player or AI) to persist progress.</p>
     */
    private void saveGameStatus() {
        final int timeToSave = elapsedSeconds;
        new Thread(() -> {
            try {
                Serializator.saveGame(playerBoard, enemyBoard, playerNickname, timeToSave);
                System.out.println("Game Auto-Saved.");
            } catch (Exception ex) {
                // If saving fails, raise it as a game exception and show it through the handler.
                ExceptionHandler.handle(new GameException(ErrorType.SAVE_LOAD, "Failed to auto-save the game."));
            }
        }).start();
    }

    // ------------------------------- AI OPPONENT ---------------------------

    /**
     * Listener implementation used by {@link MachineOpponent} to report AI shots back to the UI.
     */
    private class MachineHandler implements GameEventListener {

        /**
         * Called whenever the AI fires a shot at the player's board.
         *
         * @param target coordinate fired by the AI
         * @param result shot result code (0=water, 1=hit, 2=sunk)
         */
        @Override
        public void onEnemyShotFired(Coordinate target, int result) {
            Platform.runLater(() -> {
                try {
                    CellView cell = playerBoardView.getCell(target.getRow(), target.getCol());
                    if (cell == null) return;

                    if (result == 0) {
                        cell.markAsWater();
                        updateStatus("Enemy missed! It's YOUR turn.");
                        isMyTurn = true;
                        saveGameStatus();

                    } else if (result == 1) {
                        cell.markAsHit();
                        updateStatus("Enemy HIT your ship! Enemy shoots again...");
                        saveGameStatus();

                    } else if (result == 2) {
                        cell.markAsHit();
                        Ship sunkShip = playerBoard.getGrid().get(target);
                        revealSunkShip(playerBoardView, sunkShip);
                        updateStatus("Enemy SUNK your ship! Enemy shoots again...");
                        saveGameStatus();

                        if (playerBoard.allShipsSunk()) {
                            handleGameOver(false);
                        }
                    }
                } catch (Exception ex) {
                    ExceptionHandler.handle(ex);
                }
            });
        }
    }

    /**
     * Starts the AI turn on a background daemon thread.
     */
    private void startEnemyTurnThread() {
        MachineOpponent opponentAI = new MachineOpponent(playerBoard, new MachineHandler());
        Thread enemyThread = new Thread(opponentAI);
        enemyThread.setDaemon(true);
        enemyThread.start();
    }

    // ------------------------------- TIMER -------------------------------------

    /**
     * Starts the game timer on a daemon thread and updates the UI once per second.
     */
    private void startTimerThread() {
        isGameRunning = true;
        Thread timerThread = new Thread(() -> {
            while (isGameRunning) {
                try {
                    Thread.sleep(1000);
                    elapsedSeconds++;

                    long minutes = elapsedSeconds / 60;
                    long seconds = elapsedSeconds % 60;
                    String timeStr = String.format("Time: %02d:%02d", minutes, seconds);

                    Platform.runLater(() -> timerLabel.setText(timeStr));
                } catch (InterruptedException ignored) {
                } catch (Exception ex) {
                    Platform.runLater(() -> ExceptionHandler.handle(ex));
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    // ------------------------------- GAME OVER ------------------------------

    /**
     * Handles game over state, stops the timer/music, and shows the victory/defeat dialog.
     *
     * @param playerWon {@code true} if the player won; {@code false} if the AI won
     */
    private void handleGameOver(boolean playerWon) {
        isGameRunning = false;
        stopBackgroundMusic();
        Alert alert = new Alert(playerWon ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("GAME OVER");
        alert.setHeaderText(playerWon ? "VICTORY!" : "DEFEAT");
        alert.setContentText(playerWon ? "Congratulations Admiral " + playerNickname : "Your fleet was destroyed.");
        alert.showAndWait();
        btnRestart.setVisible(true);
    }

    // ----------------------- SHIP PLACEMENT PHASE ---------------------

    /**
     * Returns a user-friendly description of the currently selected ship type.
     *
     * @return formatted string including ship type, size, and remaining count
     */
    private String getCurrentShipName() {
        if (selectedShipType == null) return "None";
        int remaining = shipsRemaining.getOrDefault(selectedShipType, 0);
        return selectedShipType.name() + " (Size: " + selectedShipType.getSize() + ", left: " + remaining + ")";
    }

    /**
     * Builds a summary string describing how many ships remain to place per type.
     *
     * @return formatted summary string
     */
    private String getShipsSummary() {
        if (shipsRemaining == null) return "";
        int c = shipsRemaining.getOrDefault(Ship.Type.AIRCRAFT_CARRIER, 0);
        int s = shipsRemaining.getOrDefault(Ship.Type.SUBMARINE, 0);
        int d = shipsRemaining.getOrDefault(Ship.Type.DESTROYER, 0);
        int f = shipsRemaining.getOrDefault(Ship.Type.FRIGATE, 0);
        return "Remaining - Carrier: " + c +
                "  Sub: " + s +
                "  Destroyer: " + d +
                "  Frigate: " + f;
    }

    /**
     * Initializes ship counts for the placement phase and selects a default ship type.
     */
    private void initShipsToPlace() {
        shipsRemaining = new java.util.EnumMap<>(Ship.Type.class);

        shipsRemaining.put(Ship.Type.AIRCRAFT_CARRIER, 1);
        shipsRemaining.put(Ship.Type.SUBMARINE, 2);
        shipsRemaining.put(Ship.Type.DESTROYER, 3);
        shipsRemaining.put(Ship.Type.FRIGATE, 4);

        selectedShipType = Ship.Type.AIRCRAFT_CARRIER;
        updateStatus("Welcome Admiral " + playerNickname + "! Select and place your ships. " + getShipsSummary());
    }

    /**
     * Checks whether at least one ship of the given type remains to be placed.
     *
     * @param type ship type
     * @return {@code true} if remaining count is greater than 0; otherwise {@code false}
     */
    private boolean hasRemaining(Ship.Type type) {
        return shipsRemaining.getOrDefault(type, 0) > 0;
    }

    /**
     * Checks whether all ships have been placed.
     *
     * @return {@code true} if all ship counts are 0; otherwise {@code false}
     */
    private boolean allShipsPlaced() {
        for (int count : shipsRemaining.values()) {
            if (count > 0) return false;
        }
        return true;
    }

    /**
     * Selects a ship type for placement.
     *
     * @param type ship type to select
     * @throws GameException if not in placement phase or if no ships of this type remain
     */
    private void selectShipType(Ship.Type type) {
        if (!isPlacingShips) {
            throw new GameException(ErrorType.PLACEMENT, "You can only select ships during placement.");
        }
        if (!hasRemaining(type)) {
            throw new GameException(ErrorType.PLACEMENT, "No " + type.name() + " remaining.");
        }
        selectedShipType = type;
        updateStatus("Selected: " + getCurrentShipName() + " | " + getShipsSummary());
    }

    /**
     * Clears all currently highlighted preview cells.
     */
    private void clearPlacementPreview() {
        for (Coordinate c : currentPreviewCells) {
            CellView cell = playerBoardView.getCell(c.getRow(), c.getCol());
            if (cell != null) cell.setPreview(false);
        }
        currentPreviewCells.clear();
    }

    /**
     * Highlights the cells that would be occupied by the currently selected ship
     * if placed at the given starting coordinate.
     *
     * @param startRow starting row index
     * @param startCol starting column index
     */
    private void showPlacementPreview(int startRow, int startCol) {
        clearPlacementPreview();
        if (!isPlacingShips || selectedShipType == null) return;

        int length = selectedShipType.getSize();
        for (int offset = 0; offset < length; offset++) {
            int r = startRow + (isHorizontal ? 0 : offset);
            int c = startCol + (isHorizontal ? offset : 0);

            if (r < 0 || r >= 10 || c < 0 || c >= 10) continue;

            Coordinate coord = new Coordinate(r, c);
            currentPreviewCells.add(coord);

            CellView cell = playerBoardView.getCell(r, c);
            if (cell != null) cell.setPreview(true);
        }
    }

    /**
     * Attaches mouse handlers to the player's board during the placement phase:
     * <ul>
     *   <li>Click to place a ship</li>
     *   <li>Hover to show placement preview</li>
     * </ul>
     */
    private void setupPlacementEvents() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                final int r = row;
                final int c = col;

                CellView cell = playerBoardView.getCell(row, col);

                cell.setOnMouseClicked(event -> {
                    try {
                        handlePlacementClick(r, c);
                    } catch (GameException ex) {
                        ExceptionHandler.handle(ex);
                    } catch (Exception ex) {
                        ExceptionHandler.handle(ex);
                    }
                });

                cell.setOnMouseEntered(event -> showPlacementPreview(r, c));
                cell.setOnMouseExited(event -> clearPlacementPreview());
            }
        }
    }

    /**
     * Finishes the placement phase, clears preview, and enables the battle start button.
     */
    private void finishPlacementPhase() {
        isPlacingShips = false;
        clearPlacementPreview();
        statusLabel.setText("All ships placed! Press 'Start Battle' to begin.");
        btnStart.setDisable(false);
    }

    /**
     * Attempts to place the currently selected ship at the provided coordinate.
     *
     * @param row board row index
     * @param col board column index
     * @throws GameException if selection is missing, no ships remain, or the placement is invalid
     */
    private void handlePlacementClick(int row, int col) {
        if (!isPlacingShips) return;

        if (allShipsPlaced()) {
            finishPlacementPhase();
            return;
        }

        if (selectedShipType == null) {
            throw new GameException(ErrorType.PLACEMENT, "Select a ship type first.");
        }

        if (!hasRemaining(selectedShipType)) {
            throw new GameException(ErrorType.PLACEMENT, "No " + selectedShipType.name() + " remaining. Choose another ship.");
        }

        Ship newShip = new Ship(selectedShipType);
        Coordinate start = new Coordinate(row, col);

        boolean success = playerBoard.placeShip(newShip, start, isHorizontal);
        if (!success) {
            throw new GameException(ErrorType.PLACEMENT, "Invalid position! Try again.");
        }

        updateBoardView(playerBoardView, newShip, true);

        int left = shipsRemaining.get(selectedShipType) - 1;
        shipsRemaining.put(selectedShipType, left);

        if (allShipsPlaced()) {
            finishPlacementPhase();
        } else {
            updateStatus("Placed! " + getCurrentShipName() + " selected. " + getShipsSummary());
        }
    }

    /**
     * Draws a ship sprite on the specified board view.
     *
     * @param view     board view to draw on
     * @param ship     ship to render
     * @param showShip whether ships should be drawn
     */
    private void updateBoardView(BoardView view, Ship ship, boolean showShip) {
        if (!showShip) return;
        if (ship.getPositions().isEmpty()) return;

        boolean horizontal = isShipHorizontal(ship);
        Image sprite = getSpriteForShip(ship.getType(), horizontal);
        Coordinate start = ship.getPositions().get(0);

        view.addShipSprite(
                start.getRow(),
                start.getCol(),
                ship.getType().getSize(),
                horizontal,
                sprite
        );
    }

    /**
     * Marks all coordinates of a sunk ship as sunk in the given board view.
     *
     * @param view board view to update
     * @param ship ship that has been sunk
     */
    private void revealSunkShip(BoardView view, Ship ship) {
        if (ship == null) return;
        for (Coordinate c : ship.getPositions()) {
            CellView cell = view.getCell(c.getRow(), c.getCol());
            if (cell != null) cell.markAsSunk();
        }
    }

    /**
     * Toggles placement orientation between horizontal and vertical.
     */
    @FXML
    public void onRotateClick() {
        isHorizontal = !isHorizontal;
        orientationLabel.setText("Current: " + (isHorizontal ? "HORIZONTAL" : "VERTICAL"));
    }

    // ---------------------------- BATTLE PHASE -----------------------------

    /**
     * Starts the battle phase: places enemy ships randomly, enables player turn,
     * wires battle events, starts timer, music, and triggers an initial auto-save.
     */
    @FXML
    public void onStartGame() {
        try {
            btnStart.setDisable(true);
            enemyBoard.placeShipsRandomly();
            isMyTurn = true;
            isGameRunning = true;

            updateStatus("Battle Started! It's your turn.");
            setupBattleEvents();
            startTimerThread();
            playBackgroundMusic();
            saveGameStatus();
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        } catch (Exception ex) {
            ExceptionHandler.handle(ex);
        }
    }

    /**
     * Attaches click handlers to the enemy board during the battle phase.
     * Each click represents a player shot.
     */
    private void setupBattleEvents() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                CellView cell = enemyBoardView.getCell(row, col);
                final int r = row;
                final int c = col;

                cell.setOnMouseClicked(event -> {
                    try {
                        handleEnemyBoardClick(r, c);
                    } catch (GameException ex) {
                        ExceptionHandler.handle(ex);
                    } catch (Exception ex) {
                        ExceptionHandler.handle(ex);
                    }
                });
            }
        }
    }

    /**
     * Handles a player shot on the enemy board.
     *
     * @param row enemy board row index
     * @param col enemy board column index
     * @throws GameException if the player clicks a previously shot coordinate
     */
    private void handleEnemyBoardClick(int row, int col) {
        if (!isMyTurn || !isGameRunning) return;

        int result = enemyBoard.receiveShot(new Coordinate(row, col));
        CellView cell = enemyBoardView.getCell(row, col);

        if (result == -1) {
            throw new GameException(ErrorType.SHOT, "You already shot there!");
        }

        if (result == 0) {
            cell.markAsWater();
            updateStatus("Miss! Computer's turn.");
            saveGameStatus();

            isMyTurn = false;
            startEnemyTurnThread();

        } else if (result == 1) {
            cell.markAsHit();
            updateStatus("HIT! Shoot again!");
            saveGameStatus();

        } else if (result == 2) {
            cell.markAsHit();
            Ship sunkShip = enemyBoard.getGrid().get(new Coordinate(row, col));
            revealSunkShip(enemyBoardView, sunkShip);
            updateStatus("SUNK! Shoot again!");
            saveGameStatus();

            if (enemyBoard.allShipsSunk()) {
                handleGameOver(true);
            }
        }
    }

    /**
     * Toggles debug mode which reveals/hides enemy ships by re-rendering the enemy board.
     */
    @FXML
    public void onShowEnemyBoard() {
        try {
            isDebugMode = !isDebugMode;
            renderBoardFromModel(enemyBoard, enemyBoardView, isDebugMode);
            statusLabel.setText(isDebugMode
                    ? "Debug Mode: ON - Enemy ships revealed."
                    : "Debug Mode: OFF - Enemy ships hidden.");
        } catch (Exception ex) {
            ExceptionHandler.handle(ex);
        }
    }

    // ------------------------------- RESTART -------------------------------

    /**
     * Restarts the game by re-opening the game window as a new mission.
     */
    @FXML
    public void onRestartGame() {
        try {
            stopBackgroundMusic();
            isGameRunning = false;
            com.battleship.Main.showGameWindow(playerNickname, null);
        } catch (Exception ex) {
            ExceptionHandler.handle(new GameException(ErrorType.SYSTEM, "Failed to restart the game."));
        }
    }

    // ------------------------------ PLAYER NAME -----------------------------

    /**
     * Sets the player's nickname for UI messages and dialogs.
     *
     * @param nickname player nickname
     */
    public void setPlayerNickname(String nickname) {
        this.playerNickname = nickname;
        updateStatus("Welcome Admiral " + nickname + "! Place: " + getCurrentShipName());
    }

    /**
     * Updates the status label text shown to the user.
     *
     * @param msg message to display
     */
    private void updateStatus(String msg) {
        statusLabel.setText(msg);
    }

    // ------------------------------- MUSIC ------------------------------------

    /**
     * Initializes background music from the assets folder.
     *
     * @throws GameException if the music file cannot be found
     */
    private void initBackgroundMusic() {
        var url = getClass().getResource("/com/battleship/assets/backgroundmusic.wav");
        if (url == null) {
            throw new GameException(ErrorType.ASSET, "Background music file not found.");
        }

        Media media = new Media(url.toExternalForm());
        backgroundMusicPlayer = new MediaPlayer(media);
        backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        backgroundMusicPlayer.setVolume(0.3);
    }

    /**
     * Starts playback of background music (if initialized).
     */
    private void playBackgroundMusic() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.play();
    }

    /**
     * Stops playback of background music (if initialized).
     */
    private void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.stop();
    }

    // ------------------------ SHIP SELECTION BUTTONS -------------------

    /**
     * Selects the aircraft carrier for placement.
     */
    @FXML
    public void onSelectCarrier() {
        try {
            selectShipType(Ship.Type.AIRCRAFT_CARRIER);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    /**
     * Selects the submarine for placement.
     */
    @FXML
    public void onSelectSubmarine() {
        try {
            selectShipType(Ship.Type.SUBMARINE);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    /**
     * Selects the destroyer for placement.
     */
    @FXML
    public void onSelectDestroyer() {
        try {
            selectShipType(Ship.Type.DESTROYER);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    /**
     * Selects the frigate for placement.
     */
    @FXML
    public void onSelectFrigate() {
        try {
            selectShipType(Ship.Type.FRIGATE);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }
}
