package com.battleship.models;

// Interfaz para cumplir con el requisito de la rúbrica
// y desacoplar la lógica de la máquina de la interfaz gráfica.
public interface GameEventListener {
    void onEnemyShotFired(Coordinate target, int result);
}