package com.battleship.views;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class BoardView extends VBox {
    private GridPane grid;
    private CellView[][] cells; // Matriz visual para acceso rápido

    public BoardView(String title) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);

        // Título del tablero (ej: "Tablero Principal")
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("board-title");
        this.getChildren().add(titleLabel);

        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        cells = new CellView[10][10];

        initializeBoard();
        this.getChildren().add(grid);
    }

    private void initializeBoard() {
        // Añadir cabeceras de columnas (A, B, C...)
        String[] cols = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        for (int i = 0; i < 10; i++) {
            Label label = new Label(cols[i]);
            label.setMinWidth(30);
            label.setAlignment(Pos.CENTER);
            grid.add(label, i + 1, 0); // Columna i+1, Fila 0
        }

        // Añadir cabeceras de filas (1, 2, 3...)
        for (int i = 0; i < 10; i++) {
            Label label = new Label(String.valueOf(i + 1));
            label.setMinHeight(30);
            label.setMinWidth(20);
            label.setAlignment(Pos.CENTER_RIGHT);
            grid.add(label, 0, i + 1); // Columna 0, Fila i+1
        }

        // Crear las celdas
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                CellView cell = new CellView(row, col);
                cells[row][col] = cell;
                grid.add(cell, col + 1, row + 1);
            }
        }
    }

    public CellView getCell(int row, int col) {
        if (row >= 0 && row < 10 && col >= 0 && col < 10) {
            return cells[row][col];
        }
        return null;
    }

    public GridPane getGridPane() {
        return grid;
    }
}