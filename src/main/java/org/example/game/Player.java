package org.example.game;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import org.example.Packets;
import org.example.engine.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.glfw.GLFW.*;

public class Player extends GameObject {

    private final UUID playerId;
    private String username;
    private String color;
    private boolean isLocalPlayer;

    private LayeredCharacter character;
    private final SpriteManager spriteManager;

    private int health = 100;
    private int energy = 100;
    private int armor = 1;
    private boolean facingLeft = false;

    private boolean isAlive = true;

    private Vector3f position = new Vector3f(100, 200, 0);
    private float moveSpeed = 120.0f; // Faster movement speed
    private boolean isMoving = false;
    private Direction currentDirection = Direction.DOWN;

    private static Server server;
    private static Client client;

    private float networkTimer = 0f;
    private final float networkUpdateInterval = 0.05f; // More frequent updates for better responsiveness

    private Input input;
    private static final Map<UUID, Player> players = new ConcurrentHashMap<>();
    private boolean debug = true;

    // Player-specific sprite names to create unique copies
    private final String[] playerSpriteNames = new String[4]; // 0=down, 1=up, 2=right, 3=left
    private final String playerHatName ;

    public Player(UUID playerId, Input input, String username, SpriteManager spriteManager,
                  boolean isLocalPlayer, String color, float z, float x, float y) {
        this.playerId = playerId;
        this.username = username;
        this.spriteManager = spriteManager;
        this.isLocalPlayer = isLocalPlayer;
        this.color = color;
        this.input = input;
        this.position.z = z;
        this.position.x = x;
        this.position.y = y;

        // Create unique sprite names for this player
        String playerIdShort = playerId.toString().substring(0, 8);
        playerSpriteNames[0] = "player_" + playerIdShort + "_d";
        playerSpriteNames[1] = "player_" + playerIdShort + "_u";
        playerSpriteNames[2] = "player_" + playerIdShort + "_r";
        playerSpriteNames[3] = "player_" + playerIdShort + "_rr";

        playerHatName = "player_" + playerIdShort + "_us";
        // Only try to create sprites if we're on the main thread
        // This check isn't foolproof but can help avoid crashes
        if (Thread.currentThread().getName().contains("main")) {
            createPlayerSprites();
            setupCharacter();
        } else {
            System.err.println("WARNING: Player constructor called from non-main thread: " +
                    Thread.currentThread().getName());
            // Will need to create sprites later when on main thread
        }

        // Initialize character position
        if (character != null) {
            character.setPosition(position.x, position.y, position.z);
        }

        // If we're the local player, send an initial position update
        if (isLocalPlayer && client != null && client.isConnected()) {
            sendPositionUpdate();
        }
    }

