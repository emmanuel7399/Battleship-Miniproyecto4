package com.battleship.views;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

public class BoardView extends VBox {
    private GridPane grid;
    private CellView[][] cells; // Matriz visual para acceso rápido
    private List<ImageView> shipSprites = new ArrayList<>();

    public BoardView(String title) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);

        // Título del tablero (ej: "My Fleet" / "Enemy Waters")
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
        // Cabeceras de columnas (A, B, C...)
        String[] cols = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        for (int i = 0; i < 10; i++) {
            Label label = new Label(cols[i]);
            label.setMinWidth(30);
            label.setAlignment(Pos.CENTER);
            grid.add(label, i + 1, 0); // Columna i+1, Fila 0
        }

        // Cabeceras de filas (1, 2, 3...)
        for (int i = 0; i < 10; i++) {
            Label label = new Label(String.valueOf(i + 1));
            label.setMinHeight(30);
            label.setMinWidth(20);
            label.setAlignment(Pos.CENTER_RIGHT);
            grid.add(label, 0, i + 1); // Columna 0, Fila i+1
        }

        // Crear las celdas (0–9 en modelo, pero 1–10 en la GridPane)
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

    public void clearShipSprites() {
        if (grid == null) return;
        for (ImageView iv : shipSprites) {
            grid.getChildren().remove(iv);
        }
        shipSprites.clear();
    }

    public void addShipSprite(int startRow, int startCol, int length, boolean horizontal, Image sprite) {
        if (sprite == null) return;

        ImageView imageView = new ImageView(sprite);
        imageView.setPreserveRatio(false); // lo estiramos al tamaño de celdas

        // Usamos el tamaño de una celda como referencia
        CellView cell0 = cells[0][0];

        if (horizontal) {
            imageView.fitHeightProperty().bind(cell0.heightProperty());
            imageView.fitWidthProperty().bind(cell0.widthProperty().multiply(length));
        } else {
            imageView.fitWidthProperty().bind(cell0.widthProperty());
            imageView.fitHeightProperty().bind(cell0.heightProperty().multiply(length));
        }

        grid.getChildren().add(imageView);

        // 👈 AQUÍ EL CAMBIO IMPORTANTE: +1 por las cabeceras
        GridPane.setRowIndex(imageView, startRow + 1);
        GridPane.setColumnIndex(imageView, startCol + 1);

        if (horizontal) {
            GridPane.setColumnSpan(imageView, length);
        } else {
            GridPane.setRowSpan(imageView, length);
        }

        shipSprites.add(imageView);
    }
}
