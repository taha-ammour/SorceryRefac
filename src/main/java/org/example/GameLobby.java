package org.example;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import org.example.engine.FontSheet;
import org.example.engine.Shader;
import org.example.ui.UIManager;
import org.example.ui.UIPanel;
import org.example.ui.UIText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Game lobby system for server discovery, hosting, and joining games
 */
public class GameLobby implements GameNetworking.GameNetworkListener {
    // Network components
    private Server server;
    private Client client;
    private boolean isHost = false;

    // Lobby state
    private String username;
    private String selectedColor;
    private String serverName;
    private List<GameNetworking.DiscoveredServer> availableServers = new ArrayList<>();
    private List<String> playerNames = new ArrayList<>();
    private UUID localPlayerId = UUID.randomUUID();
    private UUID hostId = null;

    // Lobby UI
    private UIManager uiManager;
    private UIPanel serverListPanel;
    private UIPanel playerListPanel;
    private UIPanel chatPanel;
    private UIText statusText;
    private FontSheet fontSheet;
    private Shader fontShader;

    // Callback for when the lobby is ready to start the game
    private Consumer<GameWorld> onGameStartCallback;
    private Consumer<String> onChatMessageCallback;

    /**
     * Create a new game lobby
     * @param uiManager UI manager for lobby interface
     * @param fontSheet Font sheet for text rendering
     * @param fontShader Shader for text rendering
     */
    public GameLobby(UIManager uiManager, FontSheet fontSheet, Shader fontShader) {
        this.uiManager = uiManager;
        this.fontSheet = fontSheet;
        this.fontShader = fontShader;

        // Register as a network listener
        GameNetworking.addListener(this);

        // Create lobby UI
        createLobbyUI();
    }

    /**
     * Create the lobby UI components
     */
    private void createLobbyUI() {
        // Main panel that holds everything
        UIPanel mainPanel = new UIPanel(0, 0, 800, 600);
        mainPanel.setLayout(UIPanel.Layout.VERTICAL, 10);

        // Status text at the top
        statusText = new UIText(fontSheet, fontShader, "Welcome to the Game Lobby", 10, 10);
        mainPanel.addComponent(statusText);

        // Server list panel
        serverListPanel = new UIPanel(10, 50, 780, 150);
        serverListPanel.setLayout(UIPanel.Layout.VERTICAL, 5);
        mainPanel.addComponent(serverListPanel);

        // Player list panel
        playerListPanel = new UIPanel(10, 220, 780, 150);
        playerListPanel.setLayout(UIPanel.Layout.VERTICAL, 5);
        UIText playerListTitle = new UIText(fontSheet, fontShader, "Players in Lobby:", 10, 10);
        playerListPanel.addComponent(playerListTitle);
        mainPanel.addComponent(playerListPanel);

        // Chat panel at the bottom
        chatPanel = new UIPanel(10, 400, 780, 190);
        chatPanel.setLayout(UIPanel.Layout.VERTICAL, 5);
        UIText chatTitle = new UIText(fontSheet, fontShader, "Chat:", 10, 10);
        chatPanel.addComponent(chatTitle);
        mainPanel.addComponent(chatPanel);

        // Add the main panel to the UI manager
        uiManager.addComponent(mainPanel, false);
    }

    /**
     * Start server discovery to find available games
     */
    public void startServerDiscovery() {
        // Clear current server list
        refreshServerList(new ArrayList<>());

        // Start listening for server broadcasts
        GameNetworking.startDiscoveryListener();

        // Update status
        updateStatus("Searching for servers...");
    }

    /**
     * Stop server discovery
     */
    public void stopServerDiscovery() {
        GameNetworking.stopDiscoveryListener();
        updateStatus("Server discovery stopped");
    }

