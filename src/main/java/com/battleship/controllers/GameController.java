package com.battleship.controllers;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import com.battleship.models.*;
import com.battleship.views.BoardView;
import com.battleship.views.CellView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;

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
    private boolean isDebugMode = false; // Controla si el ojo está activado o no

    private int elapsedSeconds = 0; // Variable para controlar el tiempo real

    private java.util.EnumMap<Ship.Type, Integer> shipsRemaining;
    private Ship.Type selectedShipType;

    // Celdas actualmente en modo "preview" durante la colocación
    private java.util.List<Coordinate> currentPreviewCells = new java.util.ArrayList<>();

    // Sprites de barcos
    private Image frigateH, frigateV;
    private Image destroyerH, destroyerV;
    private Image submarineH, submarineV;
    private Image carrierH, carrierV;

    // Música de fondo
    private MediaPlayer backgroundMusicPlayer;

    @FXML
    public void initialize() {
        // Inicialización por defecto (Juego Nuevo)
        playerBoard = new Board();
        enemyBoard = new Board();

        playerBoardView = new BoardView("My Fleet");
        enemyBoardView = new BoardView("Enemy Waters");
        boardsContainer.getChildren().addAll(playerBoardView, enemyBoardView);

        initShipsToPlace();
        setupPlacementEvents();
        initShipSprites();
        initBackgroundMusic();
    }

    // ---------------------- UTILIDADES DE BARCO / SPRITES ----------------------

    private boolean isShipHorizontal(Ship ship) {
        var positions = ship.getPositions();
        if (positions.size() <= 1) return true; // da igual

        int row0 = positions.get(0).getRow();
        for (Coordinate c : positions) {
            if (c.getRow() != row0) {
                return false; // si cambia la fila, es vertical
            }
        }
        return true;
    }

    private Image loadSprite(String fileName) {
        var url = getClass().getResource("/com/battleship/assets/" + fileName);
        if (url == null) {
            System.err.println("Sprite not found: " + fileName);
            return null;
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
        this.playerBoard = data.getPlayerBoard();
        this.enemyBoard = data.getEnemyBoard();
        this.playerNickname = data.getNickname();
        this.elapsedSeconds = data.getElapsedSeconds(); // Recuperamos el tiempo

        // Estado del juego
        this.isPlacingShips = false;
        this.isGameRunning = true;
        this.isMyTurn = true;

        btnStart.setDisable(true);
        updateStatus("Game Loaded! Welcome back, " + playerNickname);

        // Renderizar tableros
        renderBoardFromModel(playerBoard, playerBoardView, true);
        renderBoardFromModel(enemyBoard, enemyBoardView, false);

        setupBattleEvents();
        initBackgroundMusic();
        playBackgroundMusic();
        startTimerThread(); // El hilo iniciará desde el 'elapsedSeconds' cargado
    }

    // ----------------------------- RENDERIZADO TABLEROS ------------------------

    private void renderBoardFromModel(Board board, BoardView view, boolean showShips) {
        // Limpiamos sprites de barcos
        view.clearShipSprites();

        // 1. Barcos (solo si corresponde)
        if (showShips) {
            for (Ship ship : board.getFleet()) {
                boolean horizontal = isShipHorizontal(ship);
                Image sprite = getSpriteForShip(ship.getType(), horizontal);
                if (sprite != null && !ship.getPositions().isEmpty()) {
                    Coordinate start = ship.getPositions().get(0);
                    view.addShipSprite(
                            start.getRow(),
                            start.getCol(),
                            ship.getType().getSize(),
                            horizontal,
                            sprite
                    );
                } else {
                    // Fallback: pinta gris por casilla
                    for (Coordinate coord : ship.getPositions()) {
                        view.getCell(coord.getRow(), coord.getCol()).markAsShip();
                    }
                }
            }
        }

        // 2. Disparos
        for (Coordinate shot : board.getShotsFired()) {
            CellView cell = view.getCell(shot.getRow(), shot.getCol());
            if (board.getGrid().containsKey(shot)) {
                Ship hitShip = board.getGrid().get(shot);
                if (hitShip.isSunk()) {
                    cell.markAsSunk();
                } else {
                    cell.markAsHit();
                }
            } else {
                cell.markAsWater();  // <-- USAMOS EL MÉTODO QUE SÍ TIENES
            }
        }

    }

    // ------------------------------- AUTO-GUARDADO -----------------------------

    private void saveGameStatus() {
        final int timeToSave = elapsedSeconds;
        new Thread(() -> {
            Serializator.saveGame(playerBoard, enemyBoard, playerNickname, timeToSave);
            System.out.println("Game Auto-Saved.");
        }).start();
    }

    // ------------------------------- MÁQUINA -----------------------------------

    private class MachineHandler implements GameEventListener {
        @Override
        public void onEnemyShotFired(Coordinate target, int result) {
            Platform.runLater(() -> {
                CellView cell = playerBoardView.getCell(target.getRow(), target.getCol());

                if (result == 0) { // AGUA
                    cell.markAsWater();
                    updateStatus("Enemy missed! It's YOUR turn.");
                    isMyTurn = true;
                    saveGameStatus();

                } else if (result == 1) { // TOCADO
                    cell.markAsHit();
                    updateStatus("Enemy HIT your ship! Enemy shoots again...");
                    saveGameStatus();

                } else if (result == 2) { // HUNDIDO
                    cell.markAsHit();
                    Ship sunkShip = playerBoard.getGrid().get(target);
                    revealSunkShip(playerBoardView, sunkShip);
                    updateStatus("Enemy SUNK your ship! Enemy shoots again...");
                    saveGameStatus();

                    if (playerBoard.allShipsSunk()) {
                        handleGameOver(false);
                    }
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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                elapsedSeconds++;

                long minutes = elapsedSeconds / 60;
                long seconds = elapsedSeconds % 60;
                String timeStr = String.format("Time: %02d:%02d", minutes, seconds);

                Platform.runLater(() -> timerLabel.setText(timeStr));
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

        selectedShipType = Ship.Type.AIRCRAFT_CARRIER; // por defecto
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
            updateStatus("You can only select ships during placement.");
            return;
        }
        if (!hasRemaining(type)) {
            updateStatus("No " + type.name() + " remaining.");
            return;
        }
        selectedShipType = type;
        updateStatus("Selected: " + getCurrentShipName() + " | " + getShipsSummary());
    }

    private void clearPlacementPreview() {
        for (Coordinate c : currentPreviewCells) {
            CellView cell = playerBoardView.getCell(c.getRow(), c.getCol());
            if (cell != null) {
                cell.setPreview(false);
            }
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

            // fuera del tablero, ignoramos
            if (r < 0 || r >= 10 || c < 0 || c >= 10) continue;

            Coordinate coord = new Coordinate(r, c);
            currentPreviewCells.add(coord);

            CellView cell = playerBoardView.getCell(r, c);
            if (cell != null) {
                cell.setPreview(true);
            }
        }
    }

    private void setupPlacementEvents() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                final int r = row;
                final int c = col;
                CellView cell = playerBoardView.getCell(row, col);

                cell.setOnMouseClicked(event -> handlePlacementClick(r, c));
                cell.setOnMouseEntered(event -> showPlacementPreview(r, c));
                cell.setOnMouseExited(event -> clearPlacementPreview());
            }
        }
    }

    private void finishPlacementPhase() {
        isPlacingShips = false;
        clearPlacementPreview(); // limpiar sombreado
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
            updateStatus("Select a ship type first.");
            return;
        }

        if (!hasRemaining(selectedShipType)) {
            updateStatus("No " + selectedShipType.name() + " remaining. Choose another ship.");
            return;
        }

        Ship newShip = new Ship(selectedShipType);
        Coordinate start = new Coordinate(row, col);

        boolean success = playerBoard.placeShip(newShip, start, isHorizontal);

        if (success) {
            updateBoardView(playerBoardView, newShip, true);

            int left = shipsRemaining.get(selectedShipType) - 1;
            shipsRemaining.put(selectedShipType, left);

            if (allShipsPlaced()) {
                finishPlacementPhase();
            } else {
                updateStatus("Placed! " + getCurrentShipName() + " selected. " + getShipsSummary());
            }
        } else {
            statusLabel.setText("Invalid position! Try again.");
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
        for (Coordinate c : ship.getPositions()) {
            CellView cell = view.getCell(c.getRow(), c.getCol());
            cell.markAsSunk();
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
        btnStart.setDisable(true);
        enemyBoard.placeShipsRandomly();
        isMyTurn = true;
        updateStatus("Battle Started! It's your turn.");
        setupBattleEvents();
        startTimerThread();
        playBackgroundMusic();
        saveGameStatus(); // Guardado inicial
    }

    private void setupBattleEvents() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                CellView cell = enemyBoardView.getCell(row, col);
                cell.setOnMouseClicked(event -> handleEnemyBoardClick(cell.getRow(), cell.getCol()));
            }
        }
    }

    private void handleEnemyBoardClick(int row, int col) {
        if (!isMyTurn || !isGameRunning) return;

        int result = enemyBoard.receiveShot(new Coordinate(row, col));
        CellView cell = enemyBoardView.getCell(row, col);

        if (result == -1) {
            statusLabel.setText("You already shot there!");
            return;
        }

        if (result == 0) { // AGUA
            cell.markAsWater();
            updateStatus("Miss! Computer's turn.");
            saveGameStatus();

            isMyTurn = false;
            startEnemyTurnThread();

        } else if (result == 1) { // TOCADO
            cell.markAsHit();
            updateStatus("HIT! Shoot again!");
            saveGameStatus();

        } else if (result == 2) { // HUNDIDO
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
        isDebugMode = !isDebugMode;

        renderBoardFromModel(enemyBoard, enemyBoardView, isDebugMode);

        if (isDebugMode) {
            statusLabel.setText("Debug Mode: ON - Enemy ships revealed.");
        } else {
            statusLabel.setText("Debug Mode: OFF - Enemy ships hidden.");
        }
    }

    // ------------------------------- REINICIO ----------------------------------

    @FXML
    public void onRestartGame() {
        stopBackgroundMusic();
        try {
            isGameRunning = false;
            com.battleship.Main.showGameWindow(playerNickname, null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
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
        try {
            var url = getClass().getResource("/com/battleship/assets/backgroundmusic.wav");
            if (url == null) {
                System.err.println("Background music file not found");
                return;
            }
            Media media = new Media(url.toExternalForm());
            backgroundMusicPlayer = new MediaPlayer(media);
            backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            backgroundMusicPlayer.setVolume(0.3); // volumen moderado
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.play();
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
        }
    }

    // ------------------------ BOTONES DE SELECCIÓN DE BARCO -------------------

    @FXML
    public void onSelectCarrier() {
        selectShipType(Ship.Type.AIRCRAFT_CARRIER);
    }

    @FXML
    public void onSelectSubmarine() {
        selectShipType(Ship.Type.SUBMARINE);
    }

    @FXML
    public void onSelectDestroyer() {
        selectShipType(Ship.Type.DESTROYER);
    }

    @FXML
    public void onSelectFrigate() {
        selectShipType(Ship.Type.FRIGATE);
    }
}
