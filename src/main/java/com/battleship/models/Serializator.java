package com.battleship.models;

import java.io.*;

/**
 * Utility class responsible for saving and loading the game state.
 *
 * <p>This class belongs to the <b>Model</b> layer and provides persistence
 * functionality using Java serialization and a complementary plain text file.</p>
 *
 * <p>Two files are used:</p>
 * <ul>
 *   <li><b>battleship_data.ser</b>: stores serialized Java objects
 *       ({@link Board} instances and elapsed time).</li>
 *   <li><b>battleship_status.txt</b>: stores human-readable metadata
 *       such as nickname and elapsed time.</li>
 * </ul>
 *
 * <p>This dual approach allows:</p>
 * <ul>
 *   <li>Reliable restoration of complex objects via serialization.</li>
 *   <li>Easy inspection/debugging of basic game information.</li>
 * </ul>
 */
public class Serializator {

    /** Binary file used to store serialized game objects. */
    private static final String SERIAL_FILE = "battleship_data.ser";

    /** Text file used to store human-readable game metadata. */
    private static final String TEXT_FILE = "battleship_status.txt";

    /**
     * Saves the current game state to disk.
     *
     * <p>The method performs two actions:</p>
     * <ol>
     *   <li>Serializes the player board, enemy board, and elapsed time
     *       into {@value #SERIAL_FILE}.</li>
     *   <li>Writes a plain text summary (nickname, shots fired, elapsed time)
     *       into {@value #TEXT_FILE}.</li>
     * </ol>
     *
     * @param playerBoard the player's board
     * @param enemyBoard  the enemy's board
     * @param nickname    the player's nickname
     * @param timeSeconds elapsed game time in seconds
     * @return {@code true} if the save operation completed successfully;
     *         {@code false} otherwise
     */
    public static boolean saveGame(Board playerBoard,
                                   Board enemyBoard,
                                   String nickname,
                                   int timeSeconds) {
        try {
            // 1) SERIALIZATION: store complex objects
            try (ObjectOutputStream oos =
                         new ObjectOutputStream(new FileOutputStream(SERIAL_FILE))) {
                oos.writeObject(playerBoard);
                oos.writeObject(enemyBoard);
                oos.writeInt(timeSeconds); // store elapsed time
            }

            // 2) PLAIN TEXT FILE: store readable metadata
            try (BufferedWriter writer =
                         new BufferedWriter(new FileWriter(TEXT_FILE))) {
                writer.write("Nickname: " + nickname);
                writer.newLine();
                writer.write("Player_Sunk_Ships: " + playerBoard.getShotsFired().size());
                writer.newLine();
                writer.write("Time_Elapsed_Seconds: " + timeSeconds);
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads a previously saved game state from disk.
     *
     * <p>The method attempts to:</p>
     * <ol>
     *   <li>Deserialize the player board, enemy board, and elapsed time
     *       from {@value #SERIAL_FILE}.</li>
     *   <li>Read the player's nickname from {@value #TEXT_FILE}.</li>
     * </ol>
     *
     * @return a {@link GameDTO} containing the restored game state,
     *         or {@code null} if loading fails or files are missing/corrupted
     */
    public static GameDTO loadGame() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(SERIAL_FILE))) {

            Board playerBoard = (Board) ois.readObject();
            Board enemyBoard = (Board) ois.readObject();
            int savedTime = ois.readInt(); // restore elapsed time

            String nickname = "Unknown";
            try (BufferedReader reader =
                         new BufferedReader(new FileReader(TEXT_FILE))) {
                String line = reader.readLine();
                if (line != null && line.startsWith("Nickname: ")) {
                    nickname = line.substring(10);
                }
            }

            return new GameDTO(playerBoard, enemyBoard, nickname, savedTime);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
