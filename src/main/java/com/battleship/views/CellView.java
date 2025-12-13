package com.battleship.views;

import javafx.geometry.Pos;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Visual representation of a single cell on the Battleship board.
 *
 * <p>This class belongs to the <b>View</b> layer of the MVC architecture.
 * It is responsible only for rendering visual states of a cell and does not
 * contain any game logic.</p>
 *
 * <p>Each {@code CellView} corresponds to a logical coordinate
 * ({@code row}, {@code col}) in the game board model.</p>
 */
public class CellView extends StackPane {

    /** Row index of this cell in the board model. */
    private final int row;

    /** Column index of this cell in the board model. */
    private final int col;

    /** Background rectangle used to display the visual state of the cell. */
    private Rectangle background;

    /**
     * Creates a new visual cell for the given board coordinates.
     *
     * @param row the row index in the board model
     * @param col the column index in the board model
     */
    public CellView(int row, int col) {
        this.row = row;
        this.col = col;

        setPrefSize(40, 40);
        setAlignment(Pos.CENTER);

        background = new Rectangle(40, 40);
        background.setArcWidth(6);
        background.setArcHeight(6);

        // Default state: water (no ship, no shot)
        background.setFill(Color.web("#ffffff20"));
        background.setStroke(Color.web("#ffffff15"));
        background.setStrokeWidth(1.2);

        getChildren().add(background);
    }

    // ===========================
    //  GETTERS
    // ===========================

    /**
     * Returns the row index of this cell.
     *
     * @return row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the column index of this cell.
     *
     * @return column index
     */
    public int getCol() {
        return col;
    }

    // ===========================
    //  VISUAL STATES
    // ===========================

    /**
     * Marks this cell as containing a ship.
     *
     * <p>This visual state is mainly used for debug mode or as a fallback
     * when ship sprites are not displayed.</p>
     */
    public void markAsShip() {
        background.setFill(Color.GRAY);
        InnerShadow metalEffect = new InnerShadow(6, Color.BLACK);
        background.setEffect(metalEffect);
    }

    /**
     * Marks this cell as water with no shot fired.
     */
    public void markAsWater() {
        background.setFill(Color.web("#4fc3f720"));
        background.setEffect(null);
    }

    /**
     * Marks this cell as a missed shot (water that has been fired upon).
     */
    public void markAsMiss() {
        background.setFill(Color.web("#4fc3f780")); // slightly stronger water color
        background.setEffect(null);
    }

    /**
     * Marks this cell as a hit ship segment.
     */
    public void markAsHit() {
        background.setFill(Color.ORANGERED);
        InnerShadow effect = new InnerShadow(10, Color.DARKRED);
        background.setEffect(effect);
    }

    /**
     * Marks this cell as part of a sunk ship.
     */
    public void markAsSunk() {
        background.setFill(Color.DARKRED);
        InnerShadow effect = new InnerShadow(10, Color.BLACK);
        background.setEffect(effect);
    }

    /**
     * Enables or disables placement preview mode for this cell.
     *
     * <p>This is used during the ship placement phase to visually indicate
     * where a ship would be placed before confirming the action.</p>
     *
     * @param active {@code true} to enable preview highlighting,
     *               {@code false} to restore the default water state
     */
    public void setPreview(boolean active) {
        if (active) {
            background.setFill(Color.web("#3498db55")); // blue preview overlay
            background.setEffect(null);
        } else {
            // Restore default water appearance
            background.setFill(Color.web("#ffffff20"));
            background.setEffect(null);
        }
    }

    /**
     * Resets the cell to its default visual state.
     *
     * <p>Used when restarting the game or re-rendering the board.</p>
     */
    public void reset() {
        background.setFill(Color.web("#ffffff20"));
        background.setEffect(null);
    }
}
