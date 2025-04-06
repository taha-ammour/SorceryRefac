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
    private boolean isCasting = false;
    private float castingTimer = 0f;
    private float castingDuration = 0.5f; // Half second cast time
    private String currentCastSpell = null;


    private boolean facingLeft = false;
    private boolean FlipY = false;

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
    private final String[] playerLegsSpriteNames = new String[4]; // 0=down, 1=up, 2=right, 3=left
    private final String playerHatName;

    private float animationTimer = 0f;
    private static final float FIXED_FRAME_DURATION = 0.2f; // 0.2 seconds per frame
    private float transitionTimer = 0f;
    private static final float TRANSITION_DURATION = 0.1f; // Continue animating for 0.2 seconds after stopping
    private int animationFrame = 0;
    private String currentLegSprite = "leg_idle_1";


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

        playerLegsSpriteNames[0] = "player_leg_" + playerIdShort + "_l";//down-up
        playerLegsSpriteNames[1] = "player_leg_" + playerIdShort + "_r";//down-up
        playerLegsSpriteNames[2] = "player_leg_" + playerIdShort + "_d";//left-right
        playerLegsSpriteNames[3] = "player_leg_" + playerIdShort + "_idle";//left-right

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

        copySprite("leg_l_1", playerLegsSpriteNames[0], palette);
        copySprite("leg_r_1", playerLegsSpriteNames[1], palette);
        copySprite("leg_d_1", playerLegsSpriteNames[2], palette);
        copySprite("leg_idle_1", playerLegsSpriteNames[3], palette);


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
        character.addLayer("hat", "hat_d_1", 0, 0, -2);
        character.addLayer("legs", playerLegsSpriteNames[0], 0, 0, -3);


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



    private void castSpell(String spellType) {
        if (client != null && client.isConnected()) {
            // Create a spell cast packet
            Packets.SpellCast spellCast = new Packets.SpellCast();
            spellCast.playerId = playerId.toString();
            spellCast.spellType = spellType;
            spellCast.x = position.x + (facingLeft ? -20 : 20); // Cast in front of player
            spellCast.y = position.y;
            spellCast.level = 1; // Default level

            // Send packet
            client.sendTCP(spellCast);

            // Optionally play a casting animation
            playCastAnimation(spellType);
        }
    }

    private void handleSpellCasting(float deltaTime) {
        // Only for local player
        if (!isLocalPlayer || !isAlive) return;

        // If currently casting, update timer
        if (isCasting) {
            castingTimer += deltaTime;
            if (castingTimer >= castingDuration) {
                // Finish casting
                completeCasting();
            }
            return; // Don't process new spell inputs while casting
        }

        // Check if spell keys are pressed
        if (input.isKeyJustPressed(GLFW_KEY_1)) {
            // Fire spell - key 1
            startCasting("fire");
        } else if (input.isKeyJustPressed(GLFW_KEY_2)) {
            // Ice spell - key 2
            startCasting("ice");
        } else if (input.isKeyJustPressed(GLFW_KEY_3)) {
            // Lightning spell - key 3
            startCasting("lightning");
        }
    }

    private void startCasting(String spellType) {
        if (isCasting) return; // Already casting

        isCasting = true;
        castingTimer = 0f;
        currentCastSpell = spellType;

        // Update character animation for casting
        playCastAnimation(spellType);

        System.out.println("Player " + username + " started casting " + spellType + " spell");
    }

    private void completeCasting() {
        if (!isCasting || currentCastSpell == null) return;

        System.out.println("Player " + username + " completing cast of " + currentCastSpell);

        // Send spell cast packet to server
        if (client != null && client.isConnected()) {
            // Create a spell cast packet
            Packets.SpellCast spellCast = new Packets.SpellCast();
            spellCast.playerId = playerId.toString();
            spellCast.spellType = currentCastSpell;

            // Calculate spell position based on player direction
            float offsetX = 32f; // Distance in front of player
            float offsetY = 0f;

            switch (currentDirection) {
                case UP:
                    offsetX = 0f;
                    offsetY = -offsetX;
                    break;
                case DOWN:
                    offsetX = 0f;
                    offsetY = offsetX;
                    break;
                case LEFT:
                    offsetX = -offsetX;
                    offsetY = 0f;
                    break;
                case RIGHT:
                    offsetX = offsetX;
                    offsetY = 0f;
                    break;
            }

            // Set final position
            spellCast.x = position.x + offsetX;
            spellCast.y = position.y + offsetY;
            spellCast.level = 1; // Default level, would need to be fetched from spell system

            System.out.println("Sending spell cast packet: " + currentCastSpell +
                    " at position " + spellCast.x + "," + spellCast.y);

            // Send packet
            client.sendTCP(spellCast);
        } else {
            System.err.println("Cannot send spell cast - client is null or disconnected");
        }

        // Reset casting state
        isCasting = false;
        currentCastSpell = null;

        // Reset character appearance
        resetCastAnimation();
    }

    private void resetCastAnimation() {
        // Reset any visual changes made during casting

        // Example:
        // character.setLayerColor("body", 0xFFFFFF, 1.0f); // Reset body color
    }


    private void playCastAnimation(String spellType) {
        // This method can be implemented to show a casting animation
        System.out.println("Playing cast animation for " + spellType + " spell");

        // Change appearance based on spell type
        switch(spellType.toLowerCase()) {
            case "fire":
                // Add visual effect for fire casting (e.g., red tint)
                break;
            case "ice":
                // Add visual effect for ice casting (e.g., blue tint)
                break;
            case "lightning":
                // Add visual effect for lightning casting (e.g., yellow glow)
                break;
        }
    }

    @Override
    public void update(float deltaTime) {
        // Only control the local player
        if (isLocalPlayer && isAlive) {
            updateMovement(deltaTime);
            handleSpellCasting(deltaTime);

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

        // Start with not moving
        isMoving = false;

        // Calculate movement vector based on all pressed keys
        float dx = 0;
        float dy = 0;

        // Check all movement keys
        if (input.isKeyDown(GLFW_KEY_W)) {
            dy -= 1; // Moving up
            currentDirection = Direction.UP;
            isMoving = true;
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            dy += 1; // Moving down
            currentDirection = Direction.DOWN;
            isMoving = true;
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            dx -= 1; // Moving left
            facingLeft = true;

            // Only change primary direction to LEFT if not moving vertically
            if (dy == 0) {
                currentDirection = Direction.LEFT;
            }
            isMoving = true;
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            dx += 1; // Moving right
            facingLeft = false;

            // Only change primary direction to RIGHT if not moving vertically
            if (dy == 0) {
                currentDirection = Direction.RIGHT;
            }
            isMoving = true;
        }

        // Normalize diagonal movement to prevent faster diagonal speed
        if (dx != 0 && dy != 0) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        // Apply movement if moving
        if (isMoving) {
            transitionTimer = TRANSITION_DURATION;
            animationTimer += deltaTime;
            if (animationTimer >= FIXED_FRAME_DURATION) {
                animationTimer = 0;
                animationFrame = (animationFrame + 1) % 2; // Toggle between 0 and 1

                if (dy != 0 && dx == 0) {
                    currentLegSprite = (animationFrame == 0) ? getDirectionLegSprite(Direction.UP) : getDirectionLegSprite(Direction.DOWN);// OR DOWN
                } else {
                    currentLegSprite = (animationFrame == 0) ? getDirectionLegSprite(Direction.LEFT) : getDirectionLegSprite(Direction.RIGHT);
                }

                // Update the leg layer in your character using the update method
                updateLegLayer(currentLegSprite);
            }

            float distance = moveSpeed * deltaTime;
            position.x += dx * distance;
            position.y += dy * distance;

            // Update character position
            character.setPosition(position.x, position.y, position.z);

            // Update sprite direction and flip
            character.setDirection(getDirectionSprite(currentDirection));
            character.setFlipX(facingLeft);
        }
        else {
            if (transitionTimer > 0) {
                transitionTimer -= deltaTime;
                animationTimer += deltaTime;
                if (animationTimer >= FIXED_FRAME_DURATION) {
                    animationTimer = 0;
                    animationFrame = (animationFrame + 1) % 2;
                    // Use your existing logic to choose leg sprite:
                    if (dy != 0 && dx == 0) {
                        currentLegSprite = (animationFrame == 0) ? getDirectionLegSprite(Direction.UP)
                                : getDirectionLegSprite(Direction.DOWN);
                    } else {
                        currentLegSprite = (animationFrame == 0) ? getDirectionLegSprite(Direction.LEFT)
                                : getDirectionLegSprite(Direction.RIGHT);
                    }
                    updateLegLayer(currentLegSprite);
                }
            }
            else {
                // Once the transition timer runs out, set the leg sprite to the idle state.
                if (facingLeft) {
                    currentLegSprite = playerLegsSpriteNames[2];
                } else {
                    currentLegSprite = getDirectionLegSprite(currentDirection);
                }
                updateLegLayer(currentLegSprite);
            }
        }
        character.setFlipX(facingLeft);

        // Send network update if direction changed or we're moving
        if ((oldDirection != currentDirection || isMoving) && client != null && client.isConnected()) {
            sendPositionUpdate();
        }
    }

    private void updateLegLayer(String spriteName) {
        if (character == null || spriteName == null) return;

        float legOffsetX = 0;
        float legOffsetY = 0;

        if ( spriteName.endsWith("_d") ) {
            legOffsetX = -1f;
        }


        boolean updated = character.updateLayerSprite("legs", spriteName, legOffsetX, legOffsetY);

        if (!updated) {
            character.removeLayer("legs");
            character.addLayer("legs", spriteName, legOffsetX, legOffsetY, -3);
        }
        character.setFlipX(facingLeft);
    }



    /**
     * Returns the player-specific sprite name for the given direction
     */
    private String getDirectionSprite(Direction direction) {
        return switch (direction) {
            case UP -> playerSpriteNames[1];
            case DOWN -> playerSpriteNames[0];
            case RIGHT -> playerSpriteNames[2];
            case LEFT -> playerSpriteNames[3];
            default -> playerSpriteNames[0];
        };
    }

    private String getDirectionLegSprite(Direction direction) {
        return switch (direction) {
            case UP -> playerLegsSpriteNames[1];
            case DOWN -> playerLegsSpriteNames[0];
            case RIGHT -> playerLegsSpriteNames[3];
            case LEFT -> playerLegsSpriteNames[2];
            default -> playerLegsSpriteNames[0];
        };
    }

    /**
     * Update this player's position based on network data
     */
    public void receivePositionUpdate(float x, float y, int directionOrdinal, boolean moving, boolean flipX, boolean flipY) {
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

        facingLeft = flipX;
        FlipY = flipY;
        character.setFlipX(facingLeft);

        // Update movement state
        isMoving = moving;
        character.setMoving(moving);
    }

    public void updateRemoteAnimation(int animationFrame, String legSprite) {
        if (!isLocalPlayer) {
            if (legSprite != null && !legSprite.isEmpty()) {
                this.animationFrame = animationFrame;

                try {
                    updateLegLayer(legSprite);
                } catch (Exception e) {
                    if (debug) {
                        System.err.println("Error updating remote player leg sprite: " + e.getMessage());
                        System.err.println("Attempted sprite: " + legSprite);
                    }
                }
            }
        }
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
            update.FlipX = facingLeft;
            update.FlipY = FlipY;

            update.animationFrame = animationFrame;
            update.currentLegSprite = currentLegSprite;

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