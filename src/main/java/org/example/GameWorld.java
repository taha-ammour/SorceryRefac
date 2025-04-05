package org.example;

import com.esotericsoftware.kryonet.Client;
import org.example.engine.*;
import org.example.game.Player;
import org.example.ui.UIText;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Main game world class that manages players, game state, networking, and scene management
 */
public class GameWorld {
    private Scene gameScene;
    private SpriteManager spriteManager;
    private Input input;
    private Camera camera;

    // Player management
    private Player localPlayer;
    private final Map<UUID, Player> remotePlayers = new ConcurrentHashMap<>();
    private final Map<String, UUID> usernameToId = new ConcurrentHashMap<>();

    // Networking
    private Client networkClient;
    private boolean isNetworkGame = false;
    private boolean debug = true; // Enable debug logging

    // UI components
    private UIText chatDisplay;
    private ConcurrentLinkedQueue<String> chatMessages = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> sceneModificationTasks = new ConcurrentLinkedQueue<>();

    private int maxChatMessages = 5;

    // Registered objects (for debugging)
    private List<String> registeredSpriteNames = new ArrayList<>();

    /**
     * Create a new game world
     * @param gameScene The scene to add game objects to
     * @param spriteManager The sprite manager for creating game entities
     * @param input The input handler
     * @param camera The game camera
     */
    public GameWorld(Scene gameScene, SpriteManager spriteManager, Input input, Camera camera) {
        this.gameScene = gameScene;
        this.spriteManager = spriteManager;
        this.input = input;
        this.camera = camera;

        // For debugging, register all defined sprites
        collectRegisteredSprites();
    }

    public void queueForMainThread(Runnable task) {
        mainThreadTasks.add(task);
    }

    /**
     * Collect all registered sprite names for debugging
     */
    private void collectRegisteredSprites() {
        // This is a rough approximation since we don't have direct access to all registered sprite names
        // In a real implementation, you'd have a way to enumerate all registered sprites
        for (int i = 1; i <= 100; i++) {
            try {
                Sprite sprite = spriteManager.getSprite(i);
                if (sprite != null) {
                    registeredSpriteNames.add("Sprite ID " + i);
                }
            } catch (Exception e) {
                // Stop when we hit an undefined sprite
                break;
            }
        }

        if (debug) {
            System.out.println("Found " + registeredSpriteNames.size() + " registered sprites");
        }
    }

    /**
     * Draw all registered sprites for debugging purposes
     * Places them in a grid pattern so they're all visible
     */
    public void drawAllRegisteredSprites() {
        if (registeredSpriteNames.isEmpty()) {
            System.out.println("No sprites to draw");
            return;
        }

        // Clear any existing debug sprites
        clearDebugSprites();

        // Create a grid to display all sprites
        int gridSize = 50; // Space between sprites
        int cols = 10;
        int row = 0;
        int col = 0;

        // Start at a position visible in the initial camera view
        int startX = 0;
        int startY = 0;

        if (debug) System.out.println("Drawing " + registeredSpriteNames.size() + " sprites");

        // Try to get sprites by ID first
        for (int i = 1; i <= 235; i++) {
            try {
                Sprite sprite = spriteManager.getSprite(i);
                if (sprite != null) {
                    // Position in grid
                    int x = startX + col * gridSize;
                    int y = startY + row * gridSize;

                    // Create a new instance to add to scene
                    Sprite debugSprite = spriteManager.getSprite(i);
                    debugSprite.setPosition(x, y);
                    debugSprite.setZ(500); // Draw on top
                    debugSprite.setScale(2.0f, 2.0f); // Make it larger

                    // Add to scene
                    gameScene.addGameObject(debugSprite);

                    // Move to next position
                    col++;
                    if (col >= cols) {
                        col = 0;
                        row++;
                    }
                }
            } catch (Exception e) {
                // Skip invalid sprites
            }
        }
    }

    /**
     * Clear debug sprites from the scene
     */
    private void clearDebugSprites() {
        // In a real implementation, you'd track debug sprites and remove them specifically
        // For now, we don't have a clean way to do this
    }

    /**
     * Creates the local player and adds them to the game
     * @param username Player's username
     * @param color Player's color (RED, BLUE, GREEN, etc.)
     * @return The created player object
     */
    public Player createLocalPlayer(String username, String color) {
        UUID playerId = UUID.randomUUID();
        if (debug) System.out.println("Creating local player with ID: " + playerId + ", username: " + username);

        // Start the player at a more visible position
        localPlayer = new Player(playerId, input, username, spriteManager, true, color, 0, 400, 300);

        // Set debug to match game world
        localPlayer.setDebug(debug);

        // Add to scene and tracking
        gameScene.addGameObject(localPlayer);
        Player.addPlayer(localPlayer);

        return localPlayer;
    }

