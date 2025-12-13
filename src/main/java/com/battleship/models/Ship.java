package com.battleship.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a ship in the Battleship game.
 *
 * <p>This class belongs to the <b>Model</b> layer of the MVC architecture and
 * encapsulates the state and behavior of a ship, including its type, occupied
 * coordinates, and remaining health.</p>
 *
 * <p>Each ship is composed of one or more {@link Coordinate} instances, and its
 * health is initialized based on its {@link Type} size.</p>
 */
public class Ship implements Serializable {

    /** Serialization identifier for save/load support. */
    private static final long serialVersionUID = 1L;

    /**
     * Enumeration of ship types and their corresponding sizes.
     *
     * <p>Ship sizes are defined according to the project specifications:</p>
     * <ul>
     *   <li>{@code AIRCRAFT_CARRIER}: size 4</li>
     *   <li>{@code SUBMARINE}: size 3</li>
     *   <li>{@code DESTROYER}: size 2</li>
     *   <li>{@code FRIGATE}: size 1</li>
     * </ul>
     */
    public enum Type {

        /** Aircraft carrier (size 4). */
        AIRCRAFT_CARRIER(4),

        /** Submarine (size 3). */
        SUBMARINE(3),

        /** Destroyer (size 2). */
        DESTROYER(2),

        /** Frigate (size 1). */
        FRIGATE(1);

        /** Number of cells occupied by this ship type. */
        private final int size;

        /**
         * Creates a ship type with the specified size.
         *
         * @param size number of cells the ship occupies
         */
        Type(int size) {
            this.size = size;
        }

        /**
         * Returns the size of the ship type.
         *
         * @return ship size in cells
         */
        public int getSize() {
            return size;
        }
    }

    /** Type of this ship. */
    private Type type;

    /** List of coordinates occupied by this ship on the board. */
    private List<Coordinate> positions;

    /** Remaining undamaged parts of the ship. */
    private int health;

    /**
     * Creates a new ship of the given type.
     *
     * <p>The ship's health is initialized to the size of its type.</p>
     *
     * @param type ship type
     */
    public Ship(Type type) {
        this.type = type;
        this.positions = new ArrayList<>();
        this.health = type.getSize();
    }

    /**
     * Adds a coordinate to the list of positions occupied by this ship.
     *
     * <p>This method is typically called when placing the ship on the board.</p>
     *
     * @param coord coordinate occupied by the ship
     */
    public void addCoordinate(Coordinate coord) {
        positions.add(coord);
    }

    /**
     * Registers a hit on the ship.
     *
     * <p>Decreases the ship's health by one, as long as health is greater than zero.</p>
     */
    public void hit() {
        if (health > 0) {
            health--;
        }
    }

    /**
     * Checks whether the ship has been sunk.
     *
     * @return {@code true} if the ship's health is zero; {@code false} otherwise
     */
    public boolean isSunk() {
        return health == 0;
    }

    /**
     * Returns the ship type.
     *
     * @return ship type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the list of coordinates occupied by the ship.
     *
     * @return list of ship coordinates
     */
    public List<Coordinate> getPositions() {
        return positions;
    }

    /**
     * Returns the current health of the ship.
     *
     * @return remaining undamaged parts
     */
    public int getHealth() {
        return health;
    }
}
