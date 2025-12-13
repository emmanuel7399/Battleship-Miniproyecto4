package com.battleship.models;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SerializatorTest {

    private static final File SERIAL_FILE = new File("battleship_data.ser");
    private static final File TEXT_FILE = new File("battleship_status.txt");

    @AfterEach
    void cleanup() {
        if (SERIAL_FILE.exists()) SERIAL_FILE.delete();
        if (TEXT_FILE.exists()) TEXT_FILE.delete();
    }

    @Test
    void saveAndLoadRestoresBoardsNicknameAndTime() {
        Board player = new Board();
        Board enemy = new Board();

        // Put something in the boards so we know they are not empty
        Ship frigate = new Ship(Ship.Type.FRIGATE);
        assertTrue(player.placeShip(frigate, new Coordinate(0, 0), true));
        enemy.placeShipsRandomly();

        boolean saved = Serializator.saveGame(player, enemy, "Daniel", 123);
        assertTrue(saved);
        assertTrue(SERIAL_FILE.exists());
        assertTrue(TEXT_FILE.exists());

        GameDTO loaded = Serializator.loadGame();
        assertNotNull(loaded);

        assertEquals("Daniel", loaded.getNickname());
        assertEquals(123, loaded.getElapsedSeconds());

        assertNotNull(loaded.getPlayerBoard());
        assertNotNull(loaded.getEnemyBoard());
        assertEquals(1, loaded.getPlayerBoard().getFleet().size());
        assertEquals(10, loaded.getEnemyBoard().getFleet().size());
    }
}
