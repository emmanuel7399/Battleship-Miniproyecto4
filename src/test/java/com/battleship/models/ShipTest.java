package com.battleship.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipTest {

    @Test
    void shipStartsWithFullHealth() {
        Ship carrier = new Ship(Ship.Type.AIRCRAFT_CARRIER);
        assertEquals(4, carrier.getHealth());
        assertFalse(carrier.isSunk());
    }

    @Test
    void hitDecreasesHealthUntilZero() {
        Ship destroyer = new Ship(Ship.Type.DESTROYER);
        assertEquals(2, destroyer.getHealth());

        destroyer.hit();
        assertEquals(1, destroyer.getHealth());
        assertFalse(destroyer.isSunk());

        destroyer.hit();
        assertEquals(0, destroyer.getHealth());
        assertTrue(destroyer.isSunk());
    }

    @Test
    void healthDoesNotGoBelowZero() {
        Ship frigate = new Ship(Ship.Type.FRIGATE);
        assertEquals(1, frigate.getHealth());

        frigate.hit();
        frigate.hit(); // extra hit should not make it negative
        assertEquals(0, frigate.getHealth());
        assertTrue(frigate.isSunk());
    }

    @Test
    void addCoordinateStoresPositions() {
        Ship sub = new Ship(Ship.Type.SUBMARINE);
        sub.addCoordinate(new Coordinate(2, 3));
        sub.addCoordinate(new Coordinate(2, 4));

        assertEquals(2, sub.getPositions().size());
        assertEquals(2, sub.getPositions().get(0).getRow());
        assertEquals(3, sub.getPositions().get(0).getCol());
    }
}
