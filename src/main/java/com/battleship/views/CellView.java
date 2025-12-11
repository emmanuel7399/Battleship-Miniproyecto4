package com.battleship.views;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;

public class CellView extends StackPane {
    private final int row;
    private final int col;
    private Rectangle background;

    public CellView(int row, int col) {
        this.row = row;
        this.col = col;

        // Tamaño de celda
        background = new Rectangle(30, 30);

        // Efecto de agua con degradado suave
        Stop[] stops = new Stop[] { new Stop(0, Color.web("#4facfe")), new Stop(1, Color.web("#00f2fe")) };
        LinearGradient waterGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);

        background.setFill(Color.TRANSPARENT); // Lo haremos transparente para ver el fondo o usar estilo
        background.setStroke(Color.web("rgba(255,255,255,0.3)")); // Borde sutil
        background.setStrokeWidth(0.5);

        // Estilo base CSS
        this.setStyle("-fx-background-color: rgba(30, 144, 255, 0.2); -fx-border-color: rgba(255,255,255,0.1);");

        getChildren().add(background);
    }

    public void markAsWater() {
        // Círculo translúcido azul oscuro para representar "agua/fallo"
        Circle miss = new Circle(8);
        miss.setFill(Color.web("#1a2a6c"));
        miss.setOpacity(0.6);
        getChildren().add(miss);
    }

    public void markAsHit() {
        // Explosión: Degradado radial de amarillo a rojo
        RadialGradient explosionGradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.YELLOW),
                new Stop(0.5, Color.ORANGE),
                new Stop(1, Color.RED)
        );

        Circle explosion = new Circle(10);
        explosion.setFill(explosionGradient);
        explosion.setEffect(new DropShadow(10, Color.RED)); // Resplandor
        getChildren().add(explosion);
    }

    public void markAsSunk() {
        // Marca de hundido: Calavera o círculo negro/rojo oscuro intenso
        RadialGradient sunkGradient = new RadialGradient(
                0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.DARKRED),
                new Stop(1, Color.BLACK)
        );

        Circle sunk = new Circle(12);
        sunk.setFill(sunkGradient);
        sunk.setStroke(Color.WHITE);
        sunk.setStrokeWidth(1);
        sunk.setEffect(new DropShadow(15, Color.BLACK));

        // Limpiamos la celda antes de poner la marca definitiva
        getChildren().clear();
        getChildren().addAll(background, sunk);
    }

    public void markAsShip() {
        // Barco: Gris metálico con sombra interior para volumen
        background.setFill(Color.GRAY);
        InnerShadow metalEffect = new InnerShadow(5, Color.BLACK);
        background.setEffect(metalEffect);
    }

    // Nuevo método para ocultar el barco (revertir visualmente a agua)
    public void hideShip() {
        background.setFill(Color.TRANSPARENT); // Vuelve al color base (agua)
        background.setEffect(null); // Quita el efecto metálico
    }

    public void reset() {
        getChildren().clear();
        background.setFill(Color.TRANSPARENT);
        background.setEffect(null);
        this.setStyle("-fx-background-color: rgba(30, 144, 255, 0.2); -fx-border-color: rgba(255,255,255,0.1);");
        getChildren().add(background);
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}