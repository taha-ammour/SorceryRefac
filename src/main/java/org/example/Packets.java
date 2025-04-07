package org.example;

import org.joml.Vector3f;

import java.util.ArrayList;

/**
 * Contains all network packet classes used for game communications
 */
public class Packets {

    /**
     * Request to join a lobby
     */
    public static class JoinRequest {
        public String username;
        public String lobbyCode;
    }

    /**
     * Response to a join request
     */
    public static class JoinResponse {
        public boolean accepted;
        public String message;
        public ArrayList<String> currentPlayers;
    }

    /**
     * Chat message sent between players
     */
    public static class ChatMessage {
        public String username;
        public String message;
    }


    /**
     * Sent when a player joins the game
     */
    public static class PlayerJoin {
        public String playerId;
        public String username;
        public String color;
        public float x;
        public float y;
    }

    /**
     * Sent when a player disconnects from the game
     */
    public static class PlayerDisconnect {
        public String playerId;
    }

    /**
     * Sent when a player's position changes
     */
    public static class PlayerPositionUpdate {
        public String playerId;
        public float x;
        public float y;
        public int direction;
        public boolean isMoving;
        public String color;
        public boolean FlipX;
        public boolean FlipY;

        public int animationFrame = 0;
        public String currentLegSprite = "";
    }

    /**
     * Sent to update player state (health, alive status, etc.)
     */
    public static class PlayerStateUpdate {
        public String playerId;
        public int health;
        public int energy;
        public boolean isAlive;
    }

    /**
     * Sent when a game action occurs (attack, item use, etc.)
     */
    public static class GameAction {
        public String playerId;
        public int actionType;
        public String targetId;  // If applicable
        public float x;  // If applicable
        public float y;  // If applicable
        public int spellLevel; // Current level of the spell being cast
    }

    /**
     * Sent when player casts a spell
     */
    public static class SpellCast {
        public String playerId;
        public String spellType; // "fire", "ice", "lightning"
        public float x; // Cast position X
        public float y; // Cast position Y
        public int level; // Spell level
    }

    /**
     * Sent when player upgrades a spell
     */
    public static class SpellUpgrade {
        public String playerId;
        public String spellType;
        public int newLevel;
    }

    // Add to your Packets.java
    public static class VoicePacket {
        public String playerId;
        public byte[] audioData;
        public float x;  // Player position x
        public float y;  // Player position y
        public float z;  // Player position z
        public float timestamp;
    }



    /**
     * Server information for discovery
     */
    public static class ServerInfo {
        public String serverName;
        public String hostUsername;
        public int tcpPort;
        public int udpPort;
        public int playerCount;
    }

    /**
     * Sent when host changes (host migration)
     */
    public static class HostMigration {
        public String newHostId;
    }

    /**
     * Sent to request the list of active games
     */
    public static class ServerListRequest {
        // No fields needed
    }

    /**
     * Response containing the list of active games
     */
    public static class ServerListResponse {
        public ArrayList<ServerInfo> servers = new ArrayList<>();
    }

    /**
     * Sent to start a game session
     */
    public static class GameStart {
        public String gameMode;
        public int mapId;
    }

    /**
     * Sent when the game ends
     */
    public static class GameEnd {
        public String winnerId;
        public String reason;
    }
}