    // Add a method to complete initialization on the main thread
    public void completeInitOnMainThread() {
        try {
            System.out.println("Completing initialization for player: " + playerId);

            // Create player-specific sprites with correct color
            if (playerSpriteNames[0] != null && !playerSpriteNames[0].isEmpty()) {
                createPlayerSprites();
            }

            // Set up character if not already done
            if (character == null) {
                setupCharacter();
            }

            // Ensure character position is set
            if (character != null) {
                character.setPosition(position.x, position.y, position.z);
                System.out.println("Player character setup complete: " + playerId);
            } else {
                System.err.println("Failed to create character for player: " + playerId);
            }
        } catch (Exception e) {
            System.err.println("Error completing player init: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void createPlayerSprites() {
        // Define player-specific sprites with their own color palette
        String[] palette = getColorPalette(color);

        // Copy sprite definitions from the original player sprites
        copySprite("player_sprite_d", playerSpriteNames[0], palette);
        copySprite("player_sprite_u", playerSpriteNames[1], palette);
        copySprite("player_sprite_r", playerSpriteNames[2], palette);
        copySprite("player_sprite_rr", playerSpriteNames[3], palette);
        copySprite("hat_d_1", playerHatName, palette);

    }

    private void copySprite(String sourceName, String targetName, String[] palette) {
        try {
            // Get original sprite
            Sprite sourceSprite = spriteManager.getSprite(sourceName);
            if (sourceSprite != null) {
                // Found the source sprite, now we can copy its properties
                int x = (int)(sourceSprite.getU0() * spriteManager.getSheet("entities").getAtlasWidth());
                int y = (int)(sourceSprite.getV0() * spriteManager.getSheet("entities").getAtlasHeight());

                if (debug) System.out.println("Creating sprite " + targetName + " at " + x + "," + y);

                spriteManager.defineSprite(
                        -1, // No numeric ID needed for the copy
                        targetName,
                        "entities", // Assuming all players are on the 'entities' sheet
                        x,
                        y,
                        16, // Standard 16x16 player sprites
                        16,
                        palette, // Use this player's palette
                        true // Mark as dynamic to avoid caching issues
                );
            } else {
                System.err.println("Source sprite not found: " + sourceName);
            }
        } catch (Exception e) {
            System.err.println("Failed to copy sprite " + sourceName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupCharacter() {
        character = new LayeredCharacter(spriteManager);

        // Add the body layer with the appropriate sprite
        character.addLayer("body", playerSpriteNames[0], 0, 0, 1);
        character.addLayer("hat", "hat_d_1", 0, -0, -2);


        character.setScale(2.0f, 2.0f);

        // Set initial position
        character.setPosition(position.x, position.y, position.z);

        if (debug) System.out.println("Player character set up with sprite: " + playerSpriteNames[0]);
    }

    private String[] getColorPalette(String color) {
        switch (color.toUpperCase()) {
            case "RED":
                return new String[]{"000", "500", "300", "555"};
            case "BLUE":
                return new String[]{"000", "005", "003", "555"};
            case "GREEN":
                return new String[]{"000", "050", "030", "555"};
            case "YELLOW":
                return new String[]{"000", "550", "530", "555"};
            case "PINK":
                return new String[]{"000", "505", "303", "555"};
            case "ORANGE":
                return new String[]{"000", "530", "320", "555"};
            case "PURPLE":
                return new String[]{"000", "305", "203", "555"};
            case "CYAN":
                return new String[]{"000", "055", "033", "555"};
            case "BLACK":
                return new String[]{"000", "111", "222", "555"};
            case "WHITE":
                return new String[]{"000", "444", "333", "555"};
            default:
                return new String[]{"000", "333", "222", "555"};
        }
    }

    @Override
    public void update(float deltaTime) {
        // Only control the local player
        if (isLocalPlayer && isAlive) {
            updateMovement(deltaTime);

            networkTimer += deltaTime;
            if ((isMoving || networkTimer >= 0.5f) && client != null && client.isConnected()) {
                // Send position updates both when moving and periodically when not moving
                sendPositionUpdate();
                networkTimer = 0;
            }
        }

        character.setMoving(isMoving);
        character.update(deltaTime);
    }

    private void updateMovement(float deltaTime) {
        boolean wasMoving = isMoving;
        Direction oldDirection = currentDirection;

        isMoving = false;

        if (input.isKeyDown(GLFW_KEY_W)) {
            move(Direction.UP, deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            move(Direction.RIGHT, deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            move(Direction.LEFT, deltaTime);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            move(Direction.DOWN, deltaTime);
        }

        // Force a position update if direction changed, even if not moving
        if (oldDirection != currentDirection && client != null && client.isConnected()) {
            sendPositionUpdate();
        }
    }

    public void move(Direction direction, float deltaTime) {
        if (!isAlive) return;

        currentDirection = direction;
        isMoving = true;

        if (direction == Direction.LEFT) {
            facingLeft = true;
        } else if (direction == Direction.RIGHT) {
            facingLeft = false;
        }

        // Use player-specific sprite for direction
        String directionSprite = getDirectionSprite(direction);
        character.setDirection(directionSprite);

        character.setFlipX(facingLeft);

        // Move the player
        float distance = moveSpeed * deltaTime;
        switch (direction) {
            case UP:
                position.y -= distance;
                break;
            case DOWN:
                position.y += distance;
                break;
            case LEFT:
                position.x -= distance;
                break;
            case RIGHT:
                position.x += distance;
                break;
        }

        // Update character position to match player position
        character.setPosition(position.x, position.y, position.z);
    }

    /**
     * Returns the player-specific sprite name for the given direction
     */
    private String getDirectionSprite(Direction direction) {
        switch (direction) {
            case UP: return playerSpriteNames[1];
            case DOWN: return playerSpriteNames[0];
            case RIGHT: return playerSpriteNames[2];
            case LEFT: return playerSpriteNames[3];
            default: return playerSpriteNames[0];
        }
    }

    /**
     * Update this player's position based on network data
     */
    public void receivePositionUpdate(float x, float y, int directionOrdinal, boolean moving) {
        if (debug) System.out.println("Received position update for " + playerId + ": " + x + ", " + y);

        // Update position
        position.x = x;
        position.y = y;

        // Update character position
        character.setPosition(x, y, position.z);

        // Update direction
        Direction newDirection = Direction.values()[directionOrdinal];
        if (currentDirection != newDirection) {
            currentDirection = newDirection;
            character.setDirection(getDirectionSprite(newDirection));
        }

        // Update movement state
        isMoving = moving;
        character.setMoving(moving);
    }

    /**
     * Send position update to server
     */
    private void sendPositionUpdate() {
        if (client != null && client.isConnected()) {
            Packets.PlayerPositionUpdate update = new Packets.PlayerPositionUpdate();
            update.playerId = playerId.toString();
            update.x = position.x;
            update.y = position.y;
            update.direction = currentDirection.ordinal();
            update.isMoving = isMoving;
            update.color = color;

            if (debug) System.out.println("Sending position update: " + position.x + ", " + position.y);

            client.sendUDP(update);
        }
    }

    @Override
    public void render(Matrix4f viewProjectionMatrix) {
        //if (debug) System.out.println("Rendering player " + username + " at " + position.x + ", " + position.y + " (visible: " + (isAlive || !isLocalPlayer) + ")");

        // Render character
        character.render(viewProjectionMatrix);

        // TODO: Add player name above character
    }

    @Override
    public void cleanup() {
        // Clean up resources if needed
    }

    // Static player management methods
    public static void addPlayer(Player player) {
        if (player != null && player.getPlayerId() != null) {
            players.put(player.getPlayerId(), player);
        }
    }


    public static void removePlayer(UUID playerId) {
        if (playerId != null) {
            players.remove(playerId);
        }
    }

    public static Player getPlayer(UUID playerId) {
        return playerId != null ? players.get(playerId) : null;
    }
    public static String getAllPlayers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Players(").append(players.size()).append("): [");
        for (UUID id : players.keySet()) {
            Player p = players.get(id);
            sb.append(id.toString(), 0, 8).append(":").append(p.getUsername()).append(", ");
        }
        if (!players.isEmpty()) {
            sb.setLength(sb.length() - 2); // Remove trailing comma
        }
        sb.append("]");
        return sb.toString();
    }

    // Network setup
    public static void setupClient(Client newClient) {
        client = newClient;

        // If client changed, send initial positions for all existing players
        for (Player player : players.values()) {
            if (player.isLocalPlayer) {
                player.sendPositionUpdate();
            }
        }
    }

    public static void setupServer(Server newServer) {
        server = newServer;
    }

    public void stopMoving() {
        isMoving = false;
    }

    public void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
        character.setPosition(x, y, z);

        // Send position update if this is the local player
        if (isLocalPlayer && client != null && client.isConnected()) {
            sendPositionUpdate();
        }
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    // Getters
    public UUID getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public String getColor() {
        return color;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Get the player's movement speed
     * @return Movement speed in units per second
     */
    public float getMoveSpeed() {
        return moveSpeed; // Assuming you have a moveSpeed field in the Player class
    }

    /**
     * Set the player's movement speed
     * @param moveSpeed Movement speed in units per second
     */
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    /**
     * Get the current movement direction
     * @return Current Direction enum value
     */
    public Direction getCurrentDirection() {
        return currentDirection; // Assuming you have a currentDirection field in the Player class
    }

    /**
     * Set the current movement direction
     * @param direction New Direction enum value
     */
    public void setCurrentDirection(Direction direction) {
        this.currentDirection = direction;
    }

    /**
     * Get the player's health
     * @return Current health value
     */
    public int getHealth() {
        return health; // Assuming you have a health field in the Player class
    }

    /**
     * Set the player's health
     * @param health New health value
     */
    public void setHealth(int health) {
        this.health = health;
    }
}