package com.battleship.models;

import java.util.Random;

/**
 * Simple AI opponent that fires shots at a target {@link Board}.
 *
 * <p>This class belongs to the <b>Model</b> layer (game logic) and runs on its own thread.
 * It implements {@link Runnable} so it can be executed by a {@link Thread} from the controller.</p>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Selects random coordinates that have not been fired upon yet.</li>
 *   <li>Calls {@link Board#receiveShot(Coordinate)} to apply the shot to the model.</li>
 *   <li>Notifies a {@link GameEventListener} (typically the controller) with the shot result.</li>
 *   <li>Continues shooting if it hits or sinks a ship; stops when it misses (water).</li>
 * </ul>
 *
 * <p>Thread control:</p>
 * <ul>
 *   <li>The {@code running} flag is {@code volatile} to allow safe stop requests from other threads.</li>
 *   <li>The loop simulates "thinking time" using {@link Thread#sleep(long)}.</li>
 * </ul>
 */
public class MachineOpponent implements Runnable {

    /** Board that the AI will fire upon (usually the player's board). */
    private final Board targetBoard;

    /** Listener to be notified after each AI shot (typically the controller). */
    private final GameEventListener listener;

    /**
     * Flag used to stop the AI loop safely from another thread.
     * Marked {@code volatile} to ensure visibility across threads.
     */
    private volatile boolean running;

    /**
     * Creates a new AI opponent.
     *
     * @param targetBoard board that the AI will attack
     * @param listener    listener that will be notified after each shot (may be {@code null})
     */
    public MachineOpponent(Board targetBoard, GameEventListener listener) {
        this.targetBoard = targetBoard;
        this.listener = listener;
        this.running = true;
    }

    /**
     * Requests the AI loop to stop.
     *
     * <p>The AI will stop after the current iteration finishes.</p>
     */
    public void stop() {
        this.running = false;
    }

    /**
     * AI main loop.
     *
     * <p>The AI continues shooting while:</p>
     * <ul>
     *   <li>{@code running} is {@code true}, and</li>
     *   <li>the last shot was not a miss (result != 0).</li>
     * </ul>
     *
     * <p>Shot result codes come from {@link Board#receiveShot(Coordinate)}:</p>
     * <ul>
     *   <li>0 = miss (water)</li>
     *   <li>1 = hit</li>
     *   <li>2 = sunk</li>
     * </ul>
     */
    @Override
    public void run() {
        boolean keepShooting = true;
        Random random = new Random();

        while (keepShooting && running) {
            try {
                Thread.sleep(1500); // Simulate "thinking time"
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 1) Select a valid target coordinate (simple random AI)
            int row, col;
            Coordinate target;
            do {
                row = random.nextInt(10);
                col = random.nextInt(10);
                target = new Coordinate(row, col);
            } while (targetBoard.getShotsFired().contains(target));

            // 2) Fire at the model
            int result = targetBoard.receiveShot(target);

            // 3) Notify the listener (controller/UI layer)
            if (listener != null) {
                listener.onEnemyShotFired(target, result);
            }

            // 4) Decide whether the AI keeps shooting
            if (result == 0) {
                keepShooting = false; // Miss: AI ends its turn
            }
            // If result is 1 (hit) or 2 (sunk), the AI continues shooting
        }
    }
}
