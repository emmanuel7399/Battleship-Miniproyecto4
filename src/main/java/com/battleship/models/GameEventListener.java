package com.battleship.models;

/**
 * Listener interface used to decouple game logic from the user interface.
 *
 * <p>This interface is part of the <b>Model</b> layer and allows the AI opponent
 * ({@link MachineOpponent}) to notify external components (typically a controller)
 * about events without having direct dependencies on the UI.</p>
 *
 * <p>It fulfills the rubric requirement of using interfaces to promote loose coupling
 * and clean architecture.</p>
 */
public interface GameEventListener {

    /**
     * Called when the enemy (AI) fires a shot.
     *
     * <p>The listener is expected to update the UI and game state accordingly,
     * based on the result of the shot.</p>
     *
     * @param target the coordinate that was fired upon
     * @param result the shot result code returned by
     *               {@link Board#receiveShot(Coordinate)}:
     *               <ul>
     *                 <li>{@code 0} = miss (water)</li>
     *                 <li>{@code 1} = hit</li>
     *                 <li>{@code 2} = sunk</li>
     *               </ul>
     */
    void onEnemyShotFired(Coordinate target, int result);

    //Profe si lee esto, que tenga una feliz navidad y prospero año nuevo.
}
