package com.battleship.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Ship implements Serializable {
    private static final long serialVersionUID = 1L;

    // Tipos de barcos y sus tamaños según enunciado
    public enum Type {
        AIRCRAFT_CARRIER(4),
        SUBMARINE(3),
        DESTROYER(2),
        FRIGATE(1);

        private final int size;
        Type(int size) { this.size = size; }
        public int getSize() { return size; }
    }

    private Type type;
    private List<Coordinate> positions; // Estructura 1: ArrayList
    private int health; // Cantidad de partes no dañadas

    public Ship(Type type) {
        this.type = type;
        this.positions = new ArrayList<>();
        this.health = type.getSize();
    }

    public void addCoordinate(Coordinate coord) {
        positions.add(coord);
    }

    public void hit() {
        if (health > 0) {
            health--;
        }
    }

    public boolean isSunk() {
        return health == 0;
    }

    public Type getType() { return type; }
    public List<Coordinate> getPositions() { return positions; }
    public int getHealth() { return health; }
}