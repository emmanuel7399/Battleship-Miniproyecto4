package com.battleship.models;

import java.io.Serializable;
import java.util.*;

public class Board implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Coordinate, Ship> grid;
    private Set<Coordinate> shotsFired;
    private Stack<Coordinate> successfulHits;

    private List<Ship> fleet;
    private int sunkShipsCount;

    public Board() {
        this.grid = new HashMap<>();
        this.shotsFired = new HashSet<>();
        this.successfulHits = new Stack<>();
        this.fleet = new ArrayList<>();
        this.sunkShipsCount = 0;
    }

    public boolean placeShip(Ship ship, Coordinate start, boolean isHorizontal) {
        List<Coordinate> proposedCoords = new ArrayList<>();
        int row = start.getRow();
        int col = start.getCol();

        for (int i = 0; i < ship.getType().getSize(); i++) {
            Coordinate c;
            if (isHorizontal) {
                c = new Coordinate(row, col + i);
            } else {
                c = new Coordinate(row + i, col);
            }

            if (!isValidCoordinate(c) || grid.containsKey(c)) {
                return false;
            }
            proposedCoords.add(c);
        }

        for (Coordinate c : proposedCoords) {
            grid.put(c, ship);
            ship.addCoordinate(c);
        }
        fleet.add(ship);
        return true;
    }

    // NUEVO: Método para que la máquina coloque barcos al azar
    public void placeShipsRandomly() {
        Random random = new Random();
        // Lista estándar de barcos
        List<Ship.Type> shipsToDistribute = new ArrayList<>();
        shipsToDistribute.add(Ship.Type.AIRCRAFT_CARRIER);
        shipsToDistribute.add(Ship.Type.SUBMARINE); shipsToDistribute.add(Ship.Type.SUBMARINE);
        shipsToDistribute.add(Ship.Type.DESTROYER); shipsToDistribute.add(Ship.Type.DESTROYER); shipsToDistribute.add(Ship.Type.DESTROYER);
        shipsToDistribute.add(Ship.Type.FRIGATE); shipsToDistribute.add(Ship.Type.FRIGATE); shipsToDistribute.add(Ship.Type.FRIGATE); shipsToDistribute.add(Ship.Type.FRIGATE);

        for (Ship.Type type : shipsToDistribute) {
            boolean placed = false;
            while (!placed) {
                int row = random.nextInt(10);
                int col = random.nextInt(10);
                boolean horizontal = random.nextBoolean();

                Ship newShip = new Ship(type);
                placed = placeShip(newShip, new Coordinate(row, col), horizontal);
            }
        }
    }

    public int receiveShot(Coordinate c) {
        if (!isValidCoordinate(c) || shotsFired.contains(c)) {
            return -1; // Inválido o repetido
        }

        shotsFired.add(c);

        if (grid.containsKey(c)) {
            Ship ship = grid.get(c);
            ship.hit();
            successfulHits.push(c);

            if (ship.isSunk()) {
                sunkShipsCount++;
                return 2; // Hundido
            }
            return 1; // Tocado
        }

        return 0; // Agua
    }

    private boolean isValidCoordinate(Coordinate c) {
        return c.getRow() >= 0 && c.getRow() < 10 && c.getCol() >= 0 && c.getCol() < 10;
    }

    public boolean allShipsSunk() {
        return sunkShipsCount == fleet.size() && !fleet.isEmpty();
    }

    // Getters
    public Set<Coordinate> getShotsFired() { return shotsFired; }
    public Map<Coordinate, Ship> getGrid() { return grid; }
    public List<Ship> getFleet() { return fleet; }
}