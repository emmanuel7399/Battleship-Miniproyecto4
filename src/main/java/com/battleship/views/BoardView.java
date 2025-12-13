package com.battleship.views;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual representation of a Battleship board.
 *
 * <p>This class belongs to the <b>View</b> layer in the MVC architecture.
 * It is responsible for rendering the board grid, row/column headers, and
 * (optionally) ship sprites. It does not contain any game logic.</p>
 *
 * <p>The internal layout uses a {@link GridPane}:</p>
 * <ul>
 *   <li>Row 0 and column 0 are reserved for headers (A–J and 1–10).</li>
 *   <li>Actual playable cells are placed from (col + 1, row + 1).</li>
 * </ul>
 *
 * <p>Ship images are drawn as a single {@link ImageView} spanning multiple
 * cells rather than repeating
 * the same image in each cell.</p>
 */
public class BoardView extends VBox {

    /** Grid container that holds headers and the 10x10 board cells. */
    private GridPane grid;

    /** Matrix of cells for fast lookup using board coordinates (row, col). */
    private CellView[][] cells;

    /** List of ship image nodes currently added to the grid (for cleanup/redraw). */
    private List<ImageView> shipSprites = new ArrayList<>();

    /**
     * Creates a new board view with a title.
     *
     * @param title the board title (e.g., "My Fleet" or "Enemy Waters")
     */
    public BoardView(String title) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);

        // Board title label (e.g., "My Fleet" / "Enemy Waters")
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("board-title");
        this.getChildren().add(titleLabel);

        grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        cells = new CellView[10][10];

        initializeBoard();
        this.getChildren().add(grid);
    }

    /**
     * Builds the visual board:
     * <ul>
     *   <li>Adds column headers (A–J) at row 0.</li>
     *   <li>Adds row headers (1–10) at column 0.</li>
     *   <li>Creates and places the 10x10 {@link CellView} instances.</li>
     * </ul>
     *
     * <p>Note: cells use model coordinates (0–9), but are placed on the {@link GridPane}
     * at (col + 1, row + 1) because headers occupy the first row/column.</p>
     */
    private void initializeBoard() {
        // Column headers (A, B, C...)
        String[] cols = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        for (int i = 0; i < 10; i++) {
            Label label = new Label(cols[i]);
            label.setMinWidth(30);
            label.setAlignment(Pos.CENTER);
            grid.add(label, i + 1, 0); // column i+1, row 0 (header row)
        }

        // Row headers (1, 2, 3...)
        for (int i = 0; i < 10; i++) {
            Label label = new Label(String.valueOf(i + 1));
            label.setMinHeight(30);
            label.setMinWidth(20);
            label.setAlignment(Pos.CENTER_RIGHT);
            grid.add(label, 0, i + 1); // column 0, row i+1 (header column)
        }

        // Create and place the 10x10 cells
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                CellView cell = new CellView(row, col);
                cells[row][col] = cell;

                // Offset by +1 due to headers occupying row 0 and column 0
                grid.add(cell, col + 1, row + 1);
            }
        }
    }

    /**
     * Returns the {@link CellView} at the given board coordinates.
     *
     * @param row row index (0–9)
     * @param col column index (0–9)
     * @return the cell at (row, col), or {@code null} if coordinates are out of bounds
     */
    public CellView getCell(int row, int col) {
        if (row >= 0 && row < 10 && col >= 0 && col < 10) {
            return cells[row][col];
        }
        return null;
    }

    /**
     * Returns the underlying {@link GridPane}.
     *
     * <p>Useful if other view components need to apply layout configuration or
     * add additional overlays.</p>
     *
     * @return the grid pane used by this board view
     */
    public GridPane getGridPane() {
        return grid;
    }

    /**
     * Removes all ship sprite image nodes from the grid.
     *
     * <p>This is typically called before re-rendering the board to avoid
     * duplicated ship images.</p>
     */
    public void clearShipSprites() {
        if (grid == null) return;
        for (ImageView iv : shipSprites) {
            grid.getChildren().remove(iv);
        }
        shipSprites.clear();
    }

    /**
     * Adds a ship sprite to the board as a single image spanning multiple cells.
     *
     * <p>The sprite is positioned at the starting cell and spans {@code length}
     * cells horizontally or vertically. Because the {@link GridPane} uses row 0 and
     * column 0 for headers, the sprite is placed at {@code startRow + 1} and
     * {@code startCol + 1}.</p>
     *
     * @param startRow   starting row in model coordinates (0–9)
     * @param startCol   starting column in model coordinates (0–9)
     * @param length     ship length in cells
     * @param horizontal {@code true} if the ship is placed horizontally; {@code false} for vertical
     * @param sprite     image to display; if {@code null}, nothing is added
     */
    public void addShipSprite(int startRow, int startCol, int length, boolean horizontal, Image sprite) {
        if (sprite == null) return;

        ImageView imageView = new ImageView(sprite);
        imageView.setPreserveRatio(false); // stretch to cell dimensions for a clean grid fit

        // Use a reference cell to bind sprite size to the grid cell size
        CellView cell0 = cells[0][0];

        if (horizontal) {
            imageView.fitHeightProperty().bind(cell0.heightProperty());
            imageView.fitWidthProperty().bind(cell0.widthProperty().multiply(length));
        } else {
            imageView.fitWidthProperty().bind(cell0.widthProperty());
            imageView.fitHeightProperty().bind(cell0.heightProperty().multiply(length));
        }

        grid.getChildren().add(imageView);

        // IMPORTANT: +1 offset due to headers occupying row 0 and column 0
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
