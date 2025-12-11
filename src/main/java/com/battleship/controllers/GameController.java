package com.battleship.controllers;

import com.battleship.models.*;
import com.battleship.views.BoardView;
import com.battleship.views.CellView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
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

    private int elapsedSeconds = 0; // <--- Variable para controlar el tiempo real

    private List<Ship.Type> shipsToPlace;
    private int currentShipIndex = 0;

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
    }

    // --- CARGAR PARTIDA GUARDADA ---
    public void loadSavedGame(GameDTO data) {
        this.playerBoard = data.getPlayerBoard();
        this.enemyBoard = data.getEnemyBoard();
        this.playerNickname = data.getNickname();
        this.elapsedSeconds = data.getElapsedSeconds(); // <--- RECUPERAMOS EL TIEMPO

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
        startTimerThread(); // El hilo iniciará desde el 'elapsedSeconds' cargado
    }

    private void renderBoardFromModel(Board board, BoardView view, boolean showShips) {
        // 1. Pintar Barcos (si corresponde)
        Map<Coordinate, Ship> grid = board.getGrid();
        if (showShips) {
            for (Coordinate coord : grid.keySet()) {
                view.getCell(coord.getRow(), coord.getCol()).markAsShip();
            }
        }

        // 2. Pintar Disparos (Agua, Tocado, Hundido)
        for (Coordinate shot : board.getShotsFired()) {
            CellView cell = view.getCell(shot.getRow(), shot.getCol());
            if (grid.containsKey(shot)) {
                // Había un barco
                Ship ship = grid.get(shot);
                if (ship.isSunk()) {
                    cell.markAsSunk();
                } else {
                    cell.markAsHit();
                }
            } else {
                // Era agua
                cell.markAsWater();
            }
        }
    }
    // ---------------------------------

    // --- AUTO-GUARDADO ---
    private void saveGameStatus() {
        // Necesitamos una variable final o efectiva para el lambda
        final int timeToSave = elapsedSeconds;
        new Thread(() -> {
            Serializator.saveGame(playerBoard, enemyBoard, playerNickname, timeToSave);
            System.out.println("Game Auto-Saved.");
        }).start();
    }
    // ---------------------

    private class MachineHandler implements GameEventListener {
        @Override
        public void onEnemyShotFired(Coordinate target, int result) {
            Platform.runLater(() -> {
                CellView cell = playerBoardView.getCell(target.getRow(), target.getCol());

                if (result == 0) { // AGUA
                    cell.markAsWater();
                    updateStatus("Enemy missed! It's YOUR turn.");
                    isMyTurn = true;
                    saveGameStatus(); // <--- GUARDAR AL TERMINAR TURNO MÁQUINA

                } else if (result == 1) { // TOCADO
                    cell.markAsHit();
                    updateStatus("Enemy HIT your ship! Enemy shoots again...");
                    saveGameStatus(); // <--- GUARDAR TRAS IMPACTO

                } else if (result == 2) { // HUNDIDO
                    cell.markAsHit();
                    Ship sunkShip = playerBoard.getGrid().get(target);
                    revealSunkShip(playerBoardView, sunkShip);
                    updateStatus("Enemy SUNK your ship! Enemy shoots again...");
                    saveGameStatus(); // <--- GUARDAR TRAS HUNDIR

                    if (playerBoard.allShipsSunk()) {
                        handleGameOver(false);
                    }
                }
            });
        }
    }

    private void startTimerThread() {
        isGameRunning = true;
        Thread timerThread = new Thread(() -> {
            // Ya no usamos System.currentTimeMillis para calcular la diferencia,
            // sino que incrementamos la variable acumulativa.
            while (isGameRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                elapsedSeconds++; // <--- Incrementamos el contador

                long minutes = elapsedSeconds / 60;
                long seconds = elapsedSeconds % 60;
                String timeStr = String.format("Time: %02d:%02d", minutes, seconds);

                Platform.runLater(() -> timerLabel.setText(timeStr));
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void startEnemyTurnThread() {
        MachineOpponent opponentAI = new MachineOpponent(playerBoard, new MachineHandler());
        Thread enemyThread = new Thread(opponentAI);
        enemyThread.setDaemon(true);
        enemyThread.start();
    }

    private void handleGameOver(boolean playerWon) {
        isGameRunning = false;
        Alert alert = new Alert(playerWon ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle("GAME OVER");
        alert.setHeaderText(playerWon ? "VICTORY!" : "DEFEAT");
        alert.setContentText(playerWon ? "Congratulations Admiral " + playerNickname : "Your fleet was destroyed.");
        alert.showAndWait();
        btnRestart.setVisible(true);
    }

    private void initShipsToPlace() {
        shipsToPlace = new ArrayList<>();
        shipsToPlace.add(Ship.Type.AIRCRAFT_CARRIER);
        shipsToPlace.add(Ship.Type.SUBMARINE); shipsToPlace.add(Ship.Type.SUBMARINE);
        shipsToPlace.add(Ship.Type.DESTROYER); shipsToPlace.add(Ship.Type.DESTROYER); shipsToPlace.add(Ship.Type.DESTROYER);
        shipsToPlace.add(Ship.Type.FRIGATE); shipsToPlace.add(Ship.Type.FRIGATE); shipsToPlace.add(Ship.Type.FRIGATE); shipsToPlace.add(Ship.Type.FRIGATE);
    }

    private void setupPlacementEvents() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                CellView cell = playerBoardView.getCell(row, col);
                cell.setOnMouseClicked(event -> handlePlacementClick(cell.getRow(), cell.getCol()));
            }
        }
    }

    private void handlePlacementClick(int row, int col) {
        if (!isPlacingShips) return;

        Ship.Type typeToPlace = shipsToPlace.get(currentShipIndex);
        Ship newShip = new Ship(typeToPlace);
        Coordinate start = new Coordinate(row, col);

        boolean success = playerBoard.placeShip(newShip, start, isHorizontal);

        if (success) {
            updateBoardView(playerBoardView, newShip, true);
            currentShipIndex++;
            if (currentShipIndex >= shipsToPlace.size()) {
                finishPlacementPhase();
            } else {
                updateStatus("Placed! Next: " + getCurrentShipName());
            }
        } else {
            statusLabel.setText("Invalid position! Try again.");
        }
    }

    private void updateBoardView(BoardView view, Ship ship, boolean showShip) {
        for (Coordinate c : ship.getPositions()) {
            CellView cell = view.getCell(c.getRow(), c.getCol());
            if (showShip) cell.markAsShip();
        }
    }

    private void revealSunkShip(BoardView view, Ship ship) {
        for (Coordinate c : ship.getPositions()) {
            CellView cell = view.getCell(c.getRow(), c.getCol());
            cell.markAsSunk();
        }
    }

    private void finishPlacementPhase() {
        isPlacingShips = false;
        statusLabel.setText("All ships placed! Press 'Start Battle' to begin.");
        btnStart.setDisable(false);
    }

    @FXML
    public void onRotateClick() {
        isHorizontal = !isHorizontal;
        orientationLabel.setText("Current: " + (isHorizontal ? "HORIZONTAL" : "VERTICAL"));
    }

    @FXML
    public void onStartGame() {
        btnStart.setDisable(true);
        enemyBoard.placeShipsRandomly();
        isMyTurn = true;
        updateStatus("Battle Started! It's your turn.");
        setupBattleEvents();
        startTimerThread();
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
            saveGameStatus(); // <--- GUARDAR JUGADA HUMANO

            isMyTurn = false;
            startEnemyTurnThread();

        } else if (result == 1) { // TOCADO
            cell.markAsHit();
            updateStatus("HIT! Shoot again!");
            saveGameStatus(); // <--- GUARDAR JUGADA HUMANO

        } else if (result == 2) { // HUNDIDO
            cell.markAsHit();
            Ship sunkShip = enemyBoard.getGrid().get(new Coordinate(row, col));
            revealSunkShip(enemyBoardView, sunkShip);
            updateStatus("SUNK! Shoot again!");
            saveGameStatus(); // <--- GUARDAR JUGADA HUMANO

            if (enemyBoard.allShipsSunk()) {
                handleGameOver(true);
            }
        }
    }

    @FXML
    public void onShowEnemyBoard() {
        // Alternamos el estado (si era true pasa a false, y viceversa)
        isDebugMode = !isDebugMode;

        // Recorremos la flota enemiga
        for (Ship ship : enemyBoard.getFleet()) {
            for (Coordinate c : ship.getPositions()) {
                CellView cell = enemyBoardView.getCell(c.getRow(), c.getCol());

                if (isDebugMode) {
                    // Si activamos el modo debug, mostramos el barco (Gris)
                    cell.markAsShip();
                } else {
                    // Si desactivamos, lo ocultamos (pero mantenemos los disparos si los hay)
                    cell.hideShip();
                }
            }
        }

        if (isDebugMode) {
            statusLabel.setText("Debug Mode: ON - Enemy ships revealed.");
        } else {
            statusLabel.setText("Debug Mode: OFF - Enemy ships hidden.");
        }
    }

    @FXML
    public void onRestartGame() {
        try {
            isGameRunning = false;
            // Para reiniciar, pasamos null como savedGame
            com.battleship.Main.showGameWindow(playerNickname, null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void setPlayerNickname(String nickname) {
        this.playerNickname = nickname;
        updateStatus("Welcome Admiral " + nickname + "! Place: " + getCurrentShipName());
    }

    private void updateStatus(String msg) {
        statusLabel.setText(msg);
    }

    private String getCurrentShipName() {
        if (currentShipIndex >= shipsToPlace.size()) return "None";
        Ship.Type type = shipsToPlace.get(currentShipIndex);
        return type.name() + " (Size: " + type.getSize() + ")";
    }
}