    /**
     * Host a new game
     * @param serverName Name of the server
     * @param username Host's username
     * @param selectedColor Host's color preference
     * @return True if hosting was successful
     */
    public boolean hostGame(String serverName, String username, String selectedColor) {
        try {
            // Get available ports
            int tcpPort = PortUtil.getFreeTcpPort();
            int udpPort = PortUtil.getFreeUdpPort();

            this.username = username;
            this.selectedColor = selectedColor;
            this.serverName = serverName;

            // Create server
            updateStatus("Creating server on TCP port " + tcpPort + " and UDP port " + udpPort);
            server = GameNetworking.createServer(serverName, username, tcpPort, udpPort);

            // Connect to our own server
            updateStatus("Connecting to local server...");
            client = GameNetworking.createClient("localhost", tcpPort, udpPort);

            isHost = true;
            hostId = localPlayerId;

            // Add ourselves to the player list
            playerNames.add(username + " (Host)");
            refreshPlayerList();

            updateStatus("Server created! Waiting for players to join...");

            return true;
        } catch (IOException e) {
            updateStatus("Error hosting game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Join an existing game
     * @param server The server to join
     * @param username Player's username
     * @param selectedColor Player's color preference
     * @return True if joining was successful
     */
    public boolean joinGame(GameNetworking.DiscoveredServer server, String username, String selectedColor) {
        try {
            this.username = username;
            this.selectedColor = selectedColor;

            updateStatus("Connecting to server " + server.getName() + " at " + server.getIp() + "...");
            client = GameNetworking.createClient(server.getIp(), server.getTcpPort(), server.getUdpPort());

            // Setup client listeners
            if (client.isConnected()) {
                updateStatus("Connected to server! Waiting for game to start...");
                return true;
            } else {
                updateStatus("Failed to connect to server");
                return false;
            }
        } catch (IOException e) {
            updateStatus("Error joining game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Start the game (host only)
     * @param gameWorld The game world to initialize
     */
    public void startGame(GameWorld gameWorld) {
        if (!isHost) {
            updateStatus("Only the host can start the game");
            return;
        }

        // Setup the game world with the client
        gameWorld.setupNetworking(client);

        // Notify players that game is starting
        if (client != null && client.isConnected()) {
            Packets.GameStart startPacket = new Packets.GameStart();
            startPacket.gameMode = "normal";
            startPacket.mapId = 1;
            client.sendTCP(startPacket);
        }

        // Call the start game callback
        if (onGameStartCallback != null) {
            onGameStartCallback.accept(gameWorld);
        }
    }

    /**
     * Send a chat message to all players
     * @param message The message to send
     */
    public void sendChatMessage(String message) {
        if (client != null && client.isConnected()) {
            Packets.ChatMessage chatPacket = new Packets.ChatMessage();
            chatPacket.username = username;
            chatPacket.message = message;
            client.sendTCP(chatPacket);

            // Add message to chat panel
            addChatMessage(username, message);
        }
    }

    /**
     * Disconnect from the current game
     */
    public void disconnect() {
        // Clean up client
        if (client != null) {
            client.close();
            client = null;
        }

        // Clean up server if we're hosting
        if (server != null) {
            server.close();
            server = null;
        }

        isHost = false;
        playerNames.clear();
        refreshPlayerList();

        updateStatus("Disconnected from server");
    }

    /**
     * Set callback for when the game starts
     * @param callback Function to call with the game world when game starts
     */
    public void setOnGameStartCallback(Consumer<GameWorld> callback) {
        this.onGameStartCallback = callback;
    }

    /**
     * Set callback for chat message received
     * @param callback Function to call with the chat message
     */
    public void setOnChatMessageCallback(Consumer<String> callback) {
        this.onChatMessageCallback = callback;
    }

    /**
     * Update the status text
     * @param status New status message
     */
    private void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText(status);
        }
        System.out.println("Lobby Status: " + status);
    }

    /**
     * Refresh the server list UI
     * @param servers List of discovered servers
     */
    private void refreshServerList(List<GameNetworking.DiscoveredServer> servers) {
        this.availableServers = servers;

        // Clear existing server list
        serverListPanel.removeAllComponents();

        // Add title
        UIText title = new UIText(fontSheet, fontShader, "Available Servers:", 10, 10);
        serverListPanel.addComponent(title);

        // Add each server
        if (servers.isEmpty()) {
            UIText noServers = new UIText(fontSheet, fontShader, "No servers found.", 20, 30);
            serverListPanel.addComponent(noServers);
        } else {
            for (int i = 0; i < servers.size(); i++) {
                GameNetworking.DiscoveredServer server = servers.get(i);
                UIText serverText = new UIText(fontSheet, fontShader,
                        (i+1) + ". " + server.getName() + " - Host: " + server.getHostUsername() +
                                " - Players: " + server.getPlayerCount(), 20, 30 + i * 20);
                serverListPanel.addComponent(serverText);
            }
        }
    }

    /**
     * Refresh the player list UI
     */
    private void refreshPlayerList() {
        // Clear existing player list (except title)
        if (playerListPanel.getComponentCount() > 1) {
            for (int i = playerListPanel.getComponentCount() - 1; i > 0; i--) {
                playerListPanel.removeComponentAt(i);
            }
        }

        // Add each player
        for (int i = 0; i < playerNames.size(); i++) {
            UIText playerText = new UIText(fontSheet, fontShader, playerNames.get(i), 20, 30 + i * 20);
            playerListPanel.addComponent(playerText);
        }
    }

    /**
     * Add a chat message to the chat panel
     * @param username Sender's username
     * @param message Message content
     */
    private void addChatMessage(String username, String message) {
        // Add message to chat panel
        UIText chatText = new UIText(fontSheet, fontShader, username + ": " + message, 20, 30);
        chatPanel.addComponent(chatText);

        // Keep only the last 5 messages
        if (chatPanel.getComponentCount() > 6) { // +1 for title
            chatPanel.removeComponentAt(1);
        }

        // Call the chat message callback
        if (onChatMessageCallback != null) {
            onChatMessageCallback.accept(username + ": " + message);
        }
    }

    // GameNetworkListener implementation

    @Override
    public void onServerDiscovered(GameNetworking.DiscoveredServer server) {
        // Check if we already have this server
        boolean found = false;
        for (GameNetworking.DiscoveredServer existingServer : availableServers) {
            if (existingServer.getIp().equals(server.getIp()) && existingServer.getTcpPort() == server.getTcpPort()) {
                found = true;
                break;
            }
        }

        // Add server and refresh list
        if (!found) {
            List<GameNetworking.DiscoveredServer> updatedServers = new ArrayList<>(availableServers);
            updatedServers.add(server);
            refreshServerList(updatedServers);
        }
    }

    @Override
    public void onPlayerJoined(UUID playerId, String playerUsername, String color, float x, float y) {
        // Add player to list
        playerNames.add(playerUsername);
        refreshPlayerList();

        // Add system message to chat
        addChatMessage("SYSTEM", playerUsername + " joined the game");
    }

    @Override
    public void onPlayerLeft(UUID playerId) {
        // We don't know the username from just the ID, so we can't remove them specifically
        // In a real implementation, we would track player IDs and usernames

        // Just refresh the whole player list (we'd get an updated list from the server)
        playerNames.clear();
        refreshPlayerList();

        // Add system message to chat
        addChatMessage("SYSTEM", "A player left the game");
    }

    @Override
    public void onHostMigration(UUID newHostId) {
        this.hostId = newHostId;

        // Check if we're the new host
        if (newHostId != null && newHostId.equals(localPlayerId)) {
            isHost = true;
            addChatMessage("SYSTEM", "You are now the host!");
        }

        // Add system message to chat
        if (newHostId != null) {
            addChatMessage("SYSTEM", "Host changed");
        }
    }

    @Override
    public void onChatMessage(String username, String message) {
        addChatMessage(username, message);
    }

    /**
     * Get the discovered server by index
     * @param index Index in the server list
     * @return The discovered server at the given index, or null if invalid
     */
    public GameNetworking.DiscoveredServer getServerByIndex(int index) {
        if (index >= 0 && index < availableServers.size()) {
            return availableServers.get(index);
        }
        return null;
    }

    /**
     * Get the number of available servers
     * @return Number of discovered servers
     */
    public int getServerCount() {
        return availableServers.size();
    }

    /**
     * Check if we're currently the host
     * @return True if this client is the host
     */
    public boolean isHost() {
        return isHost;
    }

    /**
     * Get the local player's ID
     * @return UUID of the local player
     */
    public UUID getLocalPlayerId() {
        return localPlayerId;
    }

    /**
     * Get the active client (can be used to send custom packets)
     * @return The KryoNet client, or null if not connected
     */
    public Client getClient() {
        return client;
    }
}