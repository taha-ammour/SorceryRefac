package org.example.engine;

import org.example.game.Player;
import org.example.engine.utils.Logger;

/**
 * Central class that manages high-level game functionality
 */
public class GameEngine {
    private static Player localPlayer;
    private static boolean networkGame = false;

    /**
     * Get the local player
     */
    public static Player getLocalPlayer() {
        return localPlayer;
    }

    /**
     * Set the local player
     */
    public static void setLocalPlayer(Player player) {
        localPlayer = player;
    }

    /**
     * Check if this is a network game
     */
    public static boolean isNetworkGame() {
        return networkGame;
    }

    /**
     * Set network game status
     */
    public static void setNetworkGame(boolean isNetworkGame) {
        networkGame = isNetworkGame;
    }

    /**
     * Initialize the game engine
     */
    public static void initialize() {
        Logger.info("Initializing game engine");
    }

    /**
     * Shutdown the game engine
     */
    public static void shutdown() {
        Logger.info("Shutting down game engine");
    }
}