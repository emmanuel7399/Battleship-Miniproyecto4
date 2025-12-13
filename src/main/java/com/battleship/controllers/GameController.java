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

public class GameController {

    @FXML private HBox boardsContainer;
    @FXML private Label statusLabel;
    @FXML private Label orientationLabel;
    @FXML private Label timerLabel;
    @FXML private Button btnStart;
    @FXML private Button btnRestart;

    private BoardView playerBoardView;
    private BoardView enemyBoardView;

    private Board playerBoard;
    private Board enemyBoard;
    private String playerNickname;

    private boolean isPlacingShips = true;
    private boolean isMyTurn = false;
    private boolean isHorizontal = true;
    private boolean isGameRunning = false;
    private boolean isDebugMode = false;

    private int elapsedSeconds = 0;

    private java.util.EnumMap<Ship.Type, Integer> shipsRemaining;
    private Ship.Type selectedShipType;

    // Preview placement
    private final java.util.List<Coordinate> currentPreviewCells = new java.util.ArrayList<>();

    // Sprites
    private Image frigateH, frigateV;
    private Image destroyerH, destroyerV;
    private Image submarineH, submarineV;
    private Image carrierH, carrierV;

    // Music
    private MediaPlayer backgroundMusicPlayer;

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

    // ---------------------- UTILIDADES DE BARCO / SPRITES ----------------------

    private boolean isShipHorizontal(Ship ship) {
        var positions = ship.getPositions();
        if (positions == null || positions.size() <= 1) return true;

        int row0 = positions.get(0).getRow();
        for (Coordinate c : positions) {
            if (c.getRow() != row0) return false;
        }
        return true;
    }

    private Image loadSprite(String fileName) {
        var url = getClass().getResource("/com/battleship/assets/" + fileName);
        if (url == null) {
            throw new GameException(ErrorType.ASSET, "Missing sprite: " + fileName);
        }
        return new Image(url.toExternalForm());
    }

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

    private Image getSpriteForShip(Ship.Type type, boolean horizontal) {
        return switch (type) {
            case FRIGATE -> horizontal ? frigateH : frigateV;
            case DESTROYER -> horizontal ? destroyerH : destroyerV;
            case SUBMARINE -> horizontal ? submarineH : submarineV;
            case AIRCRAFT_CARRIER -> horizontal ? carrierH : carrierV;
        };
    }

    // ------------------------------- CARGAR PARTIDA ----------------------------

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

    // ----------------------------- RENDERIZADO TABLEROS ------------------------

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

    // ------------------------------- AUTO-GUARDADO -----------------------------

    private void saveGameStatus() {
        final int timeToSave = elapsedSeconds;
        new Thread(() -> {
            try {
                Serializator.saveGame(playerBoard, enemyBoard, playerNickname, timeToSave);
                System.out.println("Game Auto-Saved.");
            } catch (Exception ex) {
                // si falla el guardado, lo elevamos como excepción del juego
                ExceptionHandler.handle(new GameException(ErrorType.SAVE_LOAD, "Failed to auto-save the game."));
            }
        }).start();
    }

    // ------------------------------- MÁQUINA -----------------------------------

    private class MachineHandler implements GameEventListener {
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

    private void startEnemyTurnThread() {
        MachineOpponent opponentAI = new MachineOpponent(playerBoard, new MachineHandler());
        Thread enemyThread = new Thread(opponentAI);
        enemyThread.setDaemon(true);
        enemyThread.start();
    }

    // ------------------------------- TIMER -------------------------------------

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

    // ------------------------------- FIN DE JUEGO ------------------------------

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

    // ----------------------- FASE DE COLOCACIÓN DE BARCOS ---------------------

    private String getCurrentShipName() {
        if (selectedShipType == null) return "None";
        int remaining = shipsRemaining.getOrDefault(selectedShipType, 0);
        return selectedShipType.name() + " (Size: " + selectedShipType.getSize() + ", left: " + remaining + ")";
    }

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

    private void initShipsToPlace() {
        shipsRemaining = new java.util.EnumMap<>(Ship.Type.class);

        shipsRemaining.put(Ship.Type.AIRCRAFT_CARRIER, 1);
        shipsRemaining.put(Ship.Type.SUBMARINE, 2);
        shipsRemaining.put(Ship.Type.DESTROYER, 3);
        shipsRemaining.put(Ship.Type.FRIGATE, 4);

        selectedShipType = Ship.Type.AIRCRAFT_CARRIER;
        updateStatus("Welcome Admiral " + playerNickname + "! Select and place your ships. " + getShipsSummary());
    }

    private boolean hasRemaining(Ship.Type type) {
        return shipsRemaining.getOrDefault(type, 0) > 0;
    }

    private boolean allShipsPlaced() {
        for (int count : shipsRemaining.values()) {
            if (count > 0) return false;
        }
        return true;
    }

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

    private void clearPlacementPreview() {
        for (Coordinate c : currentPreviewCells) {
            CellView cell = playerBoardView.getCell(c.getRow(), c.getCol());
            if (cell != null) cell.setPreview(false);
        }
        currentPreviewCells.clear();
    }

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

    private void finishPlacementPhase() {
        isPlacingShips = false;
        clearPlacementPreview();
        statusLabel.setText("All ships placed! Press 'Start Battle' to begin.");
        btnStart.setDisable(false);
    }

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

    private void revealSunkShip(BoardView view, Ship ship) {
        if (ship == null) return;
        for (Coordinate c : ship.getPositions()) {
            CellView cell = view.getCell(c.getRow(), c.getCol());
            if (cell != null) cell.markAsSunk();
        }
    }

    @FXML
    public void onRotateClick() {
        isHorizontal = !isHorizontal;
        orientationLabel.setText("Current: " + (isHorizontal ? "HORIZONTAL" : "VERTICAL"));
    }

    // ---------------------------- FASE DE BATALLA -----------------------------

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

    // ------------------------------- REINICIO ----------------------------------

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

    // ------------------------------ NOMBRE JUGADOR -----------------------------

    public void setPlayerNickname(String nickname) {
        this.playerNickname = nickname;
        updateStatus("Welcome Admiral " + nickname + "! Place: " + getCurrentShipName());
    }

    private void updateStatus(String msg) {
        statusLabel.setText(msg);
    }

    // ------------------------------- MÚSICA ------------------------------------

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

    private void playBackgroundMusic() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.play();
    }

    private void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.stop();
    }

    // ------------------------ BOTONES DE SELECCIÓN DE BARCO -------------------

    @FXML
    public void onSelectCarrier() {
        try {
            selectShipType(Ship.Type.AIRCRAFT_CARRIER);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    @FXML
    public void onSelectSubmarine() {
        try {
            selectShipType(Ship.Type.SUBMARINE);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    @FXML
    public void onSelectDestroyer() {
        try {
            selectShipType(Ship.Type.DESTROYER);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    @FXML
    public void onSelectFrigate() {
        try {
            selectShipType(Ship.Type.FRIGATE);
        } catch (GameException ex) {
            ExceptionHandler.handle(ex);
        }
    }
}
