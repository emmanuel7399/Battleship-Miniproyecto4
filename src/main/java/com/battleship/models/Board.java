package com.battleship.models;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a 10x10 Battleship board.
 *
 * <p>This class belongs to the <b>Model</b> layer of the MVC architecture and stores:</p>
 * <ul>
 *   <li>The ship placement grid (mapping coordinates to ships).</li>
 *   <li>The set of shots that have been fired on this board.</li>
 *   <li>A stack of successful hit coordinates (useful for AI strategies).</li>
 *   <li>The list of ships (fleet) placed on the board.</li>
 *   <li>A counter for how many ships have been sunk.</li>
 * </ul>
 *
 * <p>The board uses multiple data structures as required by the assignment:</p>
 * <ul>
 *   <li>{@link Map} ({@link HashMap}) for the coordinate-to-ship grid.</li>
 *   <li>{@link Set} ({@link HashSet}) for shot tracking.</li>
 *   <li>{@link Stack} for storing successful hit coordinates.</li>
 *   <li>{@link List} ({@link ArrayList}) for the fleet.</li>
 * </ul>
 */
public class Board implements Serializable {

    /** Serialization identifier for save/load support. */
    private static final long serialVersionUID = 1L;

    /**
     * Grid mapping each occupied coordinate to the ship that occupies it.
     * If a coordinate is not present, it is empty water.
     */
    private Map<Coordinate, Ship> grid;

    /**
     * Set of all coordinates that have been fired upon on this board.
     * Used to prevent repeated shots.
     */
    private Set<Coordinate> shotsFired;

    /**
     * Stack of coordinates where a shot successfully hit a ship.
     * This can be leveraged by AI logic to improve targeting.
     */
    private Stack<Coordinate> successfulHits;

    /** List of ships currently placed on this board. */
    private List<Ship> fleet;

    /** Number of ships that have been sunk on this board. */
    private int sunkShipsCount;

    /**
     * Creates an empty board with no ships placed and no shots fired.
     */
    public Board() {
        this.grid = new HashMap<>();
        this.shotsFired = new HashSet<>();
        this.successfulHits = new Stack<>();
        this.fleet = new ArrayList<>();
        this.sunkShipsCount = 0;
    }

    /**
     * Attempts to place a ship starting at the given coordinate.
     *
     * <p>The ship will occupy {@code ship.getType().getSize()} consecutive cells either
     * horizontally (to the right) or vertically (downwards), depending on {@code isHorizontal}.</p>
     *
     * <p>Placement fails if any proposed coordinate is out of bounds or already occupied.</p>
     *
     * @param ship         the ship to place
     * @param start        starting coordinate (top-left-most segment of the ship)
     * @param isHorizontal {@code true} for horizontal placement; {@code false} for vertical placement
     * @return {@code true} if the ship was placed successfully; {@code false} otherwise
     */
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

        // Commit placement
        for (Coordinate c : proposedCoords) {
            grid.put(c, ship);
            ship.addCoordinate(c);
        }
        fleet.add(ship);
        return true;
    }

    /**
     * Randomly places the standard fleet configuration on the board.
     *
     * <p>This method is typically used to set up the enemy board (AI side).
     * It repeatedly tries random starting positions and orientations until each
     * ship is successfully placed.</p>
     */
    public void placeShipsRandomly() {
        Random random = new Random();

        // Standard fleet configuration:
        // 1 Carrier, 2 Submarines, 3 Destroyers, 4 Frigates
        List<Ship.Type> shipsToDistribute = new ArrayList<>();
        shipsToDistribute.add(Ship.Type.AIRCRAFT_CARRIER);
        shipsToDistribute.add(Ship.Type.SUBMARINE);
        shipsToDistribute.add(Ship.Type.SUBMARINE);

        shipsToDistribute.add(Ship.Type.DESTROYER);
        shipsToDistribute.add(Ship.Type.DESTROYER);
        shipsToDistribute.add(Ship.Type.DESTROYER);

        shipsToDistribute.add(Ship.Type.FRIGATE);
        shipsToDistribute.add(Ship.Type.FRIGATE);
        shipsToDistribute.add(Ship.Type.FRIGATE);
        shipsToDistribute.add(Ship.Type.FRIGATE);

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

    /**
     * Processes a shot fired at the given coordinate.
     *
     * <p>Return codes:</p>
     * <ul>
     *   <li>{@code -1}: invalid coordinate or coordinate already fired upon</li>
     *   <li>{@code 0}: miss (water)</li>
     *   <li>{@code 1}: hit (ship was hit but not sunk)</li>
     *   <li>{@code 2}: sunk (ship health reached zero)</li>
     * </ul>
     *
     * @param c coordinate being targeted
     * @return shot result code (-1, 0, 1, or 2)
     */
    public int receiveShot(Coordinate c) {
        if (!isValidCoordinate(c) || shotsFired.contains(c)) {
            return -1; // invalid or repeated
        }

        shotsFired.add(c);

        if (grid.containsKey(c)) {
            Ship ship = grid.get(c);
            ship.hit();
            successfulHits.push(c);

            if (ship.isSunk()) {
                sunkShipsCount++;
                return 2; // sunk
            }
            return 1; // hit
        }

        return 0; // miss (water)
    }

    /**
     * Checks whether a coordinate is inside the 10x10 board boundaries.
     *
     * @param c coordinate to validate
     * @return {@code true} if the coordinate is valid; {@code false} otherwise
     */
    private boolean isValidCoordinate(Coordinate c) {
        return c.getRow() >= 0 && c.getRow() < 10 && c.getCol() >= 0 && c.getCol() < 10;
    }

    /**
     * Checks whether all ships on this board have been sunk.
     *
     * @return {@code true} if all ships are sunk and at least one ship exists; {@code false} otherwise
     */
    public boolean allShipsSunk() {
        return sunkShipsCount == fleet.size() && !fleet.isEmpty();
    }

    // ===========================
    // Getters
    // ===========================

    /**
     * Returns the set of coordinates that have been fired upon.
     *
     * @return shots fired set
     */
    public Set<Coordinate> getShotsFired() {
        return shotsFired;
    }

    /**
     * Returns the coordinate-to-ship grid mapping.
     *
     * @return grid map
     */
    public Map<Coordinate, Ship> getGrid() {
        return grid;
    }

    /**
     * Returns the list of ships placed on this board.
     *
     * @return fleet list
     */
    public List<Ship> getFleet() {
        return fleet;
    }
}
