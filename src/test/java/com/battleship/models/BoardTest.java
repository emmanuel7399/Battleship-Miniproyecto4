package com.battleship.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void placeShipFailsIfOutOfBoundsHorizontal() {
        Board board = new Board();
        Ship carrier = new Ship(Ship.Type.AIRCRAFT_CARRIER);

        // Start near the right edge: would overflow (col 8 + size 4)
        boolean placed = board.placeShip(carrier, new Coordinate(0, 8), true);
        assertFalse(placed);
        assertTrue(board.getFleet().isEmpty());
    }

    @Test
    void placeShipFailsIfOutOfBoundsVertical() {
        Board board = new Board();
        Ship sub = new Ship(Ship.Type.SUBMARINE);

        // Start near bottom edge: would overflow (row 9 + size 3)
        boolean placed = board.placeShip(sub, new Coordinate(9, 0), false);
        assertFalse(placed);
        assertTrue(board.getFleet().isEmpty());
    }

    @Test
    void placeShipSucceedsAndOccupiesAllCells() {
        Board board = new Board();
        Ship destroyer = new Ship(Ship.Type.DESTROYER);

        boolean placed = board.placeShip(destroyer, new Coordinate(4, 4), true);
        assertTrue(placed);
        assertEquals(1, board.getFleet().size());
        assertEquals(2, destroyer.getPositions().size());

        // Check both coordinates exist in grid
        assertTrue(board.getGrid().containsKey(new Coordinate(4, 4)));
        assertTrue(board.getGrid().containsKey(new Coordinate(4, 5)));
    }

    @Test
    void placeShipFailsIfOverlapping() {
        Board board = new Board();

        Ship destroyer = new Ship(Ship.Type.DESTROYER);
        assertTrue(board.placeShip(destroyer, new Coordinate(1, 1), true));

        Ship frigate = new Ship(Ship.Type.FRIGATE);
        // Overlaps with (1,1)
        assertFalse(board.placeShip(frigate, new Coordinate(1, 1), true));
        assertEquals(1, board.getFleet().size());
    }

    @Test
    void receiveShotReturnsMinusOneIfRepeated() {
        Board board = new Board();

        int first = board.receiveShot(new Coordinate(0, 0));
        int second = board.receiveShot(new Coordinate(0, 0));

        assertNotEquals(-1, first); // first is valid
        assertEquals(-1, second);   // repeated shot
    }

    @Test
    void receiveShotReturnsWaterIfNoShip() {
        Board board = new Board();
        int result = board.receiveShot(new Coordinate(5, 5));
        assertEquals(0, result);
        assertTrue(board.getShotsFired().contains(new Coordinate(5, 5)));
    }

    @Test
    void receiveShotReturnsHitAndThenSunk() {
        Board board = new Board();
        Ship frigate = new Ship(Ship.Type.FRIGATE);
        assertTrue(board.placeShip(frigate, new Coordinate(2, 2), true));

        int result = board.receiveShot(new Coordinate(2, 2));
        assertEquals(2, result, "A frigate should be sunk with one hit");
        assertTrue(frigate.isSunk());
        assertTrue(board.allShipsSunk());
    }

    @Test
    void allShipsSunkIsFalseWhenNoShips() {
        Board board = new Board();
        assertFalse(board.allShipsSunk());
    }

    @Test
    void placeShipsRandomlyCreatesStandardFleetSize() {
        Board board = new Board();
        board.placeShipsRandomly();

        // Standard fleet: 1 + 2 + 3 + 4 = 10 ships
        assertEquals(10, board.getFleet().size());

        // Basic sanity: every ship has positions matching its size
        for (Ship ship : board.getFleet()) {
            assertEquals(ship.getType().getSize(), ship.getPositions().size());
        }
    }
}
