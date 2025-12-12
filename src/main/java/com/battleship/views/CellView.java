package com.battleship.views;

import javafx.geometry.Pos;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class CellView extends StackPane {

    private final int row;
    private final int col;

    private Rectangle background;

    public CellView(int row, int col) {
        this.row = row;
        this.col = col;

        setPrefSize(40, 40);
        setAlignment(Pos.CENTER);

        background = new Rectangle(40, 40);
        background.setArcWidth(6);
        background.setArcHeight(6);

        // Agua por defecto
        background.setFill(Color.web("#ffffff20"));
        background.setStroke(Color.web("#ffffff15"));
        background.setStrokeWidth(1.2);

        getChildren().add(background);
    }

    // ===========================
    //  GETTERS
    // ===========================

    public int getRow() { return row; }
    public int getCol() { return col; }

    // ===========================
    //  ESTADOS VISUALES
    // ===========================

    /** Celda que pertenece a un barco (solo debug/fallback). */
    public void markAsShip() {
        background.setFill(Color.GRAY);
        InnerShadow metalEffect = new InnerShadow(6, Color.BLACK);
        background.setEffect(metalEffect);
    }

    /** Agua normal (no se ha disparado aquí). */
    public void markAsWater() {
        background.setFill(Color.web("#4fc3f720"));
        background.setEffect(null);
    }

    /** Agua donde se ha disparado (MISS). */
    public void markAsMiss() {
        background.setFill(Color.web("#4fc3f780")); // un poco más marcada
        background.setEffect(null);
    }

    /** Disparo que impactó un barco. */
    public void markAsHit() {
        background.setFill(Color.ORANGERED);
        InnerShadow effect = new InnerShadow(10, Color.DARKRED);
        background.setEffect(effect);
    }

    /** Parte hundida del barco. */
    public void markAsSunk() {
        background.setFill(Color.DARKRED);
        InnerShadow effect = new InnerShadow(10, Color.BLACK);
        background.setEffect(effect);
    }

    /** Preview de colocación (sombreado antes de hacer clic). */
    public void setPreview(boolean active) {
        if (active) {
            background.setFill(Color.web("#3498db55")); // azulito de preview
            background.setEffect(null);
        } else {
            // Volvemos al agua "normal" (estamos en fase de colocación, aún sin disparos)
            background.setFill(Color.web("#ffffff20"));
            background.setEffect(null);
        }
    }

    /** Limpia la celda (por si reinicias o redibujas tablero). */
    public void reset() {
        background.setFill(Color.web("#ffffff20"));
        background.setEffect(null);
    }
}
