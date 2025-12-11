package com.battleship.models;

import java.util.Random;

public class MachineOpponent implements Runnable {
    private final Board targetBoard; // El tablero al que dispara (el del jugador)
    private final GameEventListener listener; // A quién avisar (el controlador)
    private volatile boolean running; // Control del hilo

    public MachineOpponent(Board targetBoard, GameEventListener listener) {
        this.targetBoard = targetBoard;
        this.listener = listener;
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        boolean keepShooting = true;
        Random random = new Random();

        while (keepShooting && running) {
            try {
                Thread.sleep(1500); // Simular pensamiento
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 1. Lógica de selección de disparo (IA simple)
            int row, col;
            Coordinate target;
            do {
                row = random.nextInt(10);
                col = random.nextInt(10);
                target = new Coordinate(row, col);
            } while (targetBoard.getShotsFired().contains(target));

            // 2. Disparo en el modelo
            int result = targetBoard.receiveShot(target);

            // 3. Notificar al listener (Controlador)
            if (listener != null) {
                listener.onEnemyShotFired(target, result);
            }

            // 4. Decidir si sigue disparando
            if (result == 0) {
                keepShooting = false; // Agua, cede el turno
            }
            // Si result es 1 o 2, keepShooting sigue true y el bucle se repite
        }
    }
}