    /**
     * Add a remote player (another player connected via network)
     * @param playerId The UUID of the remote player
     * @param username The username of the remote player
     * @param color The color of the remote player
     * @param x Initial X position
     * @param y Initial Y position
     */
    public void addRemotePlayer(UUID playerId, String username, String color, float x, float y) {
        if (debug) System.out.println("Adding remote player: " + playerId + " with username: " + username);

        if (playerId == null) {
            System.err.println("Cannot add player with null ID");
            return;
        }

        // Check if player already exists by UUID
        if (remotePlayers.containsKey(playerId)) {
            if (debug) System.out.println("Remote player already exists by UUID, updating position");
            Player player = remotePlayers.get(playerId);
            player.setPosition(x, y, player.getPosition().z);
            return;
        }

        // Also check if username is already mapped to a different ID
        if (usernameToId.containsKey(username)) {
            UUID existingId = usernameToId.get(username);
            if (!existingId.equals(playerId)) {
                if (debug) System.out.println("Warning: Username " + username +
                        " was mapped to different UUID: " + existingId);
            }
        }

        // Queue player creation for the main thread
        queueForMainThread(() -> {
            try {
                Player newPlayer = new Player(
                        playerId, input, username, spriteManager,
                        false, color, 0.1f, x, y
                );
                newPlayer.setDebug(debug);

                // Make sure to complete initialization on main thread
                newPlayer.completeInitOnMainThread();

                // Update all tracking maps
                remotePlayers.put(playerId, newPlayer);
                usernameToId.put(username, playerId);  // Track username to UUID mapping
                Player.addPlayer(newPlayer);

                // Add to scene
                gameScene.addGameObject(newPlayer);

                if (debug) System.out.println("Added player to maps - UUID: " +
                        playerId + ", username: " + username);
            } catch (Exception e) {
                System.err.println("Error creating player: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Remove a player from the game (when they disconnect)
     * @param playerId The UUID of the player to remove
     */
    public void removePlayer(UUID playerId) {
        if (debug) System.out.println("Removing player: " + playerId);

        Player player = remotePlayers.get(playerId);
        if (player != null) {
            // Queue removal for main thread to avoid OpenGL context issues
            queueForMainThread(() -> {
                try {
                    gameScene.removeGameObject(player);
                    remotePlayers.remove(playerId);
                    Player.removePlayer(playerId);

                    // Display a message that the player left
                    displayChatMessage("SYSTEM", player.getUsername() + " left the game");

                    if (debug) System.out.println("Player removed successfully: " + playerId);
                } catch (Exception e) {
                    System.err.println("Error removing player: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            if (debug) System.out.println("Could not find player to remove: " + playerId);
        }
    }

    /**
     * Update a remote player's position based on network data
     * @param playerId The player's UUID
     * @param x New X position
     * @param y New Y position
     * @param directionOrdinal Direction ordinal from the Direction enum
     * @param isMoving Whether the player is currently moving
     */
    public void updatePlayerPosition(UUID playerId, float x, float y, int directionOrdinal, boolean isMoving, String color) {
        if (debug) {
            System.out.println("Updating player position: " + playerId + " to (" + x + "," + y + ")");
        }

        // Don't process updates for the local player
        if (localPlayer != null && playerId.equals(localPlayer.getPlayerId())) {
            if (debug) System.out.println("Ignoring position update for local player");
            return;
        }

        Player player = remotePlayers.get(playerId);

        // Try to find player in static map if not in remotePlayers
        if (player == null) {
            player = Player.getPlayer(playerId);
            if (player != null && !remotePlayers.containsKey(playerId)) {
                if (debug) System.out.println("Player found in static map but not in remotePlayers. Fixing...");
                remotePlayers.put(playerId, player);
            }
        }

        if (player != null) {
            // Player exists, just update position
            player.receivePositionUpdate(x, y, directionOrdinal, isMoving);
        } else {
            // Create a new player if needed
            if (!remotePlayers.containsKey(playerId) && networkClient != null && networkClient.isConnected()) {
                System.out.println("Player not found, queueing recovery for: " + playerId);

                // Store the data for use in the task
                final float posX = x;
                final float posY = y;
                final int dir = directionOrdinal;
                final boolean moving = isMoving;
                final String playerColor = (color != null && !color.isEmpty()) ? color : "RED";


                queueForMainThread(() -> {
                    try {
                        // Check again in case the player was added between queue and execution
                        if (remotePlayers.containsKey(playerId)) {
                            remotePlayers.get(playerId).receivePositionUpdate(posX, posY, dir, moving);
                            return;
                        }

                        System.out.println("Creating player on main thread: " + playerId);

                        // Create a placeholder name that won't conflict
                        String placeholderName = "Player-" + playerId.toString().substring(0, 4);

                        Player recoveredPlayer = new Player(
                                playerId, input, placeholderName, spriteManager,
                                false, playerColor, 0.1f, posX, posY
                        );
                        recoveredPlayer.setDebug(debug);
                        recoveredPlayer.completeInitOnMainThread();

                        // Add to maps
                        remotePlayers.put(playerId, recoveredPlayer);
                        usernameToId.put(placeholderName, playerId);
                        Player.addPlayer(recoveredPlayer);

                        // Add to scene
                        gameScene.addGameObject(recoveredPlayer);

                        // Apply position
                        recoveredPlayer.receivePositionUpdate(posX, posY, dir, moving);

                        System.out.println("Player recovery completed on main thread: " + playerId);
                    } catch (Exception e) {
                        System.err.println("Error recovering player: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                if (debug) {
                    System.out.println("Could not find player to update position: " + playerId);
                }
            }
        }
    }

    /**
     * Set up network client for multiplayer
     * @param client The network client
     */
    public void setupNetworking(Client client) {
        if (debug) System.out.println("Setting up networking");

        this.networkClient = client;
        this.isNetworkGame = true;

        // Set up the client for the Player class to use
        Player.setupClient(client);

        // Set up network listeners
        GameNetworking.setupClientListeners(client, this);
    }

    /**
     * Add a chat message to the display
     * @param username Sender's username
     * @param message Message content
     */
    public void displayChatMessage(String username, String message) {
        String formattedMessage = username + ": " + message;
        chatMessages.add(formattedMessage);

        // Keep only the most recent messages
        while (chatMessages.size() > maxChatMessages) {
            chatMessages.poll();
        }

        // Update the chat display if it exists
        updateChatDisplay();

        // Also log to console for debugging
        if (debug) {
            System.out.println("CHAT: " + formattedMessage);
        }
    }

    /**
     * Set the UI component used for displaying chat messages
     * @param chatDisplay The UIText component for chat
     */
    public void setChatDisplay(UIText chatDisplay) {
        this.chatDisplay = chatDisplay;
        updateChatDisplay();
    }

    /**
     * Update the chat display with current messages
     */
    private void updateChatDisplay() {
        if (chatDisplay == null) return;

        // Updates to UI text should also happen on the main thread
        queueForMainThread(() -> {
            StringBuilder chatText = new StringBuilder();
            for (String message : chatMessages) {
                chatText.append(message).append("\n");
            }

            // Set the chat text
            chatDisplay.setText(chatText.toString());
        });
    }

    public void addGameObjectNextFrame(GameObject obj) {
        sceneModificationTasks.add(() -> gameScene.addGameObject(obj));
    }

    public void removeGameObjectNextFrame(GameObject obj) {
        sceneModificationTasks.add(() -> gameScene.removeGameObject(obj));
    }

    /**
     * Update the game world each frame
     * @param deltaTime Time since last frame
     */
    public void update(float deltaTime) {
        int processedCount = 0;
        Runnable task;
        while ((task = mainThreadTasks.poll()) != null) {
            try {
                if (debug) System.out.println("Processing queued task #" + (processedCount+1));
                task.run();
                processedCount++;
            } catch (Exception e) {
                System.err.println("Error in queued task: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (processedCount > 0 && debug) {
            System.out.println("Processed " + processedCount + " queued tasks");
        }
        while ((task = sceneModificationTasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("Error in scene modification task: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Update camera to follow local player if it exists
        if (localPlayer != null && camera != null) {
            Vector3f playerPos = localPlayer.getPosition();

            // Tell camera to follow player
            camera.follow(playerPos.x, playerPos.y);

            if (debug) {
                System.out.println("Following player at position: " + playerPos.x + ", " + playerPos.y);
            }
        }
    }

    public void setCameraFollowMode(boolean follow) {
        if (camera != null) {
            if (follow) {
                if (localPlayer != null) {
                    Vector3f playerPos = localPlayer.getPosition();
                    camera.follow(playerPos.x, playerPos.y);
                }
            } else {
                camera.stopFollowing();
            }
        }
    }

    /**
     * Set camera follow smoothness
     * @param smoothness Value between 0-1, higher = faster following
     */
    public void setCameraFollowSmoothness(float smoothness) {
        if (camera != null) {
            camera.setFollowSmoothness(smoothness);
        }
    }

    /**
     * Send a chat message to all players
     * @param message The message to send
     */
    public void sendChatMessage(String message) {
        if (networkClient != null && networkClient.isConnected() && localPlayer != null) {
            Packets.ChatMessage chatPacket = new Packets.ChatMessage();
            chatPacket.username = localPlayer.getUsername();
            chatPacket.message = message;
            networkClient.sendTCP(chatPacket);

            // Also display locally
            displayChatMessage(localPlayer.getUsername(), message);
        }
    }

    // Getters
    public Player getLocalPlayer() {
        return localPlayer;
    }

    public Map<UUID, Player> getRemotePlayers() {
        return remotePlayers;
    }

    public boolean isNetworkGame() {
        return isNetworkGame;
    }

    public Scene getGameScene() {
        return gameScene;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;

        // Update debug flag for all players
        if (localPlayer != null) {
            localPlayer.setDebug(debug);
        }

        for (Player player : remotePlayers.values()) {
            player.setDebug(debug);
        }
    }

    /**
     * Get all registered sprite names
     * @return List of sprite names
     */
    public List<String> getRegisteredSpriteNames() {
        return registeredSpriteNames;
    